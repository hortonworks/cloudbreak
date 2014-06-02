package com.sequenceiq.provisioning.service.azure;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloud.azure.client.AzureClient;
import com.sequenceiq.provisioning.controller.InternalServerException;
import com.sequenceiq.provisioning.controller.json.JsonHelper;
import com.sequenceiq.provisioning.domain.AzureCredential;
import com.sequenceiq.provisioning.domain.AzureStackDescription;
import com.sequenceiq.provisioning.domain.AzureTemplate;
import com.sequenceiq.provisioning.domain.CloudPlatform;
import com.sequenceiq.provisioning.domain.Credential;
import com.sequenceiq.provisioning.domain.DetailedAzureStackDescription;
import com.sequenceiq.provisioning.domain.Stack;
import com.sequenceiq.provisioning.domain.StackDescription;
import com.sequenceiq.provisioning.domain.User;
import com.sequenceiq.provisioning.service.ProvisionService;

import groovyx.net.http.HttpResponseDecorator;
import groovyx.net.http.HttpResponseException;

@Service
public class AzureProvisionService implements ProvisionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureProvisionService.class);

    private static final String OK_STATUS = "ok";
    private static final String LOCATION = "location";
    private static final String NAME = "name";
    private static final String DESCRIPTION = "description";
    private static final String AFFINITYGROUP = "affinityGroup";
    private static final String ADDRESSPREFIX = "addressPrefix";
    private static final String SUBNETADDRESSPREFIX = "subnetAddressPrefix";
    private static final String DEPLOYMENTSLOT = "deploymentSlot";
    private static final String LABEL = "label";
    private static final String IMAGENAME = "imageName";
    private static final String IMAGESTOREURI = "imageStoreUri";
    private static final String HOSTNAME = "hostname";
    private static final String USERNAME = "username";
    private static final String PASSWORD = "password";
    private static final String SUBNETNAME = "subnetName";
    private static final String VIRTUALNETWORKNAME = "virtualNetworkName";
    private static final String VMTYPE = "vmType";
    private static final String DATADIR = "userdatas";
    private static final String SSHPUBLICKEYFINGERPRINT = "sshPublicKeyFingerprint";
    private static final String SSHPUBLICKEYPATH = "sshPublicKeyPath";
    private static final String SERVICENAME = "serviceName";
    private static final String ERROR = "\"error\":\"Could not fetch data from azure\"";
    private static final int NOT_FOUND = 404;

    @Autowired
    private JsonHelper jsonHelper;

    @Override
    @Async
    public void createStack(User user, Stack stack, Credential credential) {

        AzureTemplate azureTemplate = (AzureTemplate) stack.getTemplate();

        String filePath = AzureCredentialService.getUserJksFileName(credential, user.emailAsFolder());
        File file = new File(filePath);
        AzureClient azureClient = new AzureClient(
                ((AzureCredential) credential).getSubscriptionId(),
                file.getAbsolutePath(),
                ((AzureCredential) credential).getJks()
                );
        Map<String, String> props = new HashMap<>();
        props.put(NAME, azureTemplate.getName());
        props.put(LOCATION, AzureLocation.valueOf(azureTemplate.getLocation()).location());
        props.put(DESCRIPTION, azureTemplate.getDescription());
        HttpResponseDecorator affinityResponse = (HttpResponseDecorator) azureClient.createAffinityGroup(props);
        String requestId = (String) azureClient.getRequestId(affinityResponse);
        azureClient.waitUntilComplete(requestId);

        props = new HashMap<>();
        props.put(NAME, azureTemplate.getName());
        props.put(DESCRIPTION, azureTemplate.getDescription());
        props.put(AFFINITYGROUP, azureTemplate.getName());
        HttpResponseDecorator storageResponse = (HttpResponseDecorator) azureClient.createStorageAccount(props);
        requestId = (String) azureClient.getRequestId(storageResponse);
        azureClient.waitUntilComplete(requestId);

        props = new HashMap<>();
        props.put(NAME, azureTemplate.getName());
        props.put(AFFINITYGROUP, azureTemplate.getName());
        props.put(SUBNETNAME, azureTemplate.getSubnetAddressPrefix());
        props.put(ADDRESSPREFIX, azureTemplate.getAddressPrefix());
        props.put(SUBNETADDRESSPREFIX, azureTemplate.getSubnetAddressPrefix());
        HttpResponseDecorator virtualNetworkResponse = (HttpResponseDecorator) azureClient.createVirtualNetwork(props);
        requestId = (String) azureClient.getRequestId(virtualNetworkResponse);
        azureClient.waitUntilComplete(requestId);

        for (int i = 0; i < stack.getNodeCount(); i++) {
            String vmName = getVmName(azureTemplate.getName(), i);
            props = new HashMap<>();
            props.put(NAME, vmName);
            props.put(DESCRIPTION, azureTemplate.getDescription());
            props.put(AFFINITYGROUP, azureTemplate.getName());
            HttpResponseDecorator cloudServiceResponse = (HttpResponseDecorator) azureClient.createCloudService(props);
            requestId = (String) azureClient.getRequestId(cloudServiceResponse);
            azureClient.waitUntilComplete(requestId);
            byte[] encoded = Base64.encodeBase64(vmName.getBytes());
            String label = new String(encoded);
            props = new HashMap<>();
            props.put(NAME, vmName);
            props.put(DEPLOYMENTSLOT, azureTemplate.getDeploymentSlot());
            props.put(LABEL, label);
            props.put(IMAGENAME, azureTemplate.getImageName());
            props.put(IMAGESTOREURI,
                    String.format("http://%s.blob.core.windows.net/vhd-store/%s.vhd", azureTemplate.getName(), vmName)
                    );
            props.put(HOSTNAME, vmName);
            props.put(USERNAME, azureTemplate.getUserName());
            if (azureTemplate.getPassword() != null) {
                props.put(PASSWORD, azureTemplate.getPassword());
            } else {
                props.put(SSHPUBLICKEYFINGERPRINT, azureTemplate.getSshPublicKeyFingerprint());
                props.put(SSHPUBLICKEYPATH, azureTemplate.getSshPublicKeyPath());
            }

            props.put(SUBNETNAME, azureTemplate.getName());
            props.put(VIRTUALNETWORKNAME, azureTemplate.getName());
            props.put(VMTYPE, AzureVmType.valueOf(azureTemplate.getVmType()).vmType());
            HttpResponseDecorator virtualMachineResponse = (HttpResponseDecorator) azureClient.createVirtualMachine(props);
            requestId = (String) azureClient.getRequestId(virtualMachineResponse);
            azureClient.waitUntilComplete(requestId);
        }
    }

    private String getVmName(String azureTemplate, int i) {
        return String.format("%s-%s", azureTemplate, i);
    }

    @Override
    public StackDescription describeStack(User user, Stack stack, Credential credential) {
        String filePath = AzureCredentialService.getUserJksFileName(credential, user.emailAsFolder());
        File file = new File(filePath);
        AzureClient azureClient = new AzureClient(
                ((AzureCredential) credential).getSubscriptionId(),
                file.getAbsolutePath(),
                ((AzureCredential) credential).getJks()
                );

        AzureStackDescription azureStackDescription = new AzureStackDescription();
        String templateName = ((AzureTemplate) stack.getTemplate()).getName();
        try {
            Object cloudService = azureClient.getCloudService(templateName);
            azureStackDescription.setCloudService(jsonHelper.createJsonFromString(cloudService.toString()));
        } catch (Exception ex) {
            azureStackDescription.setCloudService(jsonHelper.createJsonFromString(String.format("{\"HostedService\": {%s}}", ERROR)));
        }
        for (int i = 0; i < stack.getNodeCount(); i++) {
            String vmName = getVmName(templateName, i);
            Map<String, String> props = new HashMap<>();
            props.put(SERVICENAME, templateName);
            props.put(NAME, vmName);
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
        String filePath = AzureCredentialService.getUserJksFileName(credential, user.emailAsFolder());
        File file = new File(filePath);
        AzureClient azureClient = new AzureClient(
                ((AzureCredential) credential).getSubscriptionId(),
                file.getAbsolutePath(),
                ((AzureCredential) credential).getJks()
                );

        DetailedAzureStackDescription detailedAzureStackDescription = new DetailedAzureStackDescription();
        String templateName = ((AzureTemplate) stack.getTemplate()).getName();
        try {
            Object affinityGroup = azureClient.getAffinityGroup(templateName);
            detailedAzureStackDescription.setAffinityGroup(jsonHelper.createJsonFromString(affinityGroup.toString()));
        } catch (Exception ex) {
            detailedAzureStackDescription.setAffinityGroup(jsonHelper.createJsonFromString(String.format("{\"AffinityGroup\": {%s}}", ERROR)));
        }
        try {
            Object cloudService = azureClient.getCloudService(templateName);
            detailedAzureStackDescription.setCloudService(jsonHelper.createJsonFromString(cloudService.toString()).toString());
        } catch (Exception ex) {
            detailedAzureStackDescription.setCloudService(jsonHelper.createJsonFromString(String.format("{\"HostedService\": {%s}}", ERROR)).toString());
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
        String filePath = AzureCredentialService.getUserJksFileName(credential, user.emailAsFolder());
        File file = new File(filePath);
        AzureClient azureClient = new AzureClient(
                ((AzureCredential) credential).getSubscriptionId(),
                file.getAbsolutePath(),
                ((AzureCredential) credential).getJks()
                );
        String templateName = ((AzureTemplate) stack.getTemplate()).getName();
        for (int i = 0; i < stack.getNodeCount(); i++) {
            String vmName = getVmName(templateName, i);
            Map<String, String> props = new HashMap<>();
            props.put(SERVICENAME, templateName);
            props.put(NAME, vmName);
            try {
                Object deleteVirtualMachineResult = azureClient.deleteVirtualMachine(props);
            } catch (HttpResponseException ex) {
                httpResponseExceptionHandler(ex, vmName, user.getId());
            } catch (Exception ex) {
                throw new InternalServerException(ex.getMessage());
            }

        }
        try {
            Map<String, String> props = new HashMap<>();
            props.put(NAME, templateName);
            Object deleteCloudServiceResult = azureClient.deleteCloudService(props);
        } catch (HttpResponseException ex) {
            httpResponseExceptionHandler(ex, templateName, user.getId());
        } catch (Exception ex) {
            throw new InternalServerException(ex.getMessage());
        }
        try {
            Map<String, String> props = new HashMap<>();
            props.put(NAME, templateName);
            Object deleteStorageAccountResult = azureClient.deleteStorageAccount(props);
        } catch (HttpResponseException ex) {
            httpResponseExceptionHandler(ex, templateName, user.getId());
        } catch (Exception ex) {
            throw new InternalServerException(ex.getMessage());
        }
        try {
            Map<String, String> props = new HashMap<>();
            props.put(NAME, templateName);
            Object affinityGroup = azureClient.deleteAffinityGroup(props);
        } catch (HttpResponseException ex) {
            httpResponseExceptionHandler(ex, templateName, user.getId());
        } catch (Exception ex) {
            throw new InternalServerException(ex.getMessage());
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
