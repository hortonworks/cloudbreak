package com.sequenceiq.cloudbreak.core.flow2.cluster.provision;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationEvent.BOOTSTRAP_MACHINES_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationEvent.BOOTSTRAP_MACHINES_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationEvent.CLUSTER_CREATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationEvent.CLUSTER_CREATION_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationEvent.CLUSTER_CREATION_FAILURE_HANDLED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationEvent.CLUSTER_CREATION_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationEvent.CLUSTER_INSTALL_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationEvent.HOST_METADATASETUP_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationEvent.HOST_METADATASETUP_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationEvent.INSTALL_CLUSTER_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationEvent.INSTALL_CLUSTER_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationEvent.LDAP_SSO_CONFIGURATION_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationEvent.LDAP_SSO_CONFIGURATION_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationEvent.MOUNT_DISKS_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationEvent.MOUNT_DISKS_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationEvent.START_AMBARI_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationEvent.START_AMBARI_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationEvent.START_AMBARI_SERVICES_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationEvent.START_AMBARI_SERVICES_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationEvent.UPLOAD_RECIPES_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationEvent.UPLOAD_RECIPES_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationState.BOOTSTRAPPING_MACHINES_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationState.CLUSTER_CREATION_FAILED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationState.CLUSTER_CREATION_FINISHED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationState.COLLECTING_HOST_METADATA_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationState.CONFIGURE_LDAP_SSO_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationState.FINAL_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationState.INIT_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationState.INSTALLING_CLUSTER_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationState.MOUNT_DISKS_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationState.STARTING_AMBARI_SERVICES_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationState.STARTING_AMBARI_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationState.UPLOAD_RECIPES_STATE;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.flow2.config.AbstractFlowConfiguration;
import com.sequenceiq.cloudbreak.core.flow2.config.AbstractFlowConfiguration.Transition.Builder;
import com.sequenceiq.cloudbreak.core.flow2.config.RetryableFlowConfiguration;

@Component
public class ClusterCreationFlowConfig extends AbstractFlowConfiguration<ClusterCreationState, ClusterCreationEvent>
        implements RetryableFlowConfiguration<ClusterCreationEvent> {
    private static final List<Transition<ClusterCreationState, ClusterCreationEvent>> TRANSITIONS =
            new Builder<ClusterCreationState, ClusterCreationEvent>().defaultFailureEvent(CLUSTER_CREATION_FAILED_EVENT)
            .from(INIT_STATE).to(BOOTSTRAPPING_MACHINES_STATE).event(CLUSTER_CREATION_EVENT).noFailureEvent()
            .from(INIT_STATE).to(INSTALLING_CLUSTER_STATE).event(CLUSTER_INSTALL_EVENT).noFailureEvent()
            .from(BOOTSTRAPPING_MACHINES_STATE).to(COLLECTING_HOST_METADATA_STATE).event(BOOTSTRAP_MACHINES_FINISHED_EVENT)
                    .failureEvent(BOOTSTRAP_MACHINES_FAILED_EVENT)
            .from(COLLECTING_HOST_METADATA_STATE).to(MOUNT_DISKS_STATE).event(HOST_METADATASETUP_FINISHED_EVENT)
                    .failureEvent(HOST_METADATASETUP_FAILED_EVENT)
            .from(MOUNT_DISKS_STATE).to(UPLOAD_RECIPES_STATE).event(MOUNT_DISKS_FINISHED_EVENT)
                    .failureEvent(MOUNT_DISKS_FAILED_EVENT)
            .from(UPLOAD_RECIPES_STATE).to(STARTING_AMBARI_SERVICES_STATE).event(UPLOAD_RECIPES_FINISHED_EVENT)
                    .failureEvent(UPLOAD_RECIPES_FAILED_EVENT)
            .from(STARTING_AMBARI_SERVICES_STATE).to(STARTING_AMBARI_STATE).event(START_AMBARI_SERVICES_FINISHED_EVENT)
                    .failureEvent(START_AMBARI_SERVICES_FAILED_EVENT)
            .from(STARTING_AMBARI_STATE).to(CONFIGURE_LDAP_SSO_STATE).event(START_AMBARI_FINISHED_EVENT)
                    .failureEvent(START_AMBARI_FAILED_EVENT)
            .from(CONFIGURE_LDAP_SSO_STATE).to(INSTALLING_CLUSTER_STATE).event(LDAP_SSO_CONFIGURATION_FINISHED_EVENT)
                    .failureEvent(LDAP_SSO_CONFIGURATION_FAILED_EVENT)
            .from(INSTALLING_CLUSTER_STATE).to(CLUSTER_CREATION_FINISHED_STATE).event(INSTALL_CLUSTER_FINISHED_EVENT)
                    .failureEvent(INSTALL_CLUSTER_FAILED_EVENT)
            .from(CLUSTER_CREATION_FINISHED_STATE).to(FINAL_STATE).event(CLUSTER_CREATION_FINISHED_EVENT).defaultFailureEvent()
            .build();

    private static final FlowEdgeConfig<ClusterCreationState, ClusterCreationEvent> EDGE_CONFIG =
            new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, CLUSTER_CREATION_FAILED_STATE, CLUSTER_CREATION_FAILURE_HANDLED_EVENT);

    public ClusterCreationFlowConfig() {
        super(ClusterCreationState.class, ClusterCreationEvent.class);
    }

    @Override
    public ClusterCreationFlowTriggerCondition getFlowTriggerCondition() {
        return getApplicationContext().getBean(ClusterCreationFlowTriggerCondition.class);
    }

    @Override
    public ClusterCreationEvent[] getEvents() {
        return ClusterCreationEvent.values();
    }

    @Override
    public ClusterCreationEvent[] getInitEvents() {
        return new ClusterCreationEvent[] {
                CLUSTER_CREATION_EVENT,
                CLUSTER_INSTALL_EVENT
        };
    }

    @Override
    protected List<Transition<ClusterCreationState, ClusterCreationEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    protected FlowEdgeConfig<ClusterCreationState, ClusterCreationEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public ClusterCreationEvent getFailHandledEvent() {
        return CLUSTER_CREATION_FAILURE_HANDLED_EVENT;
    }
}
