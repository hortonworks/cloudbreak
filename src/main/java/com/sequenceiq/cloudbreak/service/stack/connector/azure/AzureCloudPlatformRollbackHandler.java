package com.sequenceiq.cloudbreak.service.stack.connector.azure;

import static com.sequenceiq.cloudbreak.domain.CloudPlatform.AZURE;

import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.User;
import com.sequenceiq.cloudbreak.service.stack.connector.CloudPlatformRollbackHandler;

@Service
public class AzureCloudPlatformRollbackHandler implements CloudPlatformRollbackHandler {

    @Autowired
    private AzureConnector azureConnector;

    @Override
    public void rollback(User user, Stack stack, Credential credential, Set<Resource> resourceSet) {
        azureConnector.rollback(user, stack, credential, resourceSet);
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return AZURE;
    }
}
