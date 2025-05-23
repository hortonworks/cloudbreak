package com.sequenceiq.freeipa.service.stack;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.model.EventStatus;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetPlatformTemplateRequest;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetPlatformTemplateResult;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.service.OperationException;
import com.sequenceiq.freeipa.converter.cloud.CredentialToCloudCredentialConverter;
import com.sequenceiq.freeipa.dto.Credential;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.freeipa.flow.FreeIpaFlowManager;

@Service
public class StackTemplateService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackTemplateService.class);

    @Inject
    private FreeIpaFlowManager freeIpaFlowManager;

    @Inject
    private CredentialToCloudCredentialConverter credentialConverter;

    public GetPlatformTemplateRequest triggerGetTemplate(Stack stack, Credential credential) {
        CloudContext cloudContext = CloudContext.Builder.builder()
                .withPlatform(stack.getCloudPlatform())
                .withVariant(stack.getPlatformvariant())
                .build();
        CloudCredential cloudCredential = credentialConverter.convert(credential);
        return triggerGetTemplate(cloudContext, cloudCredential);
    }

    public GetPlatformTemplateRequest triggerGetTemplate(CloudContext cloudContext, CloudCredential cloudCredential) {
        GetPlatformTemplateRequest getPlatformTemplateRequest = new GetPlatformTemplateRequest(cloudContext, cloudCredential);
        freeIpaFlowManager.notifyNonFlowEvent(getPlatformTemplateRequest);
        return getPlatformTemplateRequest;
    }

    public String waitGetTemplate(GetPlatformTemplateRequest getPlatformTemplateRequest) {
        try {
            GetPlatformTemplateResult res = getPlatformTemplateRequest.await();
            LOGGER.debug("Get template result: {}", res);
            if (res.getStatus().equals(EventStatus.FAILED)) {
                LOGGER.error("Failed to get template", res.getErrorDetails());
                throw new OperationException(res.getErrorDetails());
            }
            return res.getTemplate();
        } catch (InterruptedException e) {
            LOGGER.error("Error while getting template: " + getPlatformTemplateRequest.getCloudContext(), e);
            throw new OperationException(e);
        }
    }
}
