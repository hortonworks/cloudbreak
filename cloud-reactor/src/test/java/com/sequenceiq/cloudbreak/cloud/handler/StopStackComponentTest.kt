package com.sequenceiq.cloudbreak.cloud.handler

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull

import org.junit.Test

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest
import com.sequenceiq.cloudbreak.cloud.event.instance.StopInstancesRequest
import com.sequenceiq.cloudbreak.cloud.event.instance.StopInstancesResult
import com.sequenceiq.cloudbreak.cloud.event.model.EventStatus
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus

class StopStackComponentTest : AbstractComponentTest<StopInstancesResult>() {

    @Test
    fun testStopStack() {
        val result = sendCloudRequest()

        assertEquals(1, result.results.results.size.toLong())
        assertEquals(InstanceStatus.STOPPED, result.results.results[0].status)
        assertFalse(result.status == EventStatus.FAILED)
        assertNull(result.errorDetails)
    }

    protected override val topicName: String
        get() = "STOPINSTANCESREQUEST"

    protected override val request: CloudPlatformRequest<Any>
        get() = StopInstancesRequest(g().createCloudContext(), g().createCloudCredential(),
                g().createCloudResourceList(), g().createCloudInstances())
}
