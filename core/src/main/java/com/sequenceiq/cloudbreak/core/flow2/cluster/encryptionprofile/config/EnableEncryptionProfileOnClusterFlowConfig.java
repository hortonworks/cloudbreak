package com.sequenceiq.cloudbreak.core.flow2.cluster.encryptionprofile.config;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.encryptionprofile.EnableEncryptionProfileOnClusterState.ENABLE_ENCRYPTION_PROFILE_ON_CLUSTER_FAILED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.encryptionprofile.EnableEncryptionProfileOnClusterState.ENABLE_ENCRYPTION_PROFILE_ON_CLUSTER_FINISHED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.encryptionprofile.EnableEncryptionProfileOnClusterState.FINAL_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.encryptionprofile.EnableEncryptionProfileOnClusterState.GENERATE_ALTERNATIVE_CERTIFICATE_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.encryptionprofile.EnableEncryptionProfileOnClusterState.INIT_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.encryptionprofile.EnableEncryptionProfileOnClusterState.SET_ENCRYPTION_PROFILE_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.encryptionprofile.EnableEncryptionProfileOnClusterState.UPDATE_CM_POLICY_STATE;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.flow2.StackStatusFinalizerAbstractFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.cluster.encryptionprofile.EnableEncryptionProfileOnClusterState;
import com.sequenceiq.cloudbreak.core.flow2.cluster.encryptionprofile.EnableEncryptionProfileOnClusterStateSelectors;

@Component
public class EnableEncryptionProfileOnClusterFlowConfig
        extends StackStatusFinalizerAbstractFlowConfig<EnableEncryptionProfileOnClusterState, EnableEncryptionProfileOnClusterStateSelectors> {

    private static final List<Transition<EnableEncryptionProfileOnClusterState, EnableEncryptionProfileOnClusterStateSelectors>> TRANSITIONS =
            new Transition.Builder<EnableEncryptionProfileOnClusterState, EnableEncryptionProfileOnClusterStateSelectors>()
                    .defaultFailureEvent(EnableEncryptionProfileOnClusterStateSelectors.FAILED_ENABLE_ENCRYPTION_PROFILE_ON_CLUSTER_EVENT)

                    .from(INIT_STATE)
                    .to(SET_ENCRYPTION_PROFILE_STATE)
                    .event(EnableEncryptionProfileOnClusterStateSelectors.ENABLE_ENCRYPTION_PROFILE_ON_CLUSTER_EVENT)
                    .defaultFailureEvent()

                    .from(SET_ENCRYPTION_PROFILE_STATE)
                    .to(UPDATE_CM_POLICY_STATE)
                    .event(EnableEncryptionProfileOnClusterStateSelectors.UPDATE_CM_POLICY_EVENT)
                    .defaultFailureEvent()

                    .from(UPDATE_CM_POLICY_STATE)
                    .to(GENERATE_ALTERNATIVE_CERTIFICATE_STATE)
                    .event(EnableEncryptionProfileOnClusterStateSelectors.GENERATE_ALTERNATIVE_CERTIFICATE_EVENT)
                    .defaultFailureEvent()

                    .from(GENERATE_ALTERNATIVE_CERTIFICATE_STATE)
                    .to(ENABLE_ENCRYPTION_PROFILE_ON_CLUSTER_FINISHED_STATE)
                    .event(EnableEncryptionProfileOnClusterStateSelectors.FINISH_ENABLE_ENCRYPTION_PROFILE_ON_CLUSTER_EVENT)
                    .defaultFailureEvent()

                    .from(ENABLE_ENCRYPTION_PROFILE_ON_CLUSTER_FINISHED_STATE)
                    .to(FINAL_STATE)
                    .event(EnableEncryptionProfileOnClusterStateSelectors.FINALIZE_ENABLE_ENCRYPTION_PROFILE_ON_CLUSTER_EVENT)
                    .defaultFailureEvent()

                    .build();

    protected EnableEncryptionProfileOnClusterFlowConfig() {
        super(EnableEncryptionProfileOnClusterState.class, EnableEncryptionProfileOnClusterStateSelectors.class);
    }

    @Override
    protected List<Transition<EnableEncryptionProfileOnClusterState, EnableEncryptionProfileOnClusterStateSelectors>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    public FlowEdgeConfig<EnableEncryptionProfileOnClusterState, EnableEncryptionProfileOnClusterStateSelectors> getEdgeConfig() {
        return new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, ENABLE_ENCRYPTION_PROFILE_ON_CLUSTER_FAILED_STATE,
                EnableEncryptionProfileOnClusterStateSelectors.HANDLED_FAILED_ENABLE_ENCRYPTION_PROFILE_ON_CLUSTER_EVENT);
    }

    @Override
    public EnableEncryptionProfileOnClusterStateSelectors[] getEvents() {
        return EnableEncryptionProfileOnClusterStateSelectors.values();
    }

    @Override
    public EnableEncryptionProfileOnClusterStateSelectors[] getInitEvents() {
        return new EnableEncryptionProfileOnClusterStateSelectors[] {EnableEncryptionProfileOnClusterStateSelectors.ENABLE_ENCRYPTION_PROFILE_ON_CLUSTER_EVENT};
    }

    @Override
    public String getDisplayName() {
        return "Enable Encryption Profile on Cluster";
    }

}
