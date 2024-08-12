package com.sequenceiq.cloudbreak.cloud.service;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.task.ResourcesStatePollerResult;

@Component
public class CloudResourceValidationService {
    public void validateResourcesState(CloudContext cloudContext, ResourcesStatePollerResult statePollerResult) {
        if (statePollerResult == null || statePollerResult.getResults() == null) {
            throw new IllegalStateException(String.format("%s is null, cannot check deploy status of database stack for %s",
                    statePollerResult == null ? "ResourcesStatePollerResult" : "ResourcesStatePollerResult.results", cloudContext));
        }
        List<CloudResourceStatus> results = statePollerResult.getResults();
        if (results.size() == 1 && (results.get(0).isFailed() || results.get(0).isDeleted())) {
            throw new CloudConnectorException(String.format(
                    "Failed to deploy the database stack for %s due to: %s", cloudContext, results.get(0).getStatusReason()));
        }
        List<CloudResourceStatus> failedResources = results.stream()
                .filter(r -> r.isFailed() || r.isDeleted())
                .toList();
        if (!failedResources.isEmpty()) {
            throw new CloudConnectorException(String.format("Failed to deploy the database stack for %s due to: %s", cloudContext, failedResources));
        }
    }
}
