package com.sequenceiq.environment.environment.flow.modify.tags.handler;

import static com.sequenceiq.environment.environment.EnvironmentStatus.USER_DEFINED_TAGS_MODIFICATION_ON_DATAHUBS_FAILED;
import static com.sequenceiq.environment.environment.flow.modify.tags.event.EnvTagsModificationHandlerSelectors.MODIFY_USER_DEFINED_TAGS_ON_DATAHUBS_EVENT;
import static com.sequenceiq.environment.environment.flow.modify.tags.event.EnvTagsModificationStateSelectors.FINISH_MODIFY_USER_DEFINED_TAGS_EVENT;

import java.util.Map;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.environment.environment.flow.modify.tags.event.EnvTagsModificationEvent;
import com.sequenceiq.environment.environment.flow.modify.tags.event.EnvTagsModificationFailureEvent;
import com.sequenceiq.environment.environment.service.datahub.DatahubService;
import com.sequenceiq.environment.environment.service.stack.StackService;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class ModifyUserDefinedTagsOnDatahubsHandler extends ExceptionCatcherEventHandler<EnvTagsModificationEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ModifyUserDefinedTagsOnDatahubsHandler.class);

    @Inject
    private DatahubService datahubService;

    @Inject
    private StackService stackService;

    @Override
    public String selector() {
        return MODIFY_USER_DEFINED_TAGS_ON_DATAHUBS_EVENT.selector();
    }

    @Override
    protected Selectable doAccept(HandlerEvent<EnvTagsModificationEvent> event) {
        Long resourceId = event.getData().getResourceId();
        String resourceName = event.getData().getResourceName();
        String resourceCrn = event.getData().getResourceCrn();
        Map<String, String> userDefinedTags = event.getData().getUserDefinedTags();
        try {
            datahubService.list(resourceCrn)
                    .getResponses()
                    .forEach(stackView -> stackService.modifyUserDefinedTags(stackView.getCrn(), userDefinedTags));
        } catch (Exception e) {
            LOGGER.error("Modify user defined tags on Data Hubs failed.", e);
            return new EnvTagsModificationFailureEvent(resourceId, resourceName, resourceCrn, USER_DEFINED_TAGS_MODIFICATION_ON_DATAHUBS_FAILED, e);
        }
        return EnvTagsModificationEvent.builder()
                .withSelector(FINISH_MODIFY_USER_DEFINED_TAGS_EVENT.name())
                .withResourceId(resourceId)
                .withResourceName(resourceName)
                .withResourceCrn(resourceCrn)
                .withUserDefinedTags(userDefinedTags)
                .build();
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<EnvTagsModificationEvent> event) {
        LOGGER.error("Modify user defined tags on datahubs failed.", e);
        String resourceName = event.getData().getResourceName();
        String resourceCrn = event.getData().getResourceCrn();
        return new EnvTagsModificationFailureEvent(resourceId, resourceName, resourceCrn, USER_DEFINED_TAGS_MODIFICATION_ON_DATAHUBS_FAILED, e);
    }
}