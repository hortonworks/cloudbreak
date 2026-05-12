package com.sequenceiq.environment.environment.flow.encryptionprofile;

import static com.sequenceiq.cloudbreak.event.ResourceEvent.ENABLE_ENCRYPTION_PROFILE_FINISHED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.ENABLE_ENCRYPTION_PROFILE_STARTED;
import static com.sequenceiq.environment.environment.EnvironmentStatus.ENABLE_ENCRYPTION_PROFILE_IN_PROGRESS;
import static com.sequenceiq.environment.environment.flow.encryptionprofile.EnabledEncryptionProfileState.ENABLE_ENCRYPTION_PROFILE_FAILED_STATE;
import static com.sequenceiq.environment.environment.flow.encryptionprofile.EnabledEncryptionProfileState.ENABLE_ENCRYPTION_PROFILE_FINISHED_STATE;
import static com.sequenceiq.environment.environment.flow.encryptionprofile.EnabledEncryptionProfileState.VALIDATE_ENABLE_ENCRYPTION_PROFILE_STATE;
import static com.sequenceiq.environment.environment.flow.encryptionprofile.event.EnableEncryptionProfileStateSelectors.FINALIZE_ENABLE_ENCRYPTION_PROFILE_EVENT;
import static com.sequenceiq.environment.environment.flow.encryptionprofile.event.EnableEncryptionProfileStateSelectors.HANDLED_FAILED_ENABLE_ENCRYPTION_PROFILE_EVENT;
import static com.sequenceiq.environment.environment.flow.encryptionprofile.event.EnableEncryptionProfileStateSelectors.SET_ENCRYPTION_PROFILE_HANDLER_EVENT;
import static com.sequenceiq.environment.environment.flow.encryptionprofile.event.EnableEncryptionProfileStateSelectors.UPDATE_SSL_CONFIG_FREEIPA_HANDLER_EVENT;
import static com.sequenceiq.environment.environment.flow.encryptionprofile.event.EnableEncryptionProfileStateSelectors.UPDATE_SSL_CONFIG_IN_CLUSTERS_HANDLER_EVENT;
import static com.sequenceiq.environment.environment.flow.encryptionprofile.event.EnableEncryptionProfileStateSelectors.VALIDATE_ENABLE_ENCRYPTION_PROFILE_HANDLER_EVENT;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.flow.encryptionprofile.event.EnableEncryptionProfileEvent;
import com.sequenceiq.environment.environment.flow.encryptionprofile.event.EnableEncryptionProfileFailedEvent;
import com.sequenceiq.environment.environment.flow.encryptionprofile.event.EnableEncryptionProfileStateSelectors;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.environment.environment.service.EnvironmentStatusUpdateService;
import com.sequenceiq.flow.core.CommonContext;
import com.sequenceiq.flow.core.Flow;
import com.sequenceiq.flow.core.FlowParameters;

@Configuration
public class EnableEncryptionProfileActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnableEncryptionProfileActions.class);

    private final EnvironmentStatusUpdateService environmentStatusUpdateService;

    private final EnvironmentService environmentService;

    public EnableEncryptionProfileActions(
            EnvironmentStatusUpdateService environmentStatusUpdateService, EnvironmentService environmentService) {
        this.environmentStatusUpdateService = environmentStatusUpdateService;
        this.environmentService = environmentService;
    }

    @Bean(name = "VALIDATE_ENABLE_ENCRYPTION_PROFILE_STATE")
    public Action<?, ?> validateEnableEncryptionProfileAction() {
        return new AbstractEnableEncryptionProfileActions<>(EnableEncryptionProfileEvent.class) {
            @Override
            protected void doExecute(CommonContext context, EnableEncryptionProfileEvent payload, Map<Object, Object> variables) {
                environmentStatusUpdateService
                        .updateEnvironmentStatusAndNotify(
                                context,
                                payload,
                                ENABLE_ENCRYPTION_PROFILE_IN_PROGRESS,
                                ENABLE_ENCRYPTION_PROFILE_STARTED,
                                VALIDATE_ENABLE_ENCRYPTION_PROFILE_STATE);
                sendEvent(context, VALIDATE_ENABLE_ENCRYPTION_PROFILE_HANDLER_EVENT.selector(), payload);
            }
        };
    }

    @Bean(name = "SET_ENCRYPTION_PROFILE_STATE")
    public Action<?, ?> setEncryptionProfileAction() {
        return new AbstractEnableEncryptionProfileActions<>(EnableEncryptionProfileEvent.class) {
            @Override
            protected void doExecute(CommonContext context, EnableEncryptionProfileEvent payload, Map<Object, Object> variables) {
                sendEvent(context, SET_ENCRYPTION_PROFILE_HANDLER_EVENT.selector(), payload);
            }
        };
    }

    @Bean(name = "UPDATE_SSL_CONFIG_FREEIPA_STATE")
    public Action<?, ?> updateSslConfigsInFreeIpaAction() {
        return new AbstractEnableEncryptionProfileActions<>(EnableEncryptionProfileEvent.class) {

            @Override
            protected void doExecute(CommonContext context, EnableEncryptionProfileEvent payload, Map<Object, Object> variables) {
                sendEvent(context, UPDATE_SSL_CONFIG_FREEIPA_HANDLER_EVENT.selector(), payload);
            }
        };
    }

    @Bean(name = "UPDATE_SSL_CONFIG_CLUSTERS_STATE")
    public Action<?, ?> updateSslConfigsInClustersAction() {
        return new AbstractEnableEncryptionProfileActions<>(EnableEncryptionProfileEvent.class) {

            @Override
            protected void doExecute(CommonContext context, EnableEncryptionProfileEvent payload, Map<Object, Object> variables) {
                sendEvent(context, UPDATE_SSL_CONFIG_IN_CLUSTERS_HANDLER_EVENT.selector(), payload);
            }
        };
    }

    @Bean(name = "ENABLE_ENCRYPTION_PROFILE_FINISHED_STATE")
    public Action<?, ?> finishedAction() {
        return new AbstractEnableEncryptionProfileActions<>(EnableEncryptionProfileEvent.class) {
            @Override
            protected void doExecute(CommonContext context, EnableEncryptionProfileEvent payload, Map<Object, Object> variables) {
                environmentStatusUpdateService
                        .updateEnvironmentStatusAndNotify(
                                context,
                                payload,
                                EnvironmentStatus.AVAILABLE,
                                ENABLE_ENCRYPTION_PROFILE_FINISHED,
                                ENABLE_ENCRYPTION_PROFILE_FINISHED_STATE);
                sendEvent(context, FINALIZE_ENABLE_ENCRYPTION_PROFILE_EVENT.event(), payload);
            }
        };
    }

    @Bean(name = "ENABLE_ENCRYPTION_PROFILE_FAILED_STATE")
    public Action<?, ?> failedAction() {
        return new AbstractEnableEncryptionProfileActions<>(EnableEncryptionProfileFailedEvent.class) {

            @Override
            protected CommonContext createFlowContext(FlowParameters flowParameters, StateContext<EnabledEncryptionProfileState,
                    EnableEncryptionProfileStateSelectors> stateContext, EnableEncryptionProfileFailedEvent payload) {
                Flow flow = getFlow(flowParameters.getFlowId());
                flow.setFlowFailed(payload.getException());
                return super.createFlowContext(flowParameters, stateContext, payload);
            }

            @Override
            protected void doExecute(CommonContext context, EnableEncryptionProfileFailedEvent payload, Map<Object, Object> variables) {
                LOGGER.error("Failed to enable encryption profile: {}", payload.getException().getMessage());
                environmentStatusUpdateService
                        .updateFailedEnvironmentStatusAndNotify(
                                context,
                                payload,
                                getCurrentStatus(payload.getResourceId()),
                                ResourceEvent.ENABLE_ENCRYPTION_PROFILE_FAILED,
                                ENABLE_ENCRYPTION_PROFILE_FAILED_STATE);
                sendEvent(context, HANDLED_FAILED_ENABLE_ENCRYPTION_PROFILE_EVENT.event(), payload);
            }
        };
    }

    private EnvironmentStatus getCurrentStatus(Long envId) {
        return environmentService
                .findEnvironmentById(envId)
                .map(Environment::getStatus)
                .orElseThrow(() -> new IllegalStateException(
                        String.format("Cannot get status of environment, because it does not exist: %s. ", envId)
                ));
    }
}
