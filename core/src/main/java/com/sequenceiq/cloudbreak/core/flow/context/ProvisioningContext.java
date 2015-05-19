package com.sequenceiq.cloudbreak.core.flow.context;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.service.stack.flow.CoreInstanceMetaData;

public class ProvisioningContext extends DefaultFlowContext implements FlowContext {
    private Map<String, Object> setupProperties = new HashMap<>();
    private Map<String, String> userDataParams = new HashMap<>();
    private Set<Resource> resources = new HashSet<>();
    private Set<CoreInstanceMetaData> coreInstanceMetaData = new HashSet<>();
    private String ambariIp;

    private ProvisioningContext(Builder builder) {
        super(builder.stackId, builder.cloudPlatform, builder.errorReason);
        this.setupProperties = builder.setupProperties;
        this.resources = builder.resources;
        this.coreInstanceMetaData = builder.coreInstanceMetaData;
        this.ambariIp = builder.ambariIp;
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

    @Override public String toString() {
        final StringBuilder sb = new StringBuilder("ProvisioningContext{");
        sb.append(", setupProperties=").append(setupProperties);
        sb.append(", userDataParams=").append(userDataParams);
        sb.append(", resources=").append(resources);
        sb.append(", coreInstanceMetaData=").append(coreInstanceMetaData);
        sb.append(", ambariIp='").append(ambariIp).append('\'');
        sb.append('}');
        return sb.toString();
    }

    public static class Builder {
        private Long stackId;
        private CloudPlatform cloudPlatform;
        private String errorReason = "";
        private Map<String, Object> setupProperties = new HashMap<>();
        private Set<Resource> resources = new HashSet<>();
        private Set<CoreInstanceMetaData> coreInstanceMetaData = new HashSet<>();
        private String ambariIp;

        public Builder setDefaultParams(Long stackId, CloudPlatform cloudPlatform) {
            this.stackId = stackId;
            this.cloudPlatform = cloudPlatform;
            return this;
        }

        public Builder setDefaultParams(Long stackId, CloudPlatform cloudPlatform, String errorReason) {
            this.stackId = stackId;
            this.cloudPlatform = cloudPlatform;
            this.errorReason = errorReason;
            return this;
        }

        public Builder setProvisionSetupProperties(Map<String, Object> setupProperties) {
            this.setupProperties.putAll(setupProperties);
            return this;
        }

        public Builder setProvisionedResources(Set<Resource> resources) {
            this.resources = resources;
            return this;
        }

        public Builder setCoreInstanceMetadata(Set<CoreInstanceMetaData> coreInstanceMetaData) {
            this.coreInstanceMetaData = coreInstanceMetaData;
            return this;
        }

        public Builder setAmbariIp(String ambariIp) {
            this.ambariIp = ambariIp;
            return this;
        }

        public Builder withProvisioningContext(ProvisioningContext provisioningContext) {
            this.stackId = provisioningContext.getStackId();
            this.cloudPlatform = provisioningContext.getCloudPlatform();
            this.errorReason = provisioningContext.getErrorReason();
            this.setupProperties = provisioningContext.getSetupProperties();
            this.resources = provisioningContext.getResources();
            this.coreInstanceMetaData = provisioningContext.getCoreInstanceMetaData();
            this.ambariIp = provisioningContext.getAmbariIp();
            return this;
        }

        public ProvisioningContext build() {
            return new ProvisioningContext(this);
        }
    }
}
