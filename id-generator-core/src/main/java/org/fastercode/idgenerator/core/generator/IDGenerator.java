package org.fastercode.idgenerator.core.generator;

import java.util.Date;
import java.util.HashMap;

/**
 * 分布式ID生成器
 * http://id-generator.fastercode.org
 *
 * @author huyaolong
 */
public interface IDGenerator {
    /**
     * 生成并返回一个分布式 ID
     */
    ID generate();

    /**
     * 生成并返回一个分布式 ID, 且包含 extraData
     *
     * @param extraData 附加数据
     * @return ID
     */
    ID generate(long extraData);

    /**
     * 获取实例ID
     */
    int getWorkerID();

    /**
     * 获取在线的 workerID 映射
     */
    HashMap<Object, Object> getOnlineWorkerIDs();

    /**
     * 初始化分布式id生成器
     */
    void init() throws Exception;

    /**
     * 关闭分布式id生成器
     */
    void close();

    /**
     * 从id反解出 workerID
     */
    long decodeWorkerIdFromId(final long id);

    /**
     * 从id反解出 extraData
     */
    long decodeExtraDataFromId(final long id);

    /**
     * 从id反解出 创建时间
     */
    Date decodeCreateDateFromLong64(final long id);

    /**
     * 从str反解出 创建时间
     */
    Date decodeCreateDateFromStr(final String str);

    /**
     * 从str反解出 创建时间字符串
     */
    String decodeCreateDateStringFromStr(final String str);
}
