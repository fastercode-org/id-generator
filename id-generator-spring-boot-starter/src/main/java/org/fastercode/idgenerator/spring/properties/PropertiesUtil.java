package org.fastercode.idgenerator.spring.properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.Properties;

import static org.fastercode.idgenerator.spring.properties.PropertyFileConst.*;

/**
 * @author huyaolong
 */
public class PropertiesUtil {
    private static final Logger logger = LoggerFactory.getLogger(PropertiesUtil.class);
    private static Properties props;

    synchronized static private void loadProps() {
        props = new Properties();
        InputStream in = null;
        try {
            Resource resources = findOneClassPathResources(CONFIG_FILE);
            if (resources == null) {
                throw new FileNotFoundException();
            }
            in = new BufferedInputStream(resources.getInputStream());
            props.load(in);
        } catch (FileNotFoundException e) {
            logger.info("spring 配置文件: [{}] 不存在, 忽略", CONFIG_FILE);
        } catch (IOException e) {
            logger.warn(CONFIG_FILE_READ_IOEXCEPTION);
        } finally {
            try {
                if (null != in) {
                    in.close();
                }
            } catch (IOException e) {
                logger.warn(CONFIG_FILE_CLOSE_IOEXCEPTION);
            }
        }
    }

    public static String getProperty(String key) {
        if (null == props) {
            loadProps();
        }
        return props.getProperty(key);
    }

    public static String getProperty(String key, String defaultValue) {
        if (null == props) {
            loadProps();
        }
        return props.getProperty(key, defaultValue);
    }

    /**
     * {@link PathMatchingResourcePatternResolver#findAllClassPathResources(String)}
     */
    public static Resource findOneClassPathResources(String location) throws IOException {
        String path = location;
        if (path.startsWith("/")) {
            path = path.substring(1);
        }

        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        Enumeration<URL> resourceUrls = (cl != null ? cl.getResources(path) : ClassLoader.getSystemResources(path));
        while (resourceUrls.hasMoreElements()) {
            URL url = resourceUrls.nextElement();
            if (url != null) {
                return new UrlResource(url);
            }
        }
        return null;
    }
}
