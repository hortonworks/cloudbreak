package com.sequenceiq.statuschecker.configuration;

import java.util.List;
import java.util.Optional;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.statuschecker.model.JobInitializer;
import com.sequenceiq.statuschecker.service.JobService;

@Component
public class ScheduledJobInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger(ScheduledJobInitializer.class);

    @Inject
    private StatusCheckerProperties properties;

    @Inject
    private Optional<List<JobInitializer>> initJobDefinitions;

    @Inject
    private JobService jobService;

    @PostConstruct
    private void init() {
        if (properties.isAutoSyncEnabled() && initJobDefinitions.isPresent()) {
            jobService.deleteAll();
            for (JobInitializer jobDef : initJobDefinitions.get()) {
                jobDef.initJobs();
            }
        }
    }

}
