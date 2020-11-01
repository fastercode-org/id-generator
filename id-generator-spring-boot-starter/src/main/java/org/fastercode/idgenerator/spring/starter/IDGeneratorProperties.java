package org.fastercode.idgenerator.spring.starter;

import org.fastercode.idgenerator.core.IDGenDistributedConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "id-generator")
public class IDGeneratorProperties extends IDGenDistributedConfig {
}
