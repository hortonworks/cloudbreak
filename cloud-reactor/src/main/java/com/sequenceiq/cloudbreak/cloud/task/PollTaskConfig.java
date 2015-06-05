package com.sequenceiq.cloudbreak.cloud.task;


import java.util.List;

import javax.inject.Inject;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.event.LaunchStackResult;
import com.sequenceiq.cloudbreak.cloud.event.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;

@Configuration
public class PollTaskConfig {

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Bean
    @Scope(value = "prototype")
    public PollTaskFactory newFetchResourcesStateTask() {

        return new PollTaskFactory() {

            @Override
            public PollTask<LaunchStackResult> newPollResourcesStateTask(AuthenticatedContext authenticatedContext, List<CloudResource> cloudResource) {
                CloudConnector connector = cloudPlatformConnectors.get(authenticatedContext.getStackContext().getPlatform());
                return new PollResourcesStateTask(authenticatedContext, connector, cloudResource);
            }
        };
    }

}


