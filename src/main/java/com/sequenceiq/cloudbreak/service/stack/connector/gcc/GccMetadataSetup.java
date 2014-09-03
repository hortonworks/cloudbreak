package com.sequenceiq.cloudbreak.service.stack.connector.gcc;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.api.services.compute.Compute;
import com.google.api.services.compute.model.Instance;
import com.sequenceiq.cloudbreak.conf.ReactorConfig;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.GccCredential;
import com.sequenceiq.cloudbreak.domain.GccTemplate;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.ResourceType;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.stack.connector.MetadataSetup;
import com.sequenceiq.cloudbreak.service.stack.event.MetadataSetupComplete;
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
        Set<CoreInstanceMetaData> instanceMetaDatas = new HashSet<>();
        List<Resource> resourcesByType = stack.getResourcesByType(ResourceType.VIRTUAL_MACHINE);
        GccTemplate template = (GccTemplate) stack.getTemplate();
        Compute compute = gccStackUtil.buildCompute((GccCredential) stack.getCredential(), stack);
        try {
            for (Resource resource : resourcesByType) {
                Compute.Instances.Get instanceGet = compute.instances().get(
                        template.getProjectId(), template.getGccZone().getValue(), resource.getResourceName());
                Instance executeInstance = instanceGet.execute();
                CoreInstanceMetaData coreInstanceMetaData = new CoreInstanceMetaData(
                        resource.getResourceName(),
                        executeInstance.getNetworkInterfaces().get(0).getNetworkIP(),
                        executeInstance.getNetworkInterfaces().get(0).getAccessConfigs().get(0).getNatIP(),
                        template.getVolumeCount(),
                        longName(resource.getResourceName(), template.getProjectId())

                );
                instanceMetaDatas.add(coreInstanceMetaData);
            }
        } catch (IOException ex) {
            LOGGER.info("Exception {} occured under the metadata setup: {}", ex.getMessage(), ex.getStackTrace());
        }
        LOGGER.info("Publishing {} event [StackId: '{}']", ReactorConfig.METADATA_SETUP_COMPLETE_EVENT, stack.getId());
        reactor.notify(ReactorConfig.METADATA_SETUP_COMPLETE_EVENT,
                Event.wrap(new MetadataSetupComplete(CloudPlatform.GCC, stack.getId(), instanceMetaDatas)));
    }

    private String longName(String resourceName, String projectId) {
        return String.format("%s.c.%s.internal", resourceName, projectId);
    }

    @Override
    public void addNewNodesToMetadata(Stack stack, Set<Resource> resourceList) {
        // TODO need to implement
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.GCC;
    }
}
