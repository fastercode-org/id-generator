# id-generator

[![Build Status](https://travis-ci.org/fastercode-org/id-generator.svg?branch=master)](https://travis-ci.org/fastercode-org/id-generator)
[![Codecov](https://codecov.io/gh/fastercode-org/id-generator/branch/master/graph/badge.svg)](https://codecov.io/gh/fastercode-org/id-generator/branch/master)
[![License](https://img.shields.io/github/license/fastercode-org/id-generator)](https://github.com/fastercode-org/id-generator)
[![GitHub release](https://img.shields.io/github/v/release/fastercode-org/id-generator)](https://github.com/fastercode-org/id-generator/releases)
[![Maven Central](https://img.shields.io/maven-central/v/org.fastercode/id-generator)](https://search.maven.org/artifact/org.fastercode/id-generator)

## 基于zk注册workerID的 分布式id生成器

![](https://raw.githubusercontent.com/fastercode-org/id-generator/master/id-generator.jpg)

---

## 使用

### spring-boot:

**pom.xml**

```xml
<!-- spring-boot 1.x -->
<dependency>
  <groupId>org.fastercode</groupId>
  <artifactId>id-generator-spring-boot-starter</artifactId>
  <version>${id-generator.version}</version>
</dependency>

<!-- spring-boot 2.x -->
<dependency>
  <groupId>org.fastercode</groupId>
  <artifactId>id-generator-spring-boot2-starter</artifactId>
  <version>${id-generator.version}</version>
</dependency>

```

**application.yml**

```yml
id-generator:
  serverLists: 127.0.0.1:2181
  namespace: order_id_generator
  workersBackUpFile: /tmp/order_id_generator.json
  workersBackUpInterval: 60
  minWorkerID: 1
  maxWorkerID: 999
```

**java**

```java
@Autowired
private IDGenDistributed idGenDistributed;

public void demo(){
	ID id = idGenDistributed.generate();
}
```

### spring-mvc:

```xml
<dependency>
  <groupId>org.fastercode</groupId>
  <artifactId>id-generator-core</artifactId>
  <version>${id-generator.version}</version>
</dependency>


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
```
