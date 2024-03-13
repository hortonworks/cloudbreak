package com.sequenceiq.cloudbreak.database;

import static com.sequenceiq.cloudbreak.database.DatabaseConfig.DATA_SOURCE_POSTFIX;
import static com.sequenceiq.cloudbreak.database.DatabaseConfig.DEFAULT_DATA_SOURCE;
import static com.sequenceiq.cloudbreak.database.DatabaseConfig.DEFAULT_DATA_SOURCE_PREFIX;
import static com.sequenceiq.cloudbreak.quartz.configuration.scheduler.SchedulerFactoryConfig.QUARTZ_PREFIX;

import java.util.Map;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import com.sequenceiq.cloudbreak.quartz.configuration.scheduler.QuartzThreadUtil;

public class RoutingDataSource extends AbstractRoutingDataSource {

    private static final Logger LOGGER = LoggerFactory.getLogger(RoutingDataSource.class);

    @Inject
    private Map<String, DataSource> dataSources;

    @PostConstruct
    void setUp() {
        setTargetDataSources(dataSources.entrySet().stream()
                .collect(Collectors.toMap(entry -> StringUtils.substringBefore(entry.getKey(), DATA_SOURCE_POSTFIX), Map.Entry::getValue)));
        setDefaultTargetDataSource(dataSources.get(DEFAULT_DATA_SOURCE));
    }

    @Override
    protected Object determineCurrentLookupKey() {
        if (QuartzThreadUtil.isCurrentQuartzThread()) {
            return determineQuartzLookupKey();
        } else {
            return DEFAULT_DATA_SOURCE_PREFIX;
        }
    }

    private String determineQuartzLookupKey() {
        String threadName = Thread.currentThread().getName();
        if (threadName.startsWith("QuartzScheduler_")) {
            return StringUtils.substringBetween(threadName, "QuartzScheduler_", "Scheduler-");
        } else if (threadName.endsWith("Scheduler_QuartzSchedulerThread")) {
            return StringUtils.substringBefore(threadName, "Scheduler_QuartzSchedulerThread");
        } else if (threadName.contains("Executor-")) {
            return StringUtils.substringBefore(threadName, "Executor-");
        } else {
            LOGGER.warn("Unknown quartz thread name format: {}, use default quartz lookup key: {}", threadName, QUARTZ_PREFIX);
            return QUARTZ_PREFIX;
        }
    }

}
