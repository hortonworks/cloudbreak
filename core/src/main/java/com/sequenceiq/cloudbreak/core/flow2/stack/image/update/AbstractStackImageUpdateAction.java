package com.sequenceiq.cloudbreak.core.flow2.stack.image.update;

import static com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone.availabilityZone;
import static com.sequenceiq.cloudbreak.cloud.model.Location.location;
import static com.sequenceiq.cloudbreak.cloud.model.Region.region;

import java.util.Optional;

import javax.inject.Inject;

import org.springframework.statemachine.StateContext;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.Payload;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.converter.spi.CredentialToCloudCredentialConverter;
import com.sequenceiq.cloudbreak.converter.spi.ResourceToCloudResourceConverter;
import com.sequenceiq.cloudbreak.converter.spi.StackToCloudStackConverter;
import com.sequenceiq.cloudbreak.core.flow2.AbstractAction;
import com.sequenceiq.cloudbreak.core.flow2.stack.FlowMessageService;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackContext;
import com.sequenceiq.cloudbreak.core.flow2.stack.provision.action.StackCreationService;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.repository.ResourceRepository;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.image.ImageService;
import com.sequenceiq.cloudbreak.service.stack.StackService;

abstract class AbstractStackImageUpdateAction<P extends Payload> extends AbstractAction<StackImageUpdateState, StackImageUpdateEvent, StackContext, P> {

    public static final String ORIGINAL_IMAGE = "ORIGINAL_IMAGE";

    @Inject
    private FlowMessageService flowMessageService;

    @Inject
    private StackImageUpdateService stackImageUpdateService;

    @Inject
    private StackService stackService;

    @Inject
    private StackToCloudStackConverter cloudStackConverter;

    @Inject
    private CredentialToCloudCredentialConverter credentialConverter;

    @Inject
    private StackCreationService stackCreationService;

    @Inject
    private ImageService imageService;

    @Inject
    private StackUpdater stackUpdater;

    @Inject
    private ResourceRepository resourceRepository;

    @Inject
    private ResourceToCloudResourceConverter resourceToCloudResourceConverter;

    protected AbstractStackImageUpdateAction(Class<P> payloadClass) {
        super(payloadClass);
    }

    @Override
    protected StackContext createFlowContext(String flowId, StateContext<StackImageUpdateState, StackImageUpdateEvent> stateContext, P payload) {
        Stack stack = stackService.getByIdWithListsInTransaction(payload.getStackId());
        MDCBuilder.buildMdcContext(stack);
        Location location = location(region(stack.getRegion()), availabilityZone(stack.getAvailabilityZone()));
        CloudContext cloudContext = new CloudContext(stack.getId(), stack.getName(), stack.cloudPlatform(), stack.getPlatformVariant(),
                location, stack.getCreator().getUserId(), stack.getWorkspace().getId());
        CloudCredential cloudCredential = credentialConverter.convert(stack.getCredential());
        CloudStack cloudStack = cloudStackConverter.convert(stack);
        return new StackContext(flowId, stack, cloudContext, cloudCredential, cloudStack);
    }

    @Override
    protected Object getFailurePayload(P payload, Optional<StackContext> flowContext, Exception ex) {
        return new StackFailureEvent(payload.getStackId(), ex);
    }

    protected FlowMessageService getFlowMessageService() {
        return flowMessageService;
    }

    protected StackImageUpdateService getStackImageUpdateService() {
        return stackImageUpdateService;
    }

    protected StackService getStackService() {
        return stackService;
    }

    protected StackToCloudStackConverter getCloudStackConverter() {
        return cloudStackConverter;
    }

    protected StackCreationService getStackCreationService() {
        return stackCreationService;
    }

    protected ImageService getImageService() {
        return imageService;
    }

    protected StackUpdater getStackUpdater() {
        return stackUpdater;
    }

    protected ResourceRepository getResourceRepository() {
        return resourceRepository;
    }

    protected ResourceToCloudResourceConverter getResourceToCloudResourceConverter() {
        return resourceToCloudResourceConverter;
    }
}
