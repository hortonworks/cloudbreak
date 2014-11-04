package com.sequenceiq.cloudbreak.service.stack.connector.gcc;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.api.services.compute.Compute;
import com.sequenceiq.cloudbreak.conf.ReactorConfig;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.GccCredential;
import com.sequenceiq.cloudbreak.domain.GccTemplate;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.ResourceType;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.logger.LoggerContextKey;
import com.sequenceiq.cloudbreak.logger.LoggerResourceType;
import com.sequenceiq.cloudbreak.service.stack.connector.MetadataSetup;
import com.sequenceiq.cloudbreak.service.stack.event.MetadataSetupComplete;
import com.sequenceiq.cloudbreak.service.stack.event.MetadataUpdateComplete;
import com.sequenceiq.cloudbreak.service.stack.flow.CoreInstanceMetaData;

import reactor.core.Reactor;
import reactor.event.Event;

@Component
public class GccMetadataSetup implements MetadataSetup {

    private static final Logger LOGGER = LoggerFactory.getLogger(GccMetadataSetup.class);

    @Autowired
    private Reactor reactor;

    @Autowired
    private GccStackUtil gccStackUtil;

    @Override
    public void setupMetadata(Stack stack) {
        MDC.put(LoggerContextKey.OWNER_ID.toString(), stack.getOwner());
        MDC.put(LoggerContextKey.RESOURCE_ID.toString(), stack.getId().toString());
        MDC.put(LoggerContextKey.RESOURCE_TYPE.toString(), LoggerResourceType.STACK_ID.toString());
        Set<CoreInstanceMetaData> instanceMetaDatas = new HashSet<>();
        List<Resource> resourcesByType = stack.getResourcesByType(ResourceType.GCC_INSTANCE);
        GccTemplate template = (GccTemplate) stack.getTemplate();
        instanceMetaDatas = collectMetaData(stack, template, resourcesByType);
        LOGGER.info("Publishing {} event [StackId: '{}']", ReactorConfig.METADATA_SETUP_COMPLETE_EVENT, stack.getId());
        reactor.notify(ReactorConfig.METADATA_SETUP_COMPLETE_EVENT,
                Event.wrap(new MetadataSetupComplete(CloudPlatform.GCC, stack.getId(), instanceMetaDatas)));
    }

    private Set<CoreInstanceMetaData> collectMetaData(Stack stack, GccTemplate template, List<Resource> resources) {
        Set<CoreInstanceMetaData> instanceMetaDatas = new HashSet<>();
        Compute compute = gccStackUtil.buildCompute((GccCredential) stack.getCredential(), stack.getName());
        for (Resource resource : resources) {
            instanceMetaDatas.add(getMetadata(stack, compute, resource));
        }
        return instanceMetaDatas;
    }

    @Override
    public void addNewNodesToMetadata(Stack stack, Set<Resource> resourceList) {
        MDC.put(LoggerContextKey.OWNER_ID.toString(), stack.getOwner());
        MDC.put(LoggerContextKey.RESOURCE_ID.toString(), stack.getId().toString());
        MDC.put(LoggerContextKey.RESOURCE_TYPE.toString(), LoggerResourceType.STACK_ID.toString());
        List<Resource> resources = new ArrayList<>();
        for (Resource resource : resourceList) {
            if (ResourceType.GCC_INSTANCE.equals(resource.getResourceType())) {
                resources.add(resource);
            }
        }
        Set<CoreInstanceMetaData> instanceMetaDatas = collectMetaData(stack, (GccTemplate) stack.getTemplate(), resources);
        LOGGER.info("Publishing {} event [StackId: '{}']", ReactorConfig.METADATA_UPDATE_COMPLETE_EVENT, stack.getId());
        reactor.notify(ReactorConfig.METADATA_UPDATE_COMPLETE_EVENT,
                Event.wrap(new MetadataUpdateComplete(CloudPlatform.AZURE, stack.getId(), instanceMetaDatas)));
    }

    private CoreInstanceMetaData getMetadata(Stack stack, Compute compute, Resource resource) {
        return gccStackUtil.getMetadata(stack, compute, resource.getResourceName());
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.GCC;
    }
}
