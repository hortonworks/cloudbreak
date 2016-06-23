package com.sequenceiq.cloudbreak.cloud.handler

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull

import org.junit.Test

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest
import com.sequenceiq.cloudbreak.cloud.event.resource.GetInstancesStateRequest
import com.sequenceiq.cloudbreak.cloud.event.resource.GetInstancesStateResult
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus

class InstanceStateComponentTest : AbstractComponentTest<GetInstancesStateResult>() {

    @Test
    fun testInstanceState() {
        val result = sendCloudRequest()

        assertEquals(InstanceStatus.STARTED, result.statuses[0].status)
        assertFalse(result.isFailed)
        assertNull(result.errorDetails)
    }

    protected override val topicName: String
        get() = "GETINSTANCESSTATEREQUEST"

    protected override val request: CloudPlatformRequest<Any>
        get() = GetInstancesStateRequest(g().createCloudContext(), g().createCloudCredential(), g().createCloudInstances())
}
