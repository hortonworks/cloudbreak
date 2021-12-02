package com.sequenceiq.flow.core.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class FlowFinalizerCallback {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlowFinalizerCallback.class);

    protected abstract void doFinalize(Long resourceId);

    public final void onFinalize(Long resourceId) {
        try {
            doFinalize(resourceId);
        } catch (Exception e) {
            LOGGER.error("Flow finalizer callback failed.", e);
        }
    }
}
