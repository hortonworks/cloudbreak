package com.sequenceiq.cloudbreak.service.stack.connector.azure;

import static com.sequenceiq.cloudbreak.service.stack.connector.azure.AzureStackUtil.ERROR;
import static com.sequenceiq.cloudbreak.service.stack.connector.azure.AzureStackUtil.NAME;
import static com.sequenceiq.cloudbreak.service.stack.connector.azure.AzureStackUtil.NOT_FOUND;
import static com.sequenceiq.cloudbreak.service.stack.connector.azure.AzureStackUtil.SERVICENAME;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequenceiq.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.conf.ReactorConfig;
import com.sequenceiq.cloudbreak.controller.InternalServerException;
import com.sequenceiq.cloudbreak.controller.json.JsonHelper;
import com.sequenceiq.cloudbreak.domain.AzureCredential;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.DetailedAzureStackDescription;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.ResourceType;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.StackDescription;
import com.sequenceiq.cloudbreak.service.PollingService;
import com.sequenceiq.cloudbreak.service.cluster.flow.AmbariClusterConnector;
import com.sequenceiq.cloudbreak.service.credential.azure.AzureCertificateService;
import com.sequenceiq.cloudbreak.service.stack.connector.CloudPlatformConnector;
import com.sequenceiq.cloudbreak.service.stack.event.StackDeleteComplete;
import com.sequenceiq.cloudbreak.service.stack.flow.AzureInstanceStatusCheckerTask;
import com.sequenceiq.cloudbreak.service.stack.flow.AzureInstances;

import groovyx.net.http.HttpResponseDecorator;
import groovyx.net.http.HttpResponseException;
import reactor.core.Reactor;
import reactor.event.Event;

@Service
public class AzureConnector implements CloudPlatformConnector {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureConnector.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Autowired
    private JsonHelper jsonHelper;

    @Autowired
    private Reactor reactor;

    @Autowired
    private AzureStackUtil azureStackUtil;

    @Autowired
    private PollingService<AzureInstances> azurePollingService;

    @Override
    public StackDescription describeStackWithResources(Stack stack, Credential credential) {
        String filePath = AzureCertificateService.getUserJksFileName(credential, azureStackUtil.emailAsFolder(stack.getOwner()));
        AzureClient azureClient = azureStackUtil.createAzureClient(credential, filePath);
        DetailedAzureStackDescription detailedAzureStackDescription = new DetailedAzureStackDescription();
        try {
            Object affinityGroup = azureClient.getAffinityGroup(stack.getResourcesByType(ResourceType.AFFINITY_GROUP).get(0).getResourceName());
            detailedAzureStackDescription.setAffinityGroup(jsonHelper.createJsonFromString(affinityGroup.toString()));
        } catch (Exception ex) {
            detailedAzureStackDescription.setAffinityGroup(jsonHelper.createJsonFromString(String.format("{\"AffinityGroup\": {%s}}", ERROR)));
        }
        try {
            Object storageAccount = azureClient.getStorageAccount(stack.getResourcesByType(ResourceType.STORAGE).get(0).getResourceName());
            detailedAzureStackDescription.setStorageAccount(jsonHelper.createJsonFromString(storageAccount.toString()));
        } catch (Exception ex) {
            detailedAzureStackDescription.setStorageAccount(jsonHelper.createJsonFromString(String.format("{\"StorageService\": {%s}}", ERROR)));
        }

        for (Resource resource : stack.getResourcesByType(ResourceType.VIRTUAL_MACHINE)) {
            try {
                Object cloudService = azureClient.getCloudService(resource.getResourceName());
                detailedAzureStackDescription.getCloudServices().add(jsonHelper.createJsonFromString(cloudService.toString()).toString());
            } catch (Exception ex) {
                detailedAzureStackDescription.getCloudServices().add(
                        jsonHelper.createJsonFromString(String.format("{\"HostedService\": {%s}}", ERROR)).toString());
            }
        }
        for (Resource resource : stack.getResourcesByType(ResourceType.CLOUD_SERVICE)) {
            Map<String, String> props = new HashMap<>();
            props.put(SERVICENAME, resource.getResourceName());
            props.put(NAME, resource.getResourceName());
            try {
                Object virtualMachine = azureClient.getVirtualMachine(props);
                detailedAzureStackDescription.getVirtualMachines().add(jsonHelper.createJsonFromString(virtualMachine.toString()).toString());
            } catch (Exception ex) {
                detailedAzureStackDescription.getVirtualMachines().add(
                        jsonHelper.createJsonFromString(String.format("{\"Deployment\": {%s}}", ERROR)).toString());
            }
        }
        return detailedAzureStackDescription;
    }

    public void rollback(Stack stack, Credential credential, Set<Resource> resourceSet) {
        String filePath = AzureCertificateService.getUserJksFileName(credential, azureStackUtil.emailAsFolder(stack.getOwner()));
        AzureClient azureClient = azureStackUtil.createAzureClient(credential, filePath);
        deleteVirtualMachines(stack, azureClient);
        deleteCloudServices(stack, (AzureCredential) credential, azureClient);
        deleteNetwork(stack, azureClient);
        deleteBlobs(stack, azureClient);
    }

    @Override
    public void deleteStack(Stack stack, Credential credential) {
        String filePath = AzureCertificateService.getUserJksFileName(credential, azureStackUtil.emailAsFolder(stack.getOwner()));
        AzureClient azureClient = azureStackUtil.createAzureClient(credential, filePath);
        deleteVirtualMachines(stack, azureClient);
        deleteCloudServices(stack, (AzureCredential) credential, azureClient);
        deleteNetwork(stack, azureClient);
        deleteBlobs(stack, azureClient);
        reactor.notify(ReactorConfig.DELETE_COMPLETE_EVENT, Event.wrap(new StackDeleteComplete(stack.getId())));
    }

    private void deleteBlobs(Stack stack, AzureClient azureClient) {
        Map<String, String> props;
        for (Resource resource : stack.getResourcesByType(ResourceType.BLOB)) {
            try {
                JsonNode actualObj = MAPPER.readValue((String) azureClient.getDisks(), JsonNode.class);
                List<String> disks = (List<String>) actualObj.get("Disks").findValues("Disk").get(0).findValuesAsText("Name");
                for (String jsonNode : disks) {
                    if (jsonNode.startsWith(String.format("%s-%s", resource.getResourceName(), resource.getResourceName()))) {
                        props = new HashMap<>();
                        props.put(NAME, jsonNode);
                        HttpResponseDecorator deleteDisk = (HttpResponseDecorator) azureClient.deleteDisk(props);
                        String requestId = (String) azureClient.getRequestId(deleteDisk);
                        azureClient.waitUntilComplete(requestId);
                        break;
                    }
                }
            } catch (HttpResponseException ex) {
                httpResponseExceptionHandler(ex, resource.getResourceName(), stack.getOwner());
            } catch (Exception ex) {
                throw new InternalServerException(ex.getMessage());
            }
        }
    }

    private void deleteNetwork(Stack stack, AzureClient azureClient) {
        for (Resource resource : stack.getResourcesByType(ResourceType.NETWORK)) {
            Map<String, String> props;
            try {
                props = new HashMap<>();
                props.put(NAME, resource.getResourceName());
                HttpResponseDecorator deleteVirtualNetworkResult = (HttpResponseDecorator) azureClient.deleteVirtualNetwork(props);
                String requestId = (String) azureClient.getRequestId(deleteVirtualNetworkResult);
                azureClient.waitUntilComplete(requestId);
            } catch (HttpResponseException ex) {
                httpResponseExceptionHandler(ex, resource.getResourceName(), stack.getOwner());
            } catch (Exception ex) {
                throw new InternalServerException(ex.getMessage());
            }
        }
    }

    private void deleteCloudServices(Stack stack, AzureCredential credential, AzureClient azureClient) {
        for (Resource resource : stack.getResourcesByType(ResourceType.CLOUD_SERVICE)) {
            try {
                deleteCloudService(azureClient, credential.getName().replaceAll("\\s+", ""), resource.getResourceName());
            } catch (HttpResponseException ex) {
                httpResponseExceptionHandler(ex, resource.getResourceName(), stack.getOwner());
            } catch (Exception ex) {
                throw new InternalServerException(ex.getMessage());
            }
        }
    }

    public void deleteCloudService(AzureClient azureClient, String serviceName, String name) throws Exception {
        Map<String, String> props = new HashMap<>();
        props.put(SERVICENAME, serviceName);
        props.put(NAME, name);
        HttpResponseDecorator deleteCloudServiceResult = (HttpResponseDecorator) azureClient.deleteCloudService(props);
        String requestId = (String) azureClient.getRequestId(deleteCloudServiceResult);
        azureClient.waitUntilComplete(requestId);
    }

    private void deleteVirtualMachines(Stack stack, AzureClient azureClient) {
        for (Resource resource : stack.getResourcesByType(ResourceType.VIRTUAL_MACHINE)) {
            try {
                deleteVirtualMachine(azureClient, resource.getResourceName(), resource.getResourceName());
            } catch (HttpResponseException ex) {
                httpResponseExceptionHandler(ex, resource.getResourceName(), stack.getOwner());
            } catch (Exception ex) {
                throw new InternalServerException(ex.getMessage());
            }
        }
    }

    public void deleteVirtualMachine(AzureClient azureClient, String serviceName, String name) throws Exception {
        Map<String, String> props = new HashMap<>();
        props.put(SERVICENAME, serviceName);
        props.put(NAME, name);
        HttpResponseDecorator deleteVirtualMachineResult = (HttpResponseDecorator) azureClient.deleteVirtualMachine(props);
        String requestId = (String) azureClient.getRequestId(deleteVirtualMachineResult);
        azureClient.waitUntilComplete(requestId);
    }

    private void httpResponseExceptionHandler(HttpResponseException ex, String resourceName, String user) {
        if (ex.getStatusCode() != NOT_FOUND) {
            throw new InternalServerException(ex.getMessage());
        } else {
            LOGGER.error(String.format("Azure resource not found with %s name for %s user.", resourceName, user));
        }
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.AZURE;
    }

    @Override
    public boolean startAll(Stack stack) {
        AzureClient azureClient = createAzureClient(stack);
        boolean started = setStackState(stack, azureClient, false);
        if (started) {
            List<Resource> resources = stack.getResourcesByType(ResourceType.VIRTUAL_MACHINE);
            List<String> instanceNames = new ArrayList<>(resources.size());
            for (Resource resource : resources) {
                instanceNames.add(resource.getResourceName());
            }
            azurePollingService.pollWithTimeout(
                    new AzureInstanceStatusCheckerTask(),
                    new AzureInstances(stack.getId(), azureClient, instanceNames, "Running"),
                    AmbariClusterConnector.POLLING_INTERVAL,
                    AmbariClusterConnector.MAX_ATTEMPTS_FOR_AMBARI_OPS);
            return true;
        }
        return false;
    }

    @Override
    public boolean stopAll(Stack stack) {
        return setStackState(stack, createAzureClient(stack), true);
    }

    private AzureClient createAzureClient(Stack stack) {
        Credential credential = stack.getCredential();
        String filePath = AzureCertificateService.getUserJksFileName(credential, azureStackUtil.emailAsFolder(stack.getOwner()));
        return azureStackUtil.createAzureClient(credential, filePath);
    }

    private boolean setStackState(Stack stack, AzureClient azureClient, boolean stopped) {
        boolean result = true;
        try {
            for (Resource resource : stack.getResourcesByType(ResourceType.VIRTUAL_MACHINE)) {
                Map<String, String> vmContext = azureStackUtil.createVMContext(resource.getResourceName());
                if (stopped) {
                    azureClient.stopVirtualMachine(vmContext);
                } else {
                    azureClient.startVirtualMachine(vmContext);
                }
            }
        } catch (Exception e) {
            LOGGER.error(String.format("Failed to %s AZURE instances on stack: %s", stopped ? "stop" : "start", stack.getId()));
            result = false;
        }
        return result;
    }
}
