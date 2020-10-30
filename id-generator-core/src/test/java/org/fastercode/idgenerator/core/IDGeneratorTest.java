package org.fastercode.idgenerator.core;

import org.fastercode.idgenerator.core.generator.ID;
import org.fastercode.idgenerator.core.generator.IDGenerator;
import org.junit.Assert;
import org.junit.Test;

public class IDGeneratorTest {
    @Test
    public void test() {
        long extraData = 255;
        IDGenerator idGenerator = new IDGenerator();
        ID id = idGenerator.generate(extraData);
        Assert.assertEquals(id.getCreateDate(), IDGenerator.decodeCreateDateFromLong64(id.getLong64()));
        Assert.assertEquals(id.getCreateDate(), IDGenerator.decodeCreateDateFromStr(id.getStr()));
        Assert.assertEquals(id.getCreateDate(), IDGenerator.decodeCreateDateFromStr(id.getStrWithExtraData()));
        Assert.assertEquals(extraData, IDGenerator.decodeExtraDataFromId(id.getLong64()));
    }

}
