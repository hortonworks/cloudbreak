package com.sequenceiq.cloudbreak.core.flow2.cluster.upscale

import com.sequenceiq.cloudbreak.core.flow2.FlowEvent
import com.sequenceiq.cloudbreak.core.flow2.FlowTriggers
import com.sequenceiq.cloudbreak.reactor.api.event.EventSelectorUtil
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.UpscaleClusterResult
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.UpscaleAmbariResult
import com.sequenceiq.cloudbreak.reactor.api.event.recipe.UpscalePostRecipesResult
import com.sequenceiq.cloudbreak.reactor.api.event.recipe.UpscalePreRecipesResult

enum class ClusterUpscaleEvent private constructor(private val stringRepresentation: String) : FlowEvent {
    CLUSTER_UPSCALE_TRIGGER_EVENT(FlowTriggers.CLUSTER_UPSCALE_TRIGGER_EVENT),
    UPSCALE_AMBARI_FINISHED_EVENT(EventSelectorUtil.selector(UpscaleAmbariResult::class.java)),
    UPSCALE_AMBARI_FAILED_EVENT(EventSelectorUtil.failureSelector(UpscaleAmbariResult::class.java)),
    EXECUTE_PRERECIPES_FINISHED_EVENT(EventSelectorUtil.selector(UpscalePreRecipesResult::class.java)),
    EXECUTE_PRERECIPES_FAILED_EVENT(EventSelectorUtil.failureSelector(UpscalePreRecipesResult::class.java)),
    CLUSTER_UPSCALE_FINISHED_EVENT(EventSelectorUtil.selector(UpscaleClusterResult::class.java)),
    CLUSTER_UPSCALE_FAILED_EVENT(EventSelectorUtil.failureSelector(UpscaleClusterResult::class.java)),
    EXECUTE_POSTRECIPES_FINISHED_EVENT(EventSelectorUtil.selector(UpscalePostRecipesResult::class.java)),
    EXECUTE_POSTRECIPES_FAILED_EVENT(EventSelectorUtil.failureSelector(UpscalePostRecipesResult::class.java)),
    FINALIZED_EVENT("CLUSTERUPSCALEFINALIZEDEVENT"),
    FAILURE_EVENT("CLUSTERUPSCALEFAILUREEVENT"),
    FAIL_HANDLED_EVENT("CLUSTERUPSCALEFAILHANDLEDEVENT");

    override fun stringRepresentation(): String {
        return stringRepresentation
    }
}
