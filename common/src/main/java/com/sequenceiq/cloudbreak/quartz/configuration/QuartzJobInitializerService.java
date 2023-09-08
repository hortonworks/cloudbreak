package com.sequenceiq.cloudbreak.quartz.configuration;

import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.quartz.model.JobInitializer;
import com.sequenceiq.cloudbreak.quartz.statuschecker.StatusCheckerConfig;

@Service
public class QuartzJobInitializerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(QuartzJobInitializerService.class);

    @Inject
    private Optional<List<JobInitializer>> initJobDefinitions;

    @Inject
    private StatusCheckerConfig properties;

    @Inject
    private List<TransactionalScheduler> transactionalSchedulers;

    @Retryable(value = Exception.class, maxAttempts = 5, backoff = @Backoff(delay = 5000))
    public void initQuartz() {
        if (properties.isAutoSyncEnabled() && initJobDefinitions.isPresent()) {
            try {
                LOGGER.info("AutoSync is enabled and there are job initializers, clearing the Quartz schedulers.");
                for (TransactionalScheduler transactionalScheduler : transactionalSchedulers) {
                    LOGGER.debug("Clearing scheduler: {}", transactionalScheduler.getClass().getSimpleName());
                    transactionalScheduler.clear();
                }
            } catch (TransactionService.TransactionExecutionException e) {
                LOGGER.error("Error during clearing quartz jobs", e);
                throw e.getCause();
            }
            for (JobInitializer jobDef : initJobDefinitions.get()) {
                LOGGER.debug("Initialize quartz jobs with initializer '{}'", jobDef.getClass());
                jobDef.initJobs();
            }
        }
    }
}
