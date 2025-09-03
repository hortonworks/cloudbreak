package com.sequenceiq.cloudbreak.app;

import java.util.Collection;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;

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

    public static <T> T getBean(Class<T> requiredType) throws BeansException {
        return context.getBean(requiredType);
    }

    public static String getProperty(String key, String defaultValue) {
        if (context != null && context.getEnvironment() != null) {
            return context.getEnvironment().getProperty(key, defaultValue);
        } else {
            return defaultValue;
        }
    }

    public static <T> Collection<T> getAllMatchingBeans(Class<T> requiredType) {
        return context.getBeansOfType(requiredType).values();
    }

}
