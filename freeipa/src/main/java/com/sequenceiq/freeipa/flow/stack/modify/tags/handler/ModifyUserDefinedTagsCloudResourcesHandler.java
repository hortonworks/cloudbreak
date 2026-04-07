package com.sequenceiq.freeipa.flow.stack.modify.tags.handler;

import static com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone.availabilityZone;
import static com.sequenceiq.cloudbreak.cloud.model.Location.location;
import static com.sequenceiq.cloudbreak.cloud.model.Region.region;
import static com.sequenceiq.freeipa.flow.freeipa.common.FailureType.ERROR;
import static com.sequenceiq.freeipa.flow.stack.modify.tags.event.ModifyUserDefinedTagsStateSelectors.MODIFY_USER_DEFINED_TAGS_FREEIPA_STACK_EVENT;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.StackTags;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.freeipa.converter.cloud.CredentialToCloudCredentialConverter;
import com.sequenceiq.freeipa.dto.Credential;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.stack.modify.tags.event.ModifyUserDefinedTagsCloudResourcesHandlerEvent;
import com.sequenceiq.freeipa.flow.stack.modify.tags.event.ModifyUserDefinedTagsEvent;
import com.sequenceiq.freeipa.flow.stack.modify.tags.event.ModifyUserDefinedTagsFailedEvent;
import com.sequenceiq.freeipa.service.CredentialService;
import com.sequenceiq.freeipa.service.resource.ResourceService;
import com.sequenceiq.freeipa.service.stack.StackService;

@Component
public class ModifyUserDefinedTagsCloudResourcesHandler extends ExceptionCatcherEventHandler<ModifyUserDefinedTagsCloudResourcesHandlerEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ModifyUserDefinedTagsCloudResourcesHandler.class);

    @Inject
    private StackService stackService;

    @Inject
    private ResourceService resourceService;

    @Inject
    private CredentialService credentialService;

    @Inject
    private CredentialToCloudCredentialConverter credentialToCloudCredentialConverter;

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(ModifyUserDefinedTagsCloudResourcesHandlerEvent.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<ModifyUserDefinedTagsCloudResourcesHandlerEvent> event) {
        LOGGER.error("Modify user defined tags on FreeIPA cloud resources failed.", e);
        return new ModifyUserDefinedTagsFailedEvent(resourceId, "UPDATE_USER_DEFINED_TAGS_FREEIPA_CLOUD_RESOURCES_PHASE", e, ERROR);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<ModifyUserDefinedTagsCloudResourcesHandlerEvent> event) {
        Long resourceId = event.getData().getResourceId();
        String operationId = event.getData().getOperationId();
        Map<String, String> userDefinedTags = event.getData().getUserDefinedTags();
        try {
            Stack stack = stackService.getStackById(resourceId);
            modifyUserDefinedTagsOnCloudResources(stack, userDefinedTags);
            return new ModifyUserDefinedTagsEvent(MODIFY_USER_DEFINED_TAGS_FREEIPA_STACK_EVENT.selector(), resourceId, operationId, userDefinedTags);
        } catch (Exception e) {
            return new ModifyUserDefinedTagsFailedEvent(resourceId, "UPDATE_USER_DEFINED_TAGS_FREEIPA_CLOUD_RESOURCES_PHASE", e, ERROR);
        }

    }

    private void modifyUserDefinedTagsOnCloudResources(Stack stack, Map<String, String> userDefinedTags) {
        List<CloudResource> cloudResources = resourceService.getAllCloudResource(stack.getId());
        Credential credential = credentialService.getCredentialByEnvCrn(stack.getEnvironmentCrn());
        CloudCredential cloudCredential = credentialToCloudCredentialConverter.convert(credential);
        CloudContext cloudContext = createCloudContext(stack);
        CloudConnector cloudConnector = cloudPlatformConnectors.get(cloudContext.getPlatformVariant());
        AuthenticatedContext ac = cloudConnector.authentication().authenticate(cloudContext, cloudCredential);
        Map<String, String> tagsToUpdate = new HashMap<>(userDefinedTags);
        StackTags stackTags = stack.getTags().getUnchecked(StackTags.class);
        tagsToUpdate.keySet().removeAll(stackTags.getDefaultTags().keySet());
        LOGGER.debug("Updating cloud resources tags of FreeIPA with resourceCrn: {} with tags: {}", stack.getResourceCrn(), tagsToUpdate);
        cloudConnector.resources().updateTags(ac, cloudResources, tagsToUpdate);
    }

    private static CloudContext createCloudContext(Stack stack) {
        Location location = location(region(stack.getRegion()), availabilityZone(stack.getAvailabilityZone()));
        String accountId = stack.getAccountId();
        return CloudContext.Builder.builder()
                .withId(stack.getId())
                .withName(stack.getName())
                .withCrn(stack.getResourceCrn())
                .withPlatform(stack.getCloudPlatform())
                .withVariant(stack.getPlatformvariant())
                .withLocation(location)
                .withUserName(stack.getOwner())
                .withAccountId(accountId)
                .build();
    }
}
