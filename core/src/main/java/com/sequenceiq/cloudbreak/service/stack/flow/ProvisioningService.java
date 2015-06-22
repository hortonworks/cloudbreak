package com.sequenceiq.cloudbreak.service.stack.flow;

import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.core.CloudbreakSecuritySetupException;
import com.sequenceiq.cloudbreak.service.TlsSecurityService;
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

    @Inject
    private TlsSecurityService tlsSecurityService;

    public ProvisionComplete buildStack(final CloudPlatform cloudPlatform, Stack stack, Map<String, Object> setupProperties)
            throws CloudbreakSecuritySetupException {
        stack = stackRepository.findOneWithLists(stack.getId());
        CloudPlatformConnector connector = cloudPlatformConnectors.get(cloudPlatform);
        Map<InstanceGroupType, String> userdata = userDataBuilder.buildUserData(cloudPlatform, tlsSecurityService.readPublicSshKey(stack.getId()),
                connector.getSSHUser());
        Set<Resource> resources = connector.buildStack(stack, userdata.get(InstanceGroupType.GATEWAY), userdata.get(InstanceGroupType.CORE), setupProperties);
        return new ProvisionComplete(cloudPlatform, stack.getId(), resources);
    }
}
