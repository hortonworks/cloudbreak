package com.sequenceiq.cloudbreak.core.flow2.cluster.certrotate;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.certrotate.ClusterCertificatesRotationEvent.CLUSTER_CERTIFICATES_ROTATION_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.certrotate.ClusterCertificatesRotationEvent.CLUSTER_CERTIFICATES_ROTATION_FAILURE_HANDLED_EVENT;

import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.event.ClusterCertificatesRotationTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.stack.AbstractStackFailureAction;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackFailureContext;
import com.sequenceiq.cloudbreak.core.flow2.stack.provision.action.AbstractStackCreationAction;
import com.sequenceiq.cloudbreak.core.flow2.stack.start.StackCreationContext;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.RestartClusterManagerServerRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.RestartClusterManagerServerSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.RestartClusterServicesRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.RestartClusterServicesSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.certrotate.ClusterCMCARotationSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.certrotate.ClusterHostCertificatesRotationRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.certrotate.ClusterHostCertificatesRotationSuccess;

@Configuration
public class ClusterCertificatesRotationActions {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterCertificatesRotationActions.class);

    @Inject
    private ClusterCertificatesRotationService clusterCertificatesRotationService;

    @Bean(name = "CLUSTER_CMCA_ROTATION_STATE")
    public Action<?, ?> clusterCMCARotationAction() {
        return new AbstractStackCreationAction<>(ClusterCertificatesRotationTriggerEvent.class) {
            @Override
            protected void doExecute(StackCreationContext context, ClusterCertificatesRotationTriggerEvent payload, Map<Object, Object> variables) {
                clusterCertificatesRotationService.initClusterCertificatesRotation(payload.getResourceId());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackCreationContext context) {
                return new ClusterCMCARotationSuccess(context.getStack().getId());
            }
        };
    }

    @Bean(name = "CLUSTER_HOST_CERTIFICATES_ROTATION_STATE")
    public Action<?, ?> clusterHostCertificatesRotationAction() {
        return new AbstractStackCreationAction<>(ClusterCMCARotationSuccess.class) {
            @Override
            protected void doExecute(StackCreationContext context, ClusterCMCARotationSuccess payload, Map<Object, Object> variables) {
                clusterCertificatesRotationService.hostCertificatesRotationStarted(payload.getResourceId());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackCreationContext context) {
                return new ClusterHostCertificatesRotationRequest(context.getStack().getId());
            }
        };
    }

    @Bean(name = "CLUSTER_CERTIFICATES_RESTART_CM_STATE")
    public Action<?, ?> clusterManagerRestartAction() {
        return new AbstractStackCreationAction<>(ClusterHostCertificatesRotationSuccess.class) {
            @Override
            protected void doExecute(StackCreationContext context, ClusterHostCertificatesRotationSuccess payload, Map<Object, Object> variables) {
                clusterCertificatesRotationService.restartClusterManager(payload.getResourceId());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackCreationContext context) {
                return new RestartClusterManagerServerRequest(context.getStack().getId(), false, CLUSTER_CERTIFICATES_ROTATION_FAILED_EVENT.event());
            }
        };
    }

    @Bean(name = "CLUSTER_CERTIFICATES_RESTART_CLUSTER_SERVICES_STATE")
    public Action<?, ?> clusterServicesRestartAction() {
        return new AbstractStackCreationAction<>(RestartClusterManagerServerSuccess.class) {
            @Override
            protected void doExecute(StackCreationContext context, RestartClusterManagerServerSuccess payload, Map<Object, Object> variables) {
                clusterCertificatesRotationService.restartClusterServices(payload.getResourceId());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackCreationContext context) {
                return new RestartClusterServicesRequest(context.getStack().getId(), true, CLUSTER_CERTIFICATES_ROTATION_FAILED_EVENT.event());
            }
        };
    }

    @Bean(name = "CLUSTER_CERTIFICATES_ROTATION_FINISHED_STATE")
    public Action<?, ?> clusterCertificatesRotationFinishedAction() {
        return new AbstractStackCreationAction<>(RestartClusterServicesSuccess.class) {
            @Override
            protected void doExecute(StackCreationContext context, RestartClusterServicesSuccess payload, Map<Object, Object> variables) {
                clusterCertificatesRotationService.certificatesRotationFinished(payload.getResourceId());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackCreationContext context) {
                return new StackEvent(ClusterCertificatesRotationEvent.CLUSTER_CERTIFICATES_ROTATION_FINISHED_EVENT.event(), context.getStack().getId());
            }
        };
    }

    @Bean(name = "CLUSTER_CERTIFICATES_ROTATION_FAILED_STATE")
    public Action<?, ?> clusterCertificatesRotationFailedAction() {
        return new AbstractStackFailureAction<ClusterCertificatesRotationState, ClusterCertificatesRotationEvent>() {
            @Override
            protected void doExecute(StackFailureContext context, StackFailureEvent payload, Map<Object, Object> variables) {
                clusterCertificatesRotationService.certificatesRotationFailed(context.getStackView(), payload.getException());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackFailureContext context) {
                return new StackEvent(CLUSTER_CERTIFICATES_ROTATION_FAILURE_HANDLED_EVENT.event(), context.getStackView().getId());
            }
        };
    }
}
