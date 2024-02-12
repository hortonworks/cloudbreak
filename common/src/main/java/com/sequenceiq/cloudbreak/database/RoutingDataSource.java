package com.sequenceiq.cloudbreak.database;

import java.util.Locale;
import java.util.Map;

import javax.sql.DataSource;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import com.sequenceiq.cloudbreak.quartz.configuration.scheduler.QuartzThreadUtil;

public class RoutingDataSource extends AbstractRoutingDataSource {

    public static final String QUARTZ_SCHEDULER_THREAD_NAME_PREFIX = "quartzscheduler";

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
        String threadName = Thread.currentThread().getName();
        boolean quartzThread = threadName.toLowerCase(Locale.ROOT).startsWith(QUARTZ_SCHEDULER_THREAD_NAME_PREFIX) || QuartzThreadUtil.isCurrentQuartzThread();
        return quartzThread ? QUARTZ_DATASOURCE_KEY : DEFAULT_DATASOURCE_KEY;
    }
}
