package org.fastercode.idgenerator.core.generator;

import java.util.Date;
import java.util.HashMap;

public interface IDGenerator {
    ID generate();

    ID generate(long extraData);

    int getWorkerID();

    HashMap<Object, Object> getOnlineWorkerIDs();

    void init() throws Exception;

    void close();

    long decodeWorkerIdFromId(final long id);

    long decodeExtraDataFromId(final long id);

    Date decodeCreateDateFromLong64(final long id);

    Date decodeCreateDateFromStr(final String str);

    String decodeCreateDateStringFromStr(final String str);
}
