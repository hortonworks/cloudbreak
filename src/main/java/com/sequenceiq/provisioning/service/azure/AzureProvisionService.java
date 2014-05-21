package com.sequenceiq.provisioning.service.azure;

import groovyx.net.http.HttpResponseDecorator;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloud.azure.client.AzureClient;
import com.sequenceiq.provisioning.controller.json.AzureCloudInstanceResult;
import com.sequenceiq.provisioning.controller.json.CloudInstanceResult;
import com.sequenceiq.provisioning.domain.AzureInfra;
import com.sequenceiq.provisioning.domain.CloudInstance;
import com.sequenceiq.provisioning.domain.CloudPlatform;
import com.sequenceiq.provisioning.domain.User;
import com.sequenceiq.provisioning.service.ProvisionService;

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
    private static final String DISABLESSHPASSWORDAUTHENTICATION = "disableSshPasswordAuthentication";
    private static final String SUBNETNAME = "subnetName";
    private static final String VIRTUALNETWORKNAME = "virtualNetworkName";
    private static final String VMTYPE = "vmType";
    private static final String DATADIR = "userdatas";

    @Override
    public CloudInstanceResult createCloudInstance(User user, CloudInstance cloudInstance) {

        AzureInfra azureInfra = (AzureInfra) cloudInstance.getInfra();

        String filePath = getUserJksFileName(user.emailAsFolder());
        File file = new File(filePath);
        AzureClient azureClient = new AzureClient(user.getSubscriptionId(), file.getAbsolutePath(), user.getJks());
        Map<String, String> props = new HashMap<>();
        props.put(NAME, azureInfra.getName());
        props.put(LOCATION, AzureLocation.valueOf(azureInfra.getLocation()).location());
        props.put(DESCRIPTION, azureInfra.getDescription());
        HttpResponseDecorator affinityResponse = (HttpResponseDecorator) azureClient.createAffinityGroup(props);
        String requestId = (String) azureClient.getRequestId(affinityResponse);
        azureClient.waitUntilComplete(requestId);

        props = new HashMap<>();
        props.put(NAME, azureInfra.getName());
        props.put(DESCRIPTION, azureInfra.getDescription());
        props.put(AFFINITYGROUP, azureInfra.getName());
        HttpResponseDecorator storageResponse = (HttpResponseDecorator) azureClient.createStorageAccount(props);
        requestId = (String) azureClient.getRequestId(storageResponse);
        azureClient.waitUntilComplete(requestId);

        props = new HashMap<>();
        props.put(NAME, azureInfra.getName());
        props.put(AFFINITYGROUP, azureInfra.getName());
        props.put(SUBNETNAME, azureInfra.getSubnetAddressPrefix());
        props.put(ADDRESSPREFIX, azureInfra.getSubnetAddressPrefix());
        props.put(SUBNETADDRESSPREFIX, azureInfra.getSubnetAddressPrefix());
        HttpResponseDecorator virtualNetworkResponse = (HttpResponseDecorator) azureClient.createVirtualNetwork(props);
        requestId = (String) azureClient.getRequestId(virtualNetworkResponse);
        azureClient.waitUntilComplete(requestId);

        for (int i = 0; i < cloudInstance.getClusterSize(); i++) {
            String vmName = String.format("%s-1", azureInfra.getName());
            props = new HashMap<>();
            props.put(NAME, vmName);
            props.put(DESCRIPTION, azureInfra.getDescription());
            props.put(AFFINITYGROUP, azureInfra.getName());
            HttpResponseDecorator cloudServiceResponse = (HttpResponseDecorator) azureClient.createCloudService(props);
            requestId = (String) azureClient.getRequestId(cloudServiceResponse);
            azureClient.waitUntilComplete(requestId);
            byte[] encoded = Base64.encodeBase64(vmName.getBytes());
            String label = new String(encoded);
            props = new HashMap<>();
            props.put(NAME, vmName);
            props.put(DEPLOYMENTSLOT, azureInfra.getDeploymentSlot());
            props.put(LABEL, label);
            props.put(IMAGENAME, azureInfra.getImageName());
            props.put(IMAGESTOREURI,
                    String.format("http://%s.blob.core.windows.net/vhd-store/%s.vhd", azureInfra.getName(), vmName)
                    );
            props.put(HOSTNAME, vmName);
            props.put(USERNAME, azureInfra.getUserName());
            props.put(PASSWORD, azureInfra.getPassword());
            props.put(DISABLESSHPASSWORDAUTHENTICATION, Boolean.valueOf(azureInfra.getDisableSshPasswordAuthentication()).toString());
            props.put(SUBNETNAME, azureInfra.getName());
            props.put(VIRTUALNETWORKNAME, azureInfra.getName());
            props.put(VMTYPE, AzureVmType.valueOf(azureInfra.getVmType()).vmType());
            HttpResponseDecorator virtualMachineResponse = (HttpResponseDecorator) azureClient.createVirtualMachine(props);
            requestId = (String) azureClient.getRequestId(virtualMachineResponse);
            azureClient.waitUntilComplete(requestId);
        }
        return new AzureCloudInstanceResult(OK_STATUS);
    }

    private String getUserJksFileName(String user) {
        return String.format("%s/%s/%s.jks", DATADIR, user, user);
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.AZURE;
    }

}
