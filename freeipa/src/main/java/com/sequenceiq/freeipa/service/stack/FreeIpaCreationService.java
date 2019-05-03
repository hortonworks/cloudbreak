package com.sequenceiq.freeipa.service.stack;

import java.util.concurrent.ExecutorService;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetPlatformTemplateRequest;
import com.sequenceiq.freeipa.api.model.create.CreateFreeIpaRequest;
import com.sequenceiq.freeipa.converter.CreateFreeIpaRequestToStackConverter;
import com.sequenceiq.freeipa.converter.cloud.CredentialToCloudCredentialConverter;
import com.sequenceiq.freeipa.entity.InstanceGroup;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.SecurityConfig;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.chain.FlowChainTriggers;
import com.sequenceiq.freeipa.flow.stack.StackEvent;
import com.sequenceiq.freeipa.repository.StackRepository;
import com.sequenceiq.freeipa.service.CredentialService;
import com.sequenceiq.freeipa.service.FreeIpaFlowManager;
import com.sequenceiq.freeipa.service.FreeIpaService;
import com.sequenceiq.freeipa.service.TlsSecurityService;
import com.sequenceiq.freeipa.service.image.ImageService;

@Service
public class FreeIpaCreationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaCreationService.class);

    @Inject
    private CredentialToCloudCredentialConverter credentialConverter;

    @Inject
    private ExecutorService executorService;

    @Inject
    private TlsSecurityService tlsSecurityService;

    @Inject
    private StackRepository stackRepository;

    @Inject
    private CreateFreeIpaRequestToStackConverter stackConverter;

    @Inject
    private StackTemplateService templateService;

    @Inject
    private FreeIpaFlowManager flowManager;

    @Inject
    private ImageService imageService;

    @Inject
    private CredentialService credentialService;

    @Inject
    private FreeIpaService freeIpaService;

    public void launchFreeIpa(CreateFreeIpaRequest request) {
        Stack stack = stackConverter.convert(request);
        GetPlatformTemplateRequest getPlatformTemplateRequest = templateService.triggerGetTemplate(stack);

        SecurityConfig securityConfig = tlsSecurityService.generateSecurityKeys();
        stack.setSecurityConfig(securityConfig);

        fillInstanceMetadata(stack);

        String template = templateService.waitGetTemplate(stack, getPlatformTemplateRequest);
        stack.setTemplate(template);
        stackRepository.save(stack);
        imageService.create(stack, request.getImage());
        freeIpaService.create(stack, request.getFreeIpa());
        flowManager.notify(FlowChainTriggers.PROVISION_TRIGGER_EVENT, new StackEvent(FlowChainTriggers.PROVISION_TRIGGER_EVENT, stack.getId()));
    }

    private void fillInstanceMetadata(Stack stack) {
        long privateIdNumber = 0;
        for (InstanceGroup instanceGroup : stack.getInstanceGroups()) {
            for (InstanceMetaData instanceMetaData : instanceGroup.getAllInstanceMetaData()) {
                instanceMetaData.setPrivateId(privateIdNumber++);
                instanceMetaData.setInstanceStatus(InstanceStatus.REQUESTED);
            }
        }
    }
}
