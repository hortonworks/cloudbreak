package com.sequenceiq.cloudbreak.core.flow2.cluster.upscale;

import com.sequenceiq.cloudbreak.core.flow2.FlowEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.EventSelectorUtil;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.AmbariEnsureComponentsAreStoppedResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.AmbariGatherInstalledComponentsResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.AmbariInitComponentsResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.AmbariInstallComponentsResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.AmbariRegenerateKerberosKeytabsResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.AmbariRepairSingleMasterStartResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.AmbariRestartAllResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.AmbariStartComponentsResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.AmbariStartServerAndAgentResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.AmbariStopComponentsResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.AmbariStopServerAndAgentResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.UpscaleClusterResult;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.UpscaleClusterManagerResult;
import com.sequenceiq.cloudbreak.reactor.api.event.recipe.UploadUpscaleRecipesResult;
import com.sequenceiq.cloudbreak.reactor.api.event.recipe.UpscalePostRecipesResult;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.UpscaleCheckHostMetadataResult;

public enum ClusterUpscaleEvent implements FlowEvent {
    CLUSTER_UPSCALE_TRIGGER_EVENT("CLUSTER_UPSCALE_TRIGGER_EVENT"),
    UPSCALE_AMBARI_FINISHED_EVENT(EventSelectorUtil.selector(UpscaleClusterManagerResult.class)),
    UPSCALE_AMBARI_FAILED_EVENT(EventSelectorUtil.failureSelector(UpscaleClusterManagerResult.class)),
    CLUSTER_REPAIR_SINGLE_MASTER_START_EVENT(EventSelectorUtil.selector(AmbariRepairSingleMasterStartResult.class)),
    UPLOAD_UPSCALE_RECIPES_FINISHED_EVENT(EventSelectorUtil.selector(UploadUpscaleRecipesResult.class)),
    UPLOAD_UPSCALE_RECIPES_FAILED_EVENT(EventSelectorUtil.failureSelector(UploadUpscaleRecipesResult.class)),
    CHECK_HOST_METADATA_FINISHED_EVENT(EventSelectorUtil.selector(UpscaleCheckHostMetadataResult.class)),
    CHECK_HOST_METADATA_FAILED_EVENT(EventSelectorUtil.failureSelector(UpscaleCheckHostMetadataResult.class)),

    AMBARI_REGENERATE_KERBEROS_KEYTABS_FINISHED_EVENT(EventSelectorUtil.selector(AmbariRegenerateKerberosKeytabsResult.class)),
    AMBARI_GATHER_INSTALLED_COMPONENTS_FINISHED_EVENT(EventSelectorUtil.selector(AmbariGatherInstalledComponentsResult.class)),
    AMBARI_STOP_COMPONENTS_FINISHED_EVENT(EventSelectorUtil.selector(AmbariStopComponentsResult.class)),
    AMBARI_STOP_SERVER_AGENT_FINISHED_EVENT(EventSelectorUtil.selector(AmbariStopServerAndAgentResult.class)),
    AMBARI_START_SERVER_AGENT_FINISHED_EVENT(EventSelectorUtil.selector(AmbariStartServerAndAgentResult.class)),
    AMBARI_ENSURE_COMPONENTS_STOPPED_FINISHED_EVENT(EventSelectorUtil.selector(AmbariEnsureComponentsAreStoppedResult.class)),
    AMBARI_INIT_COMPONENTS_FINISHED_EVENT(EventSelectorUtil.selector(AmbariInitComponentsResult.class)),
    AMBARI_INSTALL_COMPONENTS_FINISHED_EVENT(EventSelectorUtil.selector(AmbariInstallComponentsResult.class)),
    AMBARI_START_COMPONENTS_FINISHED_EVENT(EventSelectorUtil.selector(AmbariStartComponentsResult.class)),
    AMBARI_RESTART_ALL_FINISHED_EVENT(EventSelectorUtil.selector(AmbariRestartAllResult.class)),

    AMBARI_REGENERATE_KERBEROS_KEYTABS_FAILED_EVENT(EventSelectorUtil.failureSelector(AmbariRegenerateKerberosKeytabsResult.class)),
    AMBARI_GATHER_INSTALLED_COMPONENTS_FAILED_EVENT(EventSelectorUtil.failureSelector(AmbariGatherInstalledComponentsResult.class)),
    AMBARI_STOP_COMPONENTS_FAILED_EVENT(EventSelectorUtil.failureSelector(AmbariStopComponentsResult.class)),
    AMBARI_STOP_SERVER_AGENT_FAILED_EVENT(EventSelectorUtil.failureSelector(AmbariStopServerAndAgentResult.class)),
    AMBARI_START_SERVER_AGENT_FAILED_EVENT(EventSelectorUtil.failureSelector(AmbariStartServerAndAgentResult.class)),
    AMBARI_ENSURE_COMPONENTS_STOPPED_FAILED_EVENT(EventSelectorUtil.failureSelector(AmbariEnsureComponentsAreStoppedResult.class)),
    AMBARI_INIT_COMPONENTS_FAILED_EVENT(EventSelectorUtil.failureSelector(AmbariInitComponentsResult.class)),
    AMBARI_INSTALL_COMPONENTS_FAILED_EVENT(EventSelectorUtil.failureSelector(AmbariInstallComponentsResult.class)),
    AMBARI_START_COMPONENTS_FAILED_EVENT(EventSelectorUtil.failureSelector(AmbariStartComponentsResult.class)),
    AMBARI_RESTART_ALL_FAILED_EVENT(EventSelectorUtil.failureSelector(AmbariRestartAllResult.class)),

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
