package org.fastercode.idgenerator.spring.starter;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.google.common.base.Strings;
import lombok.extern.slf4j.Slf4j;
import org.fastercode.idgenerator.core.IDGenDistributed;
import org.fastercode.idgenerator.core.IDGenDistributedConfig;
import org.fastercode.idgenerator.spring.util.BeanFactoryPropertyUtil;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

/**
 * @author huyaolong
 */
@Slf4j
public class IDGeneratorAutoCreator implements BeanFactoryPostProcessor {

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        DefaultListableBeanFactory defaultListableBeanFactory = (DefaultListableBeanFactory) beanFactory;

        IDGeneratorProperties config = null;
        try {
            config = BeanFactoryPropertyUtil.getProperty(IDGeneratorProperties.class, defaultListableBeanFactory, "id-generator");
        } catch (Exception e) {
            throw new RuntimeException("IDGeneratorProperties error.");
        }

        //// multi
        // if (config == null || config.getInstances() == null || config.getInstances().size() == 0) {
        //     log.warn("IDGeneratorProperties can not exist.");
        //     return;
        // }

        // log.info("IDGeneratorProperties: {}", JSON.toJSONString(config.getInstances(), SerializerFeature.PrettyFormat));

        // for (IDGenDistributedConfig conf : config.getInstances()) {
        //     registerBean(defaultListableBeanFactory, conf);
        // }
        //// multi-end

        // single
        if (config == null || Strings.isNullOrEmpty(config.getServerLists()) || Strings.isNullOrEmpty(config.getNamespace())) {
            log.warn("IDGeneratorProperties can not exist.");
            return;
        }
        log.info("IDGeneratorProperties: {}", JSON.toJSONString(config, SerializerFeature.PrettyFormat));
        registerBean(defaultListableBeanFactory, config);
        // single-end
    }

    private void registerBean(DefaultListableBeanFactory defaultListableBeanFactory, IDGenDistributedConfig config) {
        BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(IDGenDistributed.class);
        builder.setScope(BeanDefinition.SCOPE_SINGLETON);
        builder.addConstructorArgValue(config);
        builder.setInitMethodName("init");
        builder.setDestroyMethodName("close");
        builder.setLazyInit(false);
        defaultListableBeanFactory.registerBeanDefinition(config.getName(), builder.getBeanDefinition());
    }

}
