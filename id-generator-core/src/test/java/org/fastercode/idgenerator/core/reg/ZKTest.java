package org.fastercode.idgenerator.core.reg;

import com.alibaba.fastjson.JSON;
import com.google.common.base.Strings;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.recipes.locks.InterProcessLock;
import org.fastercode.idgenerator.core.reg.zookeeper.ZookeeperConfiguration;
import org.fastercode.idgenerator.core.reg.zookeeper.ZookeeperRegistryCenter;
import org.fastercode.idgenerator.core.util.IPUtil;

import java.util.HashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
public class ZKTest {
    // @Test
    @SneakyThrows
    public void testTmp() {
        ZookeeperConfiguration configuration = new ZookeeperConfiguration();
        configuration.setServerLists("localhost:2181");
        configuration.setNamespace("zk-test");

        ZookeeperRegistryCenter zk = new ZookeeperRegistryCenter(configuration);
        zk.init();

        zk.persistEphemeral("/" + IPUtil.getLocalAddress() + "/running", String.valueOf(System.currentTimeMillis()));

        zk.close();
    }

    // @Test
    @SneakyThrows
    public void testZK() {
        ZookeeperConfiguration configuration = new ZookeeperConfiguration();
        configuration.setServerLists("localhost:2181");
        configuration.setNamespace("zk-test");

        ZookeeperRegistryCenter zk = new ZookeeperRegistryCenter(configuration);
        zk.init();
        String path = "/id-generator-workers";

        zk.remove(path);
        System.out.println("init = " + (zk.getDirectly(path) == null));

        int threadNum = 10;
        CountDownLatch cdPre = new CountDownLatch(threadNum);
        CountDownLatch cdDown = new CountDownLatch(threadNum);
        CountDownLatch start = new CountDownLatch(1);

        ExecutorService executorService = Executors.newCachedThreadPool();
        for (int i = 0; i < threadNum; i++) {
            int finalI = i;
            executorService.submit(() -> {
                try {
                    cdPre.countDown();
                    start.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                //lock
                InterProcessLock lock = zk.lock(path + "/lock");
                try {
                    if (lock == null || !lock.acquire(100L, TimeUnit.SECONDS)) {
                        log.warn("分布式ID 初始化 分布式加锁失败!");
                    }
                } catch (Exception e) {
                    log.warn("分布式ID 初始化 分布式加锁失败!");
                }

                String val = zk.getDirectly(path);
                HashMap map = new HashMap();
                if (!Strings.isNullOrEmpty(val)) {
                    map = JSON.parseObject(val, HashMap.class);
                }
                System.out.println("Write: " + JSON.toJSONString(map));

                map.put(finalI + " k", finalI + " v");
                zk.persist(path, JSON.toJSONString(map));

                // unlock
                try {
                    Thread.sleep(2000L);
                } catch (Exception ignored) {
                }
                zk.unlock(lock);

                cdDown.countDown();
            });
        }

        //wait prepare && start
        cdPre.await();
        start.countDown();

        //wait all down
        cdDown.await();

        System.out.println(zk.getDirectly(path));
        zk.close();
    }
}
