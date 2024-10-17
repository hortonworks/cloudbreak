package com.sequenceiq.cloudbreak.cloud.template.compute;

import java.util.ArrayList;
import java.util.Collection;
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

    public void rollbackIfNecessary(CloudFailureContext cloudFailureContext, List<CloudResourceStatus> failuresList, List<CloudResourceStatus> resourceStatuses,
            Group group, Integer requestedNodeCount) {
        if (failuresList.isEmpty()) {
            return;
        }
        LOGGER.debug("Roll back the following resources: {}", failuresList);
        doRollback(cloudFailureContext, failuresList, resourceStatuses, group, requestedNodeCount);
    }

    private void doRollback(CloudFailureContext cloudFailureContext, List<CloudResourceStatus> failuresList, List<CloudResourceStatus> resourceStatuses,
            Group group, Integer requestedNodeCount) {
        Set<Long> failures = failureCount(failuresList);
        ScaleContext stx = cloudFailureContext.getStx();
        AuthenticatedContext auth = cloudFailureContext.getAuth();
        ResourceBuilderContext ctx = cloudFailureContext.getCtx();
        if (stx.getAdjustmentType() == null && !failures.isEmpty()) {
            LOGGER.info("Failure policy is null so error will be thrown");
            throwError(failuresList);
        }

        LOGGER.info("Adjustment type is {}", stx.getAdjustmentType());
        switch (stx.getAdjustmentType()) {
            case EXACT:
                if (stx.getThreshold() > requestedNodeCount - failures.size()) {
                    LOGGER.info("Number of failures is more than the threshold ({}) so error will be thrown", stx.getThreshold());
                    rollbackEverythingAndThrowException(failuresList, resourceStatuses, group, stx, auth, ctx);
                } else if (!failures.isEmpty()) {
                    LOGGER.info("Decrease node counts because threshold was higher");
                    handleExceptions(auth, resourceStatuses, group, ctx, failures, stx.getUpscale());
                }
                break;
            case PERCENTAGE:
                double calculatedPercentage = calculatePercentage(failures.size(), requestedNodeCount);
                Double threshold = Double.valueOf(stx.getThreshold());
                if (threshold > calculatedPercentage) {
                    LOGGER.info("Calculated percentage ({}) is lower than threshold, do rollback ({})", calculatedPercentage, threshold);
                    rollbackEverythingAndThrowException(failuresList, resourceStatuses, group, stx, auth, ctx);
                } else if (!failures.isEmpty()) {
                    LOGGER.info("Decrease node counts because threshold was higher");
                    handleExceptions(auth, resourceStatuses, group, ctx, failures, stx.getUpscale());
                }
                break;
            case BEST_EFFORT:
                LOGGER.info("Decrease node counts because threshold was higher");
                handleExceptions(auth, resourceStatuses, group, ctx, failures, stx.getUpscale());
                break;
            default:
                LOGGER.info("Unsupported adjustment type so error will throw");
                rollbackEverythingAndThrowException(failuresList, resourceStatuses, group, stx, auth, ctx);
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

    private void rollbackEverythingAndThrowException(List<CloudResourceStatus> failuresList, List<CloudResourceStatus> resourceStatuses,
            Group group, ScaleContext stx, AuthenticatedContext auth, ResourceBuilderContext ctx) {
        Set<Long> rollbackIds = resourceStatuses.stream().map(CloudResourceStatus::getPrivateId).collect(Collectors.toSet());
        doRollback(auth, resourceStatuses, rollbackIds, group, ctx, stx.getUpscale());
        throwRolledbackException(failuresList);
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

    private void handleExceptions(AuthenticatedContext auth, List<CloudResourceStatus> cloudResourceStatuses, Group group,
            ResourceBuilderContext ctx, Collection<Long> ids, Boolean upscale) {
        List<CloudResource> resources = new ArrayList<>(cloudResourceStatuses.size());
        for (CloudResourceStatus exception : cloudResourceStatuses) {
            if (ResourceStatus.FAILED.equals(exception.getStatus()) || ids.contains(exception.getPrivateId())) {
                LOGGER.info("Failed to create instance with id '" + exception.getPrivateId() + "'. Reason: " + exception.getStatusReason());
                resources.add(exception.getCloudResource());
            }
        }
        if (!resources.isEmpty()) {
            LOGGER.info("Resource list not empty so rollback will start.Resource list size is: " + resources.size());
            doRollback(auth, cloudResourceStatuses, ids, group, ctx, upscale);
        }
    }

    private void doRollback(AuthenticatedContext auth, List<CloudResourceStatus> statuses, Collection<Long> rollbackIds, Group group,
            ResourceBuilderContext ctx, Boolean upscale) {
        LOGGER.info("Rollback resources for the following private ids: {}", rollbackIds);

        if (getRemovableInstanceTemplates(group, rollbackIds).size() <= 0 && !upscale) {
            LOGGER.info("InstanceGroup node count lower than 1 which is incorrect so error will be thrown");
            throwError(statuses);
        } else {
            fireResourceCreationFailedEvent(auth, statuses.stream()
                    .filter(cloudResourceStatus -> ResourceStatus.FAILED.equals(cloudResourceStatus.getStatus()))
                    .collect(Collectors.toList()));
            Set<CloudResource> cloudResources = getUniqueCloudResources(statuses, rollbackIds);
            LOGGER.debug("Rollback cloud resources: {}", cloudResources);
            List<CloudResourceStatus> deleteStatuses = computeResourceService.deleteResources(ctx, auth, cloudResources, false, false);

            if (deleteStatuses.stream().anyMatch(CloudResourceStatus::isFailed)) {
                String failedResourcesWithReason = deleteStatuses.stream().filter(CloudResourceStatus::isFailed)
                        .map(cloudResourceStatus -> cloudResourceStatus.getCloudResource().getDetailedInfo() +
                                ", Reason: " + cloudResourceStatus.getStatusReason()).collect(Collectors.joining("\n"));
                LOGGER.warn("Resource deletion failed. Failed resources: {}", failedResourcesWithReason);
                fireResourceDeletionFailedEvent(auth, failedResourcesWithReason);
            }
        }
    }

    private Set<CloudResource> getUniqueCloudResources(List<CloudResourceStatus> statuses, Collection<Long> rollbackIds) {
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

    private Collection<InstanceTemplate> getRemovableInstanceTemplates(Group group, Collection<Long> ids) {
        Collection<InstanceTemplate> instanceTemplates = new ArrayList<>();
        for (CloudInstance cloudInstance : group.getInstances()) {
            InstanceTemplate instanceTemplate = cloudInstance.getTemplate();
            if (!ids.contains(instanceTemplate.getPrivateId())) {
                instanceTemplates.add(instanceTemplate);
            }
        }
        return instanceTemplates;
    }

    private void throwRolledbackException(List<CloudResourceStatus> statuses) {
        throw new RolledbackResourcesException("Resources are rolled back because successful node count was lower than threshold. "
                + statuses.size() + " nodes are failed. Error reason: " + ResourceStatusLists.aggregateReason(statuses));
    }

    private void throwError(List<CloudResourceStatus> statuses) {
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
