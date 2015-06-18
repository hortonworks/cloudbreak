package com.sequenceiq.cloudbreak.core.flow.service;

import java.util.Map;

import javax.annotation.Resource;
import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.TlsSecurityService;
import com.sequenceiq.cloudbreak.service.stack.connector.ProvisionSetup;
import com.sequenceiq.cloudbreak.service.stack.event.ProvisionSetupComplete;

@Component
public class ProvisioningSetupService {

    @Inject
    private StackRepository stackRepository;

    @Inject
    private TlsSecurityService tlsSecurityService;

    @Resource
    private Map<CloudPlatform, ProvisionSetup> provisionSetups;

    public ProvisionSetupComplete setup(Stack stack) throws Exception {
        ProvisionSetupComplete setupComplete = (ProvisionSetupComplete) provisionSetups.get(stack.cloudPlatform()).setupProvisioning(stack);
        stackRepository.save(stack);
        tlsSecurityService.copyClientKeys(stack.getId());
        tlsSecurityService.setupSSHKeys(stack);
        return setupComplete;
    }

}
