package com.sequenceiq.environment.environment.flow.encryptionprofile;

import static com.sequenceiq.cloudbreak.event.ResourceEvent.DISABLE_ENCRYPTION_PROFILE_FINISHED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.DISABLE_ENCRYPTION_PROFILE_FINISHED_WITH_SKIPPED_CLUSTERS;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.DISABLE_ENCRYPTION_PROFILE_STARTED;
import static com.sequenceiq.environment.environment.EnvironmentStatus.AVAILABLE;
import static com.sequenceiq.environment.environment.EnvironmentStatus.DISABLE_ENCRYPTION_PROFILE_IN_PROGRESS;
import static com.sequenceiq.environment.environment.flow.encryptionprofile.DisableEncryptionProfileState.DISABLE_ENCRYPTION_PROFILE_FAILED_STATE;
import static com.sequenceiq.environment.environment.flow.encryptionprofile.DisableEncryptionProfileState.DISABLE_ENCRYPTION_PROFILE_FINISHED_STATE;
import static com.sequenceiq.environment.environment.flow.encryptionprofile.DisableEncryptionProfileState.UNSET_ENCRYPTION_PROFILE_STATE;
import static com.sequenceiq.environment.environment.flow.encryptionprofile.event.DisableEncryptionProfileStateSelectors.DISABLE_ENCRYPTION_PROFILE_ON_FREEIPA_HANDLER_EVENT;
import static com.sequenceiq.environment.environment.flow.encryptionprofile.event.DisableEncryptionProfileStateSelectors.DISABLE_ENCRYPTION_PROFILE_ON_STACKS_HANDLER_EVENT;
import static com.sequenceiq.environment.environment.flow.encryptionprofile.event.DisableEncryptionProfileStateSelectors.FINALIZE_DISABLE_ENCRYPTION_PROFILE_EVENT;
import static com.sequenceiq.environment.environment.flow.encryptionprofile.event.DisableEncryptionProfileStateSelectors.HANDLED_FAILED_DISABLE_ENCRYPTION_PROFILE_EVENT;
import static com.sequenceiq.environment.environment.flow.encryptionprofile.event.DisableEncryptionProfileStateSelectors.UNSET_ENCRYPTION_PROFILE_HANDLER_EVENT;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.environment.environment.flow.encryptionprofile.event.DisableEncryptionProfileEvent;
import com.sequenceiq.environment.environment.flow.encryptionprofile.event.DisableEncryptionProfileFailedEvent;
import com.sequenceiq.environment.environment.service.EnvironmentStatusUpdateService;
import com.sequenceiq.flow.core.CommonContext;

@Configuration
public class DisableEncryptionProfileActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(DisableEncryptionProfileActions.class);

    private final EnvironmentStatusUpdateService environmentStatusUpdateService;

    public DisableEncryptionProfileActions(EnvironmentStatusUpdateService environmentStatusUpdateService) {
        this.environmentStatusUpdateService = environmentStatusUpdateService;
    }

    @Bean(name = "UNSET_ENCRYPTION_PROFILE_STATE")
    public Action<?, ?> unsetEncryptionProfileAction() {
        return new AbstractDisableEncryptionProfileActions<>(DisableEncryptionProfileEvent.class) {
            @Override
            protected void doExecute(CommonContext context, DisableEncryptionProfileEvent payload, Map<Object, Object> variables) {
                environmentStatusUpdateService
                        .updateEnvironmentStatusAndNotify(
                                context,
                                payload,
                                DISABLE_ENCRYPTION_PROFILE_IN_PROGRESS,
                                DISABLE_ENCRYPTION_PROFILE_STARTED,
                                UNSET_ENCRYPTION_PROFILE_STATE);
                sendEvent(context, UNSET_ENCRYPTION_PROFILE_HANDLER_EVENT.selector(), payload);
            }
        };
    }

    @Bean(name = "DISABLE_ENCRYPTION_PROFILE_ON_FREEIPA_STATE")
    public Action<?, ?> disableEncryptionProfileOnFreeIpaAction() {
        return new AbstractDisableEncryptionProfileActions<>(DisableEncryptionProfileEvent.class) {
            @Override
            protected void doExecute(CommonContext context, DisableEncryptionProfileEvent payload, Map<Object, Object> variables) {
                sendEvent(context, DISABLE_ENCRYPTION_PROFILE_ON_FREEIPA_HANDLER_EVENT.selector(), payload);
            }
        };
    }

    @Bean(name = "DISABLE_ENCRYPTION_PROFILE_ON_STACKS_STATE")
    public Action<?, ?> disableEncryptionProfileOnStacksAction() {
        return new AbstractDisableEncryptionProfileActions<>(DisableEncryptionProfileEvent.class) {
            @Override
            protected void doExecute(CommonContext context, DisableEncryptionProfileEvent payload, Map<Object, Object> variables) {
                sendEvent(context, DISABLE_ENCRYPTION_PROFILE_ON_STACKS_HANDLER_EVENT.selector(), payload);
            }
        };
    }

    @Bean(name = "DISABLE_ENCRYPTION_PROFILE_FINISHED_STATE")
    public Action<?, ?> finishedAction() {
        return new AbstractDisableEncryptionProfileActions<>(DisableEncryptionProfileEvent.class) {
            @Override
            protected void doExecute(CommonContext context, DisableEncryptionProfileEvent payload, Map<Object, Object> variables) {
                List<String> skippedClusters = payload.getSkippedClusters();
                if (skippedClusters.isEmpty()) {
                    environmentStatusUpdateService
                            .updateEnvironmentStatusAndNotify(
                                    context,
                                    payload,
                                    AVAILABLE,
                                    DISABLE_ENCRYPTION_PROFILE_FINISHED,
                                    DISABLE_ENCRYPTION_PROFILE_FINISHED_STATE);
                } else {
                    LOGGER.warn("Disable encryption profile finished, but the following cluster(s) were skipped: {}", skippedClusters);
                    environmentStatusUpdateService
                            .updateEnvironmentStatusAndNotify(
                                    context,
                                    payload,
                                    AVAILABLE,
                                    DISABLE_ENCRYPTION_PROFILE_FINISHED_WITH_SKIPPED_CLUSTERS,
                                    List.of(String.join(", ", skippedClusters)),
                                    DISABLE_ENCRYPTION_PROFILE_FINISHED_STATE);
                }
                sendEvent(context, FINALIZE_DISABLE_ENCRYPTION_PROFILE_EVENT.event(), payload);
            }
        };
    }

    @Bean(name = "DISABLE_ENCRYPTION_PROFILE_FAILED_STATE")
    public Action<?, ?> failedAction() {
        return new AbstractDisableEncryptionProfileActions<>(DisableEncryptionProfileFailedEvent.class) {

            @Override
            protected void doExecute(CommonContext context, DisableEncryptionProfileFailedEvent payload, Map<Object, Object> variables) {
                String reason = getFailureReason(payload.getException());
                LOGGER.error("Failed to disable encryption profile: {}", reason, payload.getException());
                environmentStatusUpdateService
                        .updateFailedEnvironmentStatusAndNotify(
                                context,
                                payload,
                                AVAILABLE,
                                ResourceEvent.DISABLE_ENCRYPTION_PROFILE_FAILED,
                                List.of(reason),
                                DISABLE_ENCRYPTION_PROFILE_FAILED_STATE);
                sendEvent(context, HANDLED_FAILED_DISABLE_ENCRYPTION_PROFILE_EVENT.event(), payload);
            }
        };
    }
}
