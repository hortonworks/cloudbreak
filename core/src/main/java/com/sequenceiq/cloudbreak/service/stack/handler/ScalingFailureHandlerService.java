package com.sequenceiq.cloudbreak.service.stack.handler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.type.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.common.type.ResourceType;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.common.type.Status;
import com.sequenceiq.cloudbreak.repository.ResourceRepository;
import com.sequenceiq.cloudbreak.service.events.CloudbreakEventService;
import com.sequenceiq.cloudbreak.service.stack.FailureHandlerService;
import com.sequenceiq.cloudbreak.service.stack.flow.ResourceRequestResult;
import com.sequenceiq.cloudbreak.service.stack.resource.DeleteContextObject;
import com.sequenceiq.cloudbreak.service.stack.resource.ResourceBuilder;
import com.sequenceiq.cloudbreak.service.stack.resource.ResourceBuilderInit;

@Component
@Qualifier("upscaleFailureHandlerService")
public class ScalingFailureHandlerService implements FailureHandlerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ScalingFailureHandlerService.class);

    @Inject
    private ResourceRepository resourceRepository;

    @javax.annotation.Resource
    private Map<CloudPlatform, List<ResourceBuilder>> instanceBuilders;

    @javax.annotation.Resource
    private Map<CloudPlatform, ResourceBuilderInit> resourceBuilderInits;

    @Inject
    private CloudbreakEventService eventService;

    @Override
    public void handleFailure(Stack stack, List<ResourceRequestResult> failedResourceRequestResults) {
        LOGGER.info("Decrease node counts because threshold was higher");
        handleExceptions(stack, failedResourceRequestResults);
    }

    private void handleExceptions(Stack stack, List<ResourceRequestResult> failedResourceRequestResult) {
        for (ResourceRequestResult exception : failedResourceRequestResult) {
            List<Resource> resourceList = new ArrayList<>();
            LOGGER.error("Error: {}", exception.getException().orNull().getMessage());
            resourceList.addAll(collectFailedResources(stack.getId(), exception.getResources()));
            resourceList.addAll(collectFailedResources(stack.getId(), exception.getBuiltResources()));
            if (!resourceList.isEmpty()) {
                LOGGER.info("Rolling back resources for stackId: {}. Resources to be rolled back: {}", stack.getId(), resourceList.size());
                doRollback(stack, resourceList);
            }
        }
    }

    private List<Resource> collectFailedResources(Long stackId, List<Resource> resources) {
        List<Resource> resourceList = new ArrayList<>();
        for (Resource resource : resources) {
            Resource newResource = resourceRepository.findByStackIdAndNameAndType(stackId, resource.getResourceName(), resource.getResourceType());
            if (newResource != null) {
                LOGGER.info(String.format("Resource %s with id %s and type %s was not deleted so added to rollback list.",
                        newResource.getResourceName(), newResource.getId(), newResource.getResourceType()));
                resourceList.add(newResource);
            }
        }
        return resourceList;
    }

    private void doRollback(Stack stack, List<Resource> resourceList) {
        CloudPlatform cloudPlatform = stack.cloudPlatform();
        ResourceBuilderInit resourceBuilderInit = resourceBuilderInits.get(cloudPlatform);
        try {
            final DeleteContextObject dCO = resourceBuilderInit.deleteInit(stack);
            List<ResourceBuilder> resourceBuilders = instanceBuilders.get(cloudPlatform);
            for (int i = resourceBuilders.size() - 1; i >= 0; i--) {
                ResourceBuilder resourceBuilder = resourceBuilders.get(i);
                ResourceType resourceType = resourceBuilder.resourceType();
                for (Resource tmpResource : resourceList) {
                    if (resourceType.equals(tmpResource.getResourceType())) {
                        String message = String.format("Resource will be rolled back because provision failed on the resource: %s",
                                tmpResource.getResourceName());
                        eventService.fireCloudbreakEvent(stack.getId(), Status.UPDATE_IN_PROGRESS.name(), message);
                        LOGGER.info(message);
                        resourceBuilder.delete(tmpResource, dCO, stack.getRegion());
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Resource can't be deleted. Exception: {}", e.getMessage());
        }
        for (Resource tmpResource : resourceList) {
            LOGGER.info("Deleting resource with id {}, name {}, type {}.", tmpResource.getId(), tmpResource.getResourceName(), tmpResource.getResourceType());
            resourceRepository.delete(tmpResource);
        }
    }
}
