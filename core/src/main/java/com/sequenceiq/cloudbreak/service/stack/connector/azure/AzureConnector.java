package com.sequenceiq.cloudbreak.service.stack.connector.azure;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.PlatformParameters;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.common.type.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.events.CloudbreakEventService;
import com.sequenceiq.cloudbreak.service.stack.connector.CloudPlatformConnector;
import com.sequenceiq.cloudbreak.service.stack.resource.azure.builders.AzureResourceBuilderInit;

@Service
// TODO Have to be removed when the termination of the old version of azure clusters won't be supported anymore
public class AzureConnector implements CloudPlatformConnector {
    protected static final int POLLING_INTERVAL = 8000;

    @Inject
    private AzureResourceBuilderInit azureResourceBuilderInit;

    @Inject
    private ParallelCloudResourceManager cloudResourceManager;

    @Inject
    private CloudbreakEventService cloudbreakEventService;

    private Map<String, Lock> lockMap = Collections.synchronizedMap(new HashMap<String, Lock>());

    @Override
    public Set<Resource> buildStack(final Stack stack, final Map<String, Object> setupProperties) {
        throw new UnsupportedOperationException(String.format(AzureStackUtil.UNSUPPORTED_OPERATION, "buildStack"));
    }

    @Override
    public Set<Resource> addInstances(Stack stack, Integer instanceCount, String instanceGroup) {
        throw new UnsupportedOperationException(String.format(AzureStackUtil.UNSUPPORTED_OPERATION, "addInstances"));
    }

    @Override
    public Set<String> removeInstances(Stack stack, Set<String> origInstanceIds, String instanceGroup) {
        throw new UnsupportedOperationException(String.format(AzureStackUtil.UNSUPPORTED_OPERATION, "removeInstances"));
    }

    @Override
    public void deleteStack(final Stack stack, Credential credential) {
        DeleteStackOperation deleteStackOperation = buildAzureOperation(new DeleteStackOperation.DeleteStackOperationBuilder(), stack).build();
        deleteStackOperation.execute();
    }

    @Override
    public void rollback(final Stack stack, Set<Resource> resourceSet) {
        throw new UnsupportedOperationException(String.format(AzureStackUtil.UNSUPPORTED_OPERATION, "rollback"));
    }

    @Override
    public void startAll(Stack stack) {
        throw new UnsupportedOperationException(String.format(AzureStackUtil.UNSUPPORTED_OPERATION, "startAll"));
    }

    @Override
    public void stopAll(Stack stack) {
        throw new UnsupportedOperationException(String.format(AzureStackUtil.UNSUPPORTED_OPERATION, "stopAll"));
    }

    @Override
    public void updateAllowedSubnets(Stack stack) {
        throw new UnsupportedOperationException(String.format(AzureStackUtil.UNSUPPORTED_OPERATION, "updateAllowedSubnets"));
    }

    @Override
    public Set<String> getSSHFingerprints(Stack stack, String gateway) {
        throw new UnsupportedOperationException(String.format(AzureStackUtil.UNSUPPORTED_OPERATION, "getSSHFingerprints"));
    }

    @Override
    public PlatformParameters getPlatformParameters(Stack stack) {
        throw new UnsupportedOperationException(String.format(AzureStackUtil.UNSUPPORTED_OPERATION, "getPlatformParameters"));
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.AZURE;
    }

    @Override
    public Variant checkAndGetPlatformVariant(Stack stack) {
        return Variant.variant(getCloudPlatform().name());
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
