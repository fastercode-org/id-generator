package org.fastercode.idgenerator.core;

import org.fastercode.idgenerator.core.generator.ID;
import org.fastercode.idgenerator.core.generator.IDGenerator;
import org.junit.Assert;
import org.junit.Test;

public class IDGeneratorLocalTest {
    @Test
    public void test() {
        long extraData = 255;
        IDGenerator idGenerator = new IDGeneratorLocal();
        ID id = idGenerator.generate(extraData);
        Assert.assertEquals(id.getCreateDate(), idGenerator.decodeCreateDateFromLong64(id.getLong64()));
        Assert.assertEquals(id.getCreateDate(), idGenerator.decodeCreateDateFromStr(id.getStr()));
        Assert.assertEquals(id.getCreateDate(), idGenerator.decodeCreateDateFromStr(id.getStrWithExtraData()));
        Assert.assertEquals(extraData, idGenerator.decodeExtraDataFromId(id.getLong64()));
    }

}
