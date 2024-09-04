package com.sequenceiq.cloudbreak.core.flow2.cluster.rds.cert;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.rds.cert.RotateRdsCertificateEvent.CM_RESTART_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.rds.cert.RotateRdsCertificateEvent.FAIL_HANDLED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.rds.cert.RotateRdsCertificateEvent.FINALIZED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.rds.cert.RotateRdsCertificateEvent.GET_LATEST_RDS_CERTIFICATE_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.rds.cert.RotateRdsCertificateEvent.ROLLING_RESTART_SERVICES_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.rds.cert.RotateRdsCertificateEvent.ROTATE_RDS_CERTIFICATE_CHECK_PREREQUISITES_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.rds.cert.RotateRdsCertificateEvent.ROTATE_RDS_CERTIFICATE_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.rds.cert.RotateRdsCertificateEvent.ROTATE_RDS_CERTIFICATE_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.rds.cert.RotateRdsCertificateEvent.ROTATE_RDS_CERTIFICATE_ON_PROVIDER_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.rds.cert.RotateRdsCertificateEvent.ROTATE_RDS_CERTIFICATE_TLS_SETUP_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.rds.cert.RotateRdsCertificateEvent.UPDATE_TO_LATEST_RDS_CERTIFICATE_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.rds.cert.RotateRdsCertificateState.FINAL_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.rds.cert.RotateRdsCertificateState.INIT_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.rds.cert.RotateRdsCertificateState.ROTATE_RDS_CERTIFICATE_CHECK_PREREQUISITES_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.rds.cert.RotateRdsCertificateState.ROTATE_RDS_CERTIFICATE_FAILED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.rds.cert.RotateRdsCertificateState.ROTATE_RDS_CERTIFICATE_FINISHED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.rds.cert.RotateRdsCertificateState.ROTATE_RDS_CERTIFICATE_GET_LATEST_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.rds.cert.RotateRdsCertificateState.ROTATE_RDS_CERTIFICATE_ON_PROVIDER_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.rds.cert.RotateRdsCertificateState.ROTATE_RDS_CERTIFICATE_RESTART_CM_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.rds.cert.RotateRdsCertificateState.ROTATE_RDS_CERTIFICATE_ROLLING_RESTART_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.rds.cert.RotateRdsCertificateState.ROTATE_RDS_CERTIFICATE_UPDATE_TO_LATEST_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.rds.cert.RotateRdsCertificateState.ROTATE_RDS_SETUP_TLS_STATE;

import java.util.List;

import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.core.flow2.StackStatusFinalizerAbstractFlowConfig;
import com.sequenceiq.cloudbreak.structuredevent.service.telemetry.mapper.ClusterUseCaseAware;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.FlowTriggerCondition;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration.Transition.Builder;

@Component
public class RotateRdsCertificateFlowConfig extends StackStatusFinalizerAbstractFlowConfig<RotateRdsCertificateState, RotateRdsCertificateEvent>
        implements ClusterUseCaseAware {
    private static final List<Transition<RotateRdsCertificateState, RotateRdsCertificateEvent>> TRANSITIONS =
            new Builder<RotateRdsCertificateState, RotateRdsCertificateEvent>()
                    .defaultFailureEvent(ROTATE_RDS_CERTIFICATE_FAILED_EVENT)

                    .from(INIT_STATE)
                    .to(ROTATE_RDS_CERTIFICATE_CHECK_PREREQUISITES_STATE)
                    .event(ROTATE_RDS_CERTIFICATE_EVENT)
                    .defaultFailureEvent()

                    .from(ROTATE_RDS_CERTIFICATE_CHECK_PREREQUISITES_STATE)
                    .to(ROTATE_RDS_SETUP_TLS_STATE)
                    .event(ROTATE_RDS_CERTIFICATE_CHECK_PREREQUISITES_FINISHED_EVENT)
                    .defaultFailureEvent()

                    .from(ROTATE_RDS_SETUP_TLS_STATE)
                    .to(ROTATE_RDS_CERTIFICATE_GET_LATEST_STATE)
                    .event(ROTATE_RDS_CERTIFICATE_TLS_SETUP_FINISHED_EVENT)
                    .defaultFailureEvent()

                    .from(ROTATE_RDS_CERTIFICATE_GET_LATEST_STATE)
                    .to(ROTATE_RDS_CERTIFICATE_UPDATE_TO_LATEST_STATE)
                    .event(GET_LATEST_RDS_CERTIFICATE_FINISHED_EVENT)
                    .defaultFailureEvent()

                    .from(ROTATE_RDS_CERTIFICATE_UPDATE_TO_LATEST_STATE)
                    .to(ROTATE_RDS_CERTIFICATE_RESTART_CM_STATE)
                    .event(UPDATE_TO_LATEST_RDS_CERTIFICATE_FINISHED_EVENT)
                    .defaultFailureEvent()

                    .from(ROTATE_RDS_CERTIFICATE_RESTART_CM_STATE)
                    .to(ROTATE_RDS_CERTIFICATE_ROLLING_RESTART_STATE)
                    .event(CM_RESTART_FINISHED_EVENT)
                    .defaultFailureEvent()

                    .from(ROTATE_RDS_CERTIFICATE_ROLLING_RESTART_STATE)
                    .to(ROTATE_RDS_CERTIFICATE_ON_PROVIDER_STATE)
                    .event(ROLLING_RESTART_SERVICES_FINISHED_EVENT)
                    .defaultFailureEvent()

                    .from(ROTATE_RDS_CERTIFICATE_ON_PROVIDER_STATE)
                    .to(ROTATE_RDS_CERTIFICATE_FINISHED_STATE)
                    .event(ROTATE_RDS_CERTIFICATE_ON_PROVIDER_FINISHED_EVENT)
                    .defaultFailureEvent()

                    .from(ROTATE_RDS_CERTIFICATE_FINISHED_STATE)
                    .to(FINAL_STATE)
                    .event(FINALIZED_EVENT)
                    .defaultFailureEvent()

                    .build();

    private static final FlowEdgeConfig<RotateRdsCertificateState, RotateRdsCertificateEvent> EDGE_CONFIG =
            new FlowEdgeConfig<>(
                    INIT_STATE,
                    FINAL_STATE,
                    ROTATE_RDS_CERTIFICATE_FAILED_STATE,
                    FAIL_HANDLED_EVENT);

    public RotateRdsCertificateFlowConfig() {
        super(RotateRdsCertificateState.class, RotateRdsCertificateEvent.class);
    }

    @Override
    public FlowTriggerCondition getFlowTriggerCondition() {
        return getApplicationContext().getBean(RotateRdsCertificateFlowTriggerCondition.class);
    }

    @Override
    protected List<Transition<RotateRdsCertificateState, RotateRdsCertificateEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    public FlowEdgeConfig<RotateRdsCertificateState, RotateRdsCertificateEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public RotateRdsCertificateEvent[] getEvents() {
        return RotateRdsCertificateEvent.values();
    }

    @Override
    public RotateRdsCertificateEvent[] getInitEvents() {
        return new RotateRdsCertificateEvent[]{
                ROTATE_RDS_CERTIFICATE_EVENT
        };
    }

    @Override
    public String getDisplayName() {
        return "Refresh RDS certificate";
    }

    @Override
    public UsageProto.CDPClusterStatus.Value getUseCaseForFlowState(Enum<? extends FlowState> flowState) {
        if (INIT_STATE.equals(flowState)) {
            return UsageProto.CDPClusterStatus.Value.ROTATE_RDS_CERTIFICATE_STARTED;
        } else if (ROTATE_RDS_CERTIFICATE_FAILED_STATE.equals(flowState)) {
            return UsageProto.CDPClusterStatus.Value.ROTATE_RDS_CERTIFICATE_FAILED;
        } else if (ROTATE_RDS_CERTIFICATE_FINISHED_STATE.equals(flowState)) {
            return UsageProto.CDPClusterStatus.Value.ROTATE_RDS_CERTIFICATE_FINISHED;
        }
        return UsageProto.CDPClusterStatus.Value.UNSET;
    }

}
