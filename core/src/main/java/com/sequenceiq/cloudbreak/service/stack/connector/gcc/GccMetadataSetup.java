package com.sequenceiq.cloudbreak.service.stack.connector.gcc;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.api.services.compute.Compute;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.GccCredential;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.ResourceType;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.stack.connector.MetadataSetup;
import com.sequenceiq.cloudbreak.service.stack.flow.CoreInstanceMetaData;

@Component
public class GccMetadataSetup implements MetadataSetup {

    private static final Logger LOGGER = LoggerFactory.getLogger(GccMetadataSetup.class);

    @Autowired
    private GccStackUtil gccStackUtil;

    @Override
    public Set<CoreInstanceMetaData> collectMetadata(Stack stack) {
        List<Resource> resourcesByType = stack.getResourcesByType(ResourceType.GCC_INSTANCE);
        return collectMetaData(stack, resourcesByType);
    }

    private Set<CoreInstanceMetaData> collectMetaData(Stack stack, List<Resource> resources) {
        Set<CoreInstanceMetaData> instanceMetaDatas = new HashSet<>();
        Compute compute = gccStackUtil.buildCompute((GccCredential) stack.getCredential(), stack);
        for (Resource resource : resources) {
            instanceMetaDatas.add(getMetadata(stack, compute, resource));
        }
        return instanceMetaDatas;
    }

    @Override
    public Set<CoreInstanceMetaData> collectNewMetadata(Stack stack, Set<Resource> resourceList, String instanceGroup) {
        List<Resource> resources = new ArrayList<>();
        for (Resource resource : resourceList) {
            if (ResourceType.GCC_INSTANCE.equals(resource.getResourceType())) {
                resources.add(resource);
            }
        }
        return collectMetaData(stack, resources);
    }

    private CoreInstanceMetaData getMetadata(Stack stack, Compute compute, Resource resource) {
        return gccStackUtil.getMetadata(stack, compute, resource);
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.GCC;
    }
}
