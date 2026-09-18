package com.sequenceiq.cloudbreak.core.flow2.cluster.encryptionprofile.config;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.encryptionprofile.DisableEncryptionProfileOnClusterState.DISABLE_ENCRYPTION_PROFILE_ON_CLUSTER_FAILED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.encryptionprofile.DisableEncryptionProfileOnClusterState.DISABLE_ENCRYPTION_PROFILE_ON_CLUSTER_FINISHED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.encryptionprofile.DisableEncryptionProfileOnClusterState.FINAL_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.encryptionprofile.DisableEncryptionProfileOnClusterState.INIT_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.encryptionprofile.DisableEncryptionProfileOnClusterState.UNSET_ENCRYPTION_PROFILE_STATE;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.flow2.StackStatusFinalizerAbstractFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.cluster.encryptionprofile.DisableEncryptionProfileOnClusterState;
import com.sequenceiq.cloudbreak.core.flow2.cluster.encryptionprofile.DisableEncryptionProfileOnClusterStateSelectors;

@Component
public class DisableEncryptionProfileOnClusterFlowConfig
        extends StackStatusFinalizerAbstractFlowConfig<DisableEncryptionProfileOnClusterState, DisableEncryptionProfileOnClusterStateSelectors> {

    private static final List<Transition<DisableEncryptionProfileOnClusterState, DisableEncryptionProfileOnClusterStateSelectors>> TRANSITIONS =
            new Transition.Builder<DisableEncryptionProfileOnClusterState, DisableEncryptionProfileOnClusterStateSelectors>()
                    .defaultFailureEvent(DisableEncryptionProfileOnClusterStateSelectors.FAILED_DISABLE_ENCRYPTION_PROFILE_ON_CLUSTER_EVENT)

                    .from(INIT_STATE)
                    .to(UNSET_ENCRYPTION_PROFILE_STATE)
                    .event(DisableEncryptionProfileOnClusterStateSelectors.DISABLE_ENCRYPTION_PROFILE_ON_CLUSTER_EVENT)
                    .defaultFailureEvent()

                    .from(UNSET_ENCRYPTION_PROFILE_STATE)
                    .to(DISABLE_ENCRYPTION_PROFILE_ON_CLUSTER_FINISHED_STATE)
                    .event(DisableEncryptionProfileOnClusterStateSelectors.UNSET_ENCRYPTION_PROFILE_FINISHED_EVENT)
                    .defaultFailureEvent()

                    .from(DISABLE_ENCRYPTION_PROFILE_ON_CLUSTER_FINISHED_STATE)
                    .to(FINAL_STATE)
                    .event(DisableEncryptionProfileOnClusterStateSelectors.FINALIZE_DISABLE_ENCRYPTION_PROFILE_ON_CLUSTER_EVENT)
                    .defaultFailureEvent()

                    .build();

    protected DisableEncryptionProfileOnClusterFlowConfig() {
        super(DisableEncryptionProfileOnClusterState.class, DisableEncryptionProfileOnClusterStateSelectors.class);
    }

    @Override
    protected List<Transition<DisableEncryptionProfileOnClusterState, DisableEncryptionProfileOnClusterStateSelectors>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    public FlowEdgeConfig<DisableEncryptionProfileOnClusterState, DisableEncryptionProfileOnClusterStateSelectors> getEdgeConfig() {
        return new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, DISABLE_ENCRYPTION_PROFILE_ON_CLUSTER_FAILED_STATE,
                DisableEncryptionProfileOnClusterStateSelectors.HANDLED_FAILED_DISABLE_ENCRYPTION_PROFILE_ON_CLUSTER_EVENT);
    }

    @Override
    public DisableEncryptionProfileOnClusterStateSelectors[] getEvents() {
        return DisableEncryptionProfileOnClusterStateSelectors.values();
    }

    @Override
    public DisableEncryptionProfileOnClusterStateSelectors[] getInitEvents() {
        return new DisableEncryptionProfileOnClusterStateSelectors[] {
                DisableEncryptionProfileOnClusterStateSelectors.DISABLE_ENCRYPTION_PROFILE_ON_CLUSTER_EVENT
        };
    }

    @Override
    public String getDisplayName() {
        return "Disable Encryption Profile on Cluster";
    }

}
