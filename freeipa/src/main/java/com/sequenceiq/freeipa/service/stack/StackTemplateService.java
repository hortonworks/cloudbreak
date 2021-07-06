package com.sequenceiq.freeipa.service.stack;

import static com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone.availabilityZone;
import static com.sequenceiq.cloudbreak.cloud.model.Location.location;
import static com.sequenceiq.cloudbreak.cloud.model.Region.region;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.model.EventStatus;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetPlatformTemplateRequest;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetPlatformTemplateResult;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.Location;
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
        Location location = location(region(stack.getRegion()), availabilityZone(stack.getAvailabilityZone()));
        CloudContext cloudContext = CloudContext.Builder.builder()
                .withId(stack.getId())
                .withName(stack.getName())
                .withCrn(stack.getResourceCrn())
                .withPlatform(stack.getCloudPlatform())
                .withVariant(stack.getCloudPlatform())
                .withLocation(location)
                .withUserName(stack.getOwner())
                .withAccountId(stack.getAccountId())
                .build();
        CloudCredential cloudCredential = credentialConverter.convert(credential);
        GetPlatformTemplateRequest getPlatformTemplateRequest = new GetPlatformTemplateRequest(cloudContext, cloudCredential);
        freeIpaFlowManager.notify(getPlatformTemplateRequest);
        return getPlatformTemplateRequest;
    }

    public String waitGetTemplate(Stack stack, GetPlatformTemplateRequest getPlatformTemplateRequest) {
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
