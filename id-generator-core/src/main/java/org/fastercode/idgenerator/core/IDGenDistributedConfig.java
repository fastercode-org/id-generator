package org.fastercode.idgenerator.core;

import lombok.Getter;
import lombok.Setter;
import org.fastercode.idgenerator.core.reg.zookeeper.ZookeeperConfiguration;

/**
 * @author huyaolong
 */
@Getter
@Setter
public class IDGenDistributedConfig extends ZookeeperConfiguration {

    private String name = "idGenDistributed";

    /**
     * zk中worker实例 ip后的名称; 一般用在同ip同zk空间下的多个idGen实例
     */
    private String tag;

    /**
     * workers 备份的文件路径, 当迁移新的zk时, 会自动将备份恢复到新的zk中; null或空则不备份
     */
    private String workersBackUpFile;

    /**
     * workers 多少秒备份一次, <=0 则不备份
     */
    private int workersBackUpInterval = 0;

    /**
     * workerID 的最小值, 不能小于1
     */
    private int minWorkerID = 1;

    /**
     * workerID 的最大值, 不能大于1024
     */
    private int maxWorkerID = 999;
}
