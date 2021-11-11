package com.sequenceiq.cloudbreak.cloud.template.compute;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.AsyncTaskExecutor;
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
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.cloud.template.ComputeResourceBuilder;
import com.sequenceiq.cloudbreak.cloud.template.context.ResourceBuilderContext;
import com.sequenceiq.cloudbreak.cloud.template.init.ResourceBuilders;
import com.sequenceiq.common.api.type.AdjustmentType;
import com.sequenceiq.common.api.type.ResourceType;

@Service
public class CloudFailureHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudFailureHandler.class);

    private static final double ONE_HUNDRED = 100.0;

    @Inject
    private AsyncTaskExecutor resourceBuilderExecutor;

    @Inject
    private ResourceActionFactory resourceActionFactory;

    public void rollbackIfNecessary(CloudFailureContext cloudFailureContext, List<CloudResourceStatus> failuresList, List<CloudResourceStatus> resourceStatuses,
            Group group, ResourceBuilders resourceBuilders, Integer requestedNodeCount) {
        if (failuresList.isEmpty()) {
            return;
        }
        LOGGER.debug("Roll back the following resources: {}", failuresList);
        doRollback(cloudFailureContext, failuresList, resourceStatuses, group, requestedNodeCount, resourceBuilders);
    }

    private void doRollback(CloudFailureContext cloudFailureContext, List<CloudResourceStatus> failuresList, List<CloudResourceStatus> resourceStatuses,
            Group group, Integer requestedNodeCount, ResourceBuilders resourceBuilders) {
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
                    rollbackEverythingAndThrowException(failuresList, resourceStatuses, group, resourceBuilders, stx, auth, ctx);
                } else if (!failures.isEmpty()) {
                    LOGGER.info("Decrease node counts because threshold was higher");
                    handleExceptions(auth, resourceStatuses, group, ctx, resourceBuilders, failures, stx.getUpscale());
                }
                break;
            case PERCENTAGE:
                double calculatedPercentage = calculatePercentage(failures.size(), requestedNodeCount);
                Double threshold = Double.valueOf(stx.getThreshold());
                if (threshold > calculatedPercentage) {
                    LOGGER.info("Calculated percentage ({}) is lower than threshold, do rollback ({})", calculatedPercentage, threshold);
                    rollbackEverythingAndThrowException(failuresList, resourceStatuses, group, resourceBuilders, stx, auth, ctx);
                } else if (!failures.isEmpty()) {
                    LOGGER.info("Decrease node counts because threshold was higher");
                    handleExceptions(auth, resourceStatuses, group, ctx, resourceBuilders, failures, stx.getUpscale());
                }
                break;
            case BEST_EFFORT:
                LOGGER.info("Decrease node counts because threshold was higher");
                handleExceptions(auth, resourceStatuses, group, ctx, resourceBuilders, failures, stx.getUpscale());
                break;
            default:
                LOGGER.info("Unsupported adjustment type so error will throw");
                rollbackEverythingAndThrowException(failuresList, resourceStatuses, group, resourceBuilders, stx, auth, ctx);
                break;
        }
    }

    private void rollbackEverythingAndThrowException(List<CloudResourceStatus> failuresList, List<CloudResourceStatus> resourceStatuses,
            Group group, ResourceBuilders resourceBuilders, ScaleContext stx, AuthenticatedContext auth, ResourceBuilderContext ctx) {
        Set<Long> rollbackIds = resourceStatuses.stream().map(CloudResourceStatus::getPrivateId).collect(Collectors.toSet());
        doRollback(auth, resourceStatuses, rollbackIds, group, ctx, resourceBuilders, stx.getUpscale());
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
            ResourceBuilderContext ctx, ResourceBuilders resourceBuilders, Collection<Long> ids, Boolean upscale) {
        List<CloudResource> resources = new ArrayList<>(cloudResourceStatuses.size());
        for (CloudResourceStatus exception : cloudResourceStatuses) {
            if (ResourceStatus.FAILED.equals(exception.getStatus()) || ids.contains(exception.getPrivateId())) {
                LOGGER.info("Failed to create instance with id '" + exception.getPrivateId() + "'. Reason: " + exception.getStatusReason());
                resources.add(exception.getCloudResource());
            }
        }
        if (!resources.isEmpty()) {
            LOGGER.info("Resource list not empty so rollback will start.Resource list size is: " + resources.size());
            doRollback(auth, cloudResourceStatuses, ids, group, ctx, resourceBuilders, upscale);
        }
    }

    private void doRollback(AuthenticatedContext auth, List<CloudResourceStatus> statuses, Collection<Long> rollbackIds, Group group,
            ResourceBuilderContext ctx, ResourceBuilders resourceBuilders, Boolean upscale) {
        Variant variant = auth.getCloudContext().getVariant();
        LOGGER.info("Rollback resources for the following private ids: {}", rollbackIds);

        if (getRemovableInstanceTemplates(group, rollbackIds).size() <= 0 && !upscale) {
            LOGGER.info("InstanceGroup node count lower than 1 which is incorrect so error will be thrown");
            throwError(statuses);
        } else {
            Map<ResourceType, ComputeResourceBuilder<ResourceBuilderContext>> resourceBuilderMap = getResourceBuilderMap(resourceBuilders, variant);
            for (CloudResourceStatus cloudResourceStatus : statuses) {
                try {
                    if (rollbackIds.contains(cloudResourceStatus.getPrivateId())) {
                        ComputeResourceBuilder<ResourceBuilderContext> resourceBuilderForType =
                                resourceBuilderMap.get(cloudResourceStatus.getCloudResource().getType());
                        deleteResource(auth, ctx, resourceBuilderForType, cloudResourceStatus);
                    }
                } catch (Exception e) {
                    LOGGER.warn("Resource can not be deleted. Reason: " + e.getMessage(), e);
                }
            }
        }
    }

    @NotNull
    private Map<ResourceType, ComputeResourceBuilder<ResourceBuilderContext>> getResourceBuilderMap(ResourceBuilders resourceBuilders, Variant variant) {
        List<ComputeResourceBuilder<ResourceBuilderContext>> computeResourceBuilders = resourceBuilders.compute(variant);
        Map<ResourceType, ComputeResourceBuilder<ResourceBuilderContext>> resourceBuilderMap = new HashMap<>();
        for (ComputeResourceBuilder<ResourceBuilderContext> resourceBuilder : computeResourceBuilders) {
            resourceBuilderMap.put(resourceBuilder.resourceType(), resourceBuilder);
        }
        return resourceBuilderMap;
    }

    private void deleteResource(AuthenticatedContext auth, ResourceBuilderContext ctx, ComputeResourceBuilder<ResourceBuilderContext> resourceBuilder,
            CloudResourceStatus cloudResourceStatus) throws ExecutionException, InterruptedException {
        ResourceDeletionCallablePayload payload = new ResourceDeletionCallablePayload(ctx, auth, cloudResourceStatus.getCloudResource(),
                resourceBuilder, false);
        ResourceDeletionCallable deletionCallable = resourceActionFactory.buildDeletionCallable(payload);
        resourceBuilderExecutor.submit(deletionCallable).get();
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
                + statuses.size() + " nodes are failed. Error reason: " + statuses.get(0).getStatusReason());
    }

    private void throwError(List<CloudResourceStatus> statuses) {
        throw new CloudConnectorException(statuses.get(0).getStatusReason());
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
