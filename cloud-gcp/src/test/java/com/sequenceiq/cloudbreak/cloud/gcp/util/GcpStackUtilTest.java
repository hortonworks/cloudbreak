package com.sequenceiq.cloudbreak.cloud.gcp.util;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;

public class GcpStackUtilTest {

    @Test
    public void projectIdConverterWithNewNameRestrictions() {
        String projectId = GcpStackUtil.getProjectId(cloudCredential("siq-haas"));
        Assert.assertEquals("siq-haas", projectId);
        projectId = GcpStackUtil.getProjectId(cloudCredential("siq-haas123"));
        Assert.assertEquals("siq-haas123", projectId);
        projectId = GcpStackUtil.getProjectId(cloudCredential("Siq-haas123"));
        Assert.assertEquals("siq-haas123", projectId);
    }

    @Test
    public void projectIdConverterWithOldNameRestrictions() {
        String projectId = GcpStackUtil.getProjectId(cloudCredential("echo:siq-haas"));
        Assert.assertEquals("echo-siq-haas", projectId);
        projectId = GcpStackUtil.getProjectId(cloudCredential("echo:>siq>-haas"));
        Assert.assertEquals("echo--siq--haas", projectId);
        projectId = GcpStackUtil.getProjectId(cloudCredential("e?cho:siq-haas123"));
        Assert.assertEquals("e-cho-siq-haas123", projectId);
        projectId = GcpStackUtil.getProjectId(cloudCredential("echo:siq-hasfdsf12?as"));
        Assert.assertEquals("echo-siq-hasfdsf12-as", projectId);
    }

    private CloudCredential cloudCredential(String projectId) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("projectId", projectId);
        return new CloudCredential(1L, "test", "sshkey", "cloudbreak", parameters);
    }

}