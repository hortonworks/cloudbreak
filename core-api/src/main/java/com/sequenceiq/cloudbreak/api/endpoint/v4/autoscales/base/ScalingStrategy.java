package com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.base;

// This is intended more for stop-start APIs and future enhancements, and is not propagated/used at the moment
public enum ScalingStrategy {
    STOPSTART,
    STOPSTART_FALLBACK_TO_REGULAR
}
