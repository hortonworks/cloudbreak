package com.sequenceiq.cloudbreak.core.flow2.cluster.encryptionprofile;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.encryptionprofile.DisableEncryptionProfileOnClusterStateSelectors.UNSET_ENCRYPTION_PROFILE_HANDLER_EVENT;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.DISABLE_ENCRYPTION_PROFILE_FAILED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.DISABLE_ENCRYPTION_PROFILE_FINISHED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.DISABLE_ENCRYPTION_PROFILE_STARTED;

import java.util.Map;
import java.util.Optional;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.core.flow2.cluster.AbstractClusterAction;
import com.sequenceiq.cloudbreak.core.flow2.cluster.ClusterViewContext;
import com.sequenceiq.cloudbreak.core.flow2.cluster.encryptionprofile.event.DisableEncryptionProfileFailedEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.encryptionprofile.event.DisableEncryptionProfileOnClusterEvent;
import com.sequenceiq.cloudbreak.core.flow2.stack.AbstractStackFailureAction;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackFailureContext;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.service.StackUpdater;

@Configuration
public class DisableEncryptionProfileOnClusterActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(DisableEncryptionProfileOnClusterActions.class);

    private static final String UNKNOWN_ERROR = "Unknown error";

    @Inject
    private StackUpdater stackUpdater;

    @Inject
    private CloudbreakFlowMessageService flowMessageService;

    @Bean(name = "UNSET_ENCRYPTION_PROFILE_STATE")
    public Action<?, ?> unsetEncryptionProfileAction() {
        return new AbstractClusterAction<>(DisableEncryptionProfileOnClusterEvent.class) {
            @Override
            protected void doExecute(ClusterViewContext context, DisableEncryptionProfileOnClusterEvent payload, Map<Object, Object> variables) {
                LOGGER.info("Unsetting encryption profile on cluster");
                stackUpdater.updateStackStatus(context.getStackId(), DetailedStackStatus.DISABLE_ENCRYPTION_PROFILE_ON_CLUSTER_IN_PROGRESS,
                        "Disabling encryption profile on cluster");
                flowMessageService.fireEventAndLog(payload.getResourceId(),
                        DISABLE_ENCRYPTION_PROFILE_STARTED.name(),
                        DISABLE_ENCRYPTION_PROFILE_STARTED);
                sendEvent(context, new DisableEncryptionProfileOnClusterEvent(UNSET_ENCRYPTION_PROFILE_HANDLER_EVENT.selector(), context.getStackId()));
            }
        };
    }

    @Bean(name = "DISABLE_ENCRYPTION_PROFILE_ON_CLUSTER_FINISHED_STATE")
    public Action<?, ?> finishedAction() {
        return new AbstractClusterAction<>(DisableEncryptionProfileOnClusterEvent.class) {
            @Override
            protected void doExecute(ClusterViewContext context, DisableEncryptionProfileOnClusterEvent payload, Map<Object, Object> variables) {
                LOGGER.info("Encryption profile disabled on cluster");
                stackUpdater.updateStackStatus(context.getStackId(), DetailedStackStatus.DISABLE_ENCRYPTION_PROFILE_ON_CLUSTER_COMPLETE,
                        "Encryption profile disabled on cluster");
                flowMessageService.fireEventAndLog(payload.getResourceId(),
                        DISABLE_ENCRYPTION_PROFILE_FINISHED.name(),
                        DISABLE_ENCRYPTION_PROFILE_FINISHED);
                sendEvent(context, new DisableEncryptionProfileOnClusterEvent(
                        DisableEncryptionProfileOnClusterStateSelectors.FINALIZE_DISABLE_ENCRYPTION_PROFILE_ON_CLUSTER_EVENT.selector(),
                        context.getStackId()));
            }
        };
    }

    @Bean(name = "DISABLE_ENCRYPTION_PROFILE_ON_CLUSTER_FAILED_STATE")
    public Action<?, ?> failedAction() {
        return new AbstractStackFailureAction<DisableEncryptionProfileOnClusterState, DisableEncryptionProfileOnClusterStateSelectors>() {
            @Override
            protected void doExecute(StackFailureContext context, StackFailureEvent payload, Map<Object, Object> variables) {
                LOGGER.error("Failed to disable encryption profile on Stack '{}'.", payload.getResourceId(), payload.getException());
                String errorMessage = Optional.ofNullable(payload.getException())
                        .map(Exception::getMessage)
                        .filter(StringUtils::isNotBlank)
                        .orElse(UNKNOWN_ERROR);
                stackUpdater.updateStackStatus(context.getStackId(), DetailedStackStatus.DISABLE_ENCRYPTION_PROFILE_ON_CLUSTER_FAILED,
                        errorMessage);
                flowMessageService.fireEventAndLog(payload.getResourceId(),
                        DISABLE_ENCRYPTION_PROFILE_FAILED.name(),
                        DISABLE_ENCRYPTION_PROFILE_FAILED,
                        errorMessage);
                sendEvent(context, new DisableEncryptionProfileFailedEvent(
                        DisableEncryptionProfileOnClusterStateSelectors.HANDLED_FAILED_DISABLE_ENCRYPTION_PROFILE_ON_CLUSTER_EVENT.selector(),
                        payload.getResourceId(), payload.getException()));
            }
        };
    }
}
