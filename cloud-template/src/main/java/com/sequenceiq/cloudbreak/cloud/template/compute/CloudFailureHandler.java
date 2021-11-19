package com.sequenceiq.cloudbreak.cloud.template.compute;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import javax.inject.Inject;

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

@Service
public class CloudFailureHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudFailureHandler.class);

    private static final double ONE_HUNDRED = 100.0;

    @Inject
    private AsyncTaskExecutor resourceBuilderExecutor;

    @Inject
    private ResourceActionFactory resourceActionFactory;

    public void rollbackIfNecessary(CloudFailureContext cloudFailureContext, List<CloudResourceStatus> failuresList, List<CloudResourceStatus> resourceStatuses,
            Group group, ResourceBuilders resourceBuilders, Integer fullNodeCount) {
        if (failuresList.isEmpty()) {
            return;
        }
        LOGGER.debug("Roll back the following resources: {}", failuresList);
        doRollback(cloudFailureContext, failuresList, resourceStatuses, group, fullNodeCount, resourceBuilders);
    }

    private void doRollback(CloudFailureContext cloudFailureContext, List<CloudResourceStatus> failuresList, List<CloudResourceStatus> resourceStatuses,
            Group group, Integer fullNodeCount, ResourceBuilders resourceBuilders) {
        Set<Long> failures = failureCount(failuresList);
        ScaleContext stx = cloudFailureContext.getStx();
        AuthenticatedContext auth = cloudFailureContext.getAuth();
        ResourceBuilderContext ctx = cloudFailureContext.getCtx();
        if (stx.getAdjustmentType() == null && !failures.isEmpty()) {
            LOGGER.info("Failure policy is null so error will be thrown");
            throwError(failuresList);
        }
        switch (stx.getAdjustmentType()) {
            case EXACT:
                if (stx.getThreshold() > fullNodeCount - failures.size()) {
                    LOGGER.info("Number of failures is more than the threshold ({}) so error will be thrown", stx.getThreshold());
                    failures = resourceStatuses.stream().map(CloudResourceStatus::getPrivateId).collect(Collectors.toSet());
                    doRollbackAndDecreaseNodeCount(auth, resourceStatuses, failures, group, ctx, resourceBuilders, stx.getUpscale());
                    throwRolledbackException(failuresList);
                } else if (!failures.isEmpty()) {
                    LOGGER.info("Decrease node counts because threshold was higher");
                    handleExceptions(auth, failuresList, group, ctx, resourceBuilders, failures, stx.getUpscale());
                }
                break;
            case PERCENTAGE:
                if (Double.valueOf(stx.getThreshold()) > calculatePercentage(failures.size(), fullNodeCount)) {
                    LOGGER.info("Number of failures is more than the threshold so error will throw");
                    throwError(failuresList);
                } else if (!failures.isEmpty()) {
                    LOGGER.info("Decrease node counts because threshold was higher");
                    handleExceptions(auth, failuresList, group, ctx, resourceBuilders, failures, stx.getUpscale());
                }
                break;
            case BEST_EFFORT:
                LOGGER.info("Decrease node counts because threshold was higher");
                handleExceptions(auth, failuresList, group, ctx, resourceBuilders, failures, stx.getUpscale());
                break;
            default:
                LOGGER.info("Unsupported adjustment type so error will throw");
                throwError(failuresList);
                break;
        }
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

    private double calculatePercentage(double failedResourceRequestResults, double fullNodeCount) {
        return (fullNodeCount + failedResourceRequestResults) / fullNodeCount * ONE_HUNDRED;
    }

    private void handleExceptions(AuthenticatedContext auth, List<CloudResourceStatus> cloudResourceStatuses, Group group,
            ResourceBuilderContext ctx, ResourceBuilders resourceBuilders, Collection<Long> ids, Boolean upscale) {
        List<CloudResource> resources = new ArrayList<>(cloudResourceStatuses.size());
        for (CloudResourceStatus exception : cloudResourceStatuses) {
            if (ResourceStatus.FAILED.equals(exception.getStatus()) || ids.contains(exception.getPrivateId())) {
                LOGGER.info("Failed to create instance: " + exception.getStatusReason());
                resources.add(exception.getCloudResource());
            }
        }
        if (!resources.isEmpty()) {
            LOGGER.info("Resource list not empty so rollback will start.Resource list size is: " + resources.size());
            doRollbackAndDecreaseNodeCount(auth, cloudResourceStatuses, ids, group, ctx, resourceBuilders, upscale);
        }
    }

    private void doRollbackAndDecreaseNodeCount(AuthenticatedContext auth, List<CloudResourceStatus> statuses, Collection<Long> ids, Group group,
            ResourceBuilderContext ctx, ResourceBuilders resourceBuilders, Boolean upscale) {
        Variant variant = auth.getCloudContext().getVariant();
        List<ComputeResourceBuilder<ResourceBuilderContext>> compute = resourceBuilders.compute(variant);
        Collection<Future<ResourceRequestResult<List<CloudResourceStatus>>>> futures = new ArrayList<>();
        LOGGER.info("InstanceGroup {} node count decreased with one so the new node size is: {}", group.getName(), group.getInstancesSize());

        if (getRemovableInstanceTemplates(group, ids).size() <= 0 && !upscale) {
            LOGGER.info("InstanceGroup node count lower than 1 which is incorrect so error will be thrown");
            throwError(statuses);
        } else {
            for (int i = compute.size() - 1; i >= 0; i--) {
                for (CloudResourceStatus cloudResourceStatus : statuses) {
                    try {
                        if (compute.get(i).resourceType().equals(cloudResourceStatus.getCloudResource().getType())) {
                            ResourceDeletionCallablePayload payload = new ResourceDeletionCallablePayload(
                                    ctx, auth, cloudResourceStatus.getCloudResource(), compute.get(i), false);
                            ResourceDeletionCallable deletionCallable = resourceActionFactory.buildDeletionCallable(payload);
                            Future<ResourceRequestResult<List<CloudResourceStatus>>> future = resourceBuilderExecutor.submit(deletionCallable);
                            futures.add(future);
                            for (Future<ResourceRequestResult<List<CloudResourceStatus>>> future1 : futures) {
                                future1.get();
                            }
                            futures.clear();
                        }
                    } catch (Exception e) {
                        LOGGER.warn("Resource can not be deleted. Reason: " + e.getMessage(), e);
                    }
                }
            }
        }
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
        throw new RolledbackResourcesException(statuses.get(0).getStatusReason());
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
