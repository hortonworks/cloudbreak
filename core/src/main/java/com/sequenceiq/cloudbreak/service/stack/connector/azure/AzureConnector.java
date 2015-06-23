package com.sequenceiq.cloudbreak.service.stack.connector.azure;

import static com.sequenceiq.cloudbreak.domain.ResourceType.AZURE_VIRTUAL_MACHINE;
import static com.sequenceiq.cloudbreak.service.stack.connector.azure.AzureStackUtil.NAME;
import static com.sequenceiq.cloudbreak.service.stack.connector.azure.AzureStackUtil.SERVICENAME;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequenceiq.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.EnvironmentVariableConfig;
import com.sequenceiq.cloudbreak.core.flow.FlowCancelledException;
import com.sequenceiq.cloudbreak.domain.AzureCredential;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.ResourceRepository;
import com.sequenceiq.cloudbreak.service.PollingResult;
import com.sequenceiq.cloudbreak.service.PollingService;
import com.sequenceiq.cloudbreak.service.events.CloudbreakEventService;
import com.sequenceiq.cloudbreak.service.stack.connector.CloudPlatformConnector;
import com.sequenceiq.cloudbreak.service.stack.connector.ParallelCloudResourceManager;
import com.sequenceiq.cloudbreak.service.stack.resource.azure.builders.AzureResourceBuilderInit;

@Service
public class AzureConnector implements CloudPlatformConnector {
    protected static final int POLLING_INTERVAL = 8000;
    private static final Logger LOGGER = LoggerFactory.getLogger(AzureConnector.class);
    private static final int AZURE_THUMBPRINT_POLLING_ATTEMPTS = 120;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Inject
    private AzureResourceBuilderInit azureResourceBuilderInit;

    @Inject
    private ParallelCloudResourceManager cloudResourceManager;

    @Inject
    private CloudbreakEventService cloudbreakEventService;

    @Inject
    private ResourceRepository resourceRepository;

    @Inject
    private AzureStackUtil azureStackUtil;

    @Inject
    private PollingService<AzureThumbprintCheckerContext> azureThumbprintCheckerContextPollingService;

    @Inject
    private AzureThumbprintChecker azureThumbprintChecker;

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
    public void startAll(Stack stack) {
        StartStopOperation startOperation = buildAzureOperation(new StartStopOperation.Builder(), stack)
                .withStarted(true).build();
        startOperation.execute();
    }

    @Override
    public void stopAll(Stack stack) {
        StartStopOperation stopOperation = buildAzureOperation(new StartStopOperation.Builder(), stack)
                .withStarted(false).build();
        stopOperation.execute();
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
        String result = "";
        try {
            Resource resource = resourceRepository.findByStackIdAndNameAndType(stack.getId(), gateway, AZURE_VIRTUAL_MACHINE);
            AzureCredential credential = (AzureCredential) stack.getCredential();
            AzureClient azureClient = azureStackUtil.createAzureClient(credential);
            Map<String, String> props = new HashMap<>();
            props.put(SERVICENAME, resource.getResourceName());
            props.put(NAME, resource.getResourceName());

            AzureThumbprintCheckerContext azureThumbprintCheckerContext = new AzureThumbprintCheckerContext(stack, MAPPER, resource, props);
            PollingResult pollingResult = azureThumbprintCheckerContextPollingService
                    .pollWithTimeout(azureThumbprintChecker, azureThumbprintCheckerContext, POLLING_INTERVAL, AZURE_THUMBPRINT_POLLING_ATTEMPTS);
            if (PollingResult.isExited(pollingResult)) {
                throw new FlowCancelledException("Operation cancelled.");
            } else if (PollingResult.isTimeout(pollingResult)) {
                throw new AzureResourceException("Operation timed out: Couldn't get thumbrpint from azure on gateway instance.");
            }

            Object virtualMachine = azureClient.getVirtualMachine(props);
            JsonNode actualObj = MAPPER.readValue((String) virtualMachine, JsonNode.class);
            String tmpFingerPrint = actualObj.get("Deployment").get("RoleInstanceList").get("RoleInstance").get("RemoteAccessCertificateThumbprint").asText();
            result = formatFingerprint(tmpFingerPrint, ":", 2);
            result = result.substring(0, result.length() - 1);
        } catch (Exception ex) {
            throw new AzureResourceException("Couldn't parse SSH fingerprint.");
        }
        return result;
    }

    private String formatFingerprint(String text, String insert, int period) {
        Pattern p = Pattern.compile("(.{" + period + "})", Pattern.DOTALL);
        Matcher m = p.matcher(text);
        return m.replaceAll("$1" + insert);
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
