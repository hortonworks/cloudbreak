package com.sequenceiq.environment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.service.flowlog.RestartFlowService;
import com.sequenceiq.cloudbreak.service.ha.HeartbeatService;

@Component
public class EnvironmentCleanupService implements ApplicationListener<ContextRefreshedEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentCleanupService.class);

    private final HeartbeatService heartbeatService;

    private final RestartFlowService restartFlowService;

    public EnvironmentCleanupService(HeartbeatService heartbeatService, RestartFlowService restartFlowService) {
        this.heartbeatService = heartbeatService;
        this.restartFlowService = restartFlowService;
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        heartbeatService.heartbeat();
        try {
            restartFlowService.purgeTerminatedResourceFlowLogs();
        } catch (Exception e) {
            LOGGER.error("Clean up or the migration operations failed. Shutting down the node. ", e);
            ConfigurableApplicationContext applicationContext = (ConfigurableApplicationContext) event.getApplicationContext();
            applicationContext.close();
        }
    }
}
