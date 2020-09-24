package com.sequenceiq.redbeams.flow.redbeams.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.service.flowlog.RestartFlowService;

@Component
public class RedbeamsCleanupService implements ApplicationListener<ContextRefreshedEvent> {
    private static final Logger LOGGER = LoggerFactory.getLogger(RedbeamsCleanupService.class);

    private final RestartFlowService restartFlowService;

    public RedbeamsCleanupService(RestartFlowService restartFlowService) {
        this.restartFlowService = restartFlowService;
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        try {
            restartFlowService.collectUnderOperationResources();
        } catch (Exception e) {
            LOGGER.error("Cleanup or the migration operations failed. Shutting down the node. ", e);
            ConfigurableApplicationContext applicationContext = (ConfigurableApplicationContext) event.getApplicationContext();
            applicationContext.close();
        }
    }
}
