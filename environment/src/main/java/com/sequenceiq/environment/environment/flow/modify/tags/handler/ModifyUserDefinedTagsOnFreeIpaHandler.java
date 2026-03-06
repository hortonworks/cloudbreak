package com.sequenceiq.environment.environment.flow.modify.tags.handler;

import static com.sequenceiq.environment.environment.EnvironmentStatus.USER_DEFINED_TAGS_MODIFICATION_ON_FREEIPA_FAILED;
import static com.sequenceiq.environment.environment.flow.modify.tags.event.EnvTagsModificationHandlerSelectors.MODIFY_USER_DEFINED_TAGS_ON_FREEIPA_EVENT;
import static com.sequenceiq.environment.environment.flow.modify.tags.event.EnvTagsModificationStateSelectors.START_MODIFY_USER_DEFINED_TAGS_DATALAKE_EVENT;

import java.util.Map;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.environment.environment.flow.modify.tags.event.EnvTagsModificationEvent;
import com.sequenceiq.environment.environment.flow.modify.tags.event.EnvTagsModificationFailureEvent;
import com.sequenceiq.environment.environment.service.freeipa.FreeIpaService;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class ModifyUserDefinedTagsOnFreeIpaHandler extends ExceptionCatcherEventHandler<EnvTagsModificationEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ModifyUserDefinedTagsOnFreeIpaHandler.class);

    @Inject
    private FreeIpaService freeIpaService;

    @Override
    public String selector() {
        return MODIFY_USER_DEFINED_TAGS_ON_FREEIPA_EVENT.selector();
    }

    @Override
    protected Selectable doAccept(HandlerEvent<EnvTagsModificationEvent> event) {
        Long resourceId = event.getData().getResourceId();
        String resourceName = event.getData().getResourceName();
        String resourceCrn = event.getData().getResourceCrn();
        Map<String, String> userDefinedTags = event.getData().getUserDefinedTags();
        try {
            freeIpaService.modifyUserDefinedTags(resourceCrn, userDefinedTags);
        } catch (Exception e) {
            LOGGER.error("Modify user defined tags on FreeIPA failed.", e);
            return new EnvTagsModificationFailureEvent(resourceId, resourceName, resourceCrn, USER_DEFINED_TAGS_MODIFICATION_ON_FREEIPA_FAILED, e);
        }
        return EnvTagsModificationEvent.builder()
                .withSelector(START_MODIFY_USER_DEFINED_TAGS_DATALAKE_EVENT.name())
                .withResourceId(resourceId)
                .withResourceName(resourceName)
                .withResourceCrn(resourceCrn)
                .withUserDefinedTags(userDefinedTags)
                .build();
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<EnvTagsModificationEvent> event) {
        LOGGER.error("Modify user defined tags on freeIPA failed.", e);
        String resourceName = event.getData().getResourceName();
        String resourceCrn = event.getData().getResourceCrn();
        return new EnvTagsModificationFailureEvent(resourceId, resourceName, resourceCrn, USER_DEFINED_TAGS_MODIFICATION_ON_FREEIPA_FAILED, e);
    }
}