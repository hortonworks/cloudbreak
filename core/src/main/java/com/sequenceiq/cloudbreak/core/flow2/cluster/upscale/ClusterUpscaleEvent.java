package com.sequenceiq.cloudbreak.core.flow2.cluster.upscale;

import com.sequenceiq.cloudbreak.reactor.api.event.cluster.AmbariGatherInstalledComponentsResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.AmbariRepairSingleMasterStartResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.AmbariStopServerAndAgentResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.ClusterManagerEnsureComponentsAreStoppedResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.ClusterManagerInitComponentsResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.ClusterManagerInstallComponentsResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.ClusterManagerRestartAllResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.ClusterManagerStartComponentsResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.RegenerateKerberosKeytabsResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.StartServerAndAgentResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.StopClusterComponentsResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.UpscaleClusterResult;
import com.sequenceiq.cloudbreak.reactor.api.event.kerberos.KeytabConfigurationFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.kerberos.KeytabConfigurationSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.PreFlightCheckSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.UpscaleClusterManagerResult;
import com.sequenceiq.cloudbreak.reactor.api.event.recipe.UploadUpscaleRecipesResult;
import com.sequenceiq.cloudbreak.reactor.api.event.recipe.UpscalePostRecipesResult;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.UpscaleCheckHostMetadataResult;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.event.EventSelectorUtil;

public enum ClusterUpscaleEvent implements FlowEvent {
    CLUSTER_UPSCALE_TRIGGER_EVENT("CLUSTER_UPSCALE_TRIGGER_EVENT"),
    UPSCALE_CLUSTER_MANAGER_FINISHED_EVENT(EventSelectorUtil.selector(UpscaleClusterManagerResult.class)),
    UPSCALE_CLUSTER_MANAGER_FAILED_EVENT(EventSelectorUtil.failureSelector(UpscaleClusterManagerResult.class)),
    CLUSTER_REPAIR_SINGLE_MASTER_START_EVENT(EventSelectorUtil.selector(AmbariRepairSingleMasterStartResult.class)),
    UPLOAD_UPSCALE_RECIPES_FINISHED_EVENT(EventSelectorUtil.selector(UploadUpscaleRecipesResult.class)),
    UPLOAD_UPSCALE_RECIPES_FAILED_EVENT(EventSelectorUtil.failureSelector(UploadUpscaleRecipesResult.class)),
    RECONFIGURE_KEYTABS_FINISHED_EVENT(EventSelectorUtil.selector(KeytabConfigurationSuccess.class)),
    RECONFIGURE_KEYTABS_FAILED_EVENT(EventSelectorUtil.selector(KeytabConfigurationFailed.class)),
    CHECK_HOST_METADATA_FINISHED_EVENT(EventSelectorUtil.selector(UpscaleCheckHostMetadataResult.class)),
    CHECK_HOST_METADATA_FAILED_EVENT(EventSelectorUtil.failureSelector(UpscaleCheckHostMetadataResult.class)),
    PREFLIGHT_CHECK_FINISHED_EVENT(EventSelectorUtil.selector(PreFlightCheckSuccess.class)),

    CLUSTER_MANAGER_REGENERATE_KERBEROS_KEYTABS_FINISHED_EVENT(EventSelectorUtil.selector(RegenerateKerberosKeytabsResult.class)),
    CLUSTER_MANAGER_GATHER_INSTALLED_COMPONENTS_FINISHED_EVENT(EventSelectorUtil.selector(AmbariGatherInstalledComponentsResult.class)),
    CLUSTER_MANAGER_STOP_COMPONENTS_FINISHED_EVENT(EventSelectorUtil.selector(StopClusterComponentsResult.class)),
    CLUSTER_MANAGER_STOP_SERVER_AGENT_FINISHED_EVENT(EventSelectorUtil.selector(AmbariStopServerAndAgentResult.class)),
    CLUSTER_MANAGER_START_SERVER_AGENT_FINISHED_EVENT(EventSelectorUtil.selector(StartServerAndAgentResult.class)),
    CLUSTER_MANAGER_ENSURE_COMPONENTS_STOPPED_FINISHED_EVENT(EventSelectorUtil.selector(ClusterManagerEnsureComponentsAreStoppedResult.class)),
    CLUSTER_MANAGER_INIT_COMPONENTS_FINISHED_EVENT(EventSelectorUtil.selector(ClusterManagerInitComponentsResult.class)),
    CLUSTER_MANAGER_INSTALL_COMPONENTS_FINISHED_EVENT(EventSelectorUtil.selector(ClusterManagerInstallComponentsResult.class)),
    CLUSTER_MANAGER_START_COMPONENTS_FINISHED_EVENT(EventSelectorUtil.selector(ClusterManagerStartComponentsResult.class)),
    CLUSTER_MANAGER_RESTART_ALL_FINISHED_EVENT(EventSelectorUtil.selector(ClusterManagerRestartAllResult.class)),

    CLUSTER_MANAGER_REGENERATE_KERBEROS_KEYTABS_FAILED_EVENT(EventSelectorUtil.failureSelector(RegenerateKerberosKeytabsResult.class)),
    CLUSTER_MANAGER_GATHER_INSTALLED_COMPONENTS_FAILED_EVENT(EventSelectorUtil.failureSelector(AmbariGatherInstalledComponentsResult.class)),
    CLUSTER_MANAGER_STOP_COMPONENTS_FAILED_EVENT(EventSelectorUtil.failureSelector(StopClusterComponentsResult.class)),
    CLUSTER_MANAGER_STOP_SERVER_AGENT_FAILED_EVENT(EventSelectorUtil.failureSelector(AmbariStopServerAndAgentResult.class)),
    CLUSTER_MANAGER_START_SERVER_AGENT_FAILED_EVENT(EventSelectorUtil.failureSelector(StartServerAndAgentResult.class)),
    CLUSTER_MANAGER_ENSURE_COMPONENTS_STOPPED_FAILED_EVENT(EventSelectorUtil.failureSelector(ClusterManagerEnsureComponentsAreStoppedResult.class)),
    CLUSTER_MANAGER_INIT_COMPONENTS_FAILED_EVENT(EventSelectorUtil.failureSelector(ClusterManagerInitComponentsResult.class)),
    CLUSTER_MANAGER_INSTALL_COMPONENTS_FAILED_EVENT(EventSelectorUtil.failureSelector(ClusterManagerInstallComponentsResult.class)),
    CLUSTER_MANAGER_START_COMPONENTS_FAILED_EVENT(EventSelectorUtil.failureSelector(ClusterManagerStartComponentsResult.class)),
    CLUSTER_MANAGER_RESTART_ALL_FAILED_EVENT(EventSelectorUtil.failureSelector(ClusterManagerRestartAllResult.class)),

    CLUSTER_UPSCALE_FINISHED_EVENT(EventSelectorUtil.selector(UpscaleClusterResult.class)),
    CLUSTER_UPSCALE_FAILED_EVENT(EventSelectorUtil.failureSelector(UpscaleClusterResult.class)),
    EXECUTE_POSTRECIPES_FINISHED_EVENT(EventSelectorUtil.selector(UpscalePostRecipesResult.class)),
    EXECUTE_POSTRECIPES_FAILED_EVENT(EventSelectorUtil.failureSelector(UpscalePostRecipesResult.class)),
    FINALIZED_EVENT("CLUSTERUPSCALEFINALIZEDEVENT"),
    FAILURE_EVENT("CLUSTERUPSCALEFAILUREEVENT"),
    FAIL_HANDLED_EVENT("CLUSTERUPSCALEFAILHANDLEDEVENT");

    private final String event;

    ClusterUpscaleEvent(String event) {
        this.event = event;
    }

    @Override
    public String event() {
        return event;
    }
}
