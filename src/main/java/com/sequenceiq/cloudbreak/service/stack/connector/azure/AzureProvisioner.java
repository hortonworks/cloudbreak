package com.sequenceiq.cloudbreak.service.stack.connector.azure;

import static com.sequenceiq.cloudbreak.service.stack.connector.azure.AzureStackUtil.CREDENTIAL;
import static com.sequenceiq.cloudbreak.service.stack.connector.azure.AzureStackUtil.EMAILASFOLDER;
import static com.sequenceiq.cloudbreak.service.stack.connector.azure.AzureStackUtil.NAME;
import static com.sequenceiq.cloudbreak.service.stack.connector.azure.AzureStackUtil.NOT_FOUND;
import static com.sequenceiq.cloudbreak.service.stack.connector.azure.AzureStackUtil.SERVICENAME;

import java.io.FileNotFoundException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.conf.ReactorConfig;
import com.sequenceiq.cloudbreak.controller.InternalServerException;
import com.sequenceiq.cloudbreak.controller.StackCreationFailureException;
import com.sequenceiq.cloudbreak.domain.AzureCredential;
import com.sequenceiq.cloudbreak.domain.AzureTemplate;
import com.sequenceiq.cloudbreak.domain.AzureVmType;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.Port;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.ResourceType;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.Status;
import com.sequenceiq.cloudbreak.repository.RetryingStackUpdater;
import com.sequenceiq.cloudbreak.service.credential.azure.AzureCertificateService;
import com.sequenceiq.cloudbreak.service.stack.connector.Provisioner;
import com.sequenceiq.cloudbreak.service.stack.event.ProvisionComplete;

import groovyx.net.http.HttpResponseDecorator;
import groovyx.net.http.HttpResponseException;
import reactor.core.Reactor;
import reactor.event.Event;

@Component
public class AzureProvisioner implements Provisioner {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureProvisioner.class);
    private static final int VALID_IP_RANGE_START = 4;
    private static final String LOCATION = "location";
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
    private static final String SUBNETNAME = "subnetName";
    private static final String VIRTUAL_NETWORK_IP_ADDRESS = "virtualNetworkIPAddress";
    private static final String CUSTOMDATA = "customData";
    private static final String VIRTUALNETWORKNAME = "virtualNetworkName";
    private static final String VMTYPE = "vmType";
    private static final String SSHPUBLICKEYFINGERPRINT = "sshPublicKeyFingerprint";
    private static final String SSHPUBLICKEYPATH = "sshPublicKeyPath";
    private static final String PORTS = "ports";
    private static final String DISKS = "disks";
    private static final String DATA = "data";
    private static final String DEFAULT_USER_NAME = "ubuntu";
    private static final String PRODUCTION = "production";

    @Autowired
    private Reactor reactor;

    @Autowired
    private RetryingStackUpdater retryingStackUpdater;

    @Autowired
    private AzureStackUtil azureStackUtil;

    @Override
    public void buildStack(Stack stack, String userData, Map<String, Object> setupProperties) {
        AzureTemplate azureTemplate = (AzureTemplate) stack.getTemplate();
        Credential credential = (Credential) setupProperties.get(CREDENTIAL);
        String emailAsFolder = (String) setupProperties.get(EMAILASFOLDER);

        String filePath = AzureCertificateService.getUserJksFileName(credential, emailAsFolder);
        AzureClient azureClient = azureStackUtil.createAzureClient(credential, filePath);
        retryingStackUpdater.updateStackStatus(stack.getId(), Status.CREATE_IN_PROGRESS);
        String name = stack.getName().replaceAll("\\s+", "") + String.valueOf(new Date().getTime());
        String commonName = ((AzureCredential) credential).getCommonName();
        createAffinityGroup(stack, azureClient, azureTemplate, commonName);
        createStorageAccount(stack, azureClient, azureTemplate, commonName);
        createVirtualNetwork(azureClient, name, commonName);
        Set<Resource> resourceSet = new HashSet<>();
        resourceSet.add(new Resource(ResourceType.AFFINITY_GROUP, commonName, stack));
        resourceSet.add(new Resource(ResourceType.STORAGE, commonName, stack));
        resourceSet.add(new Resource(ResourceType.NETWORK, name, stack));
        for (int i = 0; i < stack.getNodeCount(); i++) {
            String vmName = azureStackUtil.getVmName(name, i) + String.valueOf(new Date().getTime());
            createCloudService(azureClient, azureTemplate, vmName, commonName);
            createServiceCertificate(azureClient, azureTemplate, credential, vmName, emailAsFolder);
            String internalIp = "172.16.0." + (i + VALID_IP_RANGE_START);
            createVirtualMachine(azureClient, azureTemplate, credential, name, vmName, commonName, userData, internalIp);
            resourceSet.add(new Resource(ResourceType.VIRTUAL_MACHINE, vmName, stack));
            resourceSet.add(new Resource(ResourceType.CLOUD_SERVICE, vmName, stack));
            resourceSet.add(new Resource(ResourceType.BLOB, vmName, stack));
        }
        LOGGER.info("Publishing {} event [StackId: '{}']", ReactorConfig.PROVISION_COMPLETE_EVENT, stack.getId());
        reactor.notify(ReactorConfig.PROVISION_COMPLETE_EVENT, Event.wrap(new ProvisionComplete(CloudPlatform.AZURE, stack.getId(), resourceSet)));
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.AZURE;
    }

    private void createVirtualMachine(AzureClient azureClient, AzureTemplate azureTemplate, Credential credential,
            String name, String vmName, String commonName, String userData, String internalIp) {
        byte[] encoded = Base64.encodeBase64(vmName.getBytes());
        String label = new String(encoded);
        Map<String, Object> props = new HashMap<>();
        List<Port> ports = new ArrayList<>();
        ports.add(new Port("Ambari", "8080", "8080", "tcp"));
        ports.add(new Port("NameNode", "50070", "50070", "tcp"));
        ports.add(new Port("ResourceManager", "8088", "8088", "tcp"));
        ports.add(new Port("Job History Server", "19888", "19888", "tcp"));
        ports.add(new Port("HBase Master", "60010", "60010", "tcp"));
        ports.add(new Port("Falcon", "15000", "15000", "tcp"));
        ports.add(new Port("Storm", "8744", "8744", "tcp"));
        ports.add(new Port("Oozie", "11000", "11000", "tcp"));
        ports.add(new Port("HTTP", "80", "80", "tcp"));
        props.put(NAME, vmName);
        props.put(DEPLOYMENTSLOT, PRODUCTION);
        props.put(LABEL, label);
        props.put(IMAGENAME,
                azureTemplate.getImageName().equals(AzureStackUtil.IMAGE_NAME) ? azureStackUtil.getOsImageName(credential) : azureTemplate.getImageName());
        props.put(IMAGESTOREURI, buildimageStoreUri(commonName, vmName));
        props.put(HOSTNAME, vmName);
        props.put(USERNAME, DEFAULT_USER_NAME);
        X509Certificate sshCert = null;
        try {
            sshCert = azureStackUtil.createX509Certificate((AzureCredential) credential, azureTemplate.getOwner().emailAsFolder());
        } catch (FileNotFoundException e) {
            throw new StackCreationFailureException(e);
        } catch (CertificateException e) {
            throw new StackCreationFailureException(e);
        }
        try {
            props.put(SSHPUBLICKEYFINGERPRINT, sshCert.getSha1Fingerprint().toUpperCase());
        } catch (CertificateEncodingException e) {
            throw new StackCreationFailureException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new StackCreationFailureException(e);
        }
        props.put(SSHPUBLICKEYPATH, String.format("/home/%s/.ssh/authorized_keys", DEFAULT_USER_NAME));
        props.put(AFFINITYGROUP, commonName);
        if (azureTemplate.getVolumeCount() > 0) {
            List<Integer> disks = new ArrayList<>();
            for (int i = 0; i < azureTemplate.getVolumeCount(); i++) {
                disks.add(azureTemplate.getVolumeSize());
            }
            props.put(DISKS, disks);
        }

        props.put(SERVICENAME, vmName);
        props.put(SUBNETNAME, name);
        props.put(VIRTUAL_NETWORK_IP_ADDRESS, internalIp);
        props.put(CUSTOMDATA, new String(Base64.encodeBase64(userData.getBytes())));
        props.put(VIRTUALNETWORKNAME, name);
        props.put(PORTS, ports);
        props.put(VMTYPE, AzureVmType.valueOf(azureTemplate.getVmType()).vmType().replaceAll(" ", ""));

        HttpResponseDecorator virtualMachineResponse = (HttpResponseDecorator) azureClient.createVirtualMachine(props);
        String requestId = (String) azureClient.getRequestId(virtualMachineResponse);
        azureClient.waitUntilComplete(requestId);
    }

    private String buildimageStoreUri(String commonName, String vmName) {
        return String.format("http://%s.blob.core.windows.net/vhd-store/%s.vhd", commonName, vmName);
    }

    private void createCloudService(AzureClient azureClient, AzureTemplate azureTemplate, String vmName, String commonName) {
        Map<String, String> props = new HashMap<>();
        props.put(NAME, vmName);
        props.put(DESCRIPTION, azureTemplate.getDescription());
        props.put(AFFINITYGROUP, commonName);
        HttpResponseDecorator cloudServiceResponse = (HttpResponseDecorator) azureClient.createCloudService(props);
        String requestId = (String) azureClient.getRequestId(cloudServiceResponse);
        azureClient.waitUntilComplete(requestId);
    }

    private void createServiceCertificate(AzureClient azureClient, AzureTemplate azureTemplate, Credential credential, String name, String emailAsFolder) {
        Map<String, String> props = new HashMap<>();
        props.put(NAME, name);
        X509Certificate sshCert = null;
        try {
            sshCert = azureStackUtil.createX509Certificate((AzureCredential) credential, emailAsFolder);
        } catch (FileNotFoundException e) {
            throw new StackCreationFailureException(e);
        } catch (CertificateException e) {
            throw new StackCreationFailureException(e);
        }
        try {
            props.put(DATA, new String(sshCert.getPem()));
        } catch (CertificateEncodingException e) {
            throw new StackCreationFailureException(e);
        }
        HttpResponseDecorator serviceCertificate = (HttpResponseDecorator) azureClient.createServiceCertificate(props);
        String requestId = (String) azureClient.getRequestId(serviceCertificate);
        azureClient.waitUntilComplete(requestId);
    }

    private void createVirtualNetwork(AzureClient azureClient, String name, String commonName) {
        if (!azureClient.getVirtualNetworkConfiguration().toString().contains(name)) {
            Map<String, String> props = new HashMap<>();
            props.put(NAME, name);
            props.put(AFFINITYGROUP, commonName);
            props.put(SUBNETNAME, name);
            props.put(ADDRESSPREFIX, "172.16.0.0/16");
            props.put(SUBNETADDRESSPREFIX, "172.16.0.0/24");
            HttpResponseDecorator virtualNetworkResponse = (HttpResponseDecorator) azureClient.createVirtualNetwork(props);
            String requestId = (String) azureClient.getRequestId(virtualNetworkResponse);
            azureClient.waitUntilComplete(requestId);
        }
    }

    private void createStorageAccount(Stack stack, AzureClient azureClient, AzureTemplate azureTemplate, String commonName) {
        try {
            azureClient.getStorageAccount(commonName);
        } catch (Exception ex) {
            if (((HttpResponseException) ex).getStatusCode() == NOT_FOUND) {
                Map<String, String> props = new HashMap<>();
                props.put(NAME, commonName);
                props.put(DESCRIPTION, azureTemplate.getDescription());
                props.put(AFFINITYGROUP, commonName);
                HttpResponseDecorator storageResponse = (HttpResponseDecorator) azureClient.createStorageAccount(props);
                String requestId = (String) azureClient.getRequestId(storageResponse);
                azureClient.waitUntilComplete(requestId);
            } else if (ex instanceof HttpResponseException) {
                LOGGER.error(String.format("Error occurs on %s stack under the storage creation", stack.getId()), ex);
                throw new InternalServerException(((HttpResponseException) ex).getResponse().toString());
            } else {
                LOGGER.error(String.format("Error occurs on %s stack under the storage creation", stack.getId()), ex);
                throw new StackCreationFailureException(ex);
            }
        }
    }

    private void createAffinityGroup(Stack stack, AzureClient azureClient, AzureTemplate azureTemplate, String name) {
        try {
            azureClient.getAffinityGroup(name);
        } catch (Exception ex) {
            if (((HttpResponseException) ex).getStatusCode() == NOT_FOUND) {
                Map<String, String> props = new HashMap<>();
                props.put(NAME, name);
                props.put(LOCATION, azureTemplate.getLocation().location());
                props.put(DESCRIPTION, azureTemplate.getDescription());
                HttpResponseDecorator affinityResponse = (HttpResponseDecorator) azureClient.createAffinityGroup(props);
                String requestId = (String) azureClient.getRequestId(affinityResponse);
                azureClient.waitUntilComplete(requestId);
            } else if (ex instanceof HttpResponseException) {
                LOGGER.error(String.format("Error occurs on %s stack under the affinity group creation", stack.getId()), ex);
                throw new InternalServerException(((HttpResponseException) ex).getResponse().toString());
            } else {
                LOGGER.error(String.format("Error occurs on %s stack under the affinity group creation", stack.getId()), ex);
                throw new StackCreationFailureException(ex);
            }
        }
    }
}
