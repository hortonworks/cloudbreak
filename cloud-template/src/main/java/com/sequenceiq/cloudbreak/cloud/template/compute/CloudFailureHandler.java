package com.sequenceiq.cloudbreak.cloud.template.compute;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.exception.RolledbackResourcesException;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.cloudbreak.cloud.template.context.ResourceBuilderContext;
import com.sequenceiq.cloudbreak.cloud.transform.ResourceStatusLists;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;
import com.sequenceiq.common.api.type.AdjustmentType;

@Service
public class CloudFailureHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudFailureHandler.class);

    private static final double ONE_HUNDRED = 100.0;

    private static final String CLOUD_PROVIDER_RESOURCE_CREATION_FAILED = "CLOUD_PROVIDER_RESOURCE_CREATION_FAILED";

    private static final String CLOUD_PROVIDER_RESOURCE_ROLLBACK_FAILED = "CLOUD_PROVIDER_ROLLBACK_FAILED";

    @Inject
    private ComputeResourceService computeResourceService;

    @Inject
    private Optional<CloudbreakEventService> cloudbreakEventService;

    public void rollbackIfNecessary(CloudFailureContext cloudFailureContext, Map<CloudResourceStatus, Group> failedResources,
            Map<CloudResourceStatus, Group> allResources, Integer requestedNodeCount) {
        if (failedResources.isEmpty()) {
            return;
        }
        LOGGER.debug("Roll back the following resources: {}", failedResources.keySet());
        doRollback(cloudFailureContext, failedResources, allResources, requestedNodeCount);
    }

    private void doRollback(CloudFailureContext cloudFailureContext, Map<CloudResourceStatus, Group> failedResources,
            Map<CloudResourceStatus, Group> allResources, Integer requestedNodeCount) {
        Set<Long> failures = failureCount(failedResources.keySet());
        ScaleContext stx = cloudFailureContext.getStx();
        AuthenticatedContext auth = cloudFailureContext.getAuth();
        ResourceBuilderContext ctx = cloudFailureContext.getCtx();
        if (stx.getAdjustmentType() == null && !failures.isEmpty()) {
            LOGGER.warn("Failure policy is null so error will be thrown");
            throwError(failedResources.keySet());
        }
        handleFailedResources(failedResources, allResources, requestedNodeCount, stx, failures, auth, ctx);
    }

    private void handleFailedResources(Map<CloudResourceStatus, Group> failedResources, Map<CloudResourceStatus, Group> allResources,
            Integer requestedNodeCount, ScaleContext stx, Set<Long> failures, AuthenticatedContext auth, ResourceBuilderContext ctx) {
        LOGGER.info("Adjustment type is {}, failures are: {}", stx.getAdjustmentType(), failures);
        int successfulNodeCount = requestedNodeCount - failures.size();
        switch (stx.getAdjustmentType()) {
            case EXACT:
                if (stx.getThreshold() > successfulNodeCount) {
                    LOGGER.warn("Successful nodes ({}) below threshold ({}). Rolling back all operations.", successfulNodeCount, stx.getThreshold());
                    rollbackEverythingAndThrowException(failedResources, allResources, stx, auth, ctx);
                } else if (!failures.isEmpty()) {
                    LOGGER.info("Successful nodes ({}) exceed threshold ({}). Rolling back failed nodes and proceeding.",
                            successfulNodeCount, stx.getThreshold());
                    handleExceptions(auth, allResources, ctx, failures, stx.getUpscale());
                }
                break;
            case PERCENTAGE:
                double successPercentage = calculatePercentage(failures.size(), requestedNodeCount);
                Double threshold = Double.valueOf(stx.getThreshold());
                if (threshold > successPercentage) {
                    LOGGER.warn("Success percentage ({}) below threshold ({}). Rolling back all operations.", successPercentage, threshold);
                    rollbackEverythingAndThrowException(failedResources, allResources, stx, auth, ctx);
                } else if (!failures.isEmpty()) {
                    LOGGER.info("Success percentage ({}) exceeds threshold ({}). Rolling back failed nodes and proceeding.", successPercentage, threshold);
                    handleExceptions(auth, allResources, ctx, failures, stx.getUpscale());
                }
                break;
            case BEST_EFFORT:
                if (successfulNodeCount == 0) {
                    LOGGER.warn("Successful node count is 0 so rolling back all operations.");
                    rollbackEverythingAndThrowException(failedResources, allResources, stx, auth, ctx);
                } else {
                    LOGGER.info("Successful node count ({}) is positive. Rolling back failed nodes and proceeding.", successfulNodeCount);
                    handleExceptions(auth, allResources, ctx, failures, stx.getUpscale());
                }
                break;
            default:
                LOGGER.info("Unsupported adjustment type so error will throw");
                rollbackEverythingAndThrowException(failedResources, allResources, stx, auth, ctx);
                break;
        }
    }

    private void fireResourceDeletionFailedEvent(AuthenticatedContext auth, String failedResourcesWithReason) {
        cloudbreakEventService.ifPresent(eventService -> eventService.fireCloudbreakEvent(auth.getCloudContext().getId(),
                CLOUD_PROVIDER_RESOURCE_ROLLBACK_FAILED, ResourceEvent.CLOUD_PROVIDER_RESOURCE_ROLLBACK_FAILED, List.of(failedResourcesWithReason)));
    }

    private void fireResourceCreationFailedEvent(AuthenticatedContext auth, List<CloudResourceStatus> failuresList) {
        if (cloudbreakEventService.isPresent()) {
            String reason = failuresList.stream()
                    .map(resourceStatus -> {
                        CloudResource cloudResource = resourceStatus.getCloudResource();
                        String detailedInfo = cloudResource.getDetailedInfo();
                        if (resourceStatus.getStatusReason() != null) {
                            detailedInfo += ": " + resourceStatus.getStatusReason();
                        }
                        return detailedInfo;
                    })
                    .collect(Collectors.joining(", "));
            cloudbreakEventService.get().fireCloudbreakEvent(auth.getCloudContext().getId(), CLOUD_PROVIDER_RESOURCE_CREATION_FAILED,
                    ResourceEvent.CLOUD_PROVIDER_RESOURCE_CREATION_FAILED, List.of(reason));
        }
    }

    private void rollbackEverythingAndThrowException(Map<CloudResourceStatus, Group> failedResources, Map<CloudResourceStatus, Group> resourceStatuses,
            ScaleContext stx, AuthenticatedContext auth, ResourceBuilderContext ctx) {
        Set<Long> rollbackIds = resourceStatuses.keySet().stream().map(CloudResourceStatus::getPrivateId).collect(Collectors.toSet());
        doRollback(auth, resourceStatuses, rollbackIds, ctx, stx.getUpscale());
        throwRolledbackException(failedResources.keySet());
    }

    private Set<Long> failureCount(Collection<CloudResourceStatus> failedResourceRequestResults) {
        Set<Long> ids = new HashSet<>(failedResourceRequestResults.size());
        for (CloudResourceStatus failedResourceRequestResult : failedResourceRequestResults) {
            if (ResourceStatus.FAILED.equals(failedResourceRequestResult.getStatus())) {
                ids.add(failedResourceRequestResult.getPrivateId());
            }
        }
        return ids;
    }

    private double calculatePercentage(double failedResourceRequestResults, double requestedNodeCount) {
        double calculatedPercentage = (requestedNodeCount - failedResourceRequestResults) / requestedNodeCount * ONE_HUNDRED;
        LOGGER.info("Calculated percentage: {}", calculatedPercentage);
        return calculatedPercentage;
    }

    private void handleExceptions(AuthenticatedContext auth, Map<CloudResourceStatus, Group> allResources,
            ResourceBuilderContext ctx, Collection<Long> ids, Boolean upscale) {
        Set<CloudResourceStatus> cloudResourceStatuses = allResources.keySet();
        List<CloudResource> resources = new ArrayList<>(cloudResourceStatuses.size());
        for (CloudResourceStatus exception : cloudResourceStatuses) {
            if (ResourceStatus.FAILED.equals(exception.getStatus()) || ids.contains(exception.getPrivateId())) {
                LOGGER.info("Failed to create instance with id '" + exception.getPrivateId() + "'. Reason: " + exception.getStatusReason());
                resources.add(exception.getCloudResource());
            }
        }
        if (!resources.isEmpty()) {
            LOGGER.info("Resource list not empty so rollback will start.Resource list size is: " + resources.size());
            doRollback(auth, allResources, ids, ctx, upscale);
        }
    }

    private void doRollback(AuthenticatedContext auth, Map<CloudResourceStatus, Group> allResources, Collection<Long> rollbackIds,
            ResourceBuilderContext ctx, Boolean upscale) {
        LOGGER.info("Rollback resources for the following private ids: {}", rollbackIds);

        List<Group> groups = allResources.values().stream().distinct().toList();
        List<CloudResourceStatus> resourceStatuses = allResources.keySet().stream()
                .sorted(Comparator.comparing(o -> o.getCloudResource().getName())).toList();
        if (getRemovableInstanceTemplates(groups, rollbackIds).size() <= 0 && !upscale) {
            LOGGER.info("InstanceGroup node count lower than 1 which is incorrect so error will be thrown");
            throwError(resourceStatuses);
        } else {
            fireResourceCreationFailedEvent(auth, resourceStatuses.stream()
                    .filter(cloudResourceStatus -> ResourceStatus.FAILED.equals(cloudResourceStatus.getStatus()))
                    .collect(Collectors.toList()));
            Set<CloudResource> deletableCloudResources = getUniqueCloudResources(resourceStatuses, rollbackIds);
            LOGGER.debug("Rollback cloud resources: {}", deletableCloudResources);
            List<CloudResourceStatus> deleteStatuses = computeResourceService.deleteResources(ctx, auth, deletableCloudResources, false, false);

            if (deleteStatuses.stream().anyMatch(CloudResourceStatus::isFailed)) {
                String failedResourcesWithReason = deleteStatuses.stream().filter(CloudResourceStatus::isFailed)
                        .map(cloudResourceStatus -> cloudResourceStatus.getCloudResource().getDetailedInfo() +
                                ", Reason: " + cloudResourceStatus.getStatusReason()).collect(Collectors.joining("\n"));
                LOGGER.warn("Resource deletion failed. Failed resources: {}", failedResourcesWithReason);
                fireResourceDeletionFailedEvent(auth, failedResourcesWithReason);
            }
        }
    }

    private Set<CloudResource> getUniqueCloudResources(Collection<CloudResourceStatus> statuses, Collection<Long> rollbackIds) {
        Map<String, CloudResource> nameToResourceMap = new LinkedHashMap<>();
        for (CloudResourceStatus cloudResourceStatus : statuses) {
            if (rollbackIds.contains(cloudResourceStatus.getPrivateId())) {
                CloudResource cloudResource = cloudResourceStatus.getCloudResource();
                String key = cloudResource.getName() + '-' + cloudResource.getType();
                nameToResourceMap.putIfAbsent(key, cloudResource);
            }
        }
        return new LinkedHashSet<>(nameToResourceMap.values());
    }

    private Collection<InstanceTemplate> getRemovableInstanceTemplates(Collection<Group> groups, Collection<Long> ids) {
        Collection<InstanceTemplate> instanceTemplates = new ArrayList<>();
        for (CloudInstance cloudInstance : groups.stream().flatMap(group -> group.getInstances().stream()).toList()) {
            InstanceTemplate instanceTemplate = cloudInstance.getTemplate();
            if (!ids.contains(instanceTemplate.getPrivateId())) {
                instanceTemplates.add(instanceTemplate);
            }
        }
        return instanceTemplates;
    }

    private void throwRolledbackException(Collection<CloudResourceStatus> statuses) {
        throw new RolledbackResourcesException("Resources are rolled back because successful node count was lower than threshold. "
                + statuses.size() + " nodes are failed. Error reason: " + ResourceStatusLists.aggregateReason(statuses));
    }

    private void throwError(Collection<CloudResourceStatus> statuses) {
        throw new CloudConnectorException("Error reason: " + ResourceStatusLists.aggregateReason(statuses));
    }

    public static class ScaleContext {
        private final Boolean upscale;

        private final AdjustmentType adjustmentType;

        private final Long threshold;

        public ScaleContext(Boolean upscale, AdjustmentType adjustmentType, Long threshold) {
            this.upscale = upscale;
            this.adjustmentType = adjustmentType;
            this.threshold = threshold;
        }

        public Boolean getUpscale() {
            return upscale;
        }

        public AdjustmentType getAdjustmentType() {
            return adjustmentType;
        }

        public Long getThreshold() {
            return threshold;
        }
    }
}
