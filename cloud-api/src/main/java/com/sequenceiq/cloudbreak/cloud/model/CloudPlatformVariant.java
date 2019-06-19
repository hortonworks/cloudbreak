package com.sequenceiq.cloudbreak.cloud.model;

import java.util.Objects;

public class CloudPlatformVariant {

    private final Platform platform;

    private final Variant variant;

    public CloudPlatformVariant(String platform, String variant) {
        this(Platform.platform(platform), Variant.variant(variant));
    }

    public CloudPlatformVariant(Platform platform, Variant variant) {
        this.platform = platform;
        this.variant = variant == null ? Variant.EMPTY : variant;
    }

    public Platform getPlatform() {
        return platform;
    }

    public Variant getVariant() {
        return variant;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || !Objects.equals(getClass(), o.getClass())) {
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
