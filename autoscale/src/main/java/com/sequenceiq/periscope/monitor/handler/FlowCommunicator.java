package com.sequenceiq.periscope.monitor.handler;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.client.CloudbreakInternalCrnClient;
import com.sequenceiq.flow.api.model.FlowCheckResponse;

@Service
public class FlowCommunicator {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlowCommunicator.class);

    @Inject
    private CloudbreakInternalCrnClient internalCrnClient;

    @Retryable(value = Exception.class, maxAttempts = 5, backoff = @Backoff(delay = 10000))
    public boolean hasActiveFlow(String flowId) {
        LOGGER.info("Invoking flow API to determine status of scaling flow with id: {}", flowId);
        boolean hasActiveFlow = false;
        if (!Strings.isNullOrEmpty(flowId)) {
            FlowCheckResponse flowCheckResponse = internalCrnClient.withInternalCrn().flowEndpoint().hasFlowRunningByFlowId(flowId);
            hasActiveFlow = flowCheckResponse.getHasActiveFlow();
        }

        return hasActiveFlow;
    }
}
