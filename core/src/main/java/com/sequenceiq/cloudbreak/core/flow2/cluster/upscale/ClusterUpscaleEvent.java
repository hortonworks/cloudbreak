package com.sequenceiq.cloudbreak.core.flow2.cluster.upscale;

import com.sequenceiq.cloudbreak.core.flow.FlowPhases;
import com.sequenceiq.cloudbreak.core.flow2.FlowEvent;
import com.sequenceiq.cloudbreak.reactor.api.ClusterPlatformResult;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.AddClusterContainersResult;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.ConfigureSssdResult;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.ExecutePostRecipesResult;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.ExecutePreRecipesResult;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.InstallFsRecipesResult;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.InstallRecipesResult;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.InstallServicesResult;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.UpdateMetadataResult;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.WaitForAmbariHostsResult;

enum ClusterUpscaleEvent implements FlowEvent {

    ADD_CONTAINERS_EVENT(FlowPhases.ADD_CLUSTER_CONTAINERS.name()),
    ADD_CONTAINERS_FINISHED_EVENT(ClusterPlatformResult.selector(AddClusterContainersResult.class)),
    ADD_CONTAINERS_FAILED_EVENT(ClusterPlatformResult.failureSelector(AddClusterContainersResult.class)),
    INSTALL_FS_RECIPES_FINISHED_EVENT(ClusterPlatformResult.selector(InstallFsRecipesResult.class)),
    INSTALL_FS_RECIPES_FAILED_EVENT(ClusterPlatformResult.failureSelector(InstallFsRecipesResult.class)),
    WAIT_FOR_AMBARI_HOSTS_FINISHED_EVENT(ClusterPlatformResult.selector(WaitForAmbariHostsResult.class)),
    WAIT_FOR_AMBARI_HOSTS_FAILED_EVENT(ClusterPlatformResult.failureSelector(WaitForAmbariHostsResult.class)),
    SSSD_CONFIG_FINISHED_EVENT(ClusterPlatformResult.selector(ConfigureSssdResult.class)),
    SSSD_CONFIG_FAILED_EVENT(ClusterPlatformResult.failureSelector(ConfigureSssdResult.class)),
    INSTALL_RECIPES_FINISHED_EVENT(ClusterPlatformResult.selector(InstallRecipesResult.class)),
    INSTALL_RECIPES_FAILED_EVENT(ClusterPlatformResult.failureSelector(InstallRecipesResult.class)),
    EXECUTE_PRE_RECIPES_FINISHED_EVENT(ClusterPlatformResult.selector(ExecutePreRecipesResult.class)),
    EXECUTE_PRE_RECIPES_FAILED_EVENT(ClusterPlatformResult.failureSelector(ExecutePreRecipesResult.class)),
    INSTALL_SERVICES_FINISHED_EVENT(ClusterPlatformResult.selector(InstallServicesResult.class)),
    INSTALL_SERVICES_FAILED_EVENT(ClusterPlatformResult.failureSelector(InstallServicesResult.class)),
    EXECUTE_POST_RECIPES_FINISHED_EVENT(ClusterPlatformResult.selector(ExecutePostRecipesResult.class)),
    EXECUTE_POST_RECIPES_FAILED_EVENT(ClusterPlatformResult.failureSelector(ExecutePostRecipesResult.class)),
    UPDATE_METADATA_FINISHED_EVENT(ClusterPlatformResult.selector(UpdateMetadataResult.class)),
    UPDATE_METADATA_FAILED_EVENT(ClusterPlatformResult.failureSelector(UpdateMetadataResult.class)),
    FINALIZED_EVENT("CLUSTERUPSCALEFINALIZEDEVENT"),
    FAILURE_EVENT("CLUSTERUPSCALEFAILUREEVENT"),
    FAIL_HANDLED_EVENT("CLUSTERUPSCALEFAILHANDLEDEVENT");

    private String stringRepresentation;

    ClusterUpscaleEvent(String stringRepresentation) {
        this.stringRepresentation = stringRepresentation;
    }

    @Override
    public String stringRepresentation() {
        return stringRepresentation;
    }
}
