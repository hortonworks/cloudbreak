package com.sequenceiq.cloudbreak.service.stack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.controller.BuildStackFailureException;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.ResourceType;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.InstanceGroupRepository;
import com.sequenceiq.cloudbreak.repository.ResourceRepository;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.stack.flow.ResourceRequestResult;
import com.sequenceiq.cloudbreak.service.stack.resource.DeleteContextObject;
import com.sequenceiq.cloudbreak.service.stack.resource.ResourceBuilder;
import com.sequenceiq.cloudbreak.service.stack.resource.ResourceBuilderInit;

@Component
@Qualifier("stackFailureHandlerService")
public class StackFailureHandlerService implements FailureHandlerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackFailureHandlerService.class);
    private static final double ONE_HUNDRED = 100.0;

    @Autowired
    private StackRepository stackRepository;

    @Autowired
    private InstanceGroupRepository instanceGroupRepository;

    @Autowired
    private ResourceRepository resourceRepository;

    @javax.annotation.Resource
    private Map<CloudPlatform, List<ResourceBuilder>> instanceResourceBuilders;

    @javax.annotation.Resource
    private Map<CloudPlatform, ResourceBuilderInit> resourceBuilderInits;

    @Override
    public void handleFailure(Stack stack, List<ResourceRequestResult> failedResourceRequestResults) {
        Stack localStack = stackRepository.findById(stack.getId());
        if (localStack.getFailurePolicy() == null) {
            if (failedResourceRequestResults.size() > 0) {
                LOGGER.info("Failure policy is null so error will throw");
                throwError(localStack, failedResourceRequestResults);
            }
        } else {
            switch (localStack.getFailurePolicy().getAdjustmentType()) {
            case EXACT:
                if (localStack.getFailurePolicy().getThreshold() > localStack.getFullNodeCount() - failedResourceRequestResults.size()) {
                    LOGGER.info("Number of failures is more than the threshold so error will throw");
                    throwError(localStack, failedResourceRequestResults);
                } else if (failedResourceRequestResults.size() != 0) {
                    LOGGER.info("Decrease node counts because threshold was higher");
                    handleExceptions(localStack, failedResourceRequestResults);
                }
                break;
            case PERCENTAGE:
                if (Double.valueOf(localStack.getFailurePolicy().getThreshold()) > calculatePercentage(failedResourceRequestResults, localStack)) {
                    LOGGER.info("Number of failures is more than the threshold so error will throw");
                    throwError(localStack, failedResourceRequestResults);
                } else if (failedResourceRequestResults.size() != 0) {
                    LOGGER.info("Decrease node counts because threshold was higher");
                    handleExceptions(localStack, failedResourceRequestResults);
                }
                break;
            case BEST_EFFORT:
                LOGGER.info("Decrease node counts because threshold was higher");
                handleExceptions(localStack, failedResourceRequestResults);
                break;
            default:
                LOGGER.info("Unsupported adjustment type so error will throw");
                throwError(localStack, failedResourceRequestResults);
                break;
            }
        }
    }

    private double calculatePercentage(List<ResourceRequestResult> failedResourceRequestResults, Stack localStack) {
        return Double.valueOf((localStack.getFullNodeCount() + failedResourceRequestResults.size()) / localStack.getFullNodeCount()) * ONE_HUNDRED;
    }

    private void handleExceptions(Stack stack, List<ResourceRequestResult> failedResourceRequestResult) {
        for (ResourceRequestResult exception : failedResourceRequestResult) {
            List<Resource> resourceList = new ArrayList<>();
            LOGGER.error("Error was occurred which is: " + exception.getException().orNull().getMessage());
            for (Resource resource : exception.getResources()) {
                Resource newResource = resourceRepository.findByStackIdAndNameAndType(stack.getId(), resource.getResourceName(), resource.getResourceType());
                if (newResource != null) {
                    LOGGER.info(String.format("Resource %s with id %s and type %s was not deleted so added to rollback list.",
                            newResource.getResourceName(), newResource.getId(), newResource.getResourceType()));
                    resourceList.add(newResource);
                }
            }
            if (!resourceList.isEmpty()) {
                LOGGER.info("Resource list not empty so rollback will start.Resource list size is: " + resourceList.size());
                doRollbackAndDecreaseNodeCount(exception, stack, resourceList, failedResourceRequestResult);
            }
        }
    }

    private void doRollbackAndDecreaseNodeCount(ResourceRequestResult exception, Stack stack, List<Resource> resourceList,
            List<ResourceRequestResult> resourceRequestResults) {
        InstanceGroup instanceGroup = instanceGroupRepository.findOne(exception.getInstanceGroup().getId());
        instanceGroup.setNodeCount(instanceGroup.getNodeCount() - 1);
        LOGGER.info(String.format("InstanceGroup %s node count decreased with one so the new node size is: %s",
                instanceGroup.getGroupName(), instanceGroup.getNodeCount()));
        if (instanceGroup.getNodeCount() <= 0) {
            LOGGER.info("InstanceGroup node count lower than 1 which is incorrect so error will throw");
            throwError(stack, resourceRequestResults);
        } else {
            LOGGER.info("InstanceGroup saving with the new node count which is: " + instanceGroup.getNodeCount());
            instanceGroupRepository.save(instanceGroup);
            CloudPlatform cloudPlatform = stack.cloudPlatform();
            ResourceBuilderInit resourceBuilderInit = resourceBuilderInits.get(cloudPlatform);
            try {
                final DeleteContextObject dCO = resourceBuilderInit.deleteInit(stack);
                for (int i = instanceResourceBuilders.get(cloudPlatform).size() - 1; i >= 0; i--) {
                    ResourceType resourceType = instanceResourceBuilders.get(cloudPlatform).get(i).resourceType();
                    for (Resource tmpResource : resourceList) {
                        if (resourceType.equals(tmpResource.getResourceType())) {
                            LOGGER.info(String.format("Resource will rollback with %s name %s id and %s type.",
                                    tmpResource.getResourceName(), tmpResource.getId(), tmpResource.getResourceType()));
                            instanceResourceBuilders.get(cloudPlatform).get(i).delete(tmpResource, dCO, stack.getRegion());
                        }
                    }
                }
            } catch (Exception e) {
                LOGGER.info("Resource can not be deleted. Reason: {} ", e.getMessage());
            }
            for (Resource tmpResource : resourceList) {
                LOGGER.info("Deleting resource. id: {}, name: {}, type: {}", tmpResource.getId(), tmpResource.getResourceName(), tmpResource.getResourceType());
                resourceRepository.delete(tmpResource);
            }
        }
    }

    private void throwError(Stack stack, List<ResourceRequestResult> resourceRequestResults) {
        Stack oneWithLists = stackRepository.findOneWithLists(stack.getId());
        StringBuilder sb = new StringBuilder();
        for (ResourceRequestResult resourceRequestResult : resourceRequestResults) {
            if (resourceRequestResult.getException().orNull() != null) {
                sb.append(String.format("%s, ", resourceRequestResult.getException().orNull().getMessage()));
            }
        }
        throw new BuildStackFailureException(sb.toString(), resourceRequestResults.get(0).getException().orNull(), oneWithLists.getResources());
    }
}
