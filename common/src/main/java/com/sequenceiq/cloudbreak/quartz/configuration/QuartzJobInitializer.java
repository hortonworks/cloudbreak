package com.sequenceiq.cloudbreak.quartz.configuration;

import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.service.LockNumber;
import com.sequenceiq.cloudbreak.common.service.LockService;
import com.sequenceiq.cloudbreak.service.executor.DelayedExecutorService;

@Component
public class QuartzJobInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger(QuartzJobInitializer.class);

    @Inject
    private QuartzJobInitializerService quartzJobInitializerService;

    @Inject
    private LockService lockService;

    @Inject
    private Optional<DelayedExecutorService> delayedExecutorServiceProvider;

    @Value("${quartz.initialization.delay}")
    private Long initializationDelay;

    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        try {
            LOGGER.info("Quartz initialization will run with delay: {}s", initializationDelay);
            if (delayedExecutorServiceProvider.isPresent()) {
                delayedExecutorServiceProvider.get().runWithDelay(() -> {
                    lockService.lockAndRunIfLockWasSuccessful(quartzJobInitializerService::initQuartz, LockNumber.QUARTZ);
                    return true;
                }, initializationDelay, TimeUnit.SECONDS);
                LOGGER.info("Quartz initialization ran with delay: {}", initializationDelay);
            } else {
                lockService.lockAndRunIfLockWasSuccessful(quartzJobInitializerService::initQuartz, LockNumber.QUARTZ);
                LOGGER.info("Quartz initialization ran without delay");
            }
        } catch (ExecutionException | InterruptedException e) {
            LOGGER.error("Error happened at delayed quartz initialization", e);
        }
    }
}
