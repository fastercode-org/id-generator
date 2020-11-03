# id-generator

[![Build Status](https://travis-ci.org/fastercode-org/id-generator.svg?branch=master)](https://travis-ci.org/fastercode-org/id-generator)
[![Codecov](https://codecov.io/gh/fastercode-org/id-generator/branch/master/graph/badge.svg)](https://codecov.io/gh/fastercode-org/id-generator/branch/master)
[![License](https://img.shields.io/github/license/fastercode-org/id-generator)](https://github.com/fastercode-org/id-generator/blob/master/LICENSE.txt)
[![JDK8+](https://img.shields.io/badge/JDK-8+-green.svg)](https://www.oracle.com/technetwork/java/javase/downloads/index.html)
[![GitHub release](https://img.shields.io/github/v/release/fastercode-org/id-generator)](https://github.com/fastercode-org/id-generator/releases)
[![Maven Central](https://img.shields.io/maven-central/v/org.fastercode/id-generator)](https://mvnrepository.com/search?q=id-generator&d=org.fastercode)

## 分布式id生成器 (workerID基于zk初始化)

![](https://raw.githubusercontent.com/fastercode-org/id-generator/master/id-generator.jpg)

- **workerID**   是不同实例在ID中的唯一标识, 基于zk初始化获取或注册.
- **extraData**  是附加在ID中的额外数据, 如附加用户ID取模的余数, 可用于定位按取模划分的库表.
- **createDate** ID的生成时间, 与`long64` `str` `strWithExtraData` 中包含的时间一致, 可用于定位按时间划分的库表.

---

## 使用

### spring-boot (1.x/2.x) :

**pom.xml**

```xml
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

```xml
<!-- pom.xml -->
<dependency>
  <groupId>org.fastercode</groupId>
  <artifactId>id-generator-core</artifactId>
  <version>${id-generator.version}</version>
</dependency>

<!-- 注册bean -->
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
private IDGenDistributed idGenDistributed;

public void Demo(){
  // 生成一个分布式ID实体
  ID id = idGenDistributed.generate();

  // 从long64订单号中反解 创建时间
  Date createDate = IDGenerator.decodeCreateDateFromLong64(id.getLong64());

  // 从long64订单号中反解 实例ID
  long workerID   = IDGenerator.decodeWorkerIdFromId(id.getLong64());

  // 从long64订单号中反解 附加数据
  long extraData  = IDGenerator.decodeExtraDataFromId(id.getLong64());

  // 从str订单号中反解 创建时间
  Date createDate = IDGenerator.decodeCreateDateFromStr(id.getStr());
}
```


