package com.sequenceiq.redbeams.flow.redbeams.stack.modify.tags.handler;

import static com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone.availabilityZone;
import static com.sequenceiq.cloudbreak.cloud.model.Location.location;
import static com.sequenceiq.cloudbreak.cloud.model.Region.region;
import static com.sequenceiq.redbeams.flow.redbeams.stack.modify.tags.event.ModifyUserDefinedTagsStateSelectors.MODIFY_USER_DEFINED_TAGS_REDBEAMS_STACK_EVENT;

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
import com.sequenceiq.redbeams.converter.cloud.CredentialToCloudCredentialConverter;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.dto.Credential;
import com.sequenceiq.redbeams.flow.redbeams.stack.modify.tags.event.ModifyUserDefinedTagsCloudResourcesHandlerEvent;
import com.sequenceiq.redbeams.flow.redbeams.stack.modify.tags.event.ModifyUserDefinedTagsEvent;
import com.sequenceiq.redbeams.flow.redbeams.stack.modify.tags.event.ModifyUserDefinedTagsFailedEvent;
import com.sequenceiq.redbeams.service.CredentialService;
import com.sequenceiq.redbeams.service.stack.DBResourceService;
import com.sequenceiq.redbeams.service.stack.DBStackService;

@Component
public class ModifyUserDefinedTagsCloudResourcesHandler extends ExceptionCatcherEventHandler<ModifyUserDefinedTagsCloudResourcesHandlerEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ModifyUserDefinedTagsCloudResourcesHandler.class);

    @Inject
    private DBStackService dbStackService;

    @Inject
    private DBResourceService dbResourceService;

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
        LOGGER.warn("Modify user defined tags on Redbeams cloud resources failed.", e);
        return new ModifyUserDefinedTagsFailedEvent(resourceId, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<ModifyUserDefinedTagsCloudResourcesHandlerEvent> event) {
        Long resourceId = event.getData().getResourceId();
        Map<String, String> userDefinedTags = event.getData().getUserDefinedTags();
        try {
            DBStack stack = dbStackService.getById(resourceId);
            LOGGER.debug("Updating cloud resources tags of external database with resourceCrn: {} with tags: {}", stack.getResourceCrn(), userDefinedTags);
            modifyUserDefinedTagsOnCloudResources(stack, userDefinedTags);
            return new ModifyUserDefinedTagsEvent(MODIFY_USER_DEFINED_TAGS_REDBEAMS_STACK_EVENT.selector(), resourceId, userDefinedTags);
        } catch (Exception e) {
            LOGGER.warn("Modify user defined tags on Redbeams cloud resources failed.", e);
            return new ModifyUserDefinedTagsFailedEvent(resourceId, e);
        }

    }

    private void modifyUserDefinedTagsOnCloudResources(DBStack stack, Map<String, String> userDefinedTags) {
        List<CloudResource> cloudResources = dbResourceService.getAllAsCloudResource(stack.getId());
        Credential credential = credentialService.getCredentialByEnvCrn(stack.getEnvironmentId());
        CloudCredential cloudCredential = credentialToCloudCredentialConverter.convert(credential);
        CloudContext cloudContext = createCloudContext(stack);
        CloudConnector cloudConnector = cloudPlatformConnectors.get(cloudContext.getPlatformVariant());
        AuthenticatedContext ac = cloudConnector.authentication().authenticate(cloudContext, cloudCredential);

        StackTags stackTags = stack.getTags().getUnchecked(StackTags.class);
        Map<String, String> tagsToUpdate = stackTags.getUserDefinedTagsWithoutDefaultTags(userDefinedTags);

        cloudConnector.resources().updateTags(ac, cloudResources, tagsToUpdate);
    }

    private static CloudContext createCloudContext(DBStack stack) {
        Location location = location(region(stack.getRegion()), availabilityZone(stack.getAvailabilityZone()));
        String accountId = stack.getAccountId();
        return CloudContext.Builder.builder()
                .withId(stack.getId())
                .withName(stack.getName())
                .withCrn(stack.getResourceCrn())
                .withPlatform(stack.getCloudPlatform())
                .withVariant(stack.getPlatformVariant())
                .withLocation(location)
                .withUserName(stack.getUserName())
                .withAccountId(accountId)
                .build();
    }
}
