package com.sequenceiq.cloudbreak.cloud.handler

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull

import org.junit.Test

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest
import com.sequenceiq.cloudbreak.cloud.event.resource.UpscaleStackRequest
import com.sequenceiq.cloudbreak.cloud.event.resource.UpscaleStackResult
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus

class UpscaleStackComponentTest : AbstractComponentTest<UpscaleStackResult>() {

    @Test
    fun testUpscaleStack() {
        val result = sendCloudRequest()


        assertEquals(ResourceStatus.UPDATED, result.resourceStatus)
        assertEquals(1, result.results.size.toLong())
        assertEquals(ResourceStatus.UPDATED, result.results[0].status)
        assertFalse(result.isFailed)
        assertNull(result.errorDetails)
    }

    protected override val topicName: String
        get() = "UPSCALESTACKREQUEST"

    protected override val request: CloudPlatformRequest<Any>
        get() = UpscaleStackRequest(g().createCloudContext(), g().createCloudCredential(), g().createCloudStack(), g().createCloudResourceList())
}
