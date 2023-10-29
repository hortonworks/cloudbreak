package com.sequenceiq.mock.model;

public enum RecommendationType {
    UPSCALE,
    DOWNSCALE;

    public RecommendationType toggle() {
        if (this == UPSCALE) {
            return DOWNSCALE;
        } else if (this == DOWNSCALE) {
            return UPSCALE;
        }
        return null;
    }
}
