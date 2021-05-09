# id-generator

[![Build Status](https://travis-ci.org/fastercode-org/id-generator.svg?branch=master)](https://travis-ci.org/fastercode-org/id-generator)
[![Codecov](https://codecov.io/gh/fastercode-org/id-generator/branch/master/graph/badge.svg)](https://codecov.io/gh/fastercode-org/id-generator/branch/master)
[![License](https://img.shields.io/github/license/fastercode-org/id-generator)](https://github.com/fastercode-org/id-generator/blob/master/LICENSE.txt)
[![JDK8+](https://img.shields.io/badge/JDK-8+-green.svg)](https://www.oracle.com/technetwork/java/javase/downloads/index.html)
[![GitHub release](https://img.shields.io/github/v/release/fastercode-org/id-generator)](https://github.com/fastercode-org/id-generator/releases)
[![Maven Central](https://img.shields.io/maven-central/v/org.fastercode/id-generator)](https://mvnrepository.com/search?q=id-generator&d=org.fastercode)

## 分布式id生成器 (workerID基于zk初始化)

![](https://cdn.yaolong.net/img/2021/05/id-generator.jpg)

- **高性能** 生成ID过程不依赖任何三方组件。
- **可用性** 分布式构建 / zk可中断 / 实例启动安全监测。
- **隔离性** workerID池可按环境/项目等配置区间。
- **无状态** 生成过程无状态，zk中的 `实例IP=>workerID` 映射关系支持备份与迁移。
- **易部署** 单独部署/混合部署/docker快速构建。
- **可伸缩** 支持实例无感快速扩容缩容，启动时进行安全监测。
- **分库分表** 生成的ID趋势单调递增，ID中可运用移位运算快速解出`生成时间/扩展数据`。

---

- **sequence**   [多线程安全] 单位时间内(秒) 的自增序列值.
- **workerID**   [分布式特性] 不同实例在ID中的唯一标识, 基于zk初始化获取或注册.
- **extraData**  [分库分表支持] 附加在ID中的额外数据, 如附加用户ID取模的余数, 可用于定位按取模划分的库表.
- **createDate** [分库分表支持] ID的生成时间, 与`long64` `str` `strWithExtraData` 中包含的时间一致, 可用于定位按时间划分的库表.

- **时钟回拨优化** 如果时钟回拨大于500ms, 生成ID时将抛出异常; 否则线程将挂起等待若干毫秒以确保生成正确的ID.
---

## 使用: docker 快速构建

```shell script
# 克隆项目
git clone https://github.com/fastercode-org/id-generator.git

# 构建docker镜像
sudo docker build --no-cache -t id-generator-app:v1 -f ./id-generator/docker/Dockerfile ./id-generator/docker

# 运行
export zkServerLists=Zookeeper地址
sudo docker run -p 8080:8080 -v /tmp:/tmp id-generator-app:v1 \
     java -Xms32m -Xmx32m -jar id-generator-app.jar --server.port=8080 \
     --id-generator.serverLists=${zkServerLists} \
     --id-generator.namespace=id_generator \
     --id-generator.workersBackUpFile=/tmp/workers.bak \
     --id-generator.workersBackUpInterval=60

# 测试
curl 'http://127.0.0.1:8080/fastercode/id-generator/generate'
```

##### 更多参数配置:

- [IDGenDistributedConfig](https://github.com/fastercode-org/id-generator/blob/master/id-generator-core/src/main/java/org/fastercode/idgenerator/core/IDGenDistributedConfig.java)
- [ZookeeperConfiguration](https://github.com/fastercode-org/id-generator/blob/master/id-generator-core/src/main/java/org/fastercode/idgenerator/core/reg/zookeeper/ZookeeperConfiguration.java)

## 使用: spring 编码形式

- 添加Maven仓库

```xml
<!-- pom.xml -->
<repositories>
  <repository>
    <id>sonatype-nexus-staging</id>
    <name>Sonatype Nexus Staging</name>
    <url>https://oss.sonatype.org/service/local/staging/deploy/maven2/</url>
    <releases>
        <enabled>true</enabled>
    </releases>
    <snapshots>
        <enabled>true</enabled>
    </snapshots>
  </repository>
</repositories>
```

### spring-boot (1.x/2.x) :

- 添加依赖

```xml
<!-- pom.xml -->
<dependency>
  <groupId>org.fastercode</groupId>
  <artifactId>id-generator-spring-boot-starter</artifactId>
  <version>${id-generator.version}</version>
</dependency>
```

**application.yml**

```yml
id-generator:
  # zk集群
  serverLists: 127.0.0.1:2181
  # zk命名空间
  namespace: order_id_generator
  # 本地备份文件路径
  workersBackUpFile: /tmp/order_id_generator.json
  # 备份间隔时间(秒)
  workersBackUpInterval: 60
  # workerID池自定义最小值
  minWorkerID: 1
  # workerID池自定义最大值
  maxWorkerID: 999
```

### spring-mvc:

- 添加依赖

```xml
<!-- pom.xml -->
<dependency>
  <groupId>org.fastercode</groupId>
  <artifactId>id-generator-core</artifactId>
  <version>${id-generator.version}</version>
</dependency>
```

- 注册bean

```xml
<!-- src/main/webapp/WEB-INF/spring/applicationContext.xml -->
<beans xmlns="http://www.springframework.org/schema/beans" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans-2.5.xsd">
  <bean id="id-generator-conf" class="org.fastercode.idgenerator.core.IDGenDistributedConfig">
      <property name="serverLists"            value="127.0.0.1:2181"/>
      <property name="namespace"              value="order_id_generator"/>
      <property name="workersBackUpFile"      value="/tmp/order_id_generator.json"/>
      <property name="workersBackUpInterval"  value="60"/>
      <property name="minWorkerID"            value="1"/>
      <property name="maxWorkerID"            value="999"/>
  </bean>
  <bean class="org.fastercode.idgenerator.core.IDGenDistributed" init-method="init" destroy-method="close">
      <constructor-arg ref="id-generator-conf"/>
  </bean>
</beans>
```

### Code

```java
@Autowired
private IDGenerator idGenerator;

public void Demo(){
  // 生成一个分布式ID实体
  ID id = idGenerator.generate();

  // 从long64订单号中反解 创建时间
  Date createDate = idGenerator.decodeCreateDateFromLong64(id.getLong64());

  // 从long64订单号中反解 实例ID
  long workerID   = idGenerator.decodeWorkerIdFromId(id.getLong64());

  // 从long64订单号中反解 附加数据
  long extraData  = idGenerator.decodeExtraDataFromId(id.getLong64());

  // 从str订单号中反解 创建时间
  Date createDate = idGenerator.decodeCreateDateFromStr(id.getStr());
}
```
