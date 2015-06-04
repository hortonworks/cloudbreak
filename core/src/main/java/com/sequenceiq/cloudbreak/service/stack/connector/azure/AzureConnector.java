package com.sequenceiq.cloudbreak.service.stack.connector.azure;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.EnvironmentVariableConfig;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.events.CloudbreakEventService;
import com.sequenceiq.cloudbreak.service.stack.connector.CloudPlatformConnector;
import com.sequenceiq.cloudbreak.service.stack.connector.ParallelCloudResourceManager;
import com.sequenceiq.cloudbreak.service.stack.resource.azure.builders.AzureResourceBuilderInit;

@Service
public class AzureConnector implements CloudPlatformConnector {
    private static final Logger LOGGER = LoggerFactory.getLogger(AzureConnector.class);

    @Inject
    private AzureResourceBuilderInit azureResourceBuilderInit;

    @Inject
    private ParallelCloudResourceManager cloudResourceManager;

    @Inject
    private CloudbreakEventService cloudbreakEventService;

    private Map<String, Lock> lockMap = Collections.synchronizedMap(new HashMap<String, Lock>());

    @Override
    public Set<Resource> buildStack(final Stack stack, final String gateWayUserData, final String coreUserData,
            final Map<String, Object> setupProperties) {
        BuildStackOperation buildStackOperation = buildAzureOperation(new BuildStackOperation.Builder(), stack)
                .withGateWayUserData(gateWayUserData)
                .withCoreUserData(coreUserData)
                .build();
        return buildStackOperation.execute();
    }

    @Override
    public Set<Resource> addInstances(Stack stack, String gateWayUserData, String coreUserData, Integer instanceCount, String instanceGroup) {
        AddInstancesOperation addInstancesOperation = buildAzureOperation(new AddInstancesOperation.Builder(), stack)
                .withGateWayUserData(gateWayUserData)
                .withCoreUserData(coreUserData)
                .withInstanceCount(instanceCount)
                .withInstanceGroup(instanceGroup)
                .build();
        return addInstancesOperation.execute();
    }

    @Override
    public Set<String> removeInstances(Stack stack, Set<String> origInstanceIds, String instanceGroup) {
        RemoveInstancesOperation removeInstancesOperation = buildAzureOperation(new RemoveInstancesOperation.Builder(), stack)
                .withOrigInstanceIds(origInstanceIds).build();
        return removeInstancesOperation.execute();
    }

    @Override
    public void deleteStack(final Stack stack, Credential credential) {
        DeleteStackOperation deleteStackOperation = buildAzureOperation(new DeleteStackOperation.DeleteStackOperationBuilder(), stack).build();
        deleteStackOperation.execute();
    }

    @Override
    public void rollback(final Stack stack, Set<Resource> resourceSet) {
        RollbackOperation rollbackOperation = buildAzureOperation(new RollbackOperation.Builder(), stack).build();
        rollbackOperation.execute();
    }

    @Override
    public boolean startAll(Stack stack) {
        StartStopOperation startOperation = buildAzureOperation(new StartStopOperation.Builder(), stack)
                .withStarted(true).build();
        return startOperation.execute();
    }

    @Override
    public boolean stopAll(Stack stack) {
        StartStopOperation stopOperation = buildAzureOperation(new StartStopOperation.Builder(), stack)
                .withStarted(false).build();
        return stopOperation.execute();
    }

    @Override
    public void updateAllowedSubnets(Stack stack, String gateWayUserData, String coreUserData) {
        UpdateAllowedSubnetsOperation updateOperation = buildAzureOperation(new UpdateAllowedSubnetsOperation.Builder(), stack).build();
        updateOperation.execute();
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
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.AZURE;
    }

    private <T extends AzureOperation.Builder> T buildAzureOperation(T builder, Stack stack) {
        builder.withCloudbreakEventService(cloudbreakEventService)
                .withLockMap(lockMap)
                .withCloudResourceManager(cloudResourceManager)
                .withAzureResourceBuilderInit(azureResourceBuilderInit)
                .withStack(stack)
                .withQueued(true);
        return builder;
    }
}
