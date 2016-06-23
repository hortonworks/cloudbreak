package com.sequenceiq.cloudbreak.cloud.handler

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull

import org.junit.Test

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest
import com.sequenceiq.cloudbreak.cloud.event.model.EventStatus
import com.sequenceiq.cloudbreak.cloud.event.resource.DownscaleStackRequest
import com.sequenceiq.cloudbreak.cloud.event.resource.DownscaleStackResult

class DownscaleStackComponentTest : AbstractComponentTest<DownscaleStackResult>() {

    @Test
    fun testUpscaleStack() {
        val result = sendCloudRequest()

        assertEquals(EventStatus.OK, result.status)
        assertEquals(1, result.downscaledResources.size.toLong())
        assertNull(result.errorDetails)
    }

    protected override val topicName: String
        get() = "DOWNSCALESTACKREQUEST"

    protected override val request: CloudPlatformRequest<Any>
        get() = DownscaleStackRequest(
                g().createCloudContext(),
                g().createCloudCredential(),
                g().createCloudStack(),
                g().createCloudResourceList(),
                g().createCloudInstances())
}
