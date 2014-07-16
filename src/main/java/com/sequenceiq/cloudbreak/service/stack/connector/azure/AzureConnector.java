package com.sequenceiq.cloudbreak.service.stack.connector.azure;

import static com.sequenceiq.cloudbreak.service.stack.connector.azure.AzureStackUtil.ERROR;
import static com.sequenceiq.cloudbreak.service.stack.connector.azure.AzureStackUtil.NAME;
import static com.sequenceiq.cloudbreak.service.stack.connector.azure.AzureStackUtil.NOT_FOUND;
import static com.sequenceiq.cloudbreak.service.stack.connector.azure.AzureStackUtil.SERVICENAME;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.controller.InternalServerException;
import com.sequenceiq.cloudbreak.controller.json.JsonHelper;
import com.sequenceiq.cloudbreak.domain.AzureCredential;
import com.sequenceiq.cloudbreak.domain.AzureStackDescription;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.DetailedAzureStackDescription;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.ResourceType;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.StackDescription;
import com.sequenceiq.cloudbreak.domain.User;
import com.sequenceiq.cloudbreak.service.credential.azure.AzureCertificateService;
import com.sequenceiq.cloudbreak.service.stack.connector.CloudPlatformConnector;

import groovyx.net.http.HttpResponseDecorator;
import groovyx.net.http.HttpResponseException;

@Service
public class AzureConnector implements CloudPlatformConnector {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureConnector.class);

    @Autowired
    private JsonHelper jsonHelper;

    @Autowired
    private AzureStackUtil azureStackUtil;

    @Override
    public StackDescription describeStack(User user, Stack stack, Credential credential) {
        String filePath = AzureCertificateService.getUserJksFileName(credential, user.emailAsFolder());
        AzureClient azureClient = azureStackUtil.createAzureClient(credential, filePath);
        AzureStackDescription azureStackDescription = new AzureStackDescription();
        for (Resource resource : stack.getResourcesByType(ResourceType.CLOUD_SERVICE)) {
            try {
                Object cloudService = azureClient.getCloudService(resource.getResourceName());
                azureStackDescription.getCloudServices().add(jsonHelper.createJsonFromString(cloudService.toString()).toString());
            } catch (Exception ex) {
                azureStackDescription.getCloudServices().add(jsonHelper.createJsonFromString(String.format("{\"HostedService\": {%s}}", ERROR)).toString());
            }
        }
        for (Resource resource : stack.getResourcesByType(ResourceType.VIRTUAL_MACHINE)) {
            Map<String, String> props = new HashMap<>();
            props.put(SERVICENAME, resource.getResourceName());
            props.put(NAME, resource.getResourceName());
            try {
                Object virtualMachine = azureClient.getVirtualMachine(props);
                azureStackDescription.getVirtualMachines().add(jsonHelper.createJsonFromString(virtualMachine.toString()).toString());
            } catch (Exception ex) {
                azureStackDescription.getVirtualMachines().add(jsonHelper.createJsonFromString(String.format("{\"Deployment\": {%s}}", ERROR)).toString());
            }
        }
        return azureStackDescription;
    }

    @Override
    public StackDescription describeStackWithResources(User user, Stack stack, Credential credential) {
        String filePath = AzureCertificateService.getUserJksFileName(credential, user.emailAsFolder());
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

    @Override
    public void deleteStack(User user, Stack stack, Credential credential) {
        String filePath = AzureCertificateService.getUserJksFileName(credential, user.emailAsFolder());
        AzureClient azureClient = azureStackUtil.createAzureClient(credential, filePath);
        for (Resource resource : stack.getResourcesByType(ResourceType.VIRTUAL_MACHINE)) {
            Map<String, String> props;
            try {
                props = new HashMap<>();
                props.put(SERVICENAME, resource.getResourceName());
                props.put(NAME, resource.getResourceName());
                HttpResponseDecorator deleteVirtualMachineResult = (HttpResponseDecorator) azureClient.deleteVirtualMachine(props);
                String requestId = (String) azureClient.getRequestId(deleteVirtualMachineResult);
                azureClient.waitUntilComplete(requestId);

            } catch (HttpResponseException ex) {
                httpResponseExceptionHandler(ex, resource.getResourceName(), user.getId());
            } catch (Exception ex) {
                throw new InternalServerException(ex.getMessage());
            }
        }
        for (Resource resource : stack.getResourcesByType(ResourceType.CLOUD_SERVICE)) {
            Map<String, String> props;
            try {
                props = new HashMap<>();
                props.put(SERVICENAME, ((AzureCredential) credential).getName().replaceAll("\\s+", ""));
                props.put(NAME, resource.getResourceName());
                HttpResponseDecorator deleteCloudServiceResult = (HttpResponseDecorator) azureClient.deleteCloudService(props);
                String requestId = (String) azureClient.getRequestId(deleteCloudServiceResult);
                azureClient.waitUntilComplete(requestId);
            } catch (HttpResponseException ex) {
                httpResponseExceptionHandler(ex, resource.getResourceName(), user.getId());
            } catch (Exception ex) {
                throw new InternalServerException(ex.getMessage());
            }
        }
        for (Resource resource : stack.getResourcesByType(ResourceType.NETWORK)) {
            Map<String, String> props;
            try {
                props = new HashMap<>();
                props.put(NAME, resource.getResourceName());
                HttpResponseDecorator deleteVirtualNetworkResult = (HttpResponseDecorator) azureClient.deleteVirtualNetwork(props);
                String requestId = (String) azureClient.getRequestId(deleteVirtualNetworkResult);
                azureClient.waitUntilComplete(requestId);
            } catch (HttpResponseException ex) {
                httpResponseExceptionHandler(ex, resource.getResourceName(), user.getId());
            } catch (Exception ex) {
                throw new InternalServerException(ex.getMessage());
            }
        }
    }

    private void httpResponseExceptionHandler(HttpResponseException ex, String resourceName, Long userId) {
        if (ex.getStatusCode() != NOT_FOUND) {
            throw new InternalServerException(ex.getMessage());
        } else {
            LOGGER.info(String.format("Azure resource not found with %s name for %s userId.", resourceName, userId));
        }
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.AZURE;
    }

    @Override
    public Boolean startAll(User user, Long stackId) {
        return Boolean.TRUE;
    }

    @Override
    public Boolean stopAll(User user, Long stackId) {
        return Boolean.TRUE;
    }
}
