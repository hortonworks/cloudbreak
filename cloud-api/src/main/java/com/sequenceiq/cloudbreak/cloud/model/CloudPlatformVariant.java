package com.sequenceiq.cloudbreak.cloud.model;

public class CloudPlatformVariant {

    private final Platform platform;

    private final Variant variant;

    public CloudPlatformVariant(Platform platform, Variant variant) {
        this.platform = platform;
        if (variant == null) {
            this.variant = Variant.EMPTY;
        } else {
            this.variant = variant;
        }
    }

    public Platform getPlatform() {
        return platform;
    }

    public Variant getVariant() {
        return variant;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        } else if (this == o) {
            return true;
        }

        CloudPlatformVariant that = (CloudPlatformVariant) o;

        return platform.equals(that.platform) && variant.equals(that.variant);

    }

    @Override
    public int hashCode() {
        return  31 * platform.hashCode() + variant.hashCode();
    }

    @Override
    public String toString() {
        return '{'
                + "platform='" + platform.value() + '\''
                + ", variant='" + variant.value() + '\''
                + '}';
    }
}
