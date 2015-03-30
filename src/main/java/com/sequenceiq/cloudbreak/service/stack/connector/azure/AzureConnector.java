package com.sequenceiq.cloudbreak.service.stack.connector.azure;

import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.stack.connector.CloudPlatformConnector;
import com.sequenceiq.cloudbreak.service.stack.connector.ParallelCloudResourceManager;
import com.sequenceiq.cloudbreak.service.stack.connector.UpdateFailedException;
import com.sequenceiq.cloudbreak.service.stack.resource.azure.builders.AzureResourceBuilderInit;

@Service
public class AzureConnector implements CloudPlatformConnector {
    private static final Logger LOGGER = LoggerFactory.getLogger(AzureConnector.class);

    @Autowired
    private AzureResourceBuilderInit azureResourceBuilderInit;

    @Autowired
    private ParallelCloudResourceManager cloudResourceManager;

    @Override
    public Set<Resource> buildStack(Stack stack, String gateWayUserData, String hostGroupUserData, Map<String, Object> setupProperties) {
        return cloudResourceManager.buildStackResources(stack, gateWayUserData, hostGroupUserData, azureResourceBuilderInit);
    }

    @Override
    public Set<Resource> addInstances(Stack stack, String gateWayUserData, String hostGroupUserData, Integer instanceCount, String instanceGroup) {
        return cloudResourceManager.addNewResources(stack, hostGroupUserData, instanceCount, instanceGroup, azureResourceBuilderInit);
    }

    @Override
    public Set<String> removeInstances(Stack stack, Set<String> origInstanceIds, String instanceGroup) {
        return cloudResourceManager.removeExistingResources(stack, origInstanceIds, azureResourceBuilderInit);
    }

    @Override
    public void deleteStack(final Stack stack, Credential credential) {
        cloudResourceManager.terminateResources(stack, azureResourceBuilderInit);
    }

    @Override
    public void rollback(final Stack stack, Set<Resource> resourceSet) {
        cloudResourceManager.rollbackResources(stack, azureResourceBuilderInit);
    }

    @Override
    public boolean startAll(Stack stack) {
        return cloudResourceManager.startStopResources(stack, true, azureResourceBuilderInit);
    }

    @Override
    public boolean stopAll(Stack stack) {
        return cloudResourceManager.startStopResources(stack, false, azureResourceBuilderInit);
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.AZURE;
    }

    @Override
    public void updateAllowedSubnets(Stack stack, String gateWayUserData, String hostGroupUserData) throws UpdateFailedException {
        cloudResourceManager.updateAllowedSubnets(stack, azureResourceBuilderInit);
    }
}
