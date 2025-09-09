package com.sequenceiq.freeipa.flow;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.flow.core.config.FlowFinalizerCallback;
import com.sequenceiq.freeipa.service.stack.StackStatusService;

@Component
public class StackStatusFinalizer extends FlowFinalizerCallback {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackStatusFinalizer.class);

    @Inject
    private StackStatusService stackStatusService;

    @Override
    protected void doFinalize(Long resourceId) {
        stackStatusService.cleanupByStackId(resourceId);
    }
}
