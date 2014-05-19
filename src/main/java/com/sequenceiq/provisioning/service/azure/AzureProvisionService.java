package com.sequenceiq.provisioning.service.azure;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloud.azure.client.AzureClient;
import com.sequenceiq.provisioning.controller.json.AzureProvisionResult;
import com.sequenceiq.provisioning.controller.json.ProvisionRequest;
import com.sequenceiq.provisioning.controller.json.ProvisionResult;
import com.sequenceiq.provisioning.domain.CloudPlatform;
import com.sequenceiq.provisioning.domain.User;
import com.sequenceiq.provisioning.service.ProvisionService;

import groovyx.net.http.HttpResponseDecorator;

@Component
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
    public ProvisionResult provisionCluster(User user, ProvisionRequest provisionRequest) {
        String filePath = getUserJksFileName(user.emailAsFolder());
        File file = new File(filePath);
        AzureClient azureClient = new AzureClient(user.getSubscriptionId(), file.getAbsolutePath(), user.getJks());
        Map<String, String> props = new HashMap<>();
        props.put(NAME, provisionRequest.getClusterName());
        props.put(LOCATION, AzureLocation.valueOf(provisionRequest.getParameters().get(LOCATION)).location());
        props.put(DESCRIPTION, provisionRequest.getParameters().get(DESCRIPTION));
        HttpResponseDecorator affinityResponse = (HttpResponseDecorator) azureClient.createAffinityGroup(props);
        String requestId = (String) azureClient.getRequestId(affinityResponse);
        azureClient.waitUntilComplete(requestId);

        props = new HashMap<>();
        props.put(NAME, provisionRequest.getClusterName());
        props.put(DESCRIPTION, provisionRequest.getParameters().get(DESCRIPTION));
        props.put(AFFINITYGROUP, provisionRequest.getClusterName());
        HttpResponseDecorator storageResponse = (HttpResponseDecorator) azureClient.createStorageAccount(props);
        requestId = (String) azureClient.getRequestId(storageResponse);
        azureClient.waitUntilComplete(requestId);

        props = new HashMap<>();
        props.put(NAME, provisionRequest.getClusterName());
        props.put(AFFINITYGROUP, provisionRequest.getClusterName());
        props.put(SUBNETNAME, provisionRequest.getClusterName());
        props.put(ADDRESSPREFIX, provisionRequest.getParameters().get(ADDRESSPREFIX));
        props.put(SUBNETADDRESSPREFIX, provisionRequest.getParameters().get(SUBNETADDRESSPREFIX));
        HttpResponseDecorator virtualNetworkResponse = (HttpResponseDecorator) azureClient.createVirtualNetwork(props);
        requestId = (String) azureClient.getRequestId(virtualNetworkResponse);
        azureClient.waitUntilComplete(requestId);


        for (int i = 0; i < provisionRequest.getClusterSize(); i++) {
            String vmName = String.format("%s-1", provisionRequest.getClusterName());
            props = new HashMap<>();
            props.put(NAME, vmName);
            props.put(DESCRIPTION, provisionRequest.getParameters().get(DESCRIPTION));
            props.put(AFFINITYGROUP, provisionRequest.getClusterName());
            HttpResponseDecorator cloudServiceResponse = (HttpResponseDecorator) azureClient.createCloudService(props);
            requestId = (String) azureClient.getRequestId(cloudServiceResponse);
            azureClient.waitUntilComplete(requestId);
            byte[] encoded = Base64.encodeBase64(vmName.getBytes());
            String label = new String(encoded);
            props = new HashMap<>();
            props.put(NAME, vmName);
            props.put(DEPLOYMENTSLOT, provisionRequest.getParameters().get(DEPLOYMENTSLOT));
            props.put(LABEL, label);
            props.put(IMAGENAME, provisionRequest.getParameters().get(IMAGENAME));
            props.put(IMAGESTOREURI, provisionRequest.getParameters().get(
                            String.format("http://%s.blob.core.windows.net/vhd-store/%s.vhd", provisionRequest.getClusterName(), vmName))
            );
            props.put(HOSTNAME, vmName);
            props.put(USERNAME, provisionRequest.getParameters().get(USERNAME));
            props.put(PASSWORD, provisionRequest.getParameters().get(PASSWORD));
            props.put(DISABLESSHPASSWORDAUTHENTICATION, Boolean.valueOf(provisionRequest.getParameters().get(DISABLESSHPASSWORDAUTHENTICATION)).toString());
            props.put(SUBNETNAME, provisionRequest.getClusterName());
            props.put(VIRTUALNETWORKNAME, provisionRequest.getClusterName());
            props.put(VMTYPE, AzureVmType.valueOf(provisionRequest.getParameters().get(VMTYPE)).vmType());
            HttpResponseDecorator virtualMachineResponse = (HttpResponseDecorator) azureClient.createVirtualMachine(props);
            requestId = (String) azureClient.getRequestId(virtualMachineResponse);
            azureClient.waitUntilComplete(requestId);
        }
        return new AzureProvisionResult(OK_STATUS);
    }

    private String getUserJksFileName(String user) {
        return String.format("%s/%s/%s.jks", DATADIR, user, user);
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.AZURE;
    }

}
