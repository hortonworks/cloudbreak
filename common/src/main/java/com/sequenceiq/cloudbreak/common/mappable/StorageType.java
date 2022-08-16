package com.sequenceiq.cloudbreak.common.mappable;

public enum StorageType {

    UNKNOWN(CloudPlatform.MOCK),
    S3(CloudPlatform.AWS),
    EBS(CloudPlatform.AWS);

    private final CloudPlatform cloudPlatform;

    StorageType(CloudPlatform cloudPlatform) {
        this.cloudPlatform = cloudPlatform;
    }

    public CloudPlatform cloudPlatform() {
        return cloudPlatform;
    }

    public String cloudPlatformName() {
        return cloudPlatform.name();
    }

    public boolean equalsIgnoreCase(String service) {
        return name().equalsIgnoreCase(service);
    }

}
