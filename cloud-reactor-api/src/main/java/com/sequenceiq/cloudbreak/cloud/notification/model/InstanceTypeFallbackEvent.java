package com.sequenceiq.cloudbreak.cloud.notification.model;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;

public class InstanceTypeFallbackEvent {

    public static final String INSTANCE_TYPE_FALLBACK_SELECTOR = "instance-type-fallback";

    private final CloudContext cloudContext;

    private final String instanceGroup;

    private final String originalType;

    private final String fallbackType;

    private final String reasonSummary;

    private final boolean exhausted;

    public InstanceTypeFallbackEvent(CloudContext cloudContext, String instanceGroup, String originalType, String fallbackType, String reasonSummary,
            boolean exhausted) {
        this.cloudContext = cloudContext;
        this.instanceGroup = instanceGroup;
        this.originalType = originalType;
        this.fallbackType = fallbackType;
        this.reasonSummary = reasonSummary;
        this.exhausted = exhausted;
    }

    public CloudContext getCloudContext() {
        return cloudContext;
    }

    public String getInstanceGroup() {
        return instanceGroup;
    }

    public String getOriginalType() {
        return originalType;
    }

    public String getFallbackType() {
        return fallbackType;
    }

    public String getReasonSummary() {
        return reasonSummary;
    }

    public boolean isExhausted() {
        return exhausted;
    }

    @Override
    public String toString() {
        return "InstanceTypeFallbackEvent{"
                + "cloudContext=" + cloudContext
                + ", instanceGroup='" + instanceGroup + '\''
                + ", originalType='" + originalType + '\''
                + ", fallbackType='" + fallbackType + '\''
                + ", reasonSummary='" + reasonSummary + '\''
                + ", exhausted=" + exhausted
                + '}';
    }
}
