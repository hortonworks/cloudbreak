package com.sequenceiq.cloudbreak.core.flow2.cluster.certrenew;

import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus.Value.RENEW_PUBLIC_CERT_FAILED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus.Value.RENEW_PUBLIC_CERT_FINISHED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus.Value.RENEW_PUBLIC_CERT_STARTED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus.Value.UNSET;
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

import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus.Value;
import com.sequenceiq.cloudbreak.core.flow2.StackStatusFinalizerAbstractFlowConfig;
import com.sequenceiq.cloudbreak.structuredevent.service.telemetry.mapper.ClusterUseCaseAware;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.FlowTriggerCondition;
import com.sequenceiq.flow.core.config.RetryableFlowConfiguration;

@Component
public class ClusterCertificateRenewFlowConfig extends StackStatusFinalizerAbstractFlowConfig<ClusterCertificateRenewState, ClusterCertificateRenewEvent>
        implements RetryableFlowConfiguration<ClusterCertificateRenewEvent>, ClusterUseCaseAware {

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

    public ClusterCertificateRenewFlowConfig() {
        super(ClusterCertificateRenewState.class, ClusterCertificateRenewEvent.class);
    }

    @Override
    protected List<Transition<ClusterCertificateRenewState, ClusterCertificateRenewEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    public FlowEdgeConfig<ClusterCertificateRenewState, ClusterCertificateRenewEvent> getEdgeConfig() {
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
    public Value getUseCaseForFlowState(Enum<? extends FlowState> flowState) {
        if (INIT_STATE.equals(flowState)) {
            return RENEW_PUBLIC_CERT_STARTED;
        } else if (CLUSTER_CERTIFICATE_RENEWAL_FINISHED_STATE.equals(flowState)) {
            return RENEW_PUBLIC_CERT_FINISHED;
        } else if (flowState.toString().endsWith("FAILED_STATE")) {
            return RENEW_PUBLIC_CERT_FAILED;
        } else {
            return UNSET;
        }
    }
}
