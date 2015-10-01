package com.sequenceiq.cloudbreak.service;

import java.util.Map;

import javax.annotation.Resource;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.type.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.service.credential.CredentialHandler;
import com.sequenceiq.cloudbreak.service.stack.connector.CloudPlatformConnector;
import com.sequenceiq.cloudbreak.service.stack.connector.MetadataSetup;
import com.sequenceiq.cloudbreak.service.stack.connector.ProvisionSetup;

@Service
public class CloudPlatformResolver {

    @Resource
    private Map<CloudPlatform, MetadataSetup> metadataSetups;

    @Resource
    private Map<CloudPlatform, ProvisionSetup> provisionSetups;

    @Resource
    private Map<CloudPlatform, CloudPlatformConnector> platformConnectors;

    @Resource
    private Map<CloudPlatform, CredentialHandler<Credential>> credentialHandlers;

    public MetadataSetup metadata(CloudPlatform platform) {
        MetadataSetup metadataSetup = metadataSetups.get(platform);
        return metadataSetup == null ? metadataSetups.get(CloudPlatform.ADAPTER) : metadataSetup;
    }

    public CloudPlatformConnector connector(CloudPlatform platform) {
        CloudPlatformConnector platformConnector = platformConnectors.get(platform);
        return platformConnector == null ? platformConnectors.get(CloudPlatform.ADAPTER) : platformConnector;
    }

    public ProvisionSetup provisioning(CloudPlatform platform) {
        ProvisionSetup provisionSetup = provisionSetups.get(platform);
        return provisionSetup == null ? provisionSetups.get(CloudPlatform.ADAPTER) : provisionSetup;
    }

    public CredentialHandler<Credential> credential(CloudPlatform platform) {
        CredentialHandler<Credential> handler = credentialHandlers.get(platform);
        return handler == null ? credentialHandlers.get(CloudPlatform.ADAPTER) : handler;
    }
}
