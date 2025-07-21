package com.sequenceiq.cloudbreak.util;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudConnectResources;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.converter.spi.CloudContextProvider;
import com.sequenceiq.cloudbreak.converter.spi.StackToCloudStackConverter;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.dto.StackDto;

@Component
public class CloudConnectorHelper {

    @Inject
    private CloudContextProvider cloudContextProvider;

    @Inject
    private StackToCloudStackConverter cloudStackConverter;

    @Inject
    private StackUtil stackUtil;

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    public CloudConnectResources getCloudConnectorResources(Stack stack) {
        CloudCredential cloudCredential = stackUtil.getCloudCredential(stack.getEnvironmentCrn());
        CloudStack cloudStack = cloudStackConverter.convert(stack);
        CloudContext cloudContext = cloudContextProvider.getCloudContext(stack);
        CloudConnector cloudConnector = cloudPlatformConnectors.get(cloudContext.getPlatformVariant());
        AuthenticatedContext authenticatedContext = cloudConnector.authentication().authenticate(cloudContext, cloudCredential);
        return new CloudConnectResources(cloudCredential, cloudContext, cloudConnector, authenticatedContext, cloudStack);
    }

    public CloudConnectResources getCloudConnectorResources(StackDto stack) {
        CloudCredential cloudCredential = stackUtil.getCloudCredential(stack.getEnvironmentCrn());
        CloudStack cloudStack = cloudStackConverter.convert(stack);
        CloudContext cloudContext = cloudContextProvider.getCloudContext(stack);
        CloudConnector cloudConnector = cloudPlatformConnectors.get(cloudContext.getPlatformVariant());
        AuthenticatedContext authenticatedContext = cloudConnector.authentication().authenticate(cloudContext, cloudCredential);
        return new CloudConnectResources(cloudCredential, cloudContext, cloudConnector, authenticatedContext, cloudStack);
    }

}
