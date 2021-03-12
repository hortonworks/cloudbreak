package com.sequenceiq.cloudbreak.core.flow2.cluster.provision;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationEvent.BOOTSTRAP_FREEIPA_ENDPOINT_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationEvent.BOOTSTRAP_MACHINES_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationEvent.BOOTSTRAP_MACHINES_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationEvent.BOOTSTRAP_PUBLIC_ENDPOINT_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationEvent.CLEANUP_FREEIPA_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationEvent.CLEANUP_FREEIPA_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationEvent.CLUSTER_CREATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationEvent.CLUSTER_CREATION_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationEvent.CLUSTER_CREATION_FAILURE_HANDLED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationEvent.CLUSTER_CREATION_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationEvent.CLUSTER_INSTALL_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationEvent.CLUSTER_PROXY_GATEWAY_REGISTRATION_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationEvent.CLUSTER_PROXY_GATEWAY_REGISTRATION_SUCCEEDED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationEvent.CLUSTER_PROXY_REGISTRATION_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationEvent.CLUSTER_PROXY_REGISTRATION_SUCCEEDED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationEvent.CLUSTER_UPDATE_CONFIG_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationEvent.CLUSTER_UPDATE_CONFIG_SUCCESS_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationEvent.CONFIGURE_KERBEROS_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationEvent.CONFIGURE_KERBEROS_SUCCESS_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationEvent.CONFIGURE_KEYTABS_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationEvent.CONFIGURE_KEYTABS_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationEvent.CONFIGURE_MANAGEMENT_SERVICES_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationEvent.CONFIGURE_MANAGEMENT_SERVICES_SUCCESS_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationEvent.CONFIGURE_SUPPORT_TAGS_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationEvent.CONFIGURE_SUPPORT_TAGS_SUCCESS_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationEvent.EXECUTE_POST_CLUSTER_MANAGER_START_RECIPES_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationEvent.EXECUTE_POST_CLUSTER_MANAGER_START_RECIPES_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationEvent.EXECUTE_POST_INSTALL_RECIPES_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationEvent.EXECUTE_POST_INSTALL_RECIPES_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationEvent.FINALIZE_CLUSTER_INSTALL_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationEvent.FINALIZE_CLUSTER_INSTALL_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationEvent.HOST_METADATASETUP_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationEvent.HOST_METADATASETUP_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationEvent.INSTALL_CLUSTER_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationEvent.INSTALL_CLUSTER_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationEvent.LDAP_SSO_CONFIGURATION_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationEvent.LDAP_SSO_CONFIGURATION_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationEvent.PREPARE_DATALAKE_RESOURCE_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationEvent.PREPARE_DATALAKE_RESOURCE_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationEvent.PREPARE_EXTENDED_TEMPLATE_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationEvent.PREPARE_EXTENDED_TEMPLATE_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationEvent.PREPARE_PROXY_CONFIG_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationEvent.PREPARE_PROXY_CONFIG_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationEvent.REFRESH_PARCEL_REPOS_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationEvent.REFRESH_PARCEL_REPOS_SUCCESS_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationEvent.SETUP_MONITORING_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationEvent.SETUP_MONITORING_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationEvent.START_AMBARI_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationEvent.START_AMBARI_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationEvent.START_AMBARI_SERVICES_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationEvent.START_AMBARI_SERVICES_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationEvent.START_MANAGEMENT_SERVICES_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationEvent.START_MANAGEMENT_SERVICES_SUCCESS_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationEvent.SUPPRESS_WARNINGS_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationEvent.SUPPRESS_WARNINGS_SUCCESS_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationEvent.UPLOAD_RECIPES_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationEvent.UPLOAD_RECIPES_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationEvent.VALIDATE_LICENCE_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationEvent.VALIDATE_LICENCE_SUCCESS_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationEvent.WAIT_FOR_CLUSTER_MANAGER_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationEvent.WAIT_FOR_CLUSTER_MANAGER_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationState.BOOTSTRAPPING_FREEIPA_ENDPOINT_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationState.BOOTSTRAPPING_MACHINES_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationState.BOOTSTRAPPING_PUBLIC_ENDPOINT_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationState.CLEANUP_FREEIPA_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationState.CLUSTER_CREATION_FAILED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationState.CLUSTER_CREATION_FINISHED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationState.CLUSTER_PROXY_GATEWAY_REGISTRATION_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationState.CLUSTER_PROXY_REGISTRATION_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationState.COLLECTING_HOST_METADATA_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationState.CONFIGURE_KERBEROS_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationState.CONFIGURE_KEYTABS_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationState.CONFIGURE_LDAP_SSO_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationState.CONFIGURE_MANAGEMENT_SERVICES_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationState.CONFIGURE_SUPPORT_TAGS_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationState.EXECUTE_POST_CLUSTER_MANAGER_START_RECIPES_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationState.EXECUTE_POST_INSTALL_RECIPES_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationState.FINAL_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationState.FINALIZE_CLUSTER_INSTALL_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationState.INIT_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationState.INSTALLING_CLUSTER_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationState.PREPARE_DATALAKE_RESOURCE_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationState.PREPARE_EXTENDED_TEMPLATE_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationState.PREPARE_PROXY_CONFIG_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationState.REFRESH_PARCEL_REPOS_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationState.SETUP_MONITORING_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationState.STARTING_CLUSTER_MANAGER_SERVICES_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationState.STARTING_CLUSTER_MANAGER_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationState.START_MANAGEMENT_SERVICES_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationState.SUPPRESS_WARNINGS_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationState.UPDATE_CONFIG_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationState.UPLOAD_RECIPES_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationState.VALIDATE_LICENCE_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationState.WAIT_FOR_CLUSTER_MANAGER_STATE;

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
            .from(INIT_STATE)
                    .to(CLUSTER_PROXY_REGISTRATION_STATE)
                    .event(CLUSTER_CREATION_EVENT)
                    .noFailureEvent()
            .from(CLUSTER_PROXY_REGISTRATION_STATE)
                    .to(BOOTSTRAPPING_MACHINES_STATE)
                    .event(CLUSTER_PROXY_REGISTRATION_SUCCEEDED_EVENT)
                    .failureEvent(CLUSTER_PROXY_REGISTRATION_FAILED_EVENT)
            .from(INIT_STATE)
                    .to(INSTALLING_CLUSTER_STATE)
                    .event(CLUSTER_INSTALL_EVENT)
                    .noFailureEvent()
            .from(BOOTSTRAPPING_MACHINES_STATE)
                    .to(COLLECTING_HOST_METADATA_STATE)
                    .event(BOOTSTRAP_MACHINES_FINISHED_EVENT)
                    .failureEvent(BOOTSTRAP_MACHINES_FAILED_EVENT)
            .from(COLLECTING_HOST_METADATA_STATE)
                    .to(CLEANUP_FREEIPA_STATE)
                    .event(HOST_METADATASETUP_FINISHED_EVENT)
                    .failureEvent(HOST_METADATASETUP_FAILED_EVENT)
            .from(CLEANUP_FREEIPA_STATE)
                    .to(BOOTSTRAPPING_PUBLIC_ENDPOINT_STATE)
                    .event(CLEANUP_FREEIPA_FINISHED_EVENT)
                    .failureEvent(CLEANUP_FREEIPA_FAILED_EVENT)
            .from(BOOTSTRAPPING_PUBLIC_ENDPOINT_STATE)
                    .to(BOOTSTRAPPING_FREEIPA_ENDPOINT_STATE)
                    .event(BOOTSTRAP_PUBLIC_ENDPOINT_FINISHED_EVENT)
                    .defaultFailureEvent()
            .from(BOOTSTRAPPING_FREEIPA_ENDPOINT_STATE)
                    .to(UPLOAD_RECIPES_STATE)
                    .event(BOOTSTRAP_FREEIPA_ENDPOINT_FINISHED_EVENT)
                .defaultFailureEvent()
            .from(UPLOAD_RECIPES_STATE)
                    .to(CONFIGURE_KEYTABS_STATE)
                    .event(UPLOAD_RECIPES_FINISHED_EVENT)
                    .failureEvent(UPLOAD_RECIPES_FAILED_EVENT)
            .from(CONFIGURE_KEYTABS_STATE)
                    .to(STARTING_CLUSTER_MANAGER_SERVICES_STATE)
                    .event(CONFIGURE_KEYTABS_FINISHED_EVENT)
                    .failureEvent(CONFIGURE_KEYTABS_FAILED_EVENT)
            .from(STARTING_CLUSTER_MANAGER_SERVICES_STATE)
                    .to(STARTING_CLUSTER_MANAGER_STATE)
                    .event(START_AMBARI_SERVICES_FINISHED_EVENT)
                    .failureEvent(START_AMBARI_SERVICES_FAILED_EVENT)
            .from(STARTING_CLUSTER_MANAGER_STATE)
                    .to(CONFIGURE_LDAP_SSO_STATE)
                    .event(START_AMBARI_FINISHED_EVENT)
                    .failureEvent(START_AMBARI_FAILED_EVENT)
            .from(CONFIGURE_LDAP_SSO_STATE)
                    .to(WAIT_FOR_CLUSTER_MANAGER_STATE)
                    .event(LDAP_SSO_CONFIGURATION_FINISHED_EVENT)
                    .failureEvent(LDAP_SSO_CONFIGURATION_FAILED_EVENT)
            .from(WAIT_FOR_CLUSTER_MANAGER_STATE)
                    .to(EXECUTE_POST_CLUSTER_MANAGER_START_RECIPES_STATE)
                    .event(WAIT_FOR_CLUSTER_MANAGER_FINISHED_EVENT)
                    .failureEvent(WAIT_FOR_CLUSTER_MANAGER_FAILED_EVENT)
            .from(EXECUTE_POST_CLUSTER_MANAGER_START_RECIPES_STATE)
                    .to(PREPARE_PROXY_CONFIG_STATE)
                    .event(EXECUTE_POST_CLUSTER_MANAGER_START_RECIPES_FINISHED_EVENT)
                    .failureEvent(EXECUTE_POST_CLUSTER_MANAGER_START_RECIPES_FAILED_EVENT)
            .from(PREPARE_PROXY_CONFIG_STATE)
                    .to(SETUP_MONITORING_STATE)
                    .event(PREPARE_PROXY_CONFIG_FINISHED_EVENT)
                    .failureEvent(PREPARE_PROXY_CONFIG_FAILED_EVENT)
            .from(SETUP_MONITORING_STATE)
                    .to(PREPARE_EXTENDED_TEMPLATE_STATE)
                    .event(SETUP_MONITORING_FINISHED_EVENT)
                    .failureEvent(SETUP_MONITORING_FAILED_EVENT)
            .from(PREPARE_EXTENDED_TEMPLATE_STATE)
                    .to(VALIDATE_LICENCE_STATE)
                    .event(PREPARE_EXTENDED_TEMPLATE_FINISHED_EVENT)
                    .failureEvent(PREPARE_EXTENDED_TEMPLATE_FAILED_EVENT)
            .from(VALIDATE_LICENCE_STATE)
                    .to(CONFIGURE_MANAGEMENT_SERVICES_STATE)
                    .event(VALIDATE_LICENCE_SUCCESS_EVENT)
                    .failureEvent(VALIDATE_LICENCE_FAILED_EVENT)
            .from(CONFIGURE_MANAGEMENT_SERVICES_STATE)
                    .to(CONFIGURE_SUPPORT_TAGS_STATE)
                    .event(CONFIGURE_MANAGEMENT_SERVICES_SUCCESS_EVENT)
                    .failureEvent(CONFIGURE_MANAGEMENT_SERVICES_FAILED_EVENT)
            .from(CONFIGURE_SUPPORT_TAGS_STATE)
                    .to(UPDATE_CONFIG_STATE)
                    .event(CONFIGURE_SUPPORT_TAGS_SUCCESS_EVENT)
                    .failureEvent(CONFIGURE_SUPPORT_TAGS_FAILED_EVENT)
            .from(UPDATE_CONFIG_STATE)
                    .to(REFRESH_PARCEL_REPOS_STATE)
                    .event(CLUSTER_UPDATE_CONFIG_SUCCESS_EVENT)
                    .failureEvent(CLUSTER_UPDATE_CONFIG_FAILED_EVENT)
            .from(REFRESH_PARCEL_REPOS_STATE)
                    .to(INSTALLING_CLUSTER_STATE)
                    .event(REFRESH_PARCEL_REPOS_SUCCESS_EVENT)
                    .failureEvent(REFRESH_PARCEL_REPOS_FAILED_EVENT)
            .from(INSTALLING_CLUSTER_STATE)
                    .to(START_MANAGEMENT_SERVICES_STATE)
                    .event(INSTALL_CLUSTER_FINISHED_EVENT)
                    .failureEvent(INSTALL_CLUSTER_FAILED_EVENT)
            .from(START_MANAGEMENT_SERVICES_STATE)
                    .to(SUPPRESS_WARNINGS_STATE)
                    .event(START_MANAGEMENT_SERVICES_SUCCESS_EVENT)
                    .failureEvent(START_MANAGEMENT_SERVICES_FAILED_EVENT)
            .from(SUPPRESS_WARNINGS_STATE)
                    .to(CONFIGURE_KERBEROS_STATE)
                    .event(SUPPRESS_WARNINGS_SUCCESS_EVENT)
                    .failureEvent(SUPPRESS_WARNINGS_FAILED_EVENT)
            .from(CONFIGURE_KERBEROS_STATE)
                    .to(EXECUTE_POST_INSTALL_RECIPES_STATE)
                    .event(CONFIGURE_KERBEROS_SUCCESS_EVENT)
                    .failureEvent(CONFIGURE_KERBEROS_FAILED_EVENT)
            .from(EXECUTE_POST_INSTALL_RECIPES_STATE)
                    .to(PREPARE_DATALAKE_RESOURCE_STATE)
                    .event(EXECUTE_POST_INSTALL_RECIPES_FINISHED_EVENT)
                    .failureEvent(EXECUTE_POST_INSTALL_RECIPES_FAILED_EVENT)
            .from(PREPARE_DATALAKE_RESOURCE_STATE)
                    .to(FINALIZE_CLUSTER_INSTALL_STATE)
                    .event(PREPARE_DATALAKE_RESOURCE_FINISHED_EVENT)
                    .failureEvent(PREPARE_DATALAKE_RESOURCE_FAILED_EVENT)
            .from(FINALIZE_CLUSTER_INSTALL_STATE)
                    .to(CLUSTER_PROXY_GATEWAY_REGISTRATION_STATE)
                    .event(FINALIZE_CLUSTER_INSTALL_FINISHED_EVENT)
                    .failureEvent(FINALIZE_CLUSTER_INSTALL_FAILED_EVENT)
            .from(CLUSTER_PROXY_GATEWAY_REGISTRATION_STATE)
                    .to(CLUSTER_CREATION_FINISHED_STATE)
                    .event(CLUSTER_PROXY_GATEWAY_REGISTRATION_SUCCEEDED_EVENT)
                    .failureEvent(CLUSTER_PROXY_GATEWAY_REGISTRATION_FAILED_EVENT)
            .from(CLUSTER_CREATION_FINISHED_STATE)
                    .to(FINAL_STATE)
                    .event(CLUSTER_CREATION_FINISHED_EVENT)
                    .defaultFailureEvent()
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
    public ClusterCreationEvent getRetryableEvent() {
        return CLUSTER_CREATION_FAILURE_HANDLED_EVENT;
    }
}
