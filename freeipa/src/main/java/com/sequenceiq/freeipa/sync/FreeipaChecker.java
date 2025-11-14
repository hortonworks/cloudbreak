package com.sequenceiq.freeipa.sync;

import static com.sequenceiq.cloudbreak.util.Benchmark.checkedMeasure;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;

import com.google.common.base.Joiner;
import com.sequenceiq.cloudbreak.client.RPCResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.stack.FreeIpaInstanceHealthDetailsService;

@Component
public class FreeipaChecker {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeipaChecker.class);

    @Inject
    private FreeIpaInstanceHealthDetailsService freeIpaInstanceHealthDetailsService;

    private Pair<Map<InstanceMetaData, DetailedStackStatus>, String> checkStatus(Stack stack, Set<InstanceMetaData> checkableInstances,
            Set<String> hostsWithSaltFailure) {
        return checkedMeasure(() -> {
            Map<InstanceMetaData, DetailedStackStatus> statuses = new HashMap<>();
            List<RPCResponse<Boolean>> responses = new LinkedList<>();
            LOGGER.info("Checking FreeIPA status for instance IDs {}",
                    checkableInstances.stream().map(InstanceMetaData::getInstanceId).collect(Collectors.toList()));
            for (InstanceMetaData instanceMetaData : checkableInstances) {
                try {
                    RPCResponse<Boolean> response = checkedMeasure(() -> freeIpaInstanceHealthDetailsService.checkFreeIpaHealth(stack, instanceMetaData), LOGGER,
                            ":::Auto sync::: FreeIPA health check ran in {}ms");
                    responses.add(response);
                    DetailedStackStatus newDetailedStackStatus;
                    if (response.getResult() && !hostsWithSaltFailure.contains(instanceMetaData.getDiscoveryFQDN())) {
                        newDetailedStackStatus = DetailedStackStatus.AVAILABLE;
                    } else {
                        newDetailedStackStatus = DetailedStackStatus.UNHEALTHY;
                    }
                    LOGGER.info("FreeIpa health check reported {} for {}", newDetailedStackStatus, instanceMetaData);
                    statuses.put(instanceMetaData, newDetailedStackStatus);
                } catch (Exception e) {
                    LOGGER.info("FreeIpaClientException occurred during status fetch for {}: {}", instanceMetaData, e.getMessage(), e);
                    statuses.put(instanceMetaData, DetailedStackStatus.UNREACHABLE);
                }
            }
            String message = getMessages(responses);
            if (!hostsWithSaltFailure.isEmpty()) {
                message = String.format("%s. %s: %s.", message, "Salt is not healthy on node(s)", Joiner.on(",").join(hostsWithSaltFailure));
            }
            return Pair.of(statuses, message);
        }, LOGGER, ":::Auto sync::: freeipa server status is checked in {}ms");
    }

    public SyncResult getStatus(Stack stack, Set<InstanceMetaData> checkableInstances, Set<String> hostsWithSaltFailure) {
        try {
            if (checkableInstances.isEmpty()) {
                throw new UnsupportedOperationException("There are no instances of FreeIPA to check the status of");
            }

            // Exclude terminated but include deleted
            Set<InstanceMetaData> notTermiatedStackInstances = stack.getAllInstanceMetaDataList().stream()
                    .filter(Predicate.not(InstanceMetaData::isTerminated))
                    .collect(Collectors.toSet());
            Pair<Map<InstanceMetaData, DetailedStackStatus>, String> statusCheckPair = checkStatus(stack, checkableInstances, hostsWithSaltFailure);
            List<DetailedStackStatus> responses = new ArrayList<>(statusCheckPair.getFirst().values());
            DetailedStackStatus status;
            if (areAllStatusTheSame(responses) && !hasMissingStatus(responses, notTermiatedStackInstances)) {
                status = responses.get(0);
            } else {
                status = DetailedStackStatus.UNHEALTHY;
            }
            String statusReason = "FreeIpa is " + status;
            if (StringUtils.isNotBlank(statusCheckPair.getSecond())) {
                statusReason += ", " + statusCheckPair.getSecond();
            }
            return new SyncResult(statusReason, status, statusCheckPair.getFirst());
        } catch (Exception e) {
            LOGGER.info("Error occurred during status fetch: " + e.getMessage(), e);
            return new SyncResult("FreeIpa is unreachable, because error occurred: " + e.getMessage(), DetailedStackStatus.UNREACHABLE, null);
        }
    }

    private boolean areAllStatusTheSame(List<DetailedStackStatus> response) {
        DetailedStackStatus first = response.get(0);
        return response.stream().allMatch(Predicate.isEqual(first));
    }

    private boolean hasMissingStatus(List<DetailedStackStatus> response, Set<InstanceMetaData> notTermiatedStackInstances) {
        return response.size() != notTermiatedStackInstances.size();
    }

    private String getMessages(List<RPCResponse<Boolean>> responses) {
        return responses.stream()
                .map(RPCResponse::getMessages)
                .flatMap(List::stream)
                .map(m -> m.getName() + ": " + m.getMessage())
                .collect(Collectors.joining(", "));
    }
}
