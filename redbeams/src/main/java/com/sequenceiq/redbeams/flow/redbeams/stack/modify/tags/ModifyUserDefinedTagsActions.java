package com.sequenceiq.redbeams.flow.redbeams.stack.modify.tags;

import static com.sequenceiq.redbeams.flow.redbeams.stack.modify.tags.event.ModifyUserDefinedTagsStateSelectors.HANDLED_FAILED_MODIFY_USER_DEFINED_TAGS_REDBEAMS_EVENT;

import java.util.Map;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.redbeams.api.model.common.DetailedDBStackStatus;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsContext;
import com.sequenceiq.redbeams.flow.redbeams.stack.modify.tags.event.ModifyUserDefinedTagsCloudResourcesHandlerEvent;
import com.sequenceiq.redbeams.flow.redbeams.stack.modify.tags.event.ModifyUserDefinedTagsEvent;
import com.sequenceiq.redbeams.flow.redbeams.stack.modify.tags.event.ModifyUserDefinedTagsFailedEvent;
import com.sequenceiq.redbeams.flow.redbeams.stack.modify.tags.event.ModifyUserDefinedTagsStackHandlerEvent;
import com.sequenceiq.redbeams.flow.redbeams.stack.modify.tags.event.ModifyUserDefinedTagsStateSelectors;
import com.sequenceiq.redbeams.service.stack.DBStackStatusUpdater;

/**
 * Updates the user-defined tags of the DB stack on an environment, both on the cloud resources
 * and in the database.
 *
 * <p>Cloud resources are updated first. If that step fails, the database is left
 * unchanged and the exception is propagated to the caller, failing the flow.
 *
 * <p>This operation is not atomic across the two steps. If the cloud resource update
 * succeeds but the subsequent database update fails, the cloud resources will already
 * carry the new tags while the database still reflects the old state.
 * Full consistency is achieved through eventual consistency: the flow can be re-run,
 * and the cloud resource tag updates are effectively idempotent since re-applying
 * the same tags overwrites them with identical values.
 */
@Configuration
public class ModifyUserDefinedTagsActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(ModifyUserDefinedTagsActions.class);

    @Inject
    private DBStackStatusUpdater stackUpdater;

    @Bean(name = "MODIFY_USER_DEFINED_TAGS_CLOUD_RESOURCES_STATE")
    public Action<?, ?> modifyCloudResourcesAction() {
        return new AbstractModifyUserDefinedTagsAction<>(ModifyUserDefinedTagsEvent.class) {
            @Override
            protected void doExecute(RedbeamsContext context, ModifyUserDefinedTagsEvent payload, Map<Object, Object> variables) {
                LOGGER.debug("Update user defined tags for external database's cloud resources {}", payload);
                DBStack stack = context.getDBStack();
                stackUpdater.updateStatus(stack.getId(), DetailedDBStackStatus.MODIFY_USER_DEFINED_TAGS_IN_PROGRESS);
                ModifyUserDefinedTagsCloudResourcesHandlerEvent modifyUserDefinedTagsCloudResourcesEvent =
                        new ModifyUserDefinedTagsCloudResourcesHandlerEvent(payload.getResourceId(), payload.getUserDefinedTags());
                sendEvent(context, modifyUserDefinedTagsCloudResourcesEvent);
            }
        };
    }

    @Bean(name = "MODIFY_USER_DEFINED_TAGS_STACK_STATE")
    public Action<?, ?> modifyStackAction() {
        return new AbstractModifyUserDefinedTagsAction<>(ModifyUserDefinedTagsEvent.class) {
            @Override
            protected void doExecute(RedbeamsContext context, ModifyUserDefinedTagsEvent payload, Map<Object, Object> variables) {
                LOGGER.debug("Update user defined tags for external database {}", payload);
                ModifyUserDefinedTagsStackHandlerEvent modifyUserDefinedTagsStackEvent =
                        new ModifyUserDefinedTagsStackHandlerEvent(payload.getResourceId(), payload.getUserDefinedTags());
                sendEvent(context, modifyUserDefinedTagsStackEvent);
            }
        };
    }

    @Bean(name = "MODIFY_USER_DEFINED_TAGS_FINISHED_STATE")
    public Action<?, ?> finishedAction() {
        return new AbstractModifyUserDefinedTagsAction<>(ModifyUserDefinedTagsEvent.class) {
            @Override
            protected void doExecute(RedbeamsContext context, ModifyUserDefinedTagsEvent payload, Map<Object, Object> variables) {
                LOGGER.debug("Update user defined tags for external database finished successfully {}", payload);
                DBStack stack = context.getDBStack();
                stackUpdater.updateStatus(stack.getId(), DetailedDBStackStatus.MODIFY_USER_DEFINED_TAGS_COMPLETED);
                String selector = ModifyUserDefinedTagsStateSelectors.FINALIZE_MODIFY_USER_DEFINED_TAGS_REDBEAMS_EVENT.event();
                ModifyUserDefinedTagsEvent finalizeEvent =
                        new ModifyUserDefinedTagsEvent(selector, payload.getResourceId(), payload.getUserDefinedTags());
                sendEvent(context, selector, finalizeEvent);
            }
        };
    }

    @Bean(name = "MODIFY_USER_DEFINED_TAGS_FAILED_STATE")
    public Action<?, ?> failedAction() {
        return new AbstractModifyUserDefinedTagsAction<>(ModifyUserDefinedTagsFailedEvent.class) {

            @Override
            protected void doExecute(RedbeamsContext context, ModifyUserDefinedTagsFailedEvent payload, Map<Object, Object> variables) {
                LOGGER.error("Update user defined tags for external database failed {}", payload);
                DBStack stack = context.getDBStack();
                String errorReason = payload.getException() == null ? "Unknown error" : payload.getException().getMessage();
                stackUpdater.updateStatus(stack.getId(), DetailedDBStackStatus.MODIFY_USER_DEFINED_TAGS_FAILED, errorReason);
                sendEvent(context, HANDLED_FAILED_MODIFY_USER_DEFINED_TAGS_REDBEAMS_EVENT.event(), payload);
            }
        };
    }
}
