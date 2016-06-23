package com.sequenceiq.cloudbreak.core.flow2.stack

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult
import com.sequenceiq.cloudbreak.core.flow2.PayloadConverter
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent

class CloudPlatformResponseToStackFailureConverter : PayloadConverter<StackFailureEvent> {
    override fun canConvert(sourceClass: Class<*>): Boolean {
        return CloudPlatformResult<CloudPlatformRequest<Any>>::class.java!!.isAssignableFrom(sourceClass)
    }

    override fun convert(payload: Any): StackFailureEvent {
        val cloudPlatformResult = payload as CloudPlatformResult<CloudPlatformRequest<Any>>
        return StackFailureEvent(cloudPlatformResult.request!!.cloudContext.id, cloudPlatformResult.errorDetails)
    }
}
