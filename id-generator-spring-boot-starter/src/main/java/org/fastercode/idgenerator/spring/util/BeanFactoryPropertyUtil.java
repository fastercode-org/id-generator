package org.fastercode.idgenerator.spring.util;

import org.fastercode.idgenerator.spring.exception.AutoConfigException;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.bind.PropertiesConfigurationFactory;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.env.*;
import org.springframework.validation.BindException;

import java.util.Iterator;
import java.util.Map;

/**
 * @author huyaolong
 */
public class BeanFactoryPropertyUtil {
    public static <T> T getProperty(Class<T> clazz, DefaultListableBeanFactory beanFactory, String prefix) throws IllegalAccessException, InstantiationException {
        T property = clazz.newInstance();
        PropertiesConfigurationFactory<Object> factory = new PropertiesConfigurationFactory<>(property);
        factory.setPropertySources(parsePropertySources(beanFactory));
        factory.setConversionService(new DefaultConversionService());
        factory.setIgnoreInvalidFields(false);
        factory.setIgnoreUnknownFields(true);
        factory.setIgnoreNestedProperties(false);
        factory.setTargetName(prefix);
        try {
            factory.bindPropertiesToTarget();
        } catch (BindException e) {
            throw new AutoConfigException(e);
        }

        return property;
    }

    /**
     * 解析配置文件
     *
     * @param beanFactory
     * @return
     */
    private static PropertySources parsePropertySources(DefaultListableBeanFactory beanFactory) {
        PropertySourcesPlaceholderConfigurer configurer = getSinglePropertySourcesPlaceholderConfigurer(beanFactory);
        if (configurer != null) {
            return new FlatPropertySources(configurer.getAppliedPropertySources());
        }

        Environment environment = new StandardEnvironment();
        MutablePropertySources propertySources = ((ConfigurableEnvironment) environment).getPropertySources();
        return new FlatPropertySources(propertySources);
    }

    private static PropertySourcesPlaceholderConfigurer getSinglePropertySourcesPlaceholderConfigurer(DefaultListableBeanFactory beanFactory) {
        Map<String, PropertySourcesPlaceholderConfigurer> beans = beanFactory.getBeansOfType(PropertySourcesPlaceholderConfigurer.class, false, false);
        if (beans.size() == 1) {
            return beans.values().iterator().next();
        }
        return null;
    }

    protected static class FlatPropertySources implements PropertySources {
        private PropertySources propertySources;

        public FlatPropertySources(PropertySources propertySources) {
            this.propertySources = propertySources;
        }

        @Override
        public boolean contains(String name) {
            return get(name) != null;
        }

        @Override
        public PropertySource<?> get(String name) {
            return getFlattened().get(name);
        }

        private MutablePropertySources getFlattened() {
            MutablePropertySources mutablePropertySources = new MutablePropertySources();
            for (PropertySource<?> propertySource : this.propertySources) {
                flattenPropertySources(propertySource, mutablePropertySources);
            }
            return mutablePropertySources;
        }

        private void flattenPropertySources(PropertySource<?> propertySource, MutablePropertySources mutablePropertySources) {
            Object source = propertySource.getSource();
            if (source instanceof ConfigurableEnvironment) {
                ConfigurableEnvironment environment = (ConfigurableEnvironment) source;
                for (PropertySource<?> childSource : environment.getPropertySources()) {
                    flattenPropertySources(childSource, mutablePropertySources);
                }
            } else {
                mutablePropertySources.addLast(propertySource);
            }
        }

        @Override
        public Iterator<PropertySource<?>> iterator() {
            MutablePropertySources mutablePropertySources = getFlattened();
            return mutablePropertySources.iterator();
        }
    }
}
