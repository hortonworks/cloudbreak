package com.sequenceiq.freeipa.service.stack;

import static java.util.function.Predicate.isEqual;
import static java.util.function.Predicate.not;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.health.HealthDetailsFreeIpaResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.health.NodeHealthDetails;
import com.sequenceiq.freeipa.configuration.HealthCheckConfig;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;

@Service
public class FreeIpaStackHealthDetailsService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaStackHealthDetailsService.class);

    private static final Set<InstanceStatus> CACHEABLE_INSTANCE_STATUS = Set.of(InstanceStatus.STOPPED, InstanceStatus.FAILED);

    private static final long TIMEOUT_SECONDS = 15L;

    @Inject
    private StackService stackService;

    @Inject
    private FreeIpaSafeInstanceHealthDetailsService healthDetailsService;

    @Inject
    @Qualifier(HealthCheckConfig.HEALTH_CHECK_TASK_EXECUTOR)
    private ExecutorService taskExecutorService;

    public HealthDetailsFreeIpaResponse getHealthDetails(String environmentCrn, String accountId) {
        Stack stack = stackService.getByEnvironmentCrnAndAccountIdWithListsAndMdcContext(environmentCrn, accountId);
        List<InstanceMetaData> instances = stack.getAllInstanceMetaDataList();

        HealthDetailsFreeIpaResponse response = new HealthDetailsFreeIpaResponse();

        List<Callable<NodeHealthDetails>> callables = instances.stream()
                .map(instance -> (Callable<NodeHealthDetails>) () -> getNodeHealthDetails(instance, stack))
                .collect(Collectors.toList());
        try {
            List<Future<NodeHealthDetails>> futures = taskExecutorService.invokeAll(callables);
            for (Future<NodeHealthDetails> future : futures) {
                try {
                    NodeHealthDetails nodeHealthDetails = future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                    response.addNodeHealthDetailsFreeIpaResponses((nodeHealthDetails));
                } catch (ExecutionException | TimeoutException e) {
                    LOGGER.warn("Error while getting health details for stack: {}", stack.getName(), e);
                    NodeHealthDetails nodeHealthDetails = healthDetailsService.createNodeResponseWithStatusAndIssue(
                            new InstanceMetaData(), InstanceStatus.UNREACHABLE, e.getMessage());
                    response.addNodeHealthDetailsFreeIpaResponses(nodeHealthDetails);
                }
            }
        } catch (InterruptedException e) {
            LOGGER.warn("Error while getting health details for stack: {}", stack.getName(), e);
            Thread.currentThread().interrupt();
        }
        return updateResponse(stack, response);
    }

    private NodeHealthDetails getNodeHealthDetails(InstanceMetaData instance, Stack stack) {
        NodeHealthDetails nodeResponse;
        if (shouldRunHealthCheck(instance)) {
            nodeResponse = healthDetailsService.getInstanceHealthDetails(stack, instance);
        } else {
            String issue = "Unable to check health as instance is " + instance.getInstanceStatus().name();
            nodeResponse = healthDetailsService.createNodeResponseWithStatusAndIssue(instance, instance.getInstanceStatus(), issue);
        }
        return nodeResponse;
    }

    private boolean shouldRunHealthCheck(InstanceMetaData instance) {
        return !(instance.isTerminated() ||
                instance.isDeletedOnProvider() ||
                CACHEABLE_INSTANCE_STATUS.contains(instance.getInstanceStatus()));
    }

    private HealthDetailsFreeIpaResponse updateResponse(Stack stack, HealthDetailsFreeIpaResponse response) {
        response.setEnvironmentCrn(stack.getEnvironmentCrn());
        response.setCrn(stack.getResourceCrn());
        response.setName(stack.getName());

        Set<String> notTerminatedStackInstanceIds = stack.getAllInstanceMetaDataList().stream()
                .filter(not(InstanceMetaData::isTerminated))
                .map(InstanceMetaData::getInstanceId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        List<InstanceStatus> nonTerminatedStatuses = response.getNodeHealthDetails().stream()
                .filter(nodeHealthDetails -> notTerminatedStackInstanceIds.contains(nodeHealthDetails.getInstanceId()))
                .map(NodeHealthDetails::getStatus)
                .collect(Collectors.toList());
        if (nonTerminatedStatuses.isEmpty()) {
            LOGGER.debug("FreeIPA is unhealthy because all instances are terminated");
            response.setStatus(Status.UNHEALTHY);
        } else if (!areAllStatusTheSame(nonTerminatedStatuses)) {
            LOGGER.debug("There are different health statuses for FreeIPA so the the overall health is unhealthy");
            response.setStatus(Status.UNHEALTHY);
        } else if (hasMissingStatus(nonTerminatedStatuses, notTerminatedStackInstanceIds)) {
            LOGGER.debug("There are missing health checks for some instances of FreeIPA so the overall health is unhealthy");
            response.setStatus(Status.UNHEALTHY);
        } else {
            response.setStatus(toStatus(nonTerminatedStatuses.getFirst()));
        }
        updateResponseWithInstanceIds(response, stack);
        return response;
    }

    private void updateResponseWithInstanceIds(HealthDetailsFreeIpaResponse response, Stack stack) {
        Map<String, String> nameIdMap = getNameIdMap(stack);
        for (NodeHealthDetails node : response.getNodeHealthDetails()) {
            if (nameIdMap.containsKey(node.getName()) && StringUtils.isNotBlank(nameIdMap.get(node.getName()))) {
                node.setInstanceId(nameIdMap.get(node.getName()));
            }
        }
    }

    private Map<String, String> getNameIdMap(Stack stack) {
        return stack.getInstanceGroups().stream().flatMap(ig -> ig.getInstanceMetaData().stream())
                .filter(im -> Objects.nonNull(im.getDiscoveryFQDN()) && Objects.nonNull(im.getInstanceId()))
                .collect(Collectors.toMap(InstanceMetaData::getDiscoveryFQDN, InstanceMetaData::getInstanceId));
    }

    private Status toStatus(InstanceStatus instanceStatus) {
        return switch (instanceStatus) {
            case REQUESTED -> Status.REQUESTED;
            case CREATED -> Status.AVAILABLE;
            case TERMINATED -> Status.DELETE_COMPLETED;
            case DELETED_ON_PROVIDER_SIDE, DELETED_BY_PROVIDER -> Status.DELETED_ON_PROVIDER_SIDE;
            case STOPPED -> Status.STOPPED;
            case REBOOTING -> Status.UPDATE_IN_PROGRESS;
            case UNREACHABLE -> Status.UNREACHABLE;
            case DELETE_REQUESTED -> Status.DELETE_IN_PROGRESS;
            default -> Status.UNHEALTHY;
        };
    }

    private boolean areAllStatusTheSame(List<InstanceStatus> response) {
        InstanceStatus first = response.getFirst();
        return response.stream().allMatch(isEqual(first));
    }

    private boolean hasMissingStatus(List<InstanceStatus> response, Set<String> notTermiatedStackInstanceIds) {
        return response.size() != notTermiatedStackInstanceIds.size();
    }
}
