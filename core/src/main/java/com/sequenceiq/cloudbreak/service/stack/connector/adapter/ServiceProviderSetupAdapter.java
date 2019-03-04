package com.sequenceiq.cloudbreak.service.stack.connector.adapter;

import static com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone.availabilityZone;
import static com.sequenceiq.cloudbreak.cloud.model.Location.location;
import static com.sequenceiq.cloudbreak.cloud.model.Region.region;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.setup.CheckImageRequest;
import com.sequenceiq.cloudbreak.cloud.event.setup.CheckImageResult;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.reactor.ErrorHandlerAwareReactorEventFactory;
import com.sequenceiq.cloudbreak.common.type.ImageStatusResult;
import com.sequenceiq.cloudbreak.converter.spi.CredentialToCloudCredentialConverter;
import com.sequenceiq.cloudbreak.converter.spi.StackToCloudStackConverter;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.OperationException;
import com.sequenceiq.cloudbreak.service.image.ImageService;

import reactor.bus.EventBus;

@Component
public class ServiceProviderSetupAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceProviderSetupAdapter.class);

    @Inject
    private EventBus eventBus;

    @Inject
    private ErrorHandlerAwareReactorEventFactory eventFactory;

    @Inject
    private CredentialToCloudCredentialConverter credentialConverter;

    @Inject
    private ImageService imageService;

    @Inject
    private StackToCloudStackConverter cloudStackConverter;

    public ImageStatusResult checkImage(Stack stack) throws Exception {
        Location location = location(region(stack.getRegion()), availabilityZone(stack.getAvailabilityZone()));
        CloudContext cloudContext = new CloudContext(stack.getId(), stack.getName(), stack.cloudPlatform(), stack.getPlatformVariant(),
                location, stack.getCreator().getUserId(), stack.getWorkspace().getId());
        CloudCredential cloudCredential = credentialConverter.convert(stack.getCredential());
        Image image = imageService.getImage(stack.getId());
        CheckImageRequest<CheckImageResult> checkImageRequest =
                new CheckImageRequest<>(cloudContext, cloudCredential, cloudStackConverter.convert(stack), image);
        LOGGER.debug("Triggering event: {}", checkImageRequest);
        eventBus.notify(checkImageRequest.selector(), eventFactory.createEvent(checkImageRequest));
        try {
            CheckImageResult res = checkImageRequest.await();
            LOGGER.debug("Result: {}", res);
            if (res.getErrorDetails() != null) {
                LOGGER.info("Failed to check image state", res.getErrorDetails());
                throw new OperationException(res.getErrorDetails());
            }
            return new ImageStatusResult(res.getImageStatus(), res.getStatusProgressValue());
        } catch (InterruptedException e) {
            LOGGER.info("Error while executing check image", e);
            throw new OperationException(e);
        }
    }
}
