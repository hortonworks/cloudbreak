package com.sequenceiq.cloudbreak.service.stack.flow;

import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.InstanceGroupType;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.Status;
import com.sequenceiq.cloudbreak.repository.RetryingStackUpdater;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.stack.connector.CloudPlatformConnector;
import com.sequenceiq.cloudbreak.service.stack.connector.UserDataBuilder;
import com.sequenceiq.cloudbreak.service.stack.event.ProvisionComplete;

@Service
public class ProvisioningService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProvisioningService.class);

    @Autowired
    private StackRepository stackRepository;

    @Autowired
    private RetryingStackUpdater stackUpdater;

    @javax.annotation.Resource
    private Map<CloudPlatform, CloudPlatformConnector> cloudPlatformConnectors;

    @Autowired
    private UserDataBuilder userDataBuilder;

    public ProvisionComplete buildStack(final CloudPlatform cloudPlatform, Long stackId, Map<String, Object> setupProperties) throws Exception {
        ProvisionComplete provisionComplete = null;
        Stack stack = stackRepository.findOneWithLists(stackId);
        if (stack.getStatus().equals(Status.REQUESTED)) {
            String statusReason = "Creation of cluster infrastructure has started on the cloud provider.";
            stack = stackUpdater.updateStackStatus(stack.getId(), Status.CREATE_IN_PROGRESS, statusReason);
            stackUpdater.updateStackStatusReason(stack.getId(), stack.getStatus().name());
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
