package com.sequenceiq.cloudbreak.service;

import java.util.Map;

import javax.annotation.Resource;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.service.stack.connector.CloudPlatformConnector;
import com.sequenceiq.cloudbreak.service.stack.connector.MetadataSetup;
import com.sequenceiq.cloudbreak.service.stack.connector.ProvisionSetup;

@Service
public class CloudPlatformResolver {

    @javax.annotation.Resource
    private Map<CloudPlatform, MetadataSetup> metadataSetups;

    @Resource
    private Map<CloudPlatform, ProvisionSetup> provisionSetups;

    @javax.annotation.Resource
    private Map<CloudPlatform, CloudPlatformConnector> cloudPlatformConnectors;

    public MetadataSetup metadata(CloudPlatform platform) {
        MetadataSetup metadataSetup = metadataSetups.get(platform);
        return metadataSetup == null ? metadataSetups.get(CloudPlatform.ADAPTER) : metadataSetup;
    }

    public CloudPlatformConnector connector(CloudPlatform platform) {
        CloudPlatformConnector platformConnector = cloudPlatformConnectors.get(platform);
        return platformConnector == null ? cloudPlatformConnectors.get(CloudPlatform.ADAPTER) : platformConnector;
    }

    public ProvisionSetup provisioning(CloudPlatform cloudPlatform) {
        ProvisionSetup provisionSetup = provisionSetups.get(cloudPlatform);
        return provisionSetup == null ? provisionSetups.get(CloudPlatform.ADAPTER) : provisionSetup;
    }
}
