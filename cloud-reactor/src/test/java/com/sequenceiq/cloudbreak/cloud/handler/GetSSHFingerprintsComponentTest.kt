package com.sequenceiq.cloudbreak.cloud.handler

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue

import javax.inject.Inject

import org.junit.Test
import org.springframework.beans.factory.annotation.Qualifier

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest
import com.sequenceiq.cloudbreak.cloud.event.instance.GetSSHFingerprintsRequest
import com.sequenceiq.cloudbreak.cloud.event.instance.GetSSHFingerprintsResult
import com.sequenceiq.cloudbreak.cloud.event.model.EventStatus
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance

class GetSSHFingerprintsComponentTest : AbstractComponentTest<GetSSHFingerprintsResult>() {
    @Inject
    @Qualifier("instance")
    private val instance: CloudInstance? = null

    @Inject
    @Qualifier("bad-instance")
    private val instanceBad: CloudInstance? = null

    @Test
    fun testGetSSHFingerprints() {
        val result = sendCloudRequest()

        assertEquals(EventStatus.OK, result.status)
        assertTrue(result.sshFingerprints.contains(g().sshFingerprint))
    }

    @Test
    fun testGetSSHFingerprintsWithBadFingerprint() {
        val result = sendCloudRequest(badRequest)

        assertEquals(EventStatus.FAILED, result.status)
        assertNull(result.sshFingerprints)
    }

    protected override val topicName: String
        get() = "GETSSHFINGERPRINTSREQUEST"

    protected override val request: CloudPlatformRequest<Any>
        get() = GetSSHFingerprintsRequest(g().createCloudContext(), g().createCloudCredential(), instance)

    protected val badRequest: CloudPlatformRequest<Any>
        get() = GetSSHFingerprintsRequest(g().createCloudContext(), g().createCloudCredential(), instanceBad)
}
