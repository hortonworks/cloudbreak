package com.sequenceiq.environment.environment.sync;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.statuschecker.model.JobInitializer;

@Component
public class EnvironmentJobInitializer implements JobInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentJobInitializer.class);

    private final EnvironmentJobService environmentJobService;

    private final EnvironmentService environmentService;

    public EnvironmentJobInitializer(EnvironmentJobService environmentJobService, EnvironmentService environmentService) {
        this.environmentJobService = environmentJobService;
        this.environmentService = environmentService;
    }

    @Override
    public void initJobs() {
        environmentJobService.deleteAll();
        List<Environment> environments = environmentService.findAllForAutoSync();
        for (Environment environment : environments) {
            environmentJobService.schedule(environment);
        }
        LOGGER.info("Auto syncer is inited with {} environments on start", environments.size());
    }
}
