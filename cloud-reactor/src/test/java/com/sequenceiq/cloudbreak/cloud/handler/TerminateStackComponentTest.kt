package com.sequenceiq.cloudbreak.cloud.handler

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull

import org.junit.Test

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest
import com.sequenceiq.cloudbreak.cloud.event.model.EventStatus
import com.sequenceiq.cloudbreak.cloud.event.resource.TerminateStackRequest
import com.sequenceiq.cloudbreak.cloud.event.resource.TerminateStackResult

class TerminateStackComponentTest : AbstractComponentTest<TerminateStackResult>() {

    @Test
    fun testTerminateStack() {
        val result = sendCloudRequest()

        assertEquals(EventStatus.OK, result.status)
        assertNull(result.errorDetails)
    }

    protected override val topicName: String
        get() = "TERMINATESTACKREQUEST"

    protected override val request: CloudPlatformRequest<Any>
        get() = TerminateStackRequest(g().createCloudContext(), g().createCloudStack(), g().createCloudCredential(), g().createCloudResourceList())
}
