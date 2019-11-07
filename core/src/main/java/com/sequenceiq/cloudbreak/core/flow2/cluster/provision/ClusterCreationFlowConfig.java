package com.sequenceiq.cloudbreak.core.flow2.cluster.provision;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationEvent.BOOTSTRAP_MACHINES_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationEvent.BOOTSTRAP_MACHINES_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationEvent.BOOTSTRAP_PUBLIC_ENDPOINT_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationEvent.CLUSTER_CREATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationEvent.CLUSTER_CREATION_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationEvent.CLUSTER_CREATION_FAILURE_HANDLED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationEvent.CLUSTER_CREATION_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationEvent.CLUSTER_INSTALL_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationEvent.CLUSTER_PROXY_GATEWAY_REGISTRATION_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationEvent.CLUSTER_PROXY_GATEWAY_REGISTRATION_SUCCEEDED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationEvent.CLUSTER_PROXY_REGISTRATION_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationEvent.CLUSTER_PROXY_REGISTRATION_SUCCEEDED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationEvent.CONFIGURE_KEYTABS_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationEvent.CONFIGURE_KEYTABS_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationEvent.HOST_METADATASETUP_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationEvent.HOST_METADATASETUP_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationEvent.INSTALL_CLUSTER_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationEvent.INSTALL_CLUSTER_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationEvent.LDAP_SSO_CONFIGURATION_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationEvent.LDAP_SSO_CONFIGURATION_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationEvent.START_AMBARI_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationEvent.START_AMBARI_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationEvent.START_AMBARI_SERVICES_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationEvent.START_AMBARI_SERVICES_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationEvent.UPLOAD_RECIPES_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationEvent.UPLOAD_RECIPES_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationState.BOOTSTRAPPING_MACHINES_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationState.BOOTSTRAPPING_PUBLIC_ENDPOINT_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationState.CLUSTER_CREATION_FAILED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationState.CLUSTER_CREATION_FINISHED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationState.CLUSTER_PROXY_GATEWAY_REGISTRATION_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationState.CLUSTER_PROXY_REGISTRATION_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationState.COLLECTING_HOST_METADATA_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationState.CONFIGURE_KEYTABS_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationState.CONFIGURE_LDAP_SSO_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationState.FINAL_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationState.INIT_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationState.INSTALLING_CLUSTER_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationState.STARTING_AMBARI_SERVICES_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationState.STARTING_AMBARI_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationState.UPLOAD_RECIPES_STATE;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration.Transition.Builder;
import com.sequenceiq.flow.core.config.RetryableFlowConfiguration;

@Component
public class ClusterCreationFlowConfig extends AbstractFlowConfiguration<ClusterCreationState, ClusterCreationEvent>
        implements RetryableFlowConfiguration<ClusterCreationEvent> {
    private static final List<Transition<ClusterCreationState, ClusterCreationEvent>> TRANSITIONS =
            new Builder<ClusterCreationState, ClusterCreationEvent>().defaultFailureEvent(CLUSTER_CREATION_FAILED_EVENT)
            .from(INIT_STATE).to(CLUSTER_PROXY_REGISTRATION_STATE).event(CLUSTER_CREATION_EVENT).noFailureEvent()
            .from(CLUSTER_PROXY_REGISTRATION_STATE).to(BOOTSTRAPPING_MACHINES_STATE).event(CLUSTER_PROXY_REGISTRATION_SUCCEEDED_EVENT)
                    .failureEvent(CLUSTER_PROXY_REGISTRATION_FAILED_EVENT)
            .from(INIT_STATE).to(INSTALLING_CLUSTER_STATE).event(CLUSTER_INSTALL_EVENT).noFailureEvent()
            .from(BOOTSTRAPPING_MACHINES_STATE).to(COLLECTING_HOST_METADATA_STATE).event(BOOTSTRAP_MACHINES_FINISHED_EVENT)
                    .failureEvent(BOOTSTRAP_MACHINES_FAILED_EVENT)
            .from(COLLECTING_HOST_METADATA_STATE).to(BOOTSTRAPPING_PUBLIC_ENDPOINT_STATE).event(HOST_METADATASETUP_FINISHED_EVENT)
                    .failureEvent(HOST_METADATASETUP_FAILED_EVENT)
            .from(BOOTSTRAPPING_PUBLIC_ENDPOINT_STATE).to(UPLOAD_RECIPES_STATE).event(BOOTSTRAP_PUBLIC_ENDPOINT_FINISHED_EVENT)
                    .defaultFailureEvent()
            .from(UPLOAD_RECIPES_STATE).to(CONFIGURE_KEYTABS_STATE).event(UPLOAD_RECIPES_FINISHED_EVENT)
                    .failureEvent(UPLOAD_RECIPES_FAILED_EVENT)
            .from(CONFIGURE_KEYTABS_STATE).to(STARTING_AMBARI_SERVICES_STATE).event(CONFIGURE_KEYTABS_FINISHED_EVENT)
                    .failureEvent(CONFIGURE_KEYTABS_FAILED_EVENT)
            .from(STARTING_AMBARI_SERVICES_STATE).to(STARTING_AMBARI_STATE).event(START_AMBARI_SERVICES_FINISHED_EVENT)
                    .failureEvent(START_AMBARI_SERVICES_FAILED_EVENT)
            .from(STARTING_AMBARI_STATE).to(CONFIGURE_LDAP_SSO_STATE).event(START_AMBARI_FINISHED_EVENT)
                    .failureEvent(START_AMBARI_FAILED_EVENT)
            .from(CONFIGURE_LDAP_SSO_STATE).to(INSTALLING_CLUSTER_STATE).event(LDAP_SSO_CONFIGURATION_FINISHED_EVENT)
                    .failureEvent(LDAP_SSO_CONFIGURATION_FAILED_EVENT)
            .from(INSTALLING_CLUSTER_STATE).to(CLUSTER_PROXY_GATEWAY_REGISTRATION_STATE).event(INSTALL_CLUSTER_FINISHED_EVENT)
                    .failureEvent(INSTALL_CLUSTER_FAILED_EVENT)
            .from(CLUSTER_PROXY_GATEWAY_REGISTRATION_STATE).to(CLUSTER_CREATION_FINISHED_STATE).event(CLUSTER_PROXY_GATEWAY_REGISTRATION_SUCCEEDED_EVENT)
                    .failureEvent(CLUSTER_PROXY_GATEWAY_REGISTRATION_FAILED_EVENT)
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
    public String getDisplayName() {
        return "Create cluster";
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
