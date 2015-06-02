package com.sequenceiq.cloudbreak.service.stack.connector.gcp;

import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.EnvironmentVariableConfig;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.stack.connector.CloudPlatformConnector;
import com.sequenceiq.cloudbreak.service.stack.connector.ParallelCloudResourceManager;
import com.sequenceiq.cloudbreak.service.stack.resource.gcp.builders.GcpResourceBuilderInit;

@Service
public class GcpConnector implements CloudPlatformConnector {
    private static final Logger LOGGER = LoggerFactory.getLogger(GcpConnector.class);

    @Inject
    private GcpResourceBuilderInit gcpResourceBuilderInit;

    @Inject
    private ParallelCloudResourceManager cloudResourceManager;

    @Override
    public Set<Resource> buildStack(Stack stack, String gateWayUserData, String coreUserData, Map<String, Object> setupProperties) {
        return cloudResourceManager.buildStackResources(stack, gateWayUserData, coreUserData, gcpResourceBuilderInit);
    }

    @Override
    public Set<Resource> addInstances(Stack stack, String gateWayUserData, String coreUserData, Integer instanceCount, String instanceGroup) {
        return cloudResourceManager.addNewResources(stack, coreUserData, instanceCount, instanceGroup, gcpResourceBuilderInit);
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
        return CloudPlatform.GCP;
    }

    @Override
    public void updateAllowedSubnets(Stack stack, String gateWayUserData, String coreUserData) {
        cloudResourceManager.updateAllowedSubnets(stack, gcpResourceBuilderInit);
    }

    @Override
    public String getSSHUser() {
        return EnvironmentVariableConfig.CB_GCP_AND_AZURE_USER_NAME;
    }

    @Override
    public String getSSHFingerprint(Stack stack, String gateway) {
        return "THUMBPRINT";
    }

    @Override
    public void cleanupTemporarySSH(Stack stack, String instanceId) {
        LOGGER.info("Not implemented");
    }
}
