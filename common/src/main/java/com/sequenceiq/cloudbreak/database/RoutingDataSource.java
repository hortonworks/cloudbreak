package com.sequenceiq.cloudbreak.database;

import static com.sequenceiq.cloudbreak.quartz.configuration.SchedulerFactoryConfig.METERING_QUARTZ_EXECUTOR_THREAD_NAME_PREFIX;
import static com.sequenceiq.cloudbreak.quartz.configuration.SchedulerFactoryConfig.QUARTZ_EXECUTOR_THREAD_NAME_PREFIX;

import java.util.Map;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

public class RoutingDataSource extends AbstractRoutingDataSource {

    public static final String QUARTZ_THREAD_NAME_PREFIX = "quartzscheduler";

    private static final String DEFAULT_DATASOURCE_KEY = "default";

    private static final String QUARTZ_DATASOURCE_KEY = "quartz";

    @Inject
    @Qualifier("defaultDataSource")
    private DataSource defaultDataSource;

    @Inject
    @Qualifier("quartzDataSource")
    private DataSource quartzDataSource;

    @PostConstruct
    void setUp() {
        setTargetDataSources(Map.of(
                DEFAULT_DATASOURCE_KEY, defaultDataSource,
                QUARTZ_DATASOURCE_KEY, quartzDataSource
        ));
        setDefaultTargetDataSource(defaultDataSource);
    }

    @Override
    protected Object determineCurrentLookupKey() {
        String threadName = Thread.currentThread().getName().toLowerCase();
        boolean quartzThread = threadName.startsWith(QUARTZ_THREAD_NAME_PREFIX)
                || threadName.startsWith(QUARTZ_EXECUTOR_THREAD_NAME_PREFIX.toLowerCase())
                || threadName.startsWith(METERING_QUARTZ_EXECUTOR_THREAD_NAME_PREFIX.toLowerCase());
        return quartzThread ? QUARTZ_DATASOURCE_KEY : DEFAULT_DATASOURCE_KEY;
    }
}
