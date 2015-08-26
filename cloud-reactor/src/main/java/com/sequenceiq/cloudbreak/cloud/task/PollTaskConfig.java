package com.sequenceiq.cloudbreak.cloud.task;


import java.util.List;

import javax.inject.Inject;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import com.sequenceiq.cloudbreak.cloud.BooleanStateConnector;
import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.event.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.event.instance.BooleanResult;
import com.sequenceiq.cloudbreak.cloud.event.instance.InstanceConsoleOutputResult;
import com.sequenceiq.cloudbreak.cloud.event.instance.InstancesStatusResult;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;

@Configuration
public class PollTaskConfig {

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Bean
    @Scope(value = "prototype")
    public PollTaskFactory newFetchStateTask() {
        return new PollTaskFactory() {
            @Override
            public PollTask<ResourcesStatePollerResult> newPollResourcesStateTask(AuthenticatedContext authenticatedContext,
                    List<CloudResource> cloudResource) {
                CloudConnector connector = cloudPlatformConnectors.get(authenticatedContext.getCloudContext().getPlatform());
                return new PollResourcesStateTask(authenticatedContext, connector.resources(), cloudResource);
            }

            @Override
            public PollTask<ResourcesStatePollerResult> newPollResourceTerminationTask(AuthenticatedContext authenticatedContext,
                    List<CloudResource> cloudResource) {
                CloudConnector connector = cloudPlatformConnectors.get(authenticatedContext.getCloudContext().getPlatform());
                return new PollResourceTerminationTask(authenticatedContext, connector.resources(), cloudResource);
            }

            @Override
            public PollTask<InstancesStatusResult> newPollInstanceStateTask(AuthenticatedContext authenticatedContext, List<CloudInstance> instances) {
                CloudConnector connector = cloudPlatformConnectors.get(authenticatedContext.getCloudContext().getPlatform());
                return new PollInstancesStateTask(authenticatedContext, connector.instances(), instances);
            }

            @Override
            public PollTask<InstanceConsoleOutputResult> newPollInstanceConsoleOutputTask(AuthenticatedContext authenticatedContext, CloudInstance instance) {
                CloudConnector connector = cloudPlatformConnectors.get(authenticatedContext.getCloudContext().getPlatform());
                return new PollInstanceConsoleOutputTask(connector.instances(), authenticatedContext, instance);
            }

            @Override
            public PollTask<BooleanResult> newPollBooleanStateTask(AuthenticatedContext authenticatedContext, BooleanStateConnector connector) {
                return new PollBooleanStateTask(authenticatedContext, connector);
            }
        };
    }

}


