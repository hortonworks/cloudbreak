package com.sequenceiq.cloudbreak.app;

import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;

/**
 * This grants you access to Spring ApplicationContext from non-Spring beans. Use it only from non-Spring beans,
 * since in a regular Spring bean you can just use the @Inject annotation.
 */
public class StaticApplicationContext {

    private static ApplicationContext context;

    private StaticApplicationContext() {
    }

    static void setApplicationContext(ApplicationContext applicationContext) {
        context = applicationContext;
    }

    public static ApplicationContext getApplicationContext() {
        return context;
    }

    public static <T> T getEnvironmentProperty(String key, Class<T> targetType, T defaultValue) {
        T ret = defaultValue;
        if (getApplicationContext() != null) {
            Environment environment = getApplicationContext().getEnvironment();
            if (environment != null) {
                ret = environment.getProperty(key, targetType, defaultValue);
            }
        }
        return ret;
    }
}
