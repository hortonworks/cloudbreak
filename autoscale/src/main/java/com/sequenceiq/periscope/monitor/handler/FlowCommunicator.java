package com.sequenceiq.periscope.monitor.handler;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.stream.Collectors.toMap;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.client.CloudbreakInternalCrnClient;
import com.sequenceiq.cloudbreak.util.Benchmark;
import com.sequenceiq.flow.api.model.FlowCheckResponse;

@Service
public class FlowCommunicator {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlowCommunicator.class);

    private static final Integer PAGE_SIZE = 50;

    private static final Integer PAGE = 0;

    @Inject
    private CloudbreakInternalCrnClient internalCrnClient;

    public Map<Long, FlowCheckResponse> getFlowStatusFromFlowIds(Map<String, Long> activityIdsByFlowIds) {
        LOGGER.info("Sending request to fetch FlowStatus of {} flowIds", activityIdsByFlowIds.size());
        List<String> allFlowIds = newArrayList(activityIdsByFlowIds.keySet());
        List<FlowCheckResponse> flowCheckResponses = newArrayList();
        Benchmark.measure(() -> Lists.partition(allFlowIds, PAGE_SIZE).forEach(flowIds ->
                flowCheckResponses.addAll(invokeCBFlowEndpoint(flowIds, flowIds.size(), PAGE))),
                LOGGER, "Fetched flow details in {} ms for {} ids", activityIdsByFlowIds.size());

        return flowCheckResponses.stream().collect(toMap(response -> activityIdsByFlowIds.get(response.getFlowChainId()), Function.identity()));
    }

    private List<FlowCheckResponse> invokeCBFlowEndpoint(List<String> flowIds, int pageSize, int page) {
        try {
            return internalCrnClient.withInternalCrn().flowEndpoint().getFlowChainsStatusesByChainIds(flowIds, pageSize, page);
        } catch (Exception e) {
            LOGGER.error("Error when communicating with cloudbreak to fetch flow status of {} flowIds", flowIds.size(), e);
        }
        return newArrayList();
    }

}
