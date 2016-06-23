package com.sequenceiq.cloudbreak.cloud.handler

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull

import org.junit.Test

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest
import com.sequenceiq.cloudbreak.cloud.event.instance.CollectMetadataRequest
import com.sequenceiq.cloudbreak.cloud.event.instance.CollectMetadataResult
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus

class CollectMetadataComponentTest : AbstractComponentTest<CollectMetadataResult>() {

    @Test
    fun testCollectMetadata() {
        val result = sendCloudRequest()

        assertEquals(1, result.results.size.toLong())
        assertEquals(InstanceStatus.IN_PROGRESS, result.results[0].cloudVmInstanceStatus.status)
        assertNull(result.errorDetails)
    }

    protected override val topicName: String
        get() = "COLLECTMETADATAREQUEST"

    protected override val request: CloudPlatformRequest<Any>
        get() = CollectMetadataRequest(
                g().createCloudContext(),
                g().createCloudCredential(),
                g().createCloudResourceList(),
                g().createCloudInstances())
}
