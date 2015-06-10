package com.sequenceiq.cloudbreak.service.stack.flow;

import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.InstanceGroupType;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.stack.connector.CloudPlatformConnector;
import com.sequenceiq.cloudbreak.service.stack.connector.UserDataBuilder;
import com.sequenceiq.cloudbreak.service.stack.event.ProvisionComplete;

@Service
public class ProvisioningService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProvisioningService.class);

    @Inject
    private StackRepository stackRepository;

    @javax.annotation.Resource
    private Map<CloudPlatform, CloudPlatformConnector> cloudPlatformConnectors;

    @Inject
    private UserDataBuilder userDataBuilder;

    public ProvisionComplete buildStack(final CloudPlatform cloudPlatform, Stack stack, Map<String, Object> setupProperties) throws Exception {
        ProvisionComplete provisionComplete = null;
        stack = stackRepository.findOneWithLists(stack.getId());
        if (stack.isRequested()) {
            Map<InstanceGroupType, String> userData = userDataBuilder.buildUserData(cloudPlatform);
            CloudPlatformConnector cloudPlatformConnector = cloudPlatformConnectors.get(cloudPlatform);
            Set<Resource> resources = cloudPlatformConnector
                    .buildStack(stack, userData.get(InstanceGroupType.GATEWAY), userData.get(InstanceGroupType.CORE), setupProperties);
            provisionComplete = new ProvisionComplete(cloudPlatform, stack.getId(), resources);
        } else {
            LOGGER.info("CloudFormation stack creation was requested for a stack, that is not in REQUESTED status anymore. [stackId: '{}', status: '{}']",
                    stack.getId(), stack.getStatus());
        }
        return provisionComplete;
    }
}
