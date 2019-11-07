package com.sequenceiq.cloudbreak.core.flow2.cluster.provision;

import com.sequenceiq.cloudbreak.reactor.api.event.cluster.InstallClusterFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.InstallClusterSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.StartClusterFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.StartClusterSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.kerberos.KeytabConfigurationFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.kerberos.KeytabConfigurationSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.ldap.LdapSSOConfigurationFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.ldap.LdapSSOConfigurationSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.BootstrapMachinesFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.BootstrapMachinesSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.BootstrapPublicEndpointSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.ClusterProxyGatewayRegistrationFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.ClusterProxyGatewayRegistrationSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.ClusterProxyRegistrationFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.ClusterProxyRegistrationSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.HostMetadataSetupFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.HostMetadataSetupSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.StartAmbariServicesFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.StartAmbariServicesSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.recipe.UploadRecipesFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.recipe.UploadRecipesSuccess;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.event.EventSelectorUtil;

public enum ClusterCreationEvent implements FlowEvent {
    CLUSTER_CREATION_EVENT("CLUSTER_PROVISION_TRIGGER_EVENT"),
    CLUSTER_INSTALL_EVENT("CLUSTER_INSTALL_TRIGGER_EVENT"),
    BOOTSTRAP_MACHINES_FINISHED_EVENT(EventSelectorUtil.selector(BootstrapMachinesSuccess.class)),
    BOOTSTRAP_MACHINES_FAILED_EVENT(EventSelectorUtil.selector(BootstrapMachinesFailed.class)),
    CLUSTER_PROXY_REGISTRATION_SUCCEEDED_EVENT(EventSelectorUtil.selector(ClusterProxyRegistrationSuccess.class)),
    CLUSTER_PROXY_GATEWAY_REGISTRATION_SUCCEEDED_EVENT(EventSelectorUtil.selector(ClusterProxyGatewayRegistrationSuccess.class)),
    CLUSTER_PROXY_REGISTRATION_FAILED_EVENT(EventSelectorUtil.selector(ClusterProxyRegistrationFailed.class)),
    CLUSTER_PROXY_GATEWAY_REGISTRATION_FAILED_EVENT(EventSelectorUtil.selector(ClusterProxyGatewayRegistrationFailed.class)),
    HOST_METADATASETUP_FINISHED_EVENT(EventSelectorUtil.selector(HostMetadataSetupSuccess.class)),
    HOST_METADATASETUP_FAILED_EVENT(EventSelectorUtil.selector(HostMetadataSetupFailed.class)),
    BOOTSTRAP_PUBLIC_ENDPOINT_FINISHED_EVENT(EventSelectorUtil.selector(BootstrapPublicEndpointSuccess.class)),
    UPLOAD_RECIPES_FINISHED_EVENT(EventSelectorUtil.selector(UploadRecipesSuccess.class)),
    UPLOAD_RECIPES_FAILED_EVENT(EventSelectorUtil.selector(UploadRecipesFailed.class)),
    CONFIGURE_KEYTABS_FINISHED_EVENT(EventSelectorUtil.selector(KeytabConfigurationSuccess.class)),
    CONFIGURE_KEYTABS_FAILED_EVENT(EventSelectorUtil.selector(KeytabConfigurationFailed.class)),
    START_AMBARI_SERVICES_FINISHED_EVENT(EventSelectorUtil.selector(StartAmbariServicesSuccess.class)),
    START_AMBARI_SERVICES_FAILED_EVENT(EventSelectorUtil.selector(StartAmbariServicesFailed.class)),
    LDAP_SSO_CONFIGURATION_FINISHED_EVENT(EventSelectorUtil.selector(LdapSSOConfigurationSuccess.class)),
    LDAP_SSO_CONFIGURATION_FAILED_EVENT(EventSelectorUtil.selector(LdapSSOConfigurationFailed.class)),
    START_AMBARI_FINISHED_EVENT(EventSelectorUtil.selector(StartClusterSuccess.class)),
    START_AMBARI_FAILED_EVENT(EventSelectorUtil.selector(StartClusterFailed.class)),
    INSTALL_CLUSTER_FINISHED_EVENT(EventSelectorUtil.selector(InstallClusterSuccess.class)),
    INSTALL_CLUSTER_FAILED_EVENT(EventSelectorUtil.selector(InstallClusterFailed.class)),
    CLUSTER_CREATION_FAILED_EVENT("CLUSTER_CREATION_FAILED"),
    CLUSTER_CREATION_FINISHED_EVENT("CLUSTER_CREATION_FINISHED"),
    CLUSTER_CREATION_FAILURE_HANDLED_EVENT("CLUSTER_CREATION_FAILHANDLED");

    private final String event;

    ClusterCreationEvent(String event) {
        this.event = event;
    }

    @Override
    public String event() {
        return event;
    }
}
