package com.sequenceiq.provisioning.service.azure;

import groovyx.net.http.HttpResponseDecorator;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloud.azure.client.AzureClient;
import com.sequenceiq.provisioning.controller.json.AzureCloudInstanceResult;
import com.sequenceiq.provisioning.controller.json.CloudInstanceRequest;
import com.sequenceiq.provisioning.controller.json.CloudInstanceResult;
import com.sequenceiq.provisioning.converter.AzureCloudInstanceConverter;
import com.sequenceiq.provisioning.domain.AzureCloudInstance;
import com.sequenceiq.provisioning.domain.CloudPlatform;
import com.sequenceiq.provisioning.domain.User;
import com.sequenceiq.provisioning.repository.UserRepository;
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

    @Autowired
    private AzureCloudInstanceConverter azureCloudInstanceConverter;

    @Autowired
    private UserRepository userRepository;

    @Override
    public CloudInstanceResult createCloudInstance(User user, CloudInstanceRequest cloudInstanceRequest) {
        AzureCloudInstance azureCloudInstance = azureCloudInstanceConverter.convert(cloudInstanceRequest);
        azureCloudInstance.setUser(user);
        user.getAzureCloudInstanceList().add(azureCloudInstance);
        userRepository.save(user);

        String filePath = getUserJksFileName(user.emailAsFolder());
        File file = new File(filePath);
        AzureClient azureClient = new AzureClient(user.getSubscriptionId(), file.getAbsolutePath(), user.getJks());
        Map<String, String> props = new HashMap<>();
        props.put(NAME, azureCloudInstance.getAzureInfra().getName());
        props.put(LOCATION, AzureLocation.valueOf(azureCloudInstance.getAzureInfra().getLocation()).location());
        props.put(DESCRIPTION, azureCloudInstance.getAzureInfra().getDescription());
        HttpResponseDecorator affinityResponse = (HttpResponseDecorator) azureClient.createAffinityGroup(props);
        String requestId = (String) azureClient.getRequestId(affinityResponse);
        azureClient.waitUntilComplete(requestId);

        props = new HashMap<>();
        props.put(NAME, azureCloudInstance.getAzureInfra().getName());
        props.put(DESCRIPTION, azureCloudInstance.getAzureInfra().getDescription());
        props.put(AFFINITYGROUP, azureCloudInstance.getAzureInfra().getName());
        HttpResponseDecorator storageResponse = (HttpResponseDecorator) azureClient.createStorageAccount(props);
        requestId = (String) azureClient.getRequestId(storageResponse);
        azureClient.waitUntilComplete(requestId);

        props = new HashMap<>();
        props.put(NAME, azureCloudInstance.getAzureInfra().getName());
        props.put(AFFINITYGROUP, azureCloudInstance.getAzureInfra().getName());
        props.put(SUBNETNAME, azureCloudInstance.getAzureInfra().getSubnetAddressPrefix());
        props.put(ADDRESSPREFIX, azureCloudInstance.getAzureInfra().getSubnetAddressPrefix());
        props.put(SUBNETADDRESSPREFIX, azureCloudInstance.getAzureInfra().getSubnetAddressPrefix());
        HttpResponseDecorator virtualNetworkResponse = (HttpResponseDecorator) azureClient.createVirtualNetwork(props);
        requestId = (String) azureClient.getRequestId(virtualNetworkResponse);
        azureClient.waitUntilComplete(requestId);

        for (int i = 0; i < azureCloudInstance.getClusterSize(); i++) {
            String vmName = String.format("%s-1", azureCloudInstance.getAzureInfra().getName());
            props = new HashMap<>();
            props.put(NAME, vmName);
            props.put(DESCRIPTION, azureCloudInstance.getAzureInfra().getDescription());
            props.put(AFFINITYGROUP, azureCloudInstance.getAzureInfra().getName());
            HttpResponseDecorator cloudServiceResponse = (HttpResponseDecorator) azureClient.createCloudService(props);
            requestId = (String) azureClient.getRequestId(cloudServiceResponse);
            azureClient.waitUntilComplete(requestId);
            byte[] encoded = Base64.encodeBase64(vmName.getBytes());
            String label = new String(encoded);
            props = new HashMap<>();
            props.put(NAME, vmName);
            props.put(DEPLOYMENTSLOT, azureCloudInstance.getAzureInfra().getDeploymentSlot());
            props.put(LABEL, label);
            props.put(IMAGENAME, azureCloudInstance.getAzureInfra().getImageName());
            props.put(IMAGESTOREURI,
                    String.format("http://%s.blob.core.windows.net/vhd-store/%s.vhd", azureCloudInstance.getAzureInfra().getName(), vmName)
                    );
            props.put(HOSTNAME, vmName);
            props.put(USERNAME, azureCloudInstance.getAzureInfra().getUserName());
            props.put(PASSWORD, azureCloudInstance.getAzureInfra().getPassword());
            props.put(DISABLESSHPASSWORDAUTHENTICATION, Boolean.valueOf(azureCloudInstance.getAzureInfra().getDisableSshPasswordAuthentication()).toString());
            props.put(SUBNETNAME, azureCloudInstance.getAzureInfra().getName());
            props.put(VIRTUALNETWORKNAME, azureCloudInstance.getAzureInfra().getName());
            props.put(VMTYPE, AzureVmType.valueOf(azureCloudInstance.getAzureInfra().getVmType()).vmType());
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
