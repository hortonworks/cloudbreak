package com.sequenceiq.freeipa.service.stack;

import static java.util.function.Predicate.not;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.health.HealthDetailsFreeIpaResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.health.NodeHealthDetails;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;

@Service
public class FreeIpaStackHealthDetailsService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaStackHealthDetailsService.class);

    private static final Set<InstanceStatus> CACHEABLE_INSTANCE_STATUS = Set.of(InstanceStatus.STOPPED, InstanceStatus.FAILED);

    @Inject
    private StackService stackService;

    @Inject
    private FreeIpaInstanceHealthDetailsService freeIpaInstanceHealthDetailsService;

    public HealthDetailsFreeIpaResponse getHealthDetails(String environmentCrn, String accountId) {
        Stack stack = stackService.getByEnvironmentCrnAndAccountIdWithLists(environmentCrn, accountId);
        MDCBuilder.buildMdcContext(stack);
        List<InstanceMetaData> instances = stack.getAllInstanceMetaDataList();
        HealthDetailsFreeIpaResponse response = new HealthDetailsFreeIpaResponse();

        for (InstanceMetaData instance: instances) {
            if (shouldRunHealthCheck(instance)) {
                try {
                    NodeHealthDetails nodeHealthDetails = freeIpaInstanceHealthDetailsService.getInstanceHealthDetails(stack, instance);
                    response.addNodeHealthDetailsFreeIpaResponses(nodeHealthDetails);
                } catch (Exception e) {
                    addUnreachableResponse(instance, response, e.getLocalizedMessage());
                    LOGGER.error(String.format("Unable to check the health of FreeIPA instance: %s", instance.getInstanceId()), e);
                }
            } else {
                NodeHealthDetails nodeResponse = new NodeHealthDetails();
                response.addNodeHealthDetailsFreeIpaResponses(nodeResponse);
                nodeResponse.setName(instance.getDiscoveryFQDN());
                nodeResponse.setStatus(instance.getInstanceStatus());
                nodeResponse.setInstanceId(instance.getInstanceId());
                nodeResponse.addIssue("Unable to check health as instance is " + instance.getInstanceStatus().name());
            }
        }
        return updateResponse(stack, response);
    }

    private boolean shouldRunHealthCheck(InstanceMetaData instance) {
        return !(instance.isTerminated() ||
                instance.isDeletedOnProvider() ||
                CACHEABLE_INSTANCE_STATUS.contains(instance.getInstanceStatus()));
    }

    private void addUnreachableResponse(InstanceMetaData instance, HealthDetailsFreeIpaResponse response, String issue) {
        NodeHealthDetails nodeResponse = new NodeHealthDetails();
        response.addNodeHealthDetailsFreeIpaResponses(nodeResponse);
        nodeResponse.setName(instance.getDiscoveryFQDN());
        nodeResponse.setStatus(InstanceStatus.UNREACHABLE);
        nodeResponse.setInstanceId(instance.getInstanceId());
        nodeResponse.addIssue(issue);
    }

    private HealthDetailsFreeIpaResponse updateResponse(Stack stack, HealthDetailsFreeIpaResponse response) {
        response.setEnvironmentCrn(stack.getEnvironmentCrn());
        response.setCrn(stack.getResourceCrn());
        response.setName(stack.getName());

        Set<String> notTermiatedStackInstanceIds = stack.getAllInstanceMetaDataList().stream()
                .filter(not(InstanceMetaData::isTerminated))
                .map(InstanceMetaData::getInstanceId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        List<InstanceStatus> nonTerminatedStatuses = response.getNodeHealthDetails().stream()
                .filter(nodeHealthDetails -> notTermiatedStackInstanceIds.contains(nodeHealthDetails.getInstanceId()))
                .map(NodeHealthDetails::getStatus)
                .collect(Collectors.toList());
        if (nonTerminatedStatuses.isEmpty()) {
            LOGGER.debug("FreeIPA is unhealthy because all instances are terminated");
            response.setStatus(Status.UNHEALTHY);
        } else if (!areAllStatusTheSame(nonTerminatedStatuses)) {
            LOGGER.debug("There are different health statuses for FreeIPA so the the overall health is unhealthy");
            response.setStatus(Status.UNHEALTHY);
        } else if (hasMissingStatus(nonTerminatedStatuses, notTermiatedStackInstanceIds)) {
            LOGGER.debug("There are missing health checks for some instances of FreeIPA so the overall health is unhealthy");
            response.setStatus(Status.UNHEALTHY);
        } else {
            response.setStatus(toStatus(nonTerminatedStatuses.get(0)));
        }
        updateResponseWithInstanceIds(response, stack);
        return response;
    }

    private void updateResponseWithInstanceIds(HealthDetailsFreeIpaResponse response, Stack stack) {
        Map<String, String> nameIdMap = getNameIdMap(stack);
        for (NodeHealthDetails node: response.getNodeHealthDetails()) {
            node.setInstanceId(nameIdMap.get(node.getName()));
        }
    }

    private Map<String, String> getNameIdMap(Stack stack) {
        return stack.getInstanceGroups().stream().flatMap(ig -> ig.getInstanceMetaData().stream())
                .filter(im -> Objects.nonNull(im.getDiscoveryFQDN()) && Objects.nonNull(im.getInstanceId()))
                .collect(Collectors.toMap(InstanceMetaData::getDiscoveryFQDN, InstanceMetaData::getInstanceId));
    }

    private Status toStatus(InstanceStatus instanceStatus) {
        switch (instanceStatus) {
            case REQUESTED:
                return Status.REQUESTED;
            case CREATED:
                return Status.AVAILABLE;
            case TERMINATED:
                return Status.DELETE_COMPLETED;
            case DELETED_ON_PROVIDER_SIDE:
            case DELETED_BY_PROVIDER:
                return Status.DELETED_ON_PROVIDER_SIDE;
            case STOPPED:
                return Status.STOPPED;
            case REBOOTING:
                return Status.UPDATE_IN_PROGRESS;
            case UNREACHABLE:
                return Status.UNREACHABLE;
            case DELETE_REQUESTED:
                return Status.DELETE_IN_PROGRESS;
            default:
                return Status.UNHEALTHY;
        }
    }

    private boolean areAllStatusTheSame(List<InstanceStatus> response) {
        InstanceStatus first = response.get(0);
        return response.stream().allMatch(Predicate.isEqual(first));
    }

    private boolean hasMissingStatus(List<InstanceStatus> response, Set<String> notTermiatedStackInstanceIds) {
        return response.size() != notTermiatedStackInstanceIds.size();
    }
}
