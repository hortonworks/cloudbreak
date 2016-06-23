package com.sequenceiq.cloudbreak.cloud.gcp.util

import java.util.HashMap

import org.junit.Assert
import org.junit.Test

import com.sequenceiq.cloudbreak.cloud.model.CloudCredential

class GcpStackUtilTest {

    @Test
    fun projectIdConverterWithNewNameRestrictions() {
        var projectId = GcpStackUtil.getProjectId(cloudCredential("siq-haas"))
        Assert.assertEquals(projectId, "siq-haas")
        projectId = GcpStackUtil.getProjectId(cloudCredential("siq-haas123"))
        Assert.assertEquals(projectId, "siq-haas123")
        projectId = GcpStackUtil.getProjectId(cloudCredential("Siq-haas123"))
        Assert.assertEquals(projectId, "siq-haas123")
    }

    @Test
    fun projectIdConverterWithOldNameRestrictions() {
        var projectId = GcpStackUtil.getProjectId(cloudCredential("echo:siq-haas"))
        Assert.assertEquals(projectId, "echo-siq-haas")
        projectId = GcpStackUtil.getProjectId(cloudCredential("echo:>siq>-haas"))
        Assert.assertEquals(projectId, "echo--siq--haas")
        projectId = GcpStackUtil.getProjectId(cloudCredential("e?cho:siq-haas123"))
        Assert.assertEquals(projectId, "e-cho-siq-haas123")
        projectId = GcpStackUtil.getProjectId(cloudCredential("echo:siq-hasfdsf12?as"))
        Assert.assertEquals(projectId, "echo-siq-hasfdsf12-as")
    }

    private fun cloudCredential(projectId: String): CloudCredential {
        val parameters = HashMap<String, Any>()
        parameters.put("projectId", projectId)
        return CloudCredential(1L, "test", "sshkey", "cloudbreak", parameters)
    }

}