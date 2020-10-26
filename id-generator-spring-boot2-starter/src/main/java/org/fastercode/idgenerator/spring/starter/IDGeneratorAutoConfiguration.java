package org.fastercode.idgenerator.spring.starter;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author huyaolong
 */
@Configuration
@ConditionalOnClass({IDGeneratorAutoCreator.class})
public class IDGeneratorAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean({IDGeneratorAutoCreator.class})
    public static IDGeneratorAutoCreator processor() {
        return new IDGeneratorAutoCreator();
    }
}
