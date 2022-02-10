package com.sequenceiq.cloudbreak.common.event;

import java.io.Serializable;

public class PayloadContext implements Serializable {

    private final String resourceCrn;

    private final String environmentCrn;

    private final String cloudPlatform;

    public PayloadContext(String resourceCrn, String environmentCrn, String cloudPlatform) {
        this.resourceCrn = resourceCrn;
        this.environmentCrn = environmentCrn;
        this.cloudPlatform = cloudPlatform;
    }

    public PayloadContext(String resourceCrn, String cloudPlatform) {
        this.resourceCrn = resourceCrn;
        this.environmentCrn = null;
        this.cloudPlatform = cloudPlatform;
    }

    public static PayloadContext create(String resourceCrn, String cloudPlatform) {
        return new PayloadContext(resourceCrn, null, cloudPlatform);
    }

    public static PayloadContext create(String resourceCrn, String environmentCrn, String cloudPlatform) {
        return new PayloadContext(resourceCrn, environmentCrn, cloudPlatform);
    }

    public String getResourceCrn() {
        return resourceCrn;
    }

    public String getCloudPlatform() {
        return cloudPlatform;
    }

    public String getEnvironmentCrn() {
        return environmentCrn;
    }

    @Override
    public String toString() {
        return "PayloadContext{" +
                "resourceCrn='" + resourceCrn + '\'' +
                ", environmentCrn='" + environmentCrn + '\'' +
                ", cloudPlatform='" + cloudPlatform + '\'' +
                '}';
    }
}
