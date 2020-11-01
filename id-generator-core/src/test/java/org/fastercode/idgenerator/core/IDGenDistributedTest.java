package org.fastercode.idgenerator.core;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.Ignore;
import org.junit.Test;

@Slf4j
public class IDGenDistributedTest {

    @Test
    @SneakyThrows
    public void test() {
        IDGenDistributedConfig idConfig = new IDGenDistributedConfig();
        idConfig.setServerLists("serverList");
        idConfig.setNamespace("namespace");
        idConfig.setWorkersBackUpFile("/tmp/x");
        idConfig.setWorkersBackUpInterval(1);
        idConfig.setMinWorkerID(301);
        idConfig.setMaxWorkerID(303);
        idConfig.setConnectionTimeoutMilliseconds(1);
        idConfig.setSessionTimeoutMilliseconds(1);
        idConfig.setMaxRetries(0);

        IDGenDistributed idGen = new IDGenDistributed(idConfig);
        try {
            idGen.init();
        } catch (Exception ignore) {
        }
        idGen.close();
    }

    @Test
    @Ignore
    @SneakyThrows
    public void testWithZK() {
        IDGenDistributedConfig idConfig = new IDGenDistributedConfig();
        idConfig.setServerLists("localhost:2181");
        idConfig.setNamespace("id-gen-test");
        idConfig.setWorkersBackUpFile("/tmp/x");
        idConfig.setWorkersBackUpInterval(1);
        idConfig.setMinWorkerID(301);
        idConfig.setMaxWorkerID(303);

        IDGenDistributed idGen = new IDGenDistributed(idConfig);

        for (int i = 1; i <= 10; i++) {
            idGen.setIp(String.valueOf(i));
            try {
                idGen.init();
            } catch (Exception e) {
                log.error("id-gen 初始化失败: {}", e.getMessage(), e);
            }

            log.warn("生成ID [{}] = [{}]", i, idGen.getIdGenerator().generate());
            log.warn("在线的workers: \n{}", JSON.toJSONString(idGen.getOnlineWorkerIDs(), SerializerFeature.PrettyFormat));
            Thread.sleep(3000L);
            idGen.close();
        }

        IDGenDistributed idGen2 = new IDGenDistributed(idConfig);
        idGen2.init();
        log.warn("在线的workers: \n{}", JSON.toJSONString(idGen2.getOnlineWorkerIDs(), SerializerFeature.PrettyFormat));
        idGen2.close();

    }
}
