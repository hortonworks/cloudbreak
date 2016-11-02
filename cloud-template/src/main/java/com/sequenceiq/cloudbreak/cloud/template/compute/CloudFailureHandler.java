package com.sequenceiq.cloudbreak.cloud.template.compute;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.model.AdjustmentType;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.cloudbreak.cloud.template.ComputeResourceBuilder;
import com.sequenceiq.cloudbreak.cloud.template.context.ResourceBuilderContext;
import com.sequenceiq.cloudbreak.cloud.template.init.ResourceBuilders;

@Service
public class CloudFailureHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudFailureHandler.class);

    private static final double ONE_HUNDRED = 100.0;

    @Inject
    private AsyncTaskExecutor resourceBuilderExecutor;

    @Inject
    private ApplicationContext applicationContext;

    public void rollback(AuthenticatedContext auth, List<CloudResourceStatus> failuresList, Group group, Integer fullNodeCount,
            ResourceBuilderContext ctx, ResourceBuilders resourceBuilders, ScaleContext stx) {
        if (failuresList.isEmpty()) {
            return;
        }
        doRollback(auth, failuresList, group, fullNodeCount, ctx, resourceBuilders, stx);
    }

    private void doRollback(AuthenticatedContext auth, List<CloudResourceStatus> failuresList, Group group, Integer fullNodeCount,
            ResourceBuilderContext ctx, ResourceBuilders resourceBuilders, ScaleContext stx) {
        Set<Long> failures = failureCount(failuresList);
        if (stx.getAdjustmentType() == null && failures.size() > 0) {
            LOGGER.info("Failure policy is null so error will throw");
            throwError(failuresList);
        }
        switch (stx.getAdjustmentType()) {
            case EXACT:
                if (stx.getThreshold() > fullNodeCount - failures.size()) {
                    LOGGER.info("Number of failures is more than the threshold so error will throw");
                    throwError(failuresList);
                } else if (failures.size() != 0) {
                    LOGGER.info("Decrease node counts because threshold was higher");
                    handleExceptions(auth, failuresList, group, ctx, resourceBuilders, failures, stx.getUpscale());
                }
                break;
            case PERCENTAGE:
                if (Double.valueOf(stx.getThreshold()) > calculatePercentage(failures.size(), fullNodeCount)) {
                    LOGGER.info("Number of failures is more than the threshold so error will throw");
                    throwError(failuresList);
                } else if (failures.size() != 0) {
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

    private Set<Long> failureCount(List<CloudResourceStatus> failedResourceRequestResults) {
        Set<Long> ids = new HashSet<>();
        for (CloudResourceStatus failedResourceRequestResult : failedResourceRequestResults) {
            if (ResourceStatus.FAILED.equals(failedResourceRequestResult.getStatus())) {
                ids.add(failedResourceRequestResult.getPrivateId());
            }
        }
        return ids;
    }

    private double calculatePercentage(Integer failedResourceRequestResults, Integer fullNodeCount) {
        return (double) ((fullNodeCount + failedResourceRequestResults) / fullNodeCount) * ONE_HUNDRED;
    }

    private void handleExceptions(AuthenticatedContext auth, List<CloudResourceStatus> cloudResourceStatuses, Group group,
            ResourceBuilderContext ctx, ResourceBuilders resourceBuilders, Set<Long> ids, Boolean upscale) {
        List<CloudResource> resources = new ArrayList<>();
        for (CloudResourceStatus exception : cloudResourceStatuses) {
            if (ResourceStatus.FAILED.equals(exception.getStatus()) || ids.contains(exception.getPrivateId())) {
                LOGGER.error("Failed to create instance: " + exception.getStatusReason());
                resources.add(exception.getCloudResource());
            }
        }
        if (!resources.isEmpty()) {
            LOGGER.info("Resource list not empty so rollback will start.Resource list size is: " + resources.size());
            doRollbackAndDecreaseNodeCount(auth, cloudResourceStatuses, ids, group, ctx, resourceBuilders, upscale);
        }
    }

    private void doRollbackAndDecreaseNodeCount(AuthenticatedContext auth, List<CloudResourceStatus> statuses, Set<Long> ids, Group group,
            ResourceBuilderContext ctx, ResourceBuilders resourceBuilders, Boolean upscale) {
        List<ComputeResourceBuilder> compute = resourceBuilders.compute(auth.getCloudContext().getPlatform());
        List<Future<ResourceRequestResult<List<CloudResourceStatus>>>> futures = new ArrayList<>();
        LOGGER.info(String.format("InstanceGroup %s node count decreased with one so the new node size is: %s", group.getName(), group.getInstances().size()));
        if (getRemovableInstanceTemplates(group, ids).size() <= 0 && !upscale) {
            LOGGER.info("InstanceGroup node count lower than 1 which is incorrect so error will throw");
            throwError(statuses);
        } else {
            for (int i = compute.size() - 1; i >= 0; i--) {
                for (CloudResourceStatus cloudResourceStatus : statuses) {
                    try {
                        if (compute.get(i).resourceType().equals(cloudResourceStatus.getCloudResource().getType())) {
                            ResourceDeleteThread thread =
                                    createThread(ResourceDeleteThread.NAME, ctx, auth, cloudResourceStatus.getCloudResource(), compute.get(i), false);
                            Future<ResourceRequestResult<List<CloudResourceStatus>>> future = resourceBuilderExecutor.submit(thread);
                            futures.add(future);
                            for (Future<ResourceRequestResult<List<CloudResourceStatus>>> future1 : futures) {
                                future1.get();
                            }
                            futures.clear();
                        }
                    } catch (Exception e) {
                        LOGGER.info("Resource can not be deleted. Reason: {} ", e.getMessage());
                    }
                }
            }
        }
    }

    private List<InstanceTemplate> getRemovableInstanceTemplates(Group group, Set<Long> ids) {
        List<InstanceTemplate> instanceTemplates = new ArrayList<>();
        for (CloudInstance cloudInstance : group.getInstances()) {
            InstanceTemplate instanceTemplate = cloudInstance.getTemplate();
            if (!ids.contains(instanceTemplate.getPrivateId())) {
                instanceTemplates.add(instanceTemplate);
            }
        }
        return instanceTemplates;
    }

    private <T> T createThread(String name, Object... args) {
        return (T) applicationContext.getBean(name, args);
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
