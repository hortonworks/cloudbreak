package com.sequenceiq.cloudbreak.core.flow2.cluster.encryptionprofile;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.encryptionprofile.UpdateSslConfigsOnClusterStateSelectors.GENERATE_ALTERNATIVE_CERTIFICATE_HANDLER_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.encryptionprofile.UpdateSslConfigsOnClusterStateSelectors.SET_ENCRYPTION_PROFILE_HANDLER_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.encryptionprofile.UpdateSslConfigsOnClusterStateSelectors.UPDATE_CM_POLICY_HANDLER_EVENT;
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
import com.sequenceiq.cloudbreak.core.flow2.cluster.encryptionprofile.event.UpdateSslConfigEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.encryptionprofile.event.UpdateSslConfigFailedEvent;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackContext;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.cloudbreak.service.StackUpdater;

@Configuration
public class UpdateSslConfigsOnClusterActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateSslConfigsOnClusterActions.class);

    @Inject
    private StackUpdater stackUpdater;

    @Inject
    private CloudbreakFlowMessageService flowMessageService;

    @Bean(name = "SET_ENCRYPTION_PROFILE_STATE")
    public Action<?, ?> setEncryptionProfileEventAction() {
        return new AbstractUpdateSslConfigsOnClusterAction<>(UpdateSslConfigEvent.class) {
            @Override
            protected void doExecute(StackContext context, UpdateSslConfigEvent payload, Map<Object, Object> variables) {
                LOGGER.debug("Updating encryption profile {}", payload);
                StackDtoDelegate stack = context.getStack();
                stackUpdater.updateStackStatus(stack.getId(), DetailedStackStatus.UPDATE_SSL_CONFIG_ON_CLUSTER_IN_PROGRESS,
                        "Starting to update SSL configs");
                flowMessageService.fireEventAndLog(payload.getResourceId(),
                        ENABLE_ENCRYPTION_PROFILE_STARTED.name(),
                        ENABLE_ENCRYPTION_PROFILE_STARTED,
                        context.getStack().getType().getResourceType(),
                        String.valueOf(payload.getResourceId()));
                UpdateSslConfigEvent updateSslConfigEvent = new UpdateSslConfigEvent(SET_ENCRYPTION_PROFILE_HANDLER_EVENT.name(), context.getStack().getId(),
                        payload.getEncryptionProfileCrn());
                sendEvent(context, updateSslConfigEvent);
            }
        };
    }

    @Bean(name = "UPDATE_CM_POLICY_STATE")
    public Action<?, ?> updateClouderaManagerPolicyAction() {
        return new AbstractUpdateSslConfigsOnClusterAction<>(UpdateSslConfigEvent.class) {
            @Override
            protected void doExecute(StackContext context, UpdateSslConfigEvent payload, Map<Object, Object> variables) {
                LOGGER.debug("Updating cloudera manager policy {}", payload);
                UpdateSslConfigEvent updateSslConfigEvent =
                        new UpdateSslConfigEvent(UPDATE_CM_POLICY_HANDLER_EVENT.name(), context.getStack().getId(),
                                payload.getEncryptionProfileCrn());
                sendEvent(context, updateSslConfigEvent);
            }
        };
    }

    @Bean(name = "GENERATE_ALTERNATIVE_CERTIFICATE_STATE")
    public Action<?, ?> generateAlternativeCertificateAction() {
        return new AbstractUpdateSslConfigsOnClusterAction<>(UpdateSslConfigEvent.class) {
            @Override
            protected void doExecute(StackContext context, UpdateSslConfigEvent payload, Map<Object, Object> variables) {
                LOGGER.debug("Generating alternative certificate {}", payload);
                UpdateSslConfigEvent updateSslConfigEvent =
                        new UpdateSslConfigEvent(GENERATE_ALTERNATIVE_CERTIFICATE_HANDLER_EVENT.name(), context.getStack().getId(),
                        payload.getEncryptionProfileCrn());
                sendEvent(context, updateSslConfigEvent);
            }
        };
    }

    @Bean(name = "UPDATE_SSL_CONFIGS_ON_CLUSTER_FINISHED_STATE")
    public Action<?, ?> finishedAction() {
        return new AbstractUpdateSslConfigsOnClusterAction<>(UpdateSslConfigEvent.class) {
            @Override
            protected void doExecute(StackContext context, UpdateSslConfigEvent payload, Map<Object, Object> variables) {
                StackDtoDelegate stack = context.getStack();
                LOGGER.debug("SSL configs for stack {} updated", stack.getName());
                stackUpdater.updateStackStatus(stack.getId(), DetailedStackStatus.UPDATE_SSL_CONFIG_ON_CLUSTER_COMPLETE,
                        "SSL configs updated");
                flowMessageService.fireEventAndLog(payload.getResourceId(),
                        ENABLE_ENCRYPTION_PROFILE_FINISHED.name(),
                        ENABLE_ENCRYPTION_PROFILE_FINISHED,
                        stack.getType().getResourceType(),
                        String.valueOf(payload.getResourceId()));
                UpdateSslConfigEvent finalizeEvent = new UpdateSslConfigEvent(
                        UpdateSslConfigsOnClusterStateSelectors.FINALIZE_UPDATE_SSL_CONFIGS_ON_CLUSTER_EVENT.selector(),
                        stack.getId(), payload.getEncryptionProfileCrn());
                sendEvent(context, finalizeEvent);
            }
        };
    }

    @Bean(name = "UPDATE_SSL_CONFIGS_ON_CLUSTER_FAILED_STATE")
    public Action<?, ?> failedAction() {
        return new AbstractUpdateSslConfigsOnClusterAction<>(UpdateSslConfigFailedEvent.class) {

            @Override
            protected void doExecute(StackContext context, UpdateSslConfigFailedEvent payload, Map<Object, Object> variables) {
                LOGGER.error("Failed to update SSL configs on Stack '{}'.", payload.getResourceId(), payload.getException());
                StackDtoDelegate stack = context.getStack();
                stackUpdater.updateStackStatus(stack.getId(), DetailedStackStatus.UPDATE_SSL_CONFIG_ON_CLUSTER_FAILED, payload.getException().getMessage());
                flowMessageService.fireEventAndLog(payload.getResourceId(),
                        ENABLE_ENCRYPTION_PROFILE_FAILED.name(),
                        ENABLE_ENCRYPTION_PROFILE_FAILED,
                        String.valueOf(payload.getResourceId()));
                UpdateSslConfigFailedEvent failedEvent = new UpdateSslConfigFailedEvent(
                        UpdateSslConfigsOnClusterStateSelectors.HANDLED_FAILED_UPDATE_SSL_CONFIGS_ON_CLUSTER_EVENT.event(),
                        payload.getResourceId(), payload.getException());
                sendEvent(context, UpdateSslConfigsOnClusterStateSelectors.HANDLED_FAILED_UPDATE_SSL_CONFIGS_ON_CLUSTER_EVENT.event(), failedEvent);
            }
        };
    }

}