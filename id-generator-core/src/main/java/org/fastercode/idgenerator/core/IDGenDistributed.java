package org.fastercode.idgenerator.core;

import com.alibaba.fastjson.JSON;
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

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
public class IDGenDistributed {

    private static final String zkLockPath = "/id-generator-lock";
    private static final String zkOnlinePath = "/id-generator-online";
    private static final String zkWorkersPath = "/id-generator-workers";
    private static final String zkRunning = "/running";

    @Getter(AccessLevel.PROTECTED)
    private final IDGenDistributedConfig config;

    @Getter(AccessLevel.PROTECTED)
    private final ZookeeperRegistryCenter zk;

    @Getter(AccessLevel.PROTECTED)
    @Setter(AccessLevel.PROTECTED)
    private String ip;

    @Getter(AccessLevel.PUBLIC)
    private int workerID;

    @Getter(AccessLevel.PUBLIC)
    private IDGenerator idGenerator;

    private ScheduledExecutorService idWorkersBackUpScheduled;

    private final AtomicBoolean hasInit = new AtomicBoolean(false);

    public IDGenDistributed(IDGenDistributedConfig config) {
        this.config = config;
        this.zk = new ZookeeperRegistryCenter(config);

        if (Strings.isNullOrEmpty(config.getTag())) {
            this.ip = IPUtil.getLocalAddress();
        } else {
            this.ip = IPUtil.getLocalAddress() + "@" + config.getTag();
        }
    }

    public void init() throws Exception {
        if (!hasInit.compareAndSet(false, true)) {
            log.warn("分布式ID[{}] 已初始化, 不能重复初始化.", config.getName());
            return;
        }

        if (Strings.isNullOrEmpty(this.config.getName())) {
            throw new IDGeneratorException("分布式ID[" + config.getName() + "] 已初始化失败! name 不能为空.");
        }

        if (this.config.getMinWorkerID() >= this.config.getMaxWorkerID()) {
            throw new IDGeneratorException("minWorkerID can not greater than or equal maxWorkerID!");
        }

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
                if (!Strings.isNullOrEmpty(backupContent)) {
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
            this.idGenerator = new IDGenerator(() -> finalWorkerID);

            // check duplicate
            duplicateCheck(map);

            // sync to zk
            zk.persist(zkWorkersPath, JSON.toJSONString(map));
            zk.persistEphemeral(zkOnlinePath + "/" + this.ip + zkRunning, String.valueOf(System.currentTimeMillis()));

        } finally {
            // release distributed-lock
            zk.unlock(lock);
        }

        // start backup scheduled
        if (this.config.getWorkersBackUpInterval() > 0) {
            idWorkersBackUpScheduled = Executors.newSingleThreadScheduledExecutor(
                    new ThreadFactoryBuilder()
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

    private void doIdWorkersBackUpScheduled() {
        if (Strings.isNullOrEmpty(config.getWorkersBackUpFile())) {
            return;
        }

        try {
            String str = zk.getDirectly(zkWorkersPath);
            FileUtil.writeFileContent(config.getWorkersBackUpFile(), str);
            if (log.isDebugEnabled()) {
                log.debug("分布式ID[{}] 备份 WorkerID 成功 [{}]", config.getName(), config.getWorkersBackUpFile());
            }
        } catch (Exception e) {
            log.warn("分布式ID[{}] 备份 WorkerID 异常: {}", config.getName(), e.getMessage(), e);
        }
    }

    public void close() {
        try {
            idWorkersBackUpScheduled.shutdown();
            if (!idWorkersBackUpScheduled.awaitTermination(2000L, TimeUnit.MILLISECONDS)) {
                idWorkersBackUpScheduled.shutdownNow();
            }
        } catch (Exception e) {
            try {
                idWorkersBackUpScheduled.shutdownNow();
            } catch (Exception e1) {
                // ignore
            }
        }
        this.zk.close();
        this.idGenerator = null;
        this.idWorkersBackUpScheduled = null;
        this.hasInit.set(false);

        log.info("分布式ID生成器[{}] close success.", config.getName());
    }

    /**
     * 生成一个分布式 ID
     */
    public ID generate() {
        return this.idGenerator.generate();
    }

    /**
     * 生成一个分布式 ID, 且包含 extraData
     */
    public ID generate(long extraData) {
        return this.idGenerator.generate(extraData);
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
    private int generatorWorkerIDFromMap(HashMap map) {
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

}
