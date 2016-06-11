package com.sequenceiq.cloudbreak.core.flow2.cluster.upscale;

import com.sequenceiq.cloudbreak.core.flow2.FlowEvent;
import com.sequenceiq.cloudbreak.core.flow2.FlowTriggers;
import com.sequenceiq.cloudbreak.reactor.api.event.EventSelectorUtil;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.UpscaleClusterResult;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.UpscaleAmbariResult;
import com.sequenceiq.cloudbreak.reactor.api.event.recipe.UpscalePostRecipesResult;
import com.sequenceiq.cloudbreak.reactor.api.event.recipe.UpscalePreRecipesResult;

public enum ClusterUpscaleEvent implements FlowEvent {
    CLUSTER_UPSCALE_TRIGGER_EVENT(FlowTriggers.CLUSTER_UPSCALE_TRIGGER_EVENT),
    UPSCALE_AMBARI_FINISHED_EVENT(EventSelectorUtil.selector(UpscaleAmbariResult.class)),
    UPSCALE_AMBARI_FAILED_EVENT(EventSelectorUtil.failureSelector(UpscaleAmbariResult.class)),
    EXECUTE_PRERECIPES_FINISHED_EVENT(EventSelectorUtil.selector(UpscalePreRecipesResult.class)),
    EXECUTE_PRERECIPES_FAILED_EVENT(EventSelectorUtil.failureSelector(UpscalePreRecipesResult.class)),
    CLUSTER_UPSCALE_FINISHED_EVENT(EventSelectorUtil.selector(UpscaleClusterResult.class)),
    CLUSTER_UPSCALE_FAILED_EVENT(EventSelectorUtil.failureSelector(UpscaleClusterResult.class)),
    EXECUTE_POSTRECIPES_FINISHED_EVENT(EventSelectorUtil.selector(UpscalePostRecipesResult.class)),
    EXECUTE_POSTRECIPES_FAILED_EVENT(EventSelectorUtil.failureSelector(UpscalePostRecipesResult.class)),
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
