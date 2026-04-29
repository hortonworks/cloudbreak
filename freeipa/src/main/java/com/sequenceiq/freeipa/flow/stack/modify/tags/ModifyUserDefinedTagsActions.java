package com.sequenceiq.freeipa.flow.stack.modify.tags;

import static com.sequenceiq.freeipa.flow.stack.modify.tags.event.ModifyUserDefinedTagsStateSelectors.HANDLED_FAILED_MODIFY_USER_DEFINED_TAGS_FREEIPA_EVENT;

import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SuccessDetails;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.stack.StackContext;
import com.sequenceiq.freeipa.flow.stack.modify.tags.event.ModifyUserDefinedTagsCloudResourcesHandlerEvent;
import com.sequenceiq.freeipa.flow.stack.modify.tags.event.ModifyUserDefinedTagsEvent;
import com.sequenceiq.freeipa.flow.stack.modify.tags.event.ModifyUserDefinedTagsFailedEvent;
import com.sequenceiq.freeipa.flow.stack.modify.tags.event.ModifyUserDefinedTagsStackHandlerEvent;
import com.sequenceiq.freeipa.flow.stack.modify.tags.event.ModifyUserDefinedTagsStateSelectors;
import com.sequenceiq.freeipa.service.operation.OperationService;
import com.sequenceiq.freeipa.service.stack.StackUpdater;

/**
 * Updates the user-defined tags of the FreeIPA stack on an environment, both on the cloud resources
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
    private StackUpdater stackUpdater;

    @Inject
    private OperationService operationService;

    @Bean(name = "MODIFY_USER_DEFINED_TAGS_CLOUD_RESOURCES_STATE")
    public Action<?, ?> modifyCloudResourcesAction() {
        return new AbstractModifyUserDefinedTagsAction<>(ModifyUserDefinedTagsEvent.class) {
            @Override
            protected void doExecute(StackContext context, ModifyUserDefinedTagsEvent payload, Map<Object, Object> variables) {
                LOGGER.debug("Update user defined tags for FreeIPA's cloud resources {}", payload);
                stackUpdater.updateStackStatus(context.getStack(), DetailedStackStatus.UPDATE_IN_PROGRESS,
                        "Starting to update user defined tags on cloud resources of FreeIPA: " + context.getStack().getName());
                ModifyUserDefinedTagsCloudResourcesHandlerEvent modifyUserDefinedTagsCloudResourcesEvent =
                        new ModifyUserDefinedTagsCloudResourcesHandlerEvent(payload.getResourceId(), payload.getOperationId(), payload.getUserDefinedTags());
                sendEvent(context, modifyUserDefinedTagsCloudResourcesEvent);
            }
        };
    }

    @Bean(name = "MODIFY_USER_DEFINED_TAGS_STACK_STATE")
    public Action<?, ?> modifyStackAction() {
        return new AbstractModifyUserDefinedTagsAction<>(ModifyUserDefinedTagsEvent.class) {
            @Override
            protected void doExecute(StackContext context, ModifyUserDefinedTagsEvent payload, Map<Object, Object> variables) {
                LOGGER.debug("Update user defined tags for FreeIPA {}", payload);
                stackUpdater.updateStackStatus(context.getStack(), DetailedStackStatus.UPDATE_IN_PROGRESS,
                        "Starting to update user defined tags on FreeIPA stack: " + context.getStack().getName());
                setOperationId(variables, payload.getOperationId());
                ModifyUserDefinedTagsStackHandlerEvent modifyUserDefinedTagsStackEvent =
                        new ModifyUserDefinedTagsStackHandlerEvent(payload.getResourceId(), payload.getOperationId(), payload.getUserDefinedTags());
                sendEvent(context, modifyUserDefinedTagsStackEvent);
            }
        };
    }

    @Bean(name = "MODIFY_USER_DEFINED_TAGS_FINISHED_STATE")
    public Action<?, ?> finishedAction() {
        return new AbstractModifyUserDefinedTagsAction<>(ModifyUserDefinedTagsEvent.class) {
            @Override
            protected void doExecute(StackContext context, ModifyUserDefinedTagsEvent payload, Map<Object, Object> variables) {
                LOGGER.debug("Update user defined tags for FreeIPA finished successfully {}", payload);
                Stack stack = context.getStack();
                stackUpdater.updateStackStatus(context.getStack(), DetailedStackStatus.UPDATE_COMPLETE,
                        "Update user defined tags finished for FreeIPA: " + stack.getName());
                SuccessDetails successDetails = new SuccessDetails(stack.getEnvironmentCrn());
                operationService.completeOperation(stack.getAccountId(), payload.getOperationId(), Set.of(successDetails), Set.of());
                String selector = ModifyUserDefinedTagsStateSelectors.FINALIZE_MODIFY_USER_DEFINED_TAGS_FREEIPA_EVENT.event();
                ModifyUserDefinedTagsEvent finalizeEvent =
                        new ModifyUserDefinedTagsEvent(selector, payload.getResourceId(), payload.getOperationId(), payload.getUserDefinedTags());
                sendEvent(context, selector, finalizeEvent);
            }
        };
    }

    @Bean(name = "MODIFY_USER_DEFINED_TAGS_FAILED_STATE")
    public Action<?, ?> failedAction() {
        return new AbstractModifyUserDefinedTagsAction<>(ModifyUserDefinedTagsFailedEvent.class) {

            @Override
            protected void doExecute(StackContext context, ModifyUserDefinedTagsFailedEvent payload, Map<Object, Object> variables) {
                LOGGER.error("Update user defined tags for FreeIPA failed {}", payload);
                Stack stack = context.getStack();
                String message = String.format("Update user defined tags failed for FreeIPA: %s at phase: %s", stack.getName(), payload.getFailedPhase());
                String errorReason = getErrorReason(payload.getException());
                stackUpdater.updateStackStatus(context.getStack(), DetailedStackStatus.UPDATE_FAILED, errorReason);
                operationService.failOperation(stack.getAccountId(), getOperationId(variables), message, List.of(), List.of());
                enableStatusChecker(stack, "Update user defined tags failed for FreeIPA.");
                sendEvent(context, HANDLED_FAILED_MODIFY_USER_DEFINED_TAGS_FREEIPA_EVENT.event(), payload);
            }
        };
    }
}
