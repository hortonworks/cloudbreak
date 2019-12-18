package com.sequenceiq.cloudbreak.init;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.service.flowlog.RestartFlowService;
import com.sequenceiq.cloudbreak.service.ha.HeartbeatService;
import com.sequenceiq.cloudbreak.startup.MissingVolumeTemplatesMigrator;

@Component
public class CloudbreakCleanupService implements ApplicationListener<ContextRefreshedEvent> {
    private static final Logger LOGGER = LoggerFactory.getLogger(CloudbreakCleanupService.class);

    @Inject
    private RestartFlowService restartFlowService;

    @Inject
    private HeartbeatService heartbeatService;

    @Inject
    private MissingVolumeTemplatesMigrator missingVolumeTemplatesMigrator;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        heartbeatService.heartbeat();
        try {
            restartFlowService.purgeTerminatedResourceFlowLogs();
            missingVolumeTemplatesMigrator.run();
        } catch (Exception e) {
            LOGGER.error("Clean up or the migration operations failed. Shutting down the node. ", e);
            ConfigurableApplicationContext applicationContext = (ConfigurableApplicationContext) event.getApplicationContext();
            applicationContext.close();
        }
    }
}
