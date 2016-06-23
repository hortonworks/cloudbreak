package com.sequenceiq.cloudbreak.core.flow2.stack.provision

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult
import com.sequenceiq.cloudbreak.cloud.event.setup.PrepareImageResult
import com.sequenceiq.cloudbreak.core.flow2.PayloadConverter
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent

class PrepareImageResultToStackEventConverter : PayloadConverter<StackEvent> {
    override fun canConvert(sourceClass: Class<*>): Boolean {
        return PrepareImageResult::class.java!!.isAssignableFrom(sourceClass)
    }

    override fun convert(payload: Any): StackEvent {
        return StackEvent((payload as CloudPlatformResult<CloudPlatformRequest<Any>>).request!!.cloudContext.id)
    }
}
