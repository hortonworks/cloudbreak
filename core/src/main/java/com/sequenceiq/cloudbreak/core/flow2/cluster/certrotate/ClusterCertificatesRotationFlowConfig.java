package com.sequenceiq.cloudbreak.core.flow2.cluster.certrotate;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.certrotate.ClusterCertificatesRotationEvent.CLUSTER_CERTIFICATES_CLUSTER_SERVICES_RESTART_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.certrotate.ClusterCertificatesRotationEvent.CLUSTER_CERTIFICATES_CM_RESTART_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.certrotate.ClusterCertificatesRotationEvent.CLUSTER_CERTIFICATES_ROTATION_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.certrotate.ClusterCertificatesRotationEvent.CLUSTER_CERTIFICATES_ROTATION_FAILURE_HANDLED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.certrotate.ClusterCertificatesRotationEvent.CLUSTER_CERTIFICATES_ROTATION_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.certrotate.ClusterCertificatesRotationEvent.CLUSTER_CMCA_ROTATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.certrotate.ClusterCertificatesRotationEvent.CLUSTER_HOST_CERTIFICATES_ROTATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.certrotate.ClusterCertificatesRotationEvent.CLUSTER_HOST_CERTIFICATES_ROTATION_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.certrotate.ClusterCertificatesRotationState.CLUSTER_CERTIFICATES_RESTART_CLUSTER_SERVICES_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.certrotate.ClusterCertificatesRotationState.CLUSTER_CERTIFICATES_RESTART_CM_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.certrotate.ClusterCertificatesRotationState.CLUSTER_CERTIFICATES_ROTATION_FAILED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.certrotate.ClusterCertificatesRotationState.CLUSTER_CERTIFICATES_ROTATION_FINISHED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.certrotate.ClusterCertificatesRotationState.CLUSTER_CMCA_ROTATION_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.certrotate.ClusterCertificatesRotationState.CLUSTER_HOST_CERTIFICATES_ROTATION_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.certrotate.ClusterCertificatesRotationState.FINAL_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.certrotate.ClusterCertificatesRotationState.INIT_STATE;

import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.flow2.StackStatusFinalizer;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;
import com.sequenceiq.flow.core.config.FlowFinalizerCallback;
import com.sequenceiq.flow.core.config.RetryableFlowConfiguration;

@Component
public class ClusterCertificatesRotationFlowConfig extends AbstractFlowConfiguration<ClusterCertificatesRotationState, ClusterCertificatesRotationEvent>
        implements RetryableFlowConfiguration<ClusterCertificatesRotationEvent> {

    private static final List<Transition<ClusterCertificatesRotationState, ClusterCertificatesRotationEvent>> TRANSITIONS =
            new Transition.Builder<ClusterCertificatesRotationState, ClusterCertificatesRotationEvent>()
                    .defaultFailureEvent(CLUSTER_CERTIFICATES_ROTATION_FAILED_EVENT)
                    .from(INIT_STATE).to(CLUSTER_CMCA_ROTATION_STATE).event(CLUSTER_CMCA_ROTATION_EVENT)
                        .noFailureEvent()
                    .from(CLUSTER_CMCA_ROTATION_STATE).to(CLUSTER_HOST_CERTIFICATES_ROTATION_STATE).event(CLUSTER_HOST_CERTIFICATES_ROTATION_EVENT)
                        .defaultFailureEvent()
                    .from(CLUSTER_HOST_CERTIFICATES_ROTATION_STATE).to(CLUSTER_CERTIFICATES_RESTART_CM_STATE)
                        .event(CLUSTER_HOST_CERTIFICATES_ROTATION_FINISHED_EVENT).defaultFailureEvent()
                    .from(CLUSTER_CERTIFICATES_RESTART_CM_STATE).to(CLUSTER_CERTIFICATES_RESTART_CLUSTER_SERVICES_STATE)
                        .event(CLUSTER_CERTIFICATES_CM_RESTART_FINISHED_EVENT).defaultFailureEvent()
                    .from(CLUSTER_CERTIFICATES_RESTART_CLUSTER_SERVICES_STATE).to(CLUSTER_CERTIFICATES_ROTATION_FINISHED_STATE)
                        .event(CLUSTER_CERTIFICATES_CLUSTER_SERVICES_RESTART_FINISHED_EVENT).defaultFailureEvent()
                    .from(CLUSTER_CERTIFICATES_ROTATION_FINISHED_STATE).to(FINAL_STATE).event(CLUSTER_CERTIFICATES_ROTATION_FINISHED_EVENT)
                        .defaultFailureEvent()
                    .build();

    private static final FlowEdgeConfig<ClusterCertificatesRotationState, ClusterCertificatesRotationEvent> EDGE_CONFIG =
            new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, CLUSTER_CERTIFICATES_ROTATION_FAILED_STATE, CLUSTER_CERTIFICATES_ROTATION_FAILURE_HANDLED_EVENT);

    @Inject
    private StackStatusFinalizer stackStatusFinalizer;

    public ClusterCertificatesRotationFlowConfig() {
        super(ClusterCertificatesRotationState.class, ClusterCertificatesRotationEvent.class);
    }

    @Override
    protected List<Transition<ClusterCertificatesRotationState, ClusterCertificatesRotationEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    protected FlowEdgeConfig<ClusterCertificatesRotationState, ClusterCertificatesRotationEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public ClusterCertificatesRotationEvent[] getEvents() {
        return ClusterCertificatesRotationEvent.values();
    }

    @Override
    public ClusterCertificatesRotationEvent[] getInitEvents() {
        return new ClusterCertificatesRotationEvent[]{
                CLUSTER_CMCA_ROTATION_EVENT
        };
    }

    @Override
    public String getDisplayName() {
        return "Renew certificate of an existing cluster";
    }

    @Override
    public ClusterCertificatesRotationEvent getRetryableEvent() {
        return CLUSTER_CERTIFICATES_ROTATION_FAILURE_HANDLED_EVENT;
    }

    @Override
    public FlowFinalizerCallback getFinalizerCallBack() {
        return stackStatusFinalizer;
    }
}
