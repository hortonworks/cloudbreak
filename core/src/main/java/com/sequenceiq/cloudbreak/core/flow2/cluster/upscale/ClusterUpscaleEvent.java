package com.sequenceiq.cloudbreak.core.flow2.cluster.upscale;

import com.sequenceiq.cloudbreak.core.flow2.FlowEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.EventSelectorUtil;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.UpscaleClusterResult;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.UpscaleAmbariResult;
import com.sequenceiq.cloudbreak.reactor.api.event.recipe.UpscalePostRecipesResult;
import com.sequenceiq.cloudbreak.reactor.api.event.recipe.UploadUpscaleRecipesResult;

public enum ClusterUpscaleEvent implements FlowEvent {
    CLUSTER_UPSCALE_TRIGGER_EVENT("CLUSTER_UPSCALE_TRIGGER_EVENT"),
    UPSCALE_AMBARI_FINISHED_EVENT(EventSelectorUtil.selector(UpscaleAmbariResult.class)),
    UPSCALE_AMBARI_FAILED_EVENT(EventSelectorUtil.failureSelector(UpscaleAmbariResult.class)),
    UPLOAD_UPSCALE_RECIPES_FINISHED_EVENT(EventSelectorUtil.selector(UploadUpscaleRecipesResult.class)),
    UPLOAD_UPSCALE_RECIPES_FAILED_EVENT(EventSelectorUtil.failureSelector(UploadUpscaleRecipesResult.class)),
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
