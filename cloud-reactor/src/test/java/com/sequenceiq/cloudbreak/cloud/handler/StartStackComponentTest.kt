package com.sequenceiq.cloudbreak.cloud.handler

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull

import org.junit.Test

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest
import com.sequenceiq.cloudbreak.cloud.event.instance.StartInstancesRequest
import com.sequenceiq.cloudbreak.cloud.event.instance.StartInstancesResult
import com.sequenceiq.cloudbreak.cloud.event.model.EventStatus
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus

class StartStackComponentTest : AbstractComponentTest<StartInstancesResult>() {

    @Test
    fun testStartStack() {
        val result = sendCloudRequest()

        assertEquals(1, result.results.results.size.toLong())
        assertEquals(InstanceStatus.STARTED, result.results.results[0].status)
        assertFalse(result.status == EventStatus.FAILED)
        assertNull(result.errorDetails)
    }

    protected override val topicName: String
        get() = "STARTINSTANCESREQUEST"

    protected override val request: CloudPlatformRequest<Any>
        get() = StartInstancesRequest(g().createCloudContext(), g().createCloudCredential(), g().createCloudResourceList(), g().createCloudInstances())
}
