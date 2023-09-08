package com.sequenceiq.cloudbreak.quartz.configuration;

import java.util.Map;
import java.util.Properties;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.quartz.QuartzProperties;
import org.springframework.boot.autoconfigure.quartz.SchedulerFactoryBeanCustomizer;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.scheduling.quartz.SpringBeanJobFactory;

public class SchedulerFactoryBeanUtil {

    private SchedulerFactoryBeanUtil() {
    }

    /**
     * Creates a SchedulerFactoryBean instance based on the default quartzScheduler implementation (QuartzAutoConfiguration quartzScheduler method)
     */
    public static SchedulerFactoryBean createSchedulerFactoryBean(QuartzProperties properties, ObjectProvider<SchedulerFactoryBeanCustomizer> customizers,
            ApplicationContext applicationContext) {
        SchedulerFactoryBean schedulerFactoryBean = new SchedulerFactoryBean();
        SpringBeanJobFactory jobFactory = new SpringBeanJobFactory();
        jobFactory.setApplicationContext(applicationContext);
        schedulerFactoryBean.setJobFactory(jobFactory);
        if (properties.getSchedulerName() != null) {
            schedulerFactoryBean.setSchedulerName(properties.getSchedulerName());
        }
        schedulerFactoryBean.setAutoStartup(properties.isAutoStartup());
        schedulerFactoryBean.setStartupDelay((int) properties.getStartupDelay().getSeconds());
        schedulerFactoryBean.setWaitForJobsToCompleteOnShutdown(properties.isWaitForJobsToCompleteOnShutdown());
        schedulerFactoryBean.setOverwriteExistingJobs(properties.isOverwriteExistingJobs());
        if (!properties.getProperties().isEmpty()) {
            schedulerFactoryBean.setQuartzProperties(asProperties(properties.getProperties()));
        }
        customizers.orderedStream().forEach((customizer) -> customizer.customize(schedulerFactoryBean));
        return schedulerFactoryBean;
    }

    private static Properties asProperties(Map<String, String> source) {
        Properties properties = new Properties();
        properties.putAll(source);
        return properties;
    }
}
