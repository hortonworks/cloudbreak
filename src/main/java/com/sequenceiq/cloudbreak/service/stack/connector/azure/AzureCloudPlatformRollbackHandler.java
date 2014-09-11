package com.sequenceiq.cloudbreak.service.stack.connector.azure;

import static com.sequenceiq.cloudbreak.domain.CloudPlatform.AZURE;

import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.stack.connector.CloudPlatformRollbackHandler;

@Service
public class AzureCloudPlatformRollbackHandler implements CloudPlatformRollbackHandler {

    @Autowired
    private AzureConnector azureConnector;

    @Override
    public void rollback(Stack stack, Set<Resource> resourceSet) {
        azureConnector.rollback(stack, stack.getCredential(), resourceSet);
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return AZURE;
    }
}
