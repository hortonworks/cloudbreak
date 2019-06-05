package com.sequenceiq.cloudbreak.core.flow2.stack.image.update;

import static com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone.availabilityZone;
import static com.sequenceiq.cloudbreak.cloud.model.Location.location;
import static com.sequenceiq.cloudbreak.cloud.model.Region.region;

import java.util.Optional;

import javax.inject.Inject;

import org.springframework.statemachine.StateContext;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.cloudbreak.converter.spi.CredentialToCloudCredentialConverter;
import com.sequenceiq.cloudbreak.converter.spi.ResourceToCloudResourceConverter;
import com.sequenceiq.cloudbreak.converter.spi.StackToCloudStackConverter;
import com.sequenceiq.cloudbreak.core.flow2.AbstractStackAction;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackContext;
import com.sequenceiq.cloudbreak.core.flow2.stack.provision.action.StackCreationService;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.image.ImageService;
import com.sequenceiq.cloudbreak.service.resource.ResourceService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.flow.core.FlowParameters;

abstract class AbstractStackImageUpdateAction<P extends Payload> extends AbstractStackAction<StackImageUpdateState, StackImageUpdateEvent, StackContext, P> {

    public static final String ORIGINAL_IMAGE = "ORIGINAL_IMAGE";

    @Inject
    private CloudbreakFlowMessageService flowMessageService;

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
    private ResourceService resourceService;

    @Inject
    private ResourceToCloudResourceConverter resourceToCloudResourceConverter;

    protected AbstractStackImageUpdateAction(Class<P> payloadClass) {
        super(payloadClass);
    }

    @Override
    protected StackContext createFlowContext(FlowParameters flowParameters, StateContext<StackImageUpdateState, StackImageUpdateEvent> stateContext,
            P payload) {
        Stack stack = stackService.getByIdWithListsInTransaction(payload.getResourceId());
        MDCBuilder.buildMdcContext(stack);
        Location location = location(region(stack.getRegion()), availabilityZone(stack.getAvailabilityZone()));
        CloudContext cloudContext = new CloudContext(stack.getId(), stack.getName(), stack.cloudPlatform(), stack.getPlatformVariant(),
                location, stack.getCreator().getUserId(), stack.getWorkspace().getId());
        CloudCredential cloudCredential = credentialConverter.convert(stack.getCredentialCrn());
        CloudStack cloudStack = cloudStackConverter.convert(stack);
        return new StackContext(flowParameters, stack, cloudContext, cloudCredential, cloudStack);
    }

    @Override
    protected Object getFailurePayload(P payload, Optional<StackContext> flowContext, Exception ex) {
        return new StackFailureEvent(payload.getResourceId(), ex);
    }

    protected CloudbreakFlowMessageService getFlowMessageService() {
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

    protected ResourceService getResourceService() {
        return resourceService;
    }

    protected ResourceToCloudResourceConverter getResourceToCloudResourceConverter() {
        return resourceToCloudResourceConverter;
    }
}
