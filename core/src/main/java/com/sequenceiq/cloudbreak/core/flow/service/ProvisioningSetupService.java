package com.sequenceiq.cloudbreak.core.flow.service;

import java.nio.file.Paths;
import java.util.Map;

import javax.annotation.Resource;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.amazonaws.util.Base64;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.SecurityConfig;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.SecurityConfigRepository;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.SimpleSecurityService;
import com.sequenceiq.cloudbreak.service.stack.connector.ProvisionSetup;
import com.sequenceiq.cloudbreak.service.stack.event.ProvisionSetupComplete;

@Component
public class ProvisioningSetupService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProvisioningSetupService.class);

    @Inject
    private StackRepository stackRepository;

    @Inject
    private SecurityConfigRepository securityConfigRepository;

    @Inject
    private SimpleSecurityService simpleSecurityService;

    @Resource
    private Map<CloudPlatform, ProvisionSetup> provisionSetups;

    public ProvisionSetupComplete setup(Stack stack) throws Exception {
        ProvisionSetupComplete setupComplete = (ProvisionSetupComplete) provisionSetups.get(stack.cloudPlatform()).setupProvisioning(stack);
        stackRepository.save(stack);
        simpleSecurityService.copyClientKeys(Paths.get(simpleSecurityService.getCertDir(stack.getId())));
        simpleSecurityService.generateTempSshKeypair(stack.getId());
        SecurityConfig securityConfig = new SecurityConfig();
        securityConfig.setClientKey(Base64.encodeAsString(simpleSecurityService.readClientKey(stack.getId()).getBytes()));
        securityConfig.setClientCert(Base64.encodeAsString(simpleSecurityService.readClientCert(stack.getId()).getBytes()));
        securityConfig.setTemporarySshPrivateKey(Base64.encodeAsString(simpleSecurityService.readPrivateSshKey(stack.getId()).getBytes()));
        securityConfig.setTemporarySshPublicKey(Base64.encodeAsString(simpleSecurityService.readPublicSshKey(stack.getId()).getBytes()));
        securityConfig.setStack(stack);
        securityConfigRepository.save(securityConfig);
        return setupComplete;
    }

}
