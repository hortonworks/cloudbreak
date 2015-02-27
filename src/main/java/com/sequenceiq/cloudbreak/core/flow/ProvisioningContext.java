package com.sequenceiq.cloudbreak.core.flow;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.service.stack.flow.CoreInstanceMetaData;

public class ProvisioningContext implements FlowContext {
    private CloudPlatform cloudPlatform;
    private Long stackId;
    private Map<String, Object> setupProperties = new HashMap<>();
    private Map<String, String> userDataParams = new HashMap<>();
    private Set<Resource> resources = new HashSet<>();
    private Set<CoreInstanceMetaData> coreInstanceMetaData = new HashSet<>();
    private String ambariIp;

    public CloudPlatform getCloudPlatform() {
        return cloudPlatform;
    }

    public void setCloudPlatform(CloudPlatform cloudPlatform) {
        this.cloudPlatform = cloudPlatform;
    }

    public Long getStackId() {
        return stackId;
    }

    public void setStackId(Long stackId) {
        this.stackId = stackId;
    }

    public Map<String, Object> getSetupProperties() {
        return setupProperties;
    }

    public Map<String, String> getUserDataParams() {
        return userDataParams;
    }

    public Set<Resource> getResources() {
        return resources;
    }

    public Set<CoreInstanceMetaData> getCoreInstanceMetaData() {
        return coreInstanceMetaData;
    }

    public String getAmbariIp() {
        return ambariIp;
    }

    public void setAmbariIp(String ambariIp) {
        this.ambariIp = ambariIp;
    }

    @Override public String toString() {
        final StringBuilder sb = new StringBuilder("ProvisioningContext{");
        sb.append("cloudPlatform=").append(cloudPlatform);
        sb.append(", stackId=").append(stackId);
        sb.append(", setupProperties=").append(setupProperties);
        sb.append(", userDataParams=").append(userDataParams);
        sb.append(", resources=").append(resources);
        sb.append(", coreInstanceMetaData=").append(coreInstanceMetaData);
        sb.append(", ambariIp='").append(ambariIp).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
