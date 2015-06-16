package com.sequenceiq.cloudbreak.service.stack.connector.openstack;

import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.stack.connector.MetadataSetup;
import com.sequenceiq.cloudbreak.service.stack.flow.CoreInstanceMetaData;

@Component
public class OpenStackMetadataSetupAdapter implements MetadataSetup {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenStackMetadataSetupAdapter.class);

    @Value("${cb.openstack.experimental.connector:false}")
    private boolean experimentalConnector;

    @Inject
    private OpenStackMetadataSetup openStackMetadataSetup;

    @Override
    public Set<CoreInstanceMetaData> collectMetadata(Stack stack) {
        if (experimentalConnector) {
            return openStackMetadataSetup.collectMetadata(stack);
        } else {
            return openStackMetadataSetup.collectMetadata(stack);
        }
    }

    @Override
    public Set<CoreInstanceMetaData> collectNewMetadata(Stack stack, Set<Resource> resourceList, final String instanceGroupName) {
        if (experimentalConnector) {
            return openStackMetadataSetup.collectNewMetadata(stack, resourceList, instanceGroupName);
        } else {
            return openStackMetadataSetup.collectNewMetadata(stack, resourceList, instanceGroupName);
        }
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.OPENSTACK;
    }

}

