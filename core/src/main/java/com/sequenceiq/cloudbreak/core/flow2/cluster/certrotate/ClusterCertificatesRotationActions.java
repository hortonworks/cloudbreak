package com.sequenceiq.cloudbreak.core.flow2.cluster.certrotate;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.certrotate.ClusterCertificatesRotationEvent.CLUSTER_CERTIFICATES_ROTATION_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.certrotate.ClusterCertificatesRotationEvent.CLUSTER_CERTIFICATES_ROTATION_FAILURE_HANDLED_EVENT;

import java.util.Map;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.CertificateRotationType;
import com.sequenceiq.cloudbreak.core.flow2.event.ClusterCertificatesRotationTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.stack.AbstractStackFailureAction;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackFailureContext;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.RestartClusterManagerServerRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.RestartClusterManagerServerSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.RestartClusterServicesRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.RestartClusterServicesSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.certrotate.ClusterCMCARotationRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.certrotate.ClusterCMCARotationSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.certrotate.ClusterCertificateRotationContext;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.certrotate.ClusterHostCertificatesRotationRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.certrotate.ClusterHostCertificatesRotationSuccess;
import com.sequenceiq.cloudbreak.rotation.CMCAValidationService;

@Configuration
public class ClusterCertificatesRotationActions {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterCertificatesRotationActions.class);

    @Inject
    private ClusterCertificatesRotationService clusterCertificatesRotationService;

    @Inject
    private CMCAValidationService cmcaValidationService;

    @Bean(name = "CLUSTER_CMCA_ROTATION_STATE")
    public Action<?, ?> clusterCMCARotationAction() {
        return new ClusterCertificateRotationAction<>(ClusterCertificatesRotationTriggerEvent.class) {

            @Override
            protected void prepareExecution(ClusterCertificatesRotationTriggerEvent payload, Map<Object, Object> variables) {
                super.prepareExecution(payload, variables);
                variables.put(ClusterCertificateRotationAction.CERT_ROTATION_TYPE, payload.getCertificateRotationType());
            }

            @Override
            protected void doExecute(ClusterCertificateRotationContext context,
                    ClusterCertificatesRotationTriggerEvent payload, Map<Object, Object> variables) {
                clusterCertificatesRotationService.initClusterCertificatesRotation(payload.getResourceId(), payload.getCertificateRotationType());
                sendEvent(context, new ClusterCMCARotationRequest(payload.getResourceId(), payload.getCertificateRotationType()));
            }
        };
    }

    @Bean(name = "CLUSTER_HOST_CERTIFICATES_ROTATION_STATE")
    public Action<?, ?> clusterHostCertificatesRotationAction() {
        return new ClusterCertificateRotationAction<>(ClusterCMCARotationSuccess.class) {
            @Override
            protected void doExecute(ClusterCertificateRotationContext context, ClusterCMCARotationSuccess payload, Map<Object, Object> variables) {
                clusterCertificatesRotationService.hostCertificatesRotationStarted(payload.getResourceId());
                sendEvent(context, new ClusterHostCertificatesRotationRequest(payload.getResourceId()));
            }
        };
    }

    @Bean(name = "CLUSTER_CERTIFICATES_RESTART_CM_STATE")
    public Action<?, ?> clusterManagerRestartAction() {
        return new ClusterCertificateRotationAction<>(ClusterHostCertificatesRotationSuccess.class) {
            @Override
            protected void doExecute(ClusterCertificateRotationContext context, ClusterHostCertificatesRotationSuccess payload, Map<Object, Object> variables) {
                clusterCertificatesRotationService.restartClusterManager(payload.getResourceId());
                sendEvent(context, new RestartClusterManagerServerRequest(payload.getResourceId(),
                        false, CLUSTER_CERTIFICATES_ROTATION_FAILED_EVENT.event()));
            }
        };
    }

    @Bean(name = "CLUSTER_CERTIFICATES_RESTART_CLUSTER_SERVICES_STATE")
    public Action<?, ?> clusterServicesRestartAction() {
        return new ClusterCertificateRotationAction<>(RestartClusterManagerServerSuccess.class) {
            @Override
            protected void doExecute(ClusterCertificateRotationContext context, RestartClusterManagerServerSuccess payload, Map<Object, Object> variables) {
                if (CertificateRotationType.ALL.equals(context.getCertificateRotationType())) {
                    cmcaValidationService.checkCMCAWithRootCert(payload.getResourceId());
                }
                clusterCertificatesRotationService.restartClusterServices(payload.getResourceId());
                sendEvent(context, new RestartClusterServicesRequest(payload.getResourceId(),
                        true, CLUSTER_CERTIFICATES_ROTATION_FAILED_EVENT.event()));
            }
        };
    }

    @Bean(name = "CLUSTER_CERTIFICATES_ROTATION_FINISHED_STATE")
    public Action<?, ?> clusterCertificatesRotationFinishedAction() {
        return new ClusterCertificateRotationAction<>(RestartClusterServicesSuccess.class) {
            @Override
            protected void doExecute(ClusterCertificateRotationContext context, RestartClusterServicesSuccess payload, Map<Object, Object> variables) {
                clusterCertificatesRotationService.certificatesRotationFinished(payload.getResourceId());
                sendEvent(context,
                        new StackEvent(ClusterCertificatesRotationEvent.CLUSTER_CERTIFICATES_ROTATION_FINISHED_EVENT.event(), payload.getResourceId()));
            }
        };
    }

    @Bean(name = "CLUSTER_CERTIFICATES_ROTATION_FAILED_STATE")
    public Action<?, ?> clusterCertificatesRotationFailedAction() {
        return new AbstractStackFailureAction<ClusterCertificatesRotationState, ClusterCertificatesRotationEvent>() {
            @Override
            protected void doExecute(StackFailureContext context, StackFailureEvent payload, Map<Object, Object> variables) {
                LOGGER.error("Cluster certificate rotation failed: ", payload.getException());
                clusterCertificatesRotationService.certificatesRotationFailed(payload.getResourceId(), payload.getException());
                sendEvent(context, new StackEvent(CLUSTER_CERTIFICATES_ROTATION_FAILURE_HANDLED_EVENT.event(), payload.getResourceId()));
            }
        };
    }
}
