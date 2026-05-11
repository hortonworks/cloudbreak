package com.sequenceiq.cloudbreak.core.flow2.cluster.modifytags.handler;

import static com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone.availabilityZone;
import static com.sequenceiq.cloudbreak.cloud.model.Location.location;
import static com.sequenceiq.cloudbreak.cloud.model.Region.region;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.modifytags.event.ModifyUserDefinedTagsStateSelectors.MODIFY_USER_DEFINED_TAGS_STACK_EVENT;

import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudPlatformVariant;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.StackTags;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.modifytags.event.ModifyUserDefinedTagsCloudResourcesHandlerEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.modifytags.event.ModifyUserDefinedTagsEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.modifytags.event.ModifyUserDefinedTagsFailedEvent;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.service.resource.ResourceService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.util.StackUtil;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class ModifyUserDefinedTagsCloudResourcesHandler extends ExceptionCatcherEventHandler<ModifyUserDefinedTagsCloudResourcesHandlerEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ModifyUserDefinedTagsCloudResourcesHandler.class);

    @Inject
    private StackService stackService;

    @Inject
    private ResourceService resourceService;

    @Inject
    private StackUtil stackUtil;

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(ModifyUserDefinedTagsCloudResourcesHandlerEvent.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<ModifyUserDefinedTagsCloudResourcesHandlerEvent> event) {
        LOGGER.warn("Modify user defined tags on stack's cloud resources failed.", e);
        return new ModifyUserDefinedTagsFailedEvent(resourceId, "UPDATE_USER_DEFINED_TAGS_CLOUD_RESOURCES_PHASE", e);
    }

    @Override
    public Selectable doAccept(HandlerEvent<ModifyUserDefinedTagsCloudResourcesHandlerEvent> event) {
        Long resourceId = event.getData().getResourceId();
        Map<String, String> userDefinedTags = event.getData().getUserDefinedTags();
        try {
            Stack stack = stackService.getById(resourceId);
            LOGGER.debug("Updating cloud resources tags of stack: {} with tags: {}", stack.getResourceCrn(), userDefinedTags);
            updateCloudResourcesTags(stack, userDefinedTags);
            return new ModifyUserDefinedTagsEvent(MODIFY_USER_DEFINED_TAGS_STACK_EVENT.selector(), resourceId, userDefinedTags);
        } catch (Exception e) {
            LOGGER.warn("Modify user defined tags on stack's cloud resources failed.", e);
            return new ModifyUserDefinedTagsFailedEvent(resourceId, "UPDATE_USER_DEFINED_TAGS_CLOUD_RESOURCES_PHASE", e);
        }
    }

    private void updateCloudResourcesTags(Stack stack, Map<String, String> userDefinedTags) {
        List<CloudResource> cloudResources = resourceService.getAllCloudResource(stack.getId());
        CloudCredential cloudCredential = stackUtil.getCloudCredential(stack.getEnvironmentCrn());
        CloudContext cloudContext = createCloudContext(stack);
        CloudPlatformVariant cloudPlatformVariant = new CloudPlatformVariant(Platform.platform(stack.getCloudPlatform()),
                Variant.variant(stack.getPlatformVariant()));
        CloudConnector cloudConnector = cloudPlatformConnectors.get(cloudPlatformVariant);
        AuthenticatedContext ac = cloudConnector.authentication().authenticate(cloudContext, cloudCredential);

        StackTags stackTags = stack.getTags().getUnchecked(StackTags.class);
        Map<String, String> tagsToUpdate = stackTags.getUserDefinedTagsWithoutDefaultTags(userDefinedTags);

        cloudConnector.resources().updateTags(ac, cloudResources, tagsToUpdate);
    }

    private CloudContext createCloudContext(Stack stack) {
        Location location = location(region(stack.getRegion()), availabilityZone(stack.getAvailabilityZone()));
        return CloudContext.Builder.builder()
                .withId(stack.getId())
                .withName(stack.getName())
                .withCrn(stack.getResourceCrn())
                .withPlatform(stack.getCloudPlatform())
                .withVariant(stack.getPlatformVariant())
                .withLocation(location)
                .withWorkspaceId(stack.getWorkspace().getId())
                .withAccountId(Crn.safeFromString(stack.getResourceCrn()).getAccountId())
                .build();
    }
}
