package com.sequenceiq.freeipa.service.cloud;

import static com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone.availabilityZone;
import static com.sequenceiq.cloudbreak.cloud.model.Location.location;
import static com.sequenceiq.cloudbreak.cloud.model.Region.region;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.PlatformParameters;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.model.EventStatus;
import com.sequenceiq.cloudbreak.cloud.event.platform.PlatformParameterRequest;
import com.sequenceiq.cloudbreak.cloud.event.platform.PlatformParameterResult;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.service.OperationException;
import com.sequenceiq.freeipa.converter.cloud.CredentialToCloudCredentialConverter;
import com.sequenceiq.freeipa.entity.Stack;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Service
public class PlatformParameterService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlatformParameterService.class);

    @Inject
    private CredentialToCloudCredentialConverter credentialConverter;

    @Inject
    private EventBus eventBus;

    public PlatformParameters getPlatformParameters(Stack stack) {
        LOGGER.debug("Get platform parameters for: {}", stack);
        Location location = location(region(stack.getRegion()), availabilityZone(stack.getAvailabilityZone()));
        CloudContext cloudContext = new CloudContext(stack.getId(), stack.getName(), stack.getCloudPlatform(), stack.getCloudPlatform(),
                location, stack.getOwner(), 0L);
        CloudCredential cloudCredential = credentialConverter.convert(stack.getCredential());
        PlatformParameterRequest parameterRequest = new PlatformParameterRequest(cloudContext, cloudCredential);
        eventBus.notify(parameterRequest.selector(), new Event<>(parameterRequest));
        try {
            PlatformParameterResult res = parameterRequest.await();
            LOGGER.debug("Platform parameter result: {}", res);
            if (res.getStatus().equals(EventStatus.FAILED)) {
                LOGGER.error("Failed to get platform parameters", res.getErrorDetails());
                throw new OperationException(res.getErrorDetails());
            }
            return res.getPlatformParameters();
        } catch (InterruptedException e) {
            LOGGER.error("Error while getting platform parameters: " + cloudContext, e);
            throw new OperationException(e);
        }
    }

}
