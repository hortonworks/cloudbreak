package com.sequenceiq.cloudbreak.core.flow2.stack

import com.sequenceiq.cloudbreak.core.flow2.PayloadConverter
import com.sequenceiq.cloudbreak.reactor.api.ClusterPlatformResult
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent

class ClusterPlatformResponseToStackFailureConverter : PayloadConverter<StackFailureEvent> {
    override fun canConvert(sourceClass: Class<*>): Boolean {
        return ClusterPlatformResult<ClusterPlatformRequest>::class.java!!.isAssignableFrom(sourceClass)
    }

    override fun convert(payload: Any): StackFailureEvent {
        val clusterPlatformResult = payload as ClusterPlatformResult<ClusterPlatformRequest>
        return StackFailureEvent(clusterPlatformResult.request.stackId, clusterPlatformResult.errorDetails)
    }
}
