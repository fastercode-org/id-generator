package org.fastercode.idgenerator.core;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.google.common.base.Strings;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.recipes.locks.InterProcessLock;
import org.fastercode.idgenerator.core.exception.IDGeneratorException;
import org.fastercode.idgenerator.core.exception.WorkerIDCanNotGenAnyMore;
import org.fastercode.idgenerator.core.generator.ID;
import org.fastercode.idgenerator.core.generator.IDGenerator;
import org.fastercode.idgenerator.core.reg.zookeeper.ZookeeperRegistryCenter;
import org.fastercode.idgenerator.core.util.FileUtil;
import org.fastercode.idgenerator.core.util.IPUtil;
import org.fastercode.idgenerator.core.util.MapUtil;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 分布式ID生成器
 * workerID 基于 zk 注册
 * 支持 zk 切换 (异步本地备份与切换更新)
 *
 * @author huyaolong
 */
@Slf4j
public class IDGenDistributed implements IDGenerator {

    private static final String zkLockPath = "/id-generator-lock";
    private static final String zkOnlinePath = "/id-generator-online";
    private static final String zkWorkersPath = "/id-generator-workers";
    private static final String zkRunning = "/running";

    @Getter(AccessLevel.PROTECTED)
    private final IDGenDistributedConfig config;

    @Getter(AccessLevel.PROTECTED)
    @Setter(AccessLevel.PROTECTED)
    private ZookeeperRegistryCenter zk;

    @Getter(AccessLevel.PROTECTED)
    @Setter(AccessLevel.PROTECTED)
    private String ip;

    @Getter(AccessLevel.PUBLIC)
    private int workerID;

    @Getter(AccessLevel.PROTECTED)
    @Setter(AccessLevel.PROTECTED)
    private IDGenerator idGeneratorRaw;

    private ScheduledExecutorService idWorkersBackUpScheduled;
    private ScheduledExecutorService runningPingScheduled;

    private final AtomicBoolean hasInit = new AtomicBoolean(false);

    public IDGenDistributed(IDGenDistributedConfig config) {
        this.config = config;
        this.zk = new ZookeeperRegistryCenter(config);

        if (config != null) {
            if (Strings.isNullOrEmpty(config.getTag())) {
                this.ip = IPUtil.getLocalAddress();
            } else {
                this.ip = IPUtil.getLocalAddress() + "@" + config.getTag();
            }
        }
    }

    @Override
    public void init() throws Exception {
        if (Strings.isNullOrEmpty(config.getServerLists()) || Strings.isNullOrEmpty(config.getNamespace())) {
            log.warn("分布式ID 初始化未完成. serverLists and namespace can not be empty.");
            return;
        }

        if (this.config.getMinWorkerID() >= this.config.getMaxWorkerID()) {
            throw new IDGeneratorException("minWorkerID can not greater than or equal maxWorkerID!");
        }

        if (!hasInit.compareAndSet(false, true)) {
            log.warn("分布式ID[{}] 已初始化, 不能重复初始化.", config.getName());
            return;
        }

        log.info("IDGenDistributedConfig:\n{}", JSON.toJSONString(this.config, SerializerFeature.PrettyFormat));
        // init zk
        zk.init();

        // distributed-lock
        InterProcessLock lock = zk.lock(zkLockPath);
        if (lock == null || !lock.acquire(30L, TimeUnit.SECONDS)) {
            throw new IDGeneratorException("分布式ID[" + config.getName() + "] 初始化 分布式加锁失败!");
        }

        try {
            // try to restore from backup-file
            if (!zk.isExisted(zkWorkersPath)) {
                String backupContent = FileUtil.readFileContent(config.getWorkersBackUpFile());
                if (!illegalBackupContent(backupContent)) {
                    zk.persist(zkWorkersPath, backupContent);
                }
            }

            // reg/get workerID && construct idGenerator
            HashMap<Object, Object> map = JSON.parseObject(zk.getDirectly(zkWorkersPath), HashMap.class);
            if (map == null) {
                map = new HashMap<>();
            }

            // generator a worker id
            try {
                workerID = generatorWorkerIDFromMap(map);
            } catch (WorkerIDCanNotGenAnyMore we) {
                // clean offline worker ids
                this.cleanOfflineWorkerIDs(map);
                log.warn("分布式ID[{}] gen-worker-id > {}, 回收zk中离线的workers 已完成.", config.getName(), config.getMaxWorkerID());
                workerID = generatorWorkerIDFromMap(map);
            }

            // check workerID
            checkWorkerID(workerID);

            // set workerID
            map.put(ip, workerID);
            int finalWorkerID = workerID;
            this.idGeneratorRaw = new IDGeneratorLocal(() -> finalWorkerID);

            // check duplicate
            duplicateCheck(map);

            // sync to zk
            zk.persist(zkWorkersPath, JSON.toJSONString(map));

        } finally {
            // release distributed-lock
            zk.unlock(lock);
        }

        // start running ping scheduled
        runningPingScheduled = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder()
                .setNameFormat("id-generator-running-ping-" + this.config.getName() + "-%d")
                .setDaemon(true).build()
        );
        runningPingScheduled.scheduleAtFixedRate(this::doRunningPingScheduled, 0, 1, TimeUnit.SECONDS);

        // start backup scheduled
        if (this.config.getWorkersBackUpInterval() > 0) {
            idWorkersBackUpScheduled = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder()
                    .setNameFormat("id-generator-back-" + this.config.getName() + "-%d")
                    .setDaemon(true).build()
            );
            idWorkersBackUpScheduled.scheduleAtFixedRate(
                    this::doIdWorkersBackUpScheduled,
                    1, this.config.getWorkersBackUpInterval(),
                    TimeUnit.SECONDS
            );
        }

        log.info("分布式ID生成器[{}] init success.", this.config.getName());
    }

    private void doRunningPingScheduled() {
        try {
            String node = zkOnlinePath + "/" + this.ip + zkRunning;
            if (!this.zk.isExisted(node)) {
                this.zk.persistEphemeral(node, String.valueOf(System.currentTimeMillis()));
                log.info("分布式ID[{}] running-ping success. [{}]", config.getName(), node);
            }
        } catch (Exception e) {
            log.warn("分布式ID[{}] running-ping fail: {}", config.getName(), e.getMessage(), e);
        }
    }

    private void doIdWorkersBackUpScheduled() {
        if (Strings.isNullOrEmpty(config.getWorkersBackUpFile())) {
            return;
        }

        try {
            String str = zk.getDirectly(zkWorkersPath);
            if (illegalBackupContent(str)) {
                return;
            }

            FileUtil.writeFileContent(config.getWorkersBackUpFile(), str);
            if (log.isDebugEnabled()) {
                log.debug("分布式ID[{}] 备份 WorkerID 成功 [{}]", config.getName(), config.getWorkersBackUpFile());
            }
        } catch (Exception e) {
            log.warn("分布式ID[{}] 备份 WorkerID 异常: {}", config.getName(), e.getMessage(), e);
        }
    }

    @Override
    public void close() {
        gracefulShutdownExecutor(runningPingScheduled, 1000L);
        gracefulShutdownExecutor(idWorkersBackUpScheduled, 2000L);
        this.zk.close();
        this.idGeneratorRaw = null;
        this.runningPingScheduled = null;
        this.idWorkersBackUpScheduled = null;
        this.hasInit.set(false);

        log.info("分布式ID生成器[{}] close success.", config.getName());
    }

    @Override
    public long decodeWorkerIdFromId(long id) {
        return this.getIdGeneratorRaw().decodeWorkerIdFromId(id);
    }

    @Override
    public long decodeExtraDataFromId(long id) {
        return this.getIdGeneratorRaw().decodeExtraDataFromId(id);
    }

    @Override
    public Date decodeCreateDateFromLong64(long id) {
        return this.getIdGeneratorRaw().decodeCreateDateFromLong64(id);
    }

    @Override
    public Date decodeCreateDateFromStr(String str) {
        return this.getIdGeneratorRaw().decodeCreateDateFromStr(str);
    }

    @Override
    public String decodeCreateDateStringFromStr(String str) {
        return this.getIdGeneratorRaw().decodeCreateDateStringFromStr(str);
    }

    /**
     * 生成一个分布式 ID
     */
    @Override
    public ID generate() {
        return this.idGeneratorRaw.generate();
    }

    /**
     * 生成一个分布式 ID, 且包含 extraData
     */
    @Override
    public ID generate(long extraData) {
        return this.idGeneratorRaw.generate(extraData);
    }

    /**
     * 清理map 回收zk中离线的workers
     */
    private void cleanOfflineWorkerIDs(HashMap<Object, Object> workersMap) throws Exception {
        if (workersMap == null) {
            return;
        }
        List<String> all = this.zk.getChildrenKeys(zkOnlinePath);

        // 清理没有znode的ip
        workersMap.entrySet().removeIf(entry -> !all.contains(String.valueOf(entry.getKey())));

        // 清理有znode但不在线的ip
        for (String ip : all) {
            String running = this.zk.getDirectly(zkOnlinePath + "/" + ip + zkRunning);
            if (running == null) {
                workersMap.remove(ip);
            }
        }
    }

    /**
     * 获取在线的 workerIDs map
     */
    @Override
    public HashMap<Object, Object> getOnlineWorkerIDs() {
        HashMap<Object, Object> map = JSON.parseObject(this.zk.getDirectly(zkWorkersPath), HashMap.class);
        try {
            this.cleanOfflineWorkerIDs(map);
        } catch (Exception ignored) {
        }
        return map;
    }

    private void checkWorkerID(int workerID) {
        if (workerID <= 0 || workerID >= 1024) {
            throw new IDGeneratorException("workerID error.");
        }
    }

    private void duplicateCheck(HashMap map) {
        for (Object v : map.values()) {
            List<String> keys = MapUtil.getKeys(map, v);
            if (keys.size() > 1) {
                throw new IDGeneratorException("分布式ID[" + config.getName() + "] 初始化失败, workerID不唯一: " + JSON.toJSONString(keys));
            }
        }
    }

    /**
     * 从map中，根据ip和这个map, 生成 workerID
     * <p>
     * map为空或null时, return min;
     * or
     * return map.get(ip);
     * or
     * return a gap val from min to max;
     */
    protected int generatorWorkerIDFromMap(HashMap map) {
        int gap = this.config.getMinWorkerID();
        if (map == null || map.size() == 0) {
            return gap;
        }

        if (map.containsKey(this.ip)) {
            return Integer.parseInt(map.get(this.ip).toString());
        }

        Set<Integer> vals = new HashSet<>();
        map.values().forEach(v -> {
            try {
                vals.add(Integer.parseInt(v.toString()));
            } catch (Exception ignored) {
            }
        });

        while (vals.contains(gap)) {
            gap++;
            if (gap > config.getMaxWorkerID()) {
                throw new WorkerIDCanNotGenAnyMore("workerID was already above [" + config.getMaxWorkerID() + "], please clean offline zk info.");
            }
        }

        return gap;
    }

    private static void gracefulShutdownExecutor(ExecutorService executor, long timeout) {
        try {
            executor.shutdown();
            if (!executor.awaitTermination(timeout, TimeUnit.MILLISECONDS)) {
                executor.shutdownNow();
            }
        } catch (Exception e) {
            try {
                executor.shutdownNow();
            } catch (Exception e1) {
                // ignore
            }
        }
    }

    protected static boolean illegalBackupContent(String str) {
        return Strings.isNullOrEmpty(str) || !str.startsWith("{") || !str.endsWith("}");
    }

}
