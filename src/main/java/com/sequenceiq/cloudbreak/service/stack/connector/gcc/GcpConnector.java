package com.sequenceiq.cloudbreak.service.stack.connector.gcc;

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
import com.sequenceiq.cloudbreak.service.stack.resource.gcc.builders.GccResourceBuilderInit;

@Service
public class GcpConnector implements CloudPlatformConnector {
    private static final Logger LOGGER = LoggerFactory.getLogger(GcpConnector.class);

    @Autowired
    private GccResourceBuilderInit gcpResourceBuilderInit;

    @Autowired
    private ParallelCloudResourceManager cloudResourceManager;

    @Override
    public Set<Resource> buildStack(Stack stack, String userDataScript, Map<String, Object> setupProperties) {
        return cloudResourceManager.buildStackResources(stack, userDataScript, gcpResourceBuilderInit);
    }

    @Override
    public Set<Resource> addInstances(Stack stack, String userData, Integer instanceCount, String instanceGroup) {
        return cloudResourceManager.addNewResources(stack, userData, instanceCount, instanceGroup, gcpResourceBuilderInit);
    }

    @Override
    public Set<String> removeInstances(Stack stack, Set<String> origInstanceIds, String instanceGroup) {
        return cloudResourceManager.removeExistingResources(stack, origInstanceIds, gcpResourceBuilderInit);
    }

    @Override
    public void deleteStack(final Stack stack, Credential credential) {
        cloudResourceManager.terminateResources(stack, gcpResourceBuilderInit);
    }

    @Override
    public void rollback(final Stack stack, Set<Resource> resourceSet) {
            cloudResourceManager.rollbackResources(stack, gcpResourceBuilderInit);
    }

    @Override
    public boolean startAll(Stack stack) {
        return cloudResourceManager.startStopResources(stack, true, gcpResourceBuilderInit);
    }

    @Override
    public boolean stopAll(Stack stack) {
        return cloudResourceManager.startStopResources(stack, false, gcpResourceBuilderInit);
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.GCC;
    }

    @Override
    public void updateAllowedSubnets(Stack stack, String userData) throws UpdateFailedException {
        cloudResourceManager.updateAllowedSubnets(stack, gcpResourceBuilderInit);
    }
}
