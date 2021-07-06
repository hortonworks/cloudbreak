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
import com.sequenceiq.common.api.type.ImageStatusResult;
import com.sequenceiq.cloudbreak.converter.spi.StackToCloudStackConverter;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.OperationException;
import com.sequenceiq.cloudbreak.service.image.ImageService;
import com.sequenceiq.cloudbreak.util.StackUtil;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;

import reactor.bus.EventBus;

@Component
public class ServiceProviderSetupAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceProviderSetupAdapter.class);

    @Inject
    private EventBus eventBus;

    @Inject
    private ErrorHandlerAwareReactorEventFactory eventFactory;

    @Inject
    private ImageService imageService;

    @Inject
    private StackToCloudStackConverter cloudStackConverter;

    @Inject
    private StackUtil stackUtil;

    public ImageStatusResult checkImage(Stack stack) throws Exception {
        Location location = location(region(stack.getRegion()), availabilityZone(stack.getAvailabilityZone()));
        CloudContext cloudContext = CloudContext.Builder.builder()
                .withId(stack.getId())
                .withName(stack.getName())
                .withCrn(stack.getResourceCrn())
                .withPlatform(stack.getCloudPlatform())
                .withVariant(stack.getPlatformVariant())
                .withLocation(location)
                .withWorkspaceId(stack.getWorkspace().getId())
                .withAccountId(stack.getTenant().getId())
                .build();
        CloudCredential cloudCredential = stackUtil.getCloudCredential(stack);
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
