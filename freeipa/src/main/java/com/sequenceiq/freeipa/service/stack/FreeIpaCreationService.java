package com.sequenceiq.freeipa.service.stack;

import java.util.Objects;
import java.util.concurrent.ExecutorService;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.event.platform.GetPlatformTemplateRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.image.ImageSettingsRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.create.CreateFreeIpaRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.DescribeFreeIpaResponse;
import com.sequenceiq.freeipa.controller.exception.BadRequestException;
import com.sequenceiq.freeipa.converter.cloud.CredentialToCloudCredentialConverter;
import com.sequenceiq.freeipa.converter.stack.CreateFreeIpaRequestToStackConverter;
import com.sequenceiq.freeipa.converter.stack.StackToDescribeFreeIpaResponseConverter;
import com.sequenceiq.freeipa.entity.FreeIpa;
import com.sequenceiq.freeipa.entity.Image;
import com.sequenceiq.freeipa.entity.InstanceGroup;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.SecurityConfig;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.chain.FlowChainTriggers;
import com.sequenceiq.freeipa.flow.stack.StackEvent;
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

    @Qualifier("freeipaListeningScheduledExecutorService")
    @Inject
    private ExecutorService executorService;

    @Inject
    private TlsSecurityService tlsSecurityService;

    @Inject
    private StackService stackService;

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

    @Inject
    private StackToDescribeFreeIpaResponseConverter stackToDescribeFreeIpaResponseConverter;

    public DescribeFreeIpaResponse launchFreeIpa(CreateFreeIpaRequest request, String accountId) {
        checkIfAlreadyExistsInEnvironment(request);
        Stack stack = stackConverter.convert(request, accountId);
        stack.setEnvironment(request.getEnvironmentId());
        stack.setAccountId(accountId);
        GetPlatformTemplateRequest getPlatformTemplateRequest = templateService.triggerGetTemplate(stack);

        SecurityConfig securityConfig = tlsSecurityService.generateSecurityKeys();
        stack.setSecurityConfig(securityConfig);

        fillInstanceMetadata(stack);

        String template = templateService.waitGetTemplate(stack, getPlatformTemplateRequest);
        stack.setTemplate(template);
        stackService.save(stack);
        ImageSettingsRequest imageSettingsRequest = request.getImage();
        Image image = imageService.create(stack, Objects.nonNull(imageSettingsRequest) ? imageSettingsRequest : new ImageSettingsRequest());
        FreeIpa freeIpa = freeIpaService.create(stack, request.getFreeIpa());
        flowManager.notify(FlowChainTriggers.PROVISION_TRIGGER_EVENT, new StackEvent(FlowChainTriggers.PROVISION_TRIGGER_EVENT, stack.getId()));
        return stackToDescribeFreeIpaResponseConverter.convert(stack, image, freeIpa);
    }

    private void checkIfAlreadyExistsInEnvironment(CreateFreeIpaRequest request) {
        stackService.findByEnvironmentCrn(request.getEnvironmentId()).ifPresent(stack -> {
            throw new BadRequestException("FreeIPA already exists in environment");
        });
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
