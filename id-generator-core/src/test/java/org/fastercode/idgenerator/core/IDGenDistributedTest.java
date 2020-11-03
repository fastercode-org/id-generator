package org.fastercode.idgenerator.core;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.fastercode.idgenerator.core.generator.ID;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.util.HashMap;

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
    public void testGeneratorWorkerIDFromMap() {
        // test min
        for (int i = 0; i < 999; i++) {
            int finalI = i;
            IDGenDistributed idGen = new IDGenDistributed(new IDGenDistributedConfig() {{
                setMinWorkerID(finalI);
                setMaxWorkerID(999);
            }});
            int workerID = idGen.generatorWorkerIDFromMap(new HashMap() {{
                put("addr", "999");
            }});
            Assert.assertEquals(workerID, i);
        }
        // test min
        int min = Integer.parseInt(RandomStringUtils.randomNumeric(3));
        for (int i = min; i < 999; i++) {
            int finalI = i;
            IDGenDistributed idGen = new IDGenDistributed(new IDGenDistributedConfig() {{
                setMinWorkerID(min);
                setMaxWorkerID(finalI);
            }});
            int workerID = idGen.generatorWorkerIDFromMap(new HashMap() {{
                put("addr", "999");
            }});
            Assert.assertEquals(workerID, min);
        }
        // test gap
        IDGenDistributed idGen = new IDGenDistributed(new IDGenDistributedConfig() {{
            setMinWorkerID(11);
            setMaxWorkerID(15);
        }});
        int workerID = idGen.generatorWorkerIDFromMap(new HashMap() {{
            put("a", "11");
            put("b", "15");
            put("c", "16");
        }});
        Assert.assertEquals(workerID, 12);
    }

    @Test
    public void testDecodes() {
        for (int i = 0; i < 10; i++) {
            long workerID = Long.parseLong(RandomStringUtils.randomNumeric(3));
            IDGenDistributed idGen = new IDGenDistributed(null);
            idGen.setIdGeneratorRaw(new IDGeneratorLocal(() -> workerID));

            for (int j = 0; j < 10; j++) {
                // log.info("workerID={}", workerID);
                Assert.assertEquals(workerID, idGen.decodeWorkerIdFromId(idGen.generate().getLong64()));

                long extraData = Long.parseLong(RandomStringUtils.randomNumeric(3));
                extraData = extraData < 511 ? extraData : extraData / 2;
                // log.info("extraData={}", extraData);
                Assert.assertEquals(extraData, idGen.decodeExtraDataFromId(idGen.generate(extraData).getLong64()));

                ID id1 = idGen.generate();
                ID id2 = idGen.generate(extraData);
                // log.info("createDate1={}", id1.getCreateDate());
                // log.info("createDate2={}", id2.getCreateDate());
                Assert.assertEquals(id1.getCreateDate(), idGen.decodeCreateDateFromLong64(id1.getLong64()));
                Assert.assertEquals(id2.getCreateDate(), idGen.decodeCreateDateFromLong64(id2.getLong64()));

                Assert.assertEquals(id1.getCreateDate(), idGen.decodeCreateDateFromStr(id1.getStr()));
                Assert.assertEquals(id2.getCreateDate(), idGen.decodeCreateDateFromStr(id2.getStr()));

                Assert.assertEquals(id1.getCreateDate(), idGen.decodeCreateDateFromStr(id1.getStrWithExtraData()));
                Assert.assertEquals(id2.getCreateDate(), idGen.decodeCreateDateFromStr(id2.getStrWithExtraData()));
            }
        }
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

            log.warn("生成ID [{}] = [{}]", i, idGen.getIdGeneratorRaw().generate());
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
