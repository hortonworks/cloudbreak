package com.sequenceiq.cloudbreak.cloud;

public class CloudPlatformVariant {
    private String platform;
    private String variant;

    public CloudPlatformVariant(String platform, String variant) {
        this.platform = platform;
        if (variant == null || variant.isEmpty()) {
            this.variant = this.platform;
        } else {
            this.variant = variant;
        }
    }

    public String getPlatform() {
        return platform;
    }

    public String getVariant() {
        return variant;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        CloudPlatformVariant that = (CloudPlatformVariant) o;

        if (!platform.equals(that.platform)) {
            return false;
        }
        return variant.equals(that.variant);

    }

    @Override
    public int hashCode() {
        return  31 * platform.hashCode() + variant.hashCode();
    }

    @Override
    public String toString() {
        return "{"
                + "platform='" + platform + '\''
                + ", variant='" + variant + '\''
                + '}';
    }
}
