package com.sequenceiq.cloudbreak.service.stack.flow;

import static com.sequenceiq.cloudbreak.domain.InstanceGroupType.GATEWAY;
import static com.sequenceiq.cloudbreak.domain.InstanceGroupType.HOSTGROUP;

import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.Status;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
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

    public ProvisionComplete buildStack(final CloudPlatform cloudPlatform, Long stackId, Map<String, Object> setupProperties,
            Map<String, String> userDataParams) throws Exception {
        ProvisionComplete provisionComplete = null;

        Stack stack = stackRepository.findOneWithLists(stackId);
        MDCBuilder.buildMdcContext(stack);
        if (stack.getStatus().equals(Status.REQUESTED)) {
            String statusReason = "Creation of cluster infrastructure has started on the cloud provider.";
            stack = stackUpdater.updateStackStatus(stack.getId(), Status.CREATE_IN_PROGRESS, statusReason);
            stackUpdater.updateStackStatusReason(stack.getId(), stack.getStatus().name());
            String gateWayUserDataScript = userDataBuilder.buildUserData(cloudPlatform, stack.getHash(), stack.getConsulServers(), userDataParams, GATEWAY);
            String hostGroupUserDataScript = userDataBuilder.buildUserData(cloudPlatform, stack.getHash(), stack.getConsulServers(), userDataParams, HOSTGROUP);
            CloudPlatformConnector cloudPlatformConnector = cloudPlatformConnectors.get(cloudPlatform);
            Set<Resource> resources = cloudPlatformConnector.buildStack(stack, gateWayUserDataScript, hostGroupUserDataScript, setupProperties);
            provisionComplete = new ProvisionComplete(cloudPlatform, stack.getId(), resources);
        } else {
            LOGGER.info("CloudFormation stack creation was requested for a stack, that is not in REQUESTED status anymore. [stackId: '{}', status: '{}']",
                    stack.getId(), stack.getStatus());
        }
        return provisionComplete;
    }
}
