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
import com.sequenceiq.freeipa.entity.Stack;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Service
public class StackTemplateService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackTemplateService.class);

    @Inject
    private EventBus eventBus;

    @Inject
    private CredentialToCloudCredentialConverter credentialConverter;

    public GetPlatformTemplateRequest triggerGetTemplate(Stack stack) {
        Location location = location(region(stack.getRegion()), availabilityZone(stack.getAvailabilityZone()));
        CloudContext cloudContext = new CloudContext(stack.getId(), stack.getName(), stack.getCloudPlatform(), stack.getCloudPlatform(),
                location, stack.getOwner(), 0L);
        CloudCredential cloudCredential = credentialConverter.convert(stack.getCredential());
        GetPlatformTemplateRequest getPlatformTemplateRequest = new GetPlatformTemplateRequest(cloudContext, cloudCredential);
        eventBus.notify(getPlatformTemplateRequest.selector(), new Event<>(getPlatformTemplateRequest));
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
