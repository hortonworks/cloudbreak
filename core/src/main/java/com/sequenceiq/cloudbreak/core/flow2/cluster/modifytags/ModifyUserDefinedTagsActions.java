package com.sequenceiq.cloudbreak.core.flow2.cluster.modifytags;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.modifytags.event.ModifyUserDefinedTagsStateSelectors.HANDLED_FAILED_MODIFY_USER_DEFINED_TAGS_EVENT;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.STACK_UPDATE_USER_DEFINED_TAGS;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.STACK_UPDATE_USER_DEFINED_TAGS_COMPLETE;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.STACK_UPDATE_USER_DEFINED_TAGS_FAILED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.STACK_UPDATE_USER_DEFINED_TAGS_ON_CLOUD_RESOURCES;

import java.util.Map;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.core.flow2.cluster.modifytags.event.ModifyUserDefinedTagsCloudResourcesHandlerEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.modifytags.event.ModifyUserDefinedTagsEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.modifytags.event.ModifyUserDefinedTagsFailedEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.modifytags.event.ModifyUserDefinedTagsStackHandlerEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.modifytags.event.ModifyUserDefinedTagsStateSelectors;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackContext;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.cloudbreak.service.StackUpdater;

/**
 * Updates the user-defined tags of the Data Lake and Data Hub stacks on an environment, both on the cloud resources
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
    private CloudbreakFlowMessageService flowMessageService;

    @Bean(name = "MODIFY_USER_DEFINED_TAGS_CLOUD_RESOURCES_STATE")
    public Action<?, ?> modifyCloudResourcesAction() {
        return new AbstractModifyUserDefinedTagsAction<>(ModifyUserDefinedTagsEvent.class) {
            @Override
            protected void doExecute(StackContext context, ModifyUserDefinedTagsEvent payload, Map<Object, Object> variables) {
                LOGGER.debug("Update user defined tags for cloud resources of stack {}", payload);
                StackDtoDelegate stack = context.getStack();
                stackUpdater.updateStackStatus(stack.getId(), DetailedStackStatus.USER_DEFINED_TAGS_UPDATE_IN_PROGRESS);
                flowMessageService.fireEventAndLog(payload.getResourceId(),
                        STACK_UPDATE_USER_DEFINED_TAGS_ON_CLOUD_RESOURCES.name(),
                        STACK_UPDATE_USER_DEFINED_TAGS_ON_CLOUD_RESOURCES,
                        String.valueOf(stack.getName()));
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
            protected void doExecute(StackContext context, ModifyUserDefinedTagsEvent payload, Map<Object, Object> variables) {
                LOGGER.debug("Update user defined tags for stack {}", payload);
                StackDtoDelegate stack = context.getStack();
                flowMessageService.fireEventAndLog(payload.getResourceId(),
                        STACK_UPDATE_USER_DEFINED_TAGS.name(),
                        STACK_UPDATE_USER_DEFINED_TAGS,
                        String.valueOf(stack.getName()));
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
            protected void doExecute(StackContext context, ModifyUserDefinedTagsEvent payload, Map<Object, Object> variables) {
                LOGGER.debug("Update user defined tags for stack finished successfully {}", payload);
                StackDtoDelegate stack = context.getStack();
                stackUpdater.updateStackStatus(stack.getId(), DetailedStackStatus.USER_DEFINED_TAGS_UPDATE_COMPLETE);
                flowMessageService.fireEventAndLog(payload.getResourceId(),
                        STACK_UPDATE_USER_DEFINED_TAGS_COMPLETE.name(),
                        STACK_UPDATE_USER_DEFINED_TAGS_COMPLETE,
                        String.valueOf(stack.getName()));
                String selector = ModifyUserDefinedTagsStateSelectors.FINALIZE_MODIFY_USER_DEFINED_TAGS_EVENT.event();
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
            protected void doExecute(StackContext context, ModifyUserDefinedTagsFailedEvent payload, Map<Object, Object> variables) {
                LOGGER.error("Update user defined tags for stack failed {}", payload);
                StackDtoDelegate stack = context.getStack();
                stackUpdater.updateStackStatus(stack.getId(), DetailedStackStatus.USER_DEFINED_TAGS_UPDATE_FAILED, payload.getException().getMessage());
                flowMessageService.fireEventAndLog(payload.getResourceId(),
                        STACK_UPDATE_USER_DEFINED_TAGS_FAILED.name(),
                        STACK_UPDATE_USER_DEFINED_TAGS_FAILED,
                        String.valueOf(stack.getName()));
                sendEvent(context, HANDLED_FAILED_MODIFY_USER_DEFINED_TAGS_EVENT.event(), payload);
            }
        };
    }
}
