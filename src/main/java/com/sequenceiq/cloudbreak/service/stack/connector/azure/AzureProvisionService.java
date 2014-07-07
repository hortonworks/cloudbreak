package com.sequenceiq.cloudbreak.service.stack.connector.azure;

import static com.sequenceiq.cloudbreak.service.stack.connector.azure.AzureStackUtil.ERROR;
import static com.sequenceiq.cloudbreak.service.stack.connector.azure.AzureStackUtil.NAME;
import static com.sequenceiq.cloudbreak.service.stack.connector.azure.AzureStackUtil.NOT_FOUND;
import static com.sequenceiq.cloudbreak.service.stack.connector.azure.AzureStackUtil.SERVICENAME;
import static com.sequenceiq.cloudbreak.service.stack.connector.azure.AzureStackUtil.getVmName;

import java.io.File;
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
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.StackDescription;
import com.sequenceiq.cloudbreak.domain.User;
import com.sequenceiq.cloudbreak.service.credential.azure.AzureCertificateService;
import com.sequenceiq.cloudbreak.service.stack.connector.CloudPlatformConnector;

import groovyx.net.http.HttpResponseDecorator;
import groovyx.net.http.HttpResponseException;

@Service
public class AzureProvisionService implements CloudPlatformConnector {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureProvisionService.class);

    @Autowired
    private JsonHelper jsonHelper;

    @Override
    public StackDescription describeStack(User user, Stack stack, Credential credential) {
        String filePath = AzureCertificateService.getUserJksFileName(credential, user.emailAsFolder());
        File file = new File(filePath);
        AzureClient azureClient = new AzureClient(
                ((AzureCredential) credential).getSubscriptionId(), file.getAbsolutePath(), ((AzureCredential) credential).getJks()
                );
        AzureStackDescription azureStackDescription = new AzureStackDescription();
        for (int i = 0; i < stack.getNodeCount(); i++) {
            String vmName = getVmName(stack.getName(), i);
            Map<String, String> props = new HashMap<>();
            props.put(SERVICENAME, vmName);
            props.put(NAME, vmName);
            try {
                Object cloudService = azureClient.getCloudService(vmName);
                azureStackDescription.getCloudServices().add(jsonHelper.createJsonFromString(cloudService.toString()).toString());
            } catch (Exception ex) {
                azureStackDescription.getCloudServices().add(jsonHelper.createJsonFromString(String.format("{\"HostedService\": {%s}}", ERROR)).toString());
            }
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
        File file = new File(filePath);
        AzureClient azureClient = new AzureClient(
                ((AzureCredential) credential).getSubscriptionId(),
                file.getAbsolutePath(),
                ((AzureCredential) credential).getJks()
                );

        DetailedAzureStackDescription detailedAzureStackDescription = new DetailedAzureStackDescription();
        String templateName = stack.getName();
        try {
            Object affinityGroup = azureClient.getAffinityGroup(templateName);
            detailedAzureStackDescription.setAffinityGroup(jsonHelper.createJsonFromString(affinityGroup.toString()));
        } catch (Exception ex) {
            detailedAzureStackDescription.setAffinityGroup(jsonHelper.createJsonFromString(String.format("{\"AffinityGroup\": {%s}}", ERROR)));
        }
        try {
            Object storageAccount = azureClient.getStorageAccount(templateName);
            detailedAzureStackDescription.setStorageAccount(jsonHelper.createJsonFromString(storageAccount.toString()));
        } catch (Exception ex) {
            detailedAzureStackDescription.setStorageAccount(jsonHelper.createJsonFromString(String.format("{\"StorageService\": {%s}}", ERROR)));
        }

        for (int i = 0; i < stack.getNodeCount(); i++) {
            String vmName = getVmName(templateName, i);
            Map<String, String> props = new HashMap<>();
            props.put(SERVICENAME, templateName);
            props.put(NAME, vmName);
            try {
                Object cloudService = azureClient.getCloudService(vmName);
                detailedAzureStackDescription.getCloudServices().add(jsonHelper.createJsonFromString(cloudService.toString()).toString());
            } catch (Exception ex) {
                detailedAzureStackDescription.getCloudServices()
                        .add(jsonHelper.createJsonFromString(String.format("{\"HostedService\": {%s}}", ERROR)).toString());
            }
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
        File file = new File(filePath);
        AzureClient azureClient = new AzureClient(
                ((AzureCredential) credential).getSubscriptionId(), file.getAbsolutePath(), ((AzureCredential) credential).getJks()
                );
        for (int i = 0; i < stack.getNodeCount(); i++) {
            String vmName = getVmName(stack.getName(), i);
            Map<String, String> props;
            try {
                props = new HashMap<>();
                props.put(SERVICENAME, vmName);
                props.put(NAME, vmName);
                HttpResponseDecorator deleteVirtualMachineResult = (HttpResponseDecorator) azureClient.deleteVirtualMachine(props);
                String requestId = (String) azureClient.getRequestId(deleteVirtualMachineResult);
                azureClient.waitUntilComplete(requestId);

            } catch (HttpResponseException ex) {
                httpResponseExceptionHandler(ex, vmName, user.getId());
            } catch (Exception ex) {
                throw new InternalServerException(ex.getMessage());
            }
            try {
                props = new HashMap<>();
                props.put(SERVICENAME, ((AzureCredential) credential).getName().replaceAll("\\s+", ""));
                props.put(NAME, vmName);
                HttpResponseDecorator deleteCloudServiceResult = (HttpResponseDecorator) azureClient.deleteCloudService(props);
                String requestId = (String) azureClient.getRequestId(deleteCloudServiceResult);
                azureClient.waitUntilComplete(requestId);
            } catch (HttpResponseException ex) {
                httpResponseExceptionHandler(ex, vmName, user.getId());
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
