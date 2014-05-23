package com.sequenceiq.provisioning.service.azure;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloud.azure.client.AzureClient;
import com.sequenceiq.provisioning.controller.json.AzureStackResult;
import com.sequenceiq.provisioning.controller.json.StackResult;
import com.sequenceiq.provisioning.domain.AzureTemplate;
import com.sequenceiq.provisioning.domain.CloudPlatform;
import com.sequenceiq.provisioning.domain.Stack;
import com.sequenceiq.provisioning.domain.StackDescription;
import com.sequenceiq.provisioning.domain.User;
import com.sequenceiq.provisioning.service.ProvisionService;

import groovyx.net.http.HttpResponseDecorator;

@Service
public class AzureProvisionService implements ProvisionService {
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

    @Override
    @Async
    public StackResult createStack(User user, Stack stack) {

        AzureTemplate azureTemplate = (AzureTemplate) stack.getTemplate();

        String filePath = getUserJksFileName(user.emailAsFolder());
        File file = new File(filePath);
        AzureClient azureClient = new AzureClient(user.getSubscriptionId(), file.getAbsolutePath(), user.getJks());
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

        for (int i = 0; i < stack.getClusterSize(); i++) {
            String vmName = String.format("%s-1", azureTemplate.getName());
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
        return new AzureStackResult(OK_STATUS);
    }

    private String getUserJksFileName(String user) {
        return String.format("%s/%s/%s.jks", DATADIR, user, user);
    }

    @Override
    public StackDescription describeStack(User user, Stack stack) {
        // TODO
        return null;
    }

    @Override
    public StackDescription describeStackWithResources(User user, Stack stack) {
        // TODO
        return null;
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.AZURE;
    }
}
