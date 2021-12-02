package com.sequenceiq.cloudbreak.core.flow2.cluster.certrenew;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.certrenew.ClusterCertificateRenewEvent.CLUSTER_CERTIFICATES_REDEPLOY_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.certrenew.ClusterCertificateRenewEvent.CLUSTER_CERTIFICATES_REDEPLOY_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.certrenew.ClusterCertificateRenewEvent.CLUSTER_CERTIFICATE_REISSUE_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.certrenew.ClusterCertificateRenewEvent.CLUSTER_CERTIFICATE_RENEW_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.certrenew.ClusterCertificateRenewEvent.CLUSTER_CERTIFICATE_RENEW_FAILURE_HANDLED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.certrenew.ClusterCertificateRenewEvent.CLUSTER_CERTIFICATE_RENEW_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.certrenew.ClusterCertificateRenewState.CLUSTER_CERTIFICATE_REDEPLOY_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.certrenew.ClusterCertificateRenewState.CLUSTER_CERTIFICATE_REISSUE_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.certrenew.ClusterCertificateRenewState.CLUSTER_CERTIFICATE_RENEWAL_FINISHED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.certrenew.ClusterCertificateRenewState.CLUSTER_CERTIFICATE_RENEW_FAILED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.certrenew.ClusterCertificateRenewState.FINAL_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.certrenew.ClusterCertificateRenewState.INIT_STATE;

import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.flow2.StackStatusFinalizer;
import com.sequenceiq.flow.core.FlowTriggerCondition;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;
import com.sequenceiq.flow.core.config.FlowFinalizerCallback;
import com.sequenceiq.flow.core.config.RetryableFlowConfiguration;

@Component
public class ClusterCertificateRenewFlowConfig extends AbstractFlowConfiguration<ClusterCertificateRenewState, ClusterCertificateRenewEvent>
        implements RetryableFlowConfiguration<ClusterCertificateRenewEvent> {

    private static final List<Transition<ClusterCertificateRenewState, ClusterCertificateRenewEvent>> TRANSITIONS =
            new Transition.Builder<ClusterCertificateRenewState, ClusterCertificateRenewEvent>().defaultFailureEvent(CLUSTER_CERTIFICATE_RENEW_FAILED_EVENT)
                    .from(INIT_STATE).to(CLUSTER_CERTIFICATE_REISSUE_STATE).event(CLUSTER_CERTIFICATE_REISSUE_EVENT)
                        .noFailureEvent()
                    .from(CLUSTER_CERTIFICATE_REISSUE_STATE).to(CLUSTER_CERTIFICATE_REDEPLOY_STATE).event(CLUSTER_CERTIFICATES_REDEPLOY_EVENT)
                        .defaultFailureEvent()
                    .from(CLUSTER_CERTIFICATE_REDEPLOY_STATE).to(CLUSTER_CERTIFICATE_RENEWAL_FINISHED_STATE).event(CLUSTER_CERTIFICATES_REDEPLOY_FINISHED_EVENT)
                        .defaultFailureEvent()
                    .from(CLUSTER_CERTIFICATE_RENEWAL_FINISHED_STATE).to(FINAL_STATE).event(CLUSTER_CERTIFICATE_RENEW_FINISHED_EVENT)
                        .defaultFailureEvent()
                    .build();

    private static final FlowEdgeConfig<ClusterCertificateRenewState, ClusterCertificateRenewEvent> EDGE_CONFIG =
            new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, CLUSTER_CERTIFICATE_RENEW_FAILED_STATE, CLUSTER_CERTIFICATE_RENEW_FAILURE_HANDLED_EVENT);

    @Inject
    private StackStatusFinalizer stackStatusFinalizer;

    public ClusterCertificateRenewFlowConfig() {
        super(ClusterCertificateRenewState.class, ClusterCertificateRenewEvent.class);
    }

    @Override
    protected List<Transition<ClusterCertificateRenewState, ClusterCertificateRenewEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    protected FlowEdgeConfig<ClusterCertificateRenewState, ClusterCertificateRenewEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public ClusterCertificateRenewEvent[] getEvents() {
        return ClusterCertificateRenewEvent.values();
    }

    @Override
    public ClusterCertificateRenewEvent[] getInitEvents() {
        return new ClusterCertificateRenewEvent[]{
                CLUSTER_CERTIFICATE_REISSUE_EVENT
        };
    }

    @Override
    public String getDisplayName() {
        return "Renew certificate of an existing cluster";
    }

    @Override
    public ClusterCertificateRenewEvent getRetryableEvent() {
        return CLUSTER_CERTIFICATE_RENEW_FAILURE_HANDLED_EVENT;
    }

    @Override
    public FlowTriggerCondition getFlowTriggerCondition() {
        return getApplicationContext().getBean(ClusterCertificateRenewTriggerCondition.class);
    }

    @Override
    public FlowFinalizerCallback getFinalizerCallBack() {
        return stackStatusFinalizer;
    }
}
