package com.sequenceiq.cloudbreak.service.stack.connector.gcp;

import static com.sequenceiq.cloudbreak.domain.ResourceType.GCP_INSTANCE;

import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.api.services.compute.Compute;
import com.sequenceiq.cloudbreak.EnvironmentVariableConfig;
import com.sequenceiq.cloudbreak.core.flow.FlowCancelledException;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.CloudRegion;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.GcpCredential;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.ResourceRepository;
import com.sequenceiq.cloudbreak.service.PollingResult;
import com.sequenceiq.cloudbreak.service.PollingService;
import com.sequenceiq.cloudbreak.service.stack.connector.CloudPlatformConnector;
import com.sequenceiq.cloudbreak.service.stack.connector.ParallelCloudResourceManager;
import com.sequenceiq.cloudbreak.service.stack.flow.FingerprintParserUtil;
import com.sequenceiq.cloudbreak.service.stack.resource.gcp.builders.GcpResourceBuilderInit;

@Service
public class GcpConnector implements CloudPlatformConnector {
    protected static final int POLLING_INTERVAL = 5000;
    private static final Logger LOGGER = LoggerFactory.getLogger(GcpConnector.class);
    private static final int CONSOLE_OUTPUT_POLLING_ATTEMPTS = 120;

    @Inject
    private GcpResourceBuilderInit gcpResourceBuilderInit;

    @Inject
    private ParallelCloudResourceManager cloudResourceManager;

    @Inject
    private ResourceRepository resourceRepository;

    @Inject
    private GcpStackUtil gcpStackUtil;

    @Inject
    private PollingService<GcpConsoleOutputContext> consoleOutputPollingService;

    @Inject
    private GcpConsoleOutputCheckerTask consoleOutputCheckerTask;

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
    public void startAll(Stack stack) {
        cloudResourceManager.startStopResources(stack, true, gcpResourceBuilderInit);
    }

    @Override
    public void stopAll(Stack stack) {
        cloudResourceManager.startStopResources(stack, false, gcpResourceBuilderInit);
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
    public Set<String> getSSHFingerprints(Stack stack, String gateway) {
        try {
            GcpCredential credential = (GcpCredential) stack.getCredential();
            Compute compute = gcpStackUtil.buildCompute(credential, stack);
            Resource instance = resourceRepository.findByStackIdAndNameAndType(stack.getId(), gateway, GCP_INSTANCE);
            Compute.Instances.GetSerialPortOutput instanceGet = compute.instances()
                    .getSerialPortOutput(credential.getProjectId(),
                            CloudRegion.valueOf(stack.getRegion()).value(), instance.getResourceName());
            GcpConsoleOutputContext gcpConsoleOutputContext = new GcpConsoleOutputContext(stack, instanceGet);
            PollingResult pollingResult = consoleOutputPollingService
                    .pollWithTimeout(consoleOutputCheckerTask, gcpConsoleOutputContext, POLLING_INTERVAL, CONSOLE_OUTPUT_POLLING_ATTEMPTS);
            if (PollingResult.isExited(pollingResult)) {
                throw new FlowCancelledException("Operation cancelled.");
            } else if (PollingResult.isTimeout(pollingResult)) {
                throw new GcpResourceException("Operation timed out: Couldn't get console output of gateway instance.");
            }
            String consoleOutput = instanceGet.execute().getContents();
            Set<String> result = FingerprintParserUtil.parseFingerprints(consoleOutput);
            if (result.isEmpty()) {
                throw new GcpResourceException("Couldn't parse SSH fingerprint from console output.");
            }
            return result;
        } catch (Exception e) {
            throw new GcpResourceException("Couldn't parse SSH fingerprint from console output.", e);
        }
    }

}
