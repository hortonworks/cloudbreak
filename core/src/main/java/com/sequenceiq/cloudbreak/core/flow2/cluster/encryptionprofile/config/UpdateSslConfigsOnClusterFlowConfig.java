package com.sequenceiq.cloudbreak.core.flow2.cluster.encryptionprofile.config;


import static com.sequenceiq.cloudbreak.core.flow2.cluster.encryptionprofile.UpdateSslConfigsOnClusterState.FINAL_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.encryptionprofile.UpdateSslConfigsOnClusterState.GENERATE_ALTERNATIVE_CERTIFICATE_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.encryptionprofile.UpdateSslConfigsOnClusterState.INIT_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.encryptionprofile.UpdateSslConfigsOnClusterState.SET_ENCRYPTION_PROFILE_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.encryptionprofile.UpdateSslConfigsOnClusterState.UPDATE_CM_POLICY_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.encryptionprofile.UpdateSslConfigsOnClusterState.UPDATE_SSL_CONFIGS_ON_CLUSTER_FAILED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.encryptionprofile.UpdateSslConfigsOnClusterState.UPDATE_SSL_CONFIGS_ON_CLUSTER_FINISHED_STATE;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.flow2.StackStatusFinalizerAbstractFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.cluster.encryptionprofile.UpdateSslConfigsOnClusterState;
import com.sequenceiq.cloudbreak.core.flow2.cluster.encryptionprofile.UpdateSslConfigsOnClusterStateSelectors;

@Component
public class UpdateSslConfigsOnClusterFlowConfig
        extends StackStatusFinalizerAbstractFlowConfig<UpdateSslConfigsOnClusterState, UpdateSslConfigsOnClusterStateSelectors> {

    private static final List<Transition<UpdateSslConfigsOnClusterState, UpdateSslConfigsOnClusterStateSelectors>> TRANSITIONS =
            new Transition.Builder<UpdateSslConfigsOnClusterState, UpdateSslConfigsOnClusterStateSelectors>()
                    .defaultFailureEvent(UpdateSslConfigsOnClusterStateSelectors.FAILED_UPDATE_SSL_CONFIGS_ON_CLUSTER_EVENT)

                    .from(INIT_STATE)
                    .to(SET_ENCRYPTION_PROFILE_STATE)
                    .event(UpdateSslConfigsOnClusterStateSelectors.UPDATE_SSL_CONFIGS_ON_CLUSTER_EVENT)
                    .defaultFailureEvent()

                    .from(SET_ENCRYPTION_PROFILE_STATE)
                    .to(UPDATE_CM_POLICY_STATE)
                    .event(UpdateSslConfigsOnClusterStateSelectors.UPDATE_CM_POLICY_EVENT)
                    .defaultFailureEvent()

                    .from(UPDATE_CM_POLICY_STATE)
                    .to(GENERATE_ALTERNATIVE_CERTIFICATE_STATE)
                    .event(UpdateSslConfigsOnClusterStateSelectors.GENERATE_ALTERNATIVE_CERTIFICATE_EVENT)
                    .defaultFailureEvent()

                    .from(GENERATE_ALTERNATIVE_CERTIFICATE_STATE)
                    .to(UPDATE_SSL_CONFIGS_ON_CLUSTER_FINISHED_STATE)
                    .event(UpdateSslConfigsOnClusterStateSelectors.FINISH_UPDATE_SSL_CONFIGS_ON_CLUSTER_EVENT)
                    .defaultFailureEvent()

                    .from(UPDATE_SSL_CONFIGS_ON_CLUSTER_FINISHED_STATE)
                    .to(FINAL_STATE)
                    .event(UpdateSslConfigsOnClusterStateSelectors.FINALIZE_UPDATE_SSL_CONFIGS_ON_CLUSTER_EVENT)
                    .defaultFailureEvent()

                    .build();

    protected UpdateSslConfigsOnClusterFlowConfig() {
        super(UpdateSslConfigsOnClusterState.class, UpdateSslConfigsOnClusterStateSelectors.class);
    }

    @Override
    protected List<Transition<UpdateSslConfigsOnClusterState, UpdateSslConfigsOnClusterStateSelectors>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    public FlowEdgeConfig<UpdateSslConfigsOnClusterState, UpdateSslConfigsOnClusterStateSelectors> getEdgeConfig() {
        return new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, UPDATE_SSL_CONFIGS_ON_CLUSTER_FAILED_STATE,
                UpdateSslConfigsOnClusterStateSelectors.HANDLED_FAILED_UPDATE_SSL_CONFIGS_ON_CLUSTER_EVENT);
    }

    @Override
    public UpdateSslConfigsOnClusterStateSelectors[] getEvents() {
        return UpdateSslConfigsOnClusterStateSelectors.values();
    }

    @Override
    public UpdateSslConfigsOnClusterStateSelectors[] getInitEvents() {
        return new UpdateSslConfigsOnClusterStateSelectors[] {UpdateSslConfigsOnClusterStateSelectors.UPDATE_SSL_CONFIGS_ON_CLUSTER_EVENT};
    }

    @Override
    public String getDisplayName() {
        return "Update SSL Configs on Cluster";
    }

}