package com.sequenceiq.cloudbreak.cloud.handler

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull

import org.junit.Test

import com.sequenceiq.cloudbreak.api.model.AdjustmentType
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest
import com.sequenceiq.cloudbreak.cloud.event.resource.LaunchStackRequest
import com.sequenceiq.cloudbreak.cloud.event.resource.LaunchStackResult
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus

class LaunchStackComponentTest : AbstractComponentTest<LaunchStackResult>() {

    @Test
    fun testLaunchStack() {
        val lsr = sendCloudRequest()
        val r = lsr.results

        assertEquals(ResourceStatus.CREATED, r[0].status)
        assertNull(lsr.errorDetails)
    }

    protected override val topicName: String
        get() = "LAUNCHSTACKREQUEST"

    protected override val request: CloudPlatformRequest<Any>
        get() = LaunchStackRequest(g().createCloudContext(), g().createCloudCredential(), g().createCloudStack(), AdjustmentType.BEST_EFFORT, 0L)
}
