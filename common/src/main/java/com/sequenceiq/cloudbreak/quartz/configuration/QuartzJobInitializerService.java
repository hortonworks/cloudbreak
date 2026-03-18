package com.sequenceiq.cloudbreak.quartz.configuration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.quartz.configuration.scheduler.TransactionalScheduler;
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
            Map<String, Exception> initJobExceptions = new HashMap<>();
            for (JobInitializer jobDef : initJobDefinitions.get()) {
                LOGGER.debug("Initialize quartz jobs with initializer '{}'", jobDef.getClass());
                try {
                    jobDef.initJobs();
                } catch (Exception e) {
                    LOGGER.error("Error during quartz job init for job class: {}", jobDef.getClass(), e);
                    initJobExceptions.put(jobDef.getClass().getName(), e);
                }
            }
            if (!initJobExceptions.isEmpty()) {
                String exceptions = initJobExceptions.entrySet().stream()
                        .map(exceptionEntry -> exceptionEntry.getKey() + ": " + exceptionEntry.getValue().getMessage())
                        .collect(Collectors.joining(","));
                LOGGER.error("The following initJobs failed: [{}]", exceptions);
                CloudbreakServiceException initQuartzException = new CloudbreakServiceException("Failed to init all the quartz jobs: [" + exceptions + "]");
                initJobExceptions.values().forEach(initQuartzException::addSuppressed);
                throw initQuartzException;
            }
        }
    }
}
