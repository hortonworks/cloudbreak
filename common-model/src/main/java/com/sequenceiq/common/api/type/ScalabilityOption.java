package com.sequenceiq.common.api.type;

public enum ScalabilityOption {
    ALLOWED,
    FORBIDDEN,
    ONLY_UPSCALE,
    ONLY_DOWNSCALE;

    public boolean upscalable() {
        return equals(ALLOWED) || equals(ONLY_UPSCALE);
    }

    public boolean downscalable() {
        return equals(ALLOWED) || equals(ONLY_DOWNSCALE);
    }
}
