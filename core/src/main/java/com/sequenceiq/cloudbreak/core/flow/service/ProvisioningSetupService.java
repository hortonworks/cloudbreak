package com.sequenceiq.cloudbreak.core.flow.service;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.CloudPlatformResolver;
import com.sequenceiq.cloudbreak.service.TlsSecurityService;
import com.sequenceiq.cloudbreak.service.stack.event.ProvisionSetupComplete;

@Component
public class ProvisioningSetupService {

    @Inject
    private StackRepository stackRepository;

    @Inject
    private TlsSecurityService tlsSecurityService;

    @Inject
    private CloudPlatformResolver cloudPlatformResolver;

    public ProvisionSetupComplete setup(Stack stack) throws Exception {
        ProvisionSetupComplete setupComplete = (ProvisionSetupComplete)
                cloudPlatformResolver.provisioning(stack.cloudPlatform()).setupProvisioning(stack);
        tlsSecurityService.copyClientKeys(stack.getId());
        tlsSecurityService.setupSSHKeys(stack);
        return setupComplete;
    }

}
