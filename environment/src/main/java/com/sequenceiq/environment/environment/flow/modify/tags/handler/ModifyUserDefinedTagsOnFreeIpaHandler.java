package com.sequenceiq.environment.environment.flow.modify.tags.handler;

import static com.sequenceiq.environment.environment.EnvironmentStatus.USER_DEFINED_TAGS_MODIFICATION_ON_FREEIPA_FAILED;
import static com.sequenceiq.environment.environment.flow.modify.tags.event.EnvTagsModificationHandlerSelectors.MODIFY_USER_DEFINED_TAGS_ON_FREEIPA_EVENT;
import static com.sequenceiq.environment.environment.flow.modify.tags.event.EnvTagsModificationStateSelectors.START_MODIFY_USER_DEFINED_TAGS_DATALAKE_EVENT;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.environment.environment.flow.modify.tags.EnvTagsModificationSupport;
import com.sequenceiq.environment.environment.flow.modify.tags.event.EnvTagsModificationEvent;
import com.sequenceiq.environment.environment.flow.modify.tags.event.EnvTagsModificationFailureEvent;
import com.sequenceiq.environment.environment.service.freeipa.FreeIpaPollerService;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class ModifyUserDefinedTagsOnFreeIpaHandler extends ExceptionCatcherEventHandler<EnvTagsModificationEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ModifyUserDefinedTagsOnFreeIpaHandler.class);

    private final FreeIpaPollerService freeIpaPollerService;

    public ModifyUserDefinedTagsOnFreeIpaHandler(FreeIpaPollerService freeIpaPollerService) {
        this.freeIpaPollerService = freeIpaPollerService;
    }

    @Override
    public String selector() {
        return MODIFY_USER_DEFINED_TAGS_ON_FREEIPA_EVENT.selector();
    }

    @Override
    protected Selectable doAccept(HandlerEvent<EnvTagsModificationEvent> event) {
        EnvTagsModificationEvent data = event.getData();
        try {
            freeIpaPollerService.waitForUserDefinedTagsModification(
                    data.getResourceId(), data.getResourceCrn(), data.getUserDefinedTags(), data.getTagsToRemove());
        } catch (Exception e) {
            LOGGER.warn("Modify user defined tags on FreeIPA failed.", e);
            return new EnvTagsModificationFailureEvent(
                    data.getResourceId(), data.getResourceName(), data.getResourceCrn(), USER_DEFINED_TAGS_MODIFICATION_ON_FREEIPA_FAILED, e);
        }
        return EnvTagsModificationSupport.nextEvent(data, START_MODIFY_USER_DEFINED_TAGS_DATALAKE_EVENT.name());
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<EnvTagsModificationEvent> event) {
        LOGGER.warn("Modify user defined tags on FreeIPA failed.", e);
        EnvTagsModificationEvent data = event.getData();
        return new EnvTagsModificationFailureEvent(
                data.getResourceId(), data.getResourceName(), data.getResourceCrn(), USER_DEFINED_TAGS_MODIFICATION_ON_FREEIPA_FAILED, e);
    }
}
