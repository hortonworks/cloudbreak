package com.sequenceiq.cloudbreak.core.flow2.cluster.encryptionprofile;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.encryptionprofile.EnableEncryptionProfileOnClusterStateSelectors.GENERATE_ALTERNATIVE_CERTIFICATE_HANDLER_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.encryptionprofile.EnableEncryptionProfileOnClusterStateSelectors.SET_ENCRYPTION_PROFILE_HANDLER_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.encryptionprofile.EnableEncryptionProfileOnClusterStateSelectors.UPDATE_CM_POLICY_HANDLER_EVENT;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.ENABLE_ENCRYPTION_PROFILE_FAILED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.ENABLE_ENCRYPTION_PROFILE_FINISHED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.ENABLE_ENCRYPTION_PROFILE_STARTED;

import java.util.Map;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.core.flow2.cluster.encryptionprofile.event.EnableEncryptionProfileFailedEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.encryptionprofile.event.EnableEncryptionProfileOnClusterEvent;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackContext;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.cloudbreak.service.StackUpdater;

@Configuration
public class EnableEncryptionProfileOnClusterActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnableEncryptionProfileOnClusterActions.class);

    @Inject
    private StackUpdater stackUpdater;

    @Inject
    private CloudbreakFlowMessageService flowMessageService;

    @Bean(name = "SET_ENCRYPTION_PROFILE_STATE")
    public Action<?, ?> setEncryptionProfileEventAction() {
        return new AbstractEnableEncryptionProfileOnClusterAction<>(EnableEncryptionProfileOnClusterEvent.class) {
            @Override
            protected void doExecute(StackContext context, EnableEncryptionProfileOnClusterEvent payload, Map<Object, Object> variables) {
                StackDtoDelegate stack = context.getStack();
                LOGGER.info("Setting encryption profile on cluster for stack {}", stack.getName());
                stackUpdater.updateStackStatus(stack.getId(), DetailedStackStatus.ENABLE_ENCRYPTION_PROFILE_ON_CLUSTER_IN_PROGRESS,
                        "Enabling encryption profile on cluster");
                flowMessageService.fireEventAndLog(payload.getResourceId(),
                        ENABLE_ENCRYPTION_PROFILE_STARTED.name(),
                        ENABLE_ENCRYPTION_PROFILE_STARTED);
                EnableEncryptionProfileOnClusterEvent updateEvent = new EnableEncryptionProfileOnClusterEvent(SET_ENCRYPTION_PROFILE_HANDLER_EVENT.name(),
                        context.getStack().getId(),
                        payload.getEncryptionProfileCrn());
                sendEvent(context, updateEvent);
            }
        };
    }

    @Bean(name = "UPDATE_CM_POLICY_STATE")
    public Action<?, ?> updateClouderaManagerPolicyAction() {
        return new AbstractEnableEncryptionProfileOnClusterAction<>(EnableEncryptionProfileOnClusterEvent.class) {
            @Override
            protected void doExecute(StackContext context, EnableEncryptionProfileOnClusterEvent payload, Map<Object, Object> variables) {
                LOGGER.debug("Updating cloudera manager policy {}", payload);
                EnableEncryptionProfileOnClusterEvent updateEvent =
                        new EnableEncryptionProfileOnClusterEvent(UPDATE_CM_POLICY_HANDLER_EVENT.name(), context.getStack().getId(),
                                payload.getEncryptionProfileCrn());
                sendEvent(context, updateEvent);
            }
        };
    }

    @Bean(name = "GENERATE_ALTERNATIVE_CERTIFICATE_STATE")
    public Action<?, ?> generateAlternativeCertificateAction() {
        return new AbstractEnableEncryptionProfileOnClusterAction<>(EnableEncryptionProfileOnClusterEvent.class) {
            @Override
            protected void doExecute(StackContext context, EnableEncryptionProfileOnClusterEvent payload, Map<Object, Object> variables) {
                LOGGER.debug("Generating alternative certificate {}", payload);
                EnableEncryptionProfileOnClusterEvent updateEvent =
                        new EnableEncryptionProfileOnClusterEvent(GENERATE_ALTERNATIVE_CERTIFICATE_HANDLER_EVENT.name(), context.getStack().getId(),
                        payload.getEncryptionProfileCrn());
                sendEvent(context, updateEvent);
            }
        };
    }

    @Bean(name = "ENABLE_ENCRYPTION_PROFILE_ON_CLUSTER_FINISHED_STATE")
    public Action<?, ?> finishedAction() {
        return new AbstractEnableEncryptionProfileOnClusterAction<>(EnableEncryptionProfileOnClusterEvent.class) {
            @Override
            protected void doExecute(StackContext context, EnableEncryptionProfileOnClusterEvent payload, Map<Object, Object> variables) {
                StackDtoDelegate stack = context.getStack();
                LOGGER.info("Encryption profile enabled on stack {}", stack.getName());
                stackUpdater.updateStackStatus(stack.getId(), DetailedStackStatus.ENABLE_ENCRYPTION_PROFILE_ON_CLUSTER_COMPLETE,
                        "Encryption profile enabled on cluster");
                flowMessageService.fireEventAndLog(payload.getResourceId(),
                        ENABLE_ENCRYPTION_PROFILE_FINISHED.name(),
                        ENABLE_ENCRYPTION_PROFILE_FINISHED);
                EnableEncryptionProfileOnClusterEvent finalizeEvent = new EnableEncryptionProfileOnClusterEvent(
                        EnableEncryptionProfileOnClusterStateSelectors.FINALIZE_ENABLE_ENCRYPTION_PROFILE_ON_CLUSTER_EVENT.selector(),
                        stack.getId(), payload.getEncryptionProfileCrn());
                sendEvent(context, finalizeEvent);
            }
        };
    }

    @Bean(name = "ENABLE_ENCRYPTION_PROFILE_ON_CLUSTER_FAILED_STATE")
    public Action<?, ?> failedAction() {
        return new AbstractEnableEncryptionProfileOnClusterAction<>(EnableEncryptionProfileFailedEvent.class) {

            @Override
            protected void doExecute(StackContext context, EnableEncryptionProfileFailedEvent payload, Map<Object, Object> variables) {
                LOGGER.error("Failed to enable encryption profile on Stack '{}'.", payload.getResourceId(), payload.getException());
                StackDtoDelegate stack = context.getStack();
                stackUpdater.updateStackStatus(stack.getId(), DetailedStackStatus.ENABLE_ENCRYPTION_PROFILE_ON_CLUSTER_FAILED,
                        payload.getException().getMessage());
                flowMessageService.fireEventAndLog(payload.getResourceId(),
                        ENABLE_ENCRYPTION_PROFILE_FAILED.name(),
                        ENABLE_ENCRYPTION_PROFILE_FAILED,
                        payload.getException() != null ? payload.getException().getMessage() : "unknown");
                EnableEncryptionProfileFailedEvent failedEvent = new EnableEncryptionProfileFailedEvent(
                        EnableEncryptionProfileOnClusterStateSelectors.HANDLED_FAILED_ENABLE_ENCRYPTION_PROFILE_ON_CLUSTER_EVENT.event(),
                        payload.getResourceId(), payload.getException());
                sendEvent(context, EnableEncryptionProfileOnClusterStateSelectors.HANDLED_FAILED_ENABLE_ENCRYPTION_PROFILE_ON_CLUSTER_EVENT.event(),
                        failedEvent);
            }
        };
    }

}
