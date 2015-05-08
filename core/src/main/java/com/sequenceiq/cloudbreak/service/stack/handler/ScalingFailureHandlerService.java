package com.sequenceiq.cloudbreak.service.stack.handler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.ResourceType;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.InstanceGroupRepository;
import com.sequenceiq.cloudbreak.repository.ResourceRepository;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.stack.FailureHandlerService;
import com.sequenceiq.cloudbreak.service.stack.flow.ResourceRequestResult;
import com.sequenceiq.cloudbreak.service.stack.resource.DeleteContextObject;
import com.sequenceiq.cloudbreak.service.stack.resource.ResourceBuilder;
import com.sequenceiq.cloudbreak.service.stack.resource.ResourceBuilderInit;

@Component
@Qualifier("upscaleFailureHandlerService")
public class ScalingFailureHandlerService implements FailureHandlerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ScalingFailureHandlerService.class);
    private static final double ONE_HUNDRED = 100.0;

    @Autowired
    private StackRepository stackRepository;
    @Autowired
    private ResourceRepository resourceRepository;
    @Autowired
    private InstanceGroupRepository instanceGroupRepository;

    @javax.annotation.Resource
    private Map<CloudPlatform, List<ResourceBuilder>> instanceResourceBuilders;

    @javax.annotation.Resource
    private Map<CloudPlatform, ResourceBuilderInit> resourceBuilderInits;

    @Override
    public void handleFailure(Stack stack, List<ResourceRequestResult> failedResourceRequestResults) {
        LOGGER.info("Decrease node counts because threshold was higher");
        handleExceptions(stack, failedResourceRequestResults);
    }

    private void handleExceptions(Stack stack, List<ResourceRequestResult> failedResourceRequestResult) {
        for (ResourceRequestResult exception : failedResourceRequestResult) {
            List<Resource> resourceList = new ArrayList<>();
            LOGGER.error("Error: {}", exception.getException().orNull().getMessage());
            for (Resource resource : exception.getResources()) {
                Resource newResource = resourceRepository.findByStackIdAndNameAndType(stack.getId(), resource.getResourceName(), resource.getResourceType());
                if (newResource != null) {
                    LOGGER.info("Resource will be rolled back. name: {}, id: {}, type: {}", newResource.getResourceName(), newResource.getId(),
                            newResource.getResourceType());
                    resourceList.add(newResource);
                }
            }
            if (!resourceList.isEmpty()) {
                LOGGER.info("Rolling back resources for stackId: {}. Resources to be rolled back: {}", stack.getId(), resourceList.size());
                doRollback(stack, resourceList);
            }
        }
    }

    private void doRollback(Stack stack, List<Resource> resourceList) {
        CloudPlatform cloudPlatform = stack.cloudPlatform();
        ResourceBuilderInit resourceBuilderInit = resourceBuilderInits.get(cloudPlatform);
        try {
            final DeleteContextObject dCO = resourceBuilderInit.deleteInit(stack);
            for (int i = instanceResourceBuilders.get(cloudPlatform).size() - 1; i >= 0; i--) {
                ResourceType resourceType = instanceResourceBuilders.get(cloudPlatform).get(i).resourceType();
                for (Resource tmpResource : resourceList) {
                    if (resourceType.equals(tmpResource.getResourceType())) {
                        LOGGER.info("Resource will be rolled back. name: {}, id: {}, type: {}", tmpResource.getResourceName(), tmpResource.getId(),
                                tmpResource.getResourceType());
                        instanceResourceBuilders.get(cloudPlatform).get(i).delete(tmpResource, dCO, stack.getRegion());
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
