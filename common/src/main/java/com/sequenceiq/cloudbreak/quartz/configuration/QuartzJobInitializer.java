package com.sequenceiq.cloudbreak.quartz.configuration;

import java.util.List;
import java.util.Optional;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.quartz.model.JobInitializer;
import com.sequenceiq.cloudbreak.quartz.statuschecker.StatusCheckerConfig;

@Component
public class QuartzJobInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger(QuartzJobInitializer.class);

    @Inject
    private StatusCheckerConfig properties;

    @Inject
    private Optional<List<JobInitializer>> initJobDefinitions;

    @Inject
    private Scheduler scheduler;

    @PostConstruct
    private void init() {
        if (properties.isAutoSyncEnabled() && initJobDefinitions.isPresent()) {
            try {
                LOGGER.info("AutoSync is enabled and there are job initializers, clearing the Quartz scheduler.");
                scheduler.clear();
            } catch (SchedulerException e) {
                LOGGER.error("Error during clearing quartz jobs", e);
            }
            for (JobInitializer jobDef : initJobDefinitions.get()) {
                LOGGER.debug("Initialize quartz jobs with initializer '{}'", jobDef.getClass());
                jobDef.initJobs();
            }
        }
    }

}
