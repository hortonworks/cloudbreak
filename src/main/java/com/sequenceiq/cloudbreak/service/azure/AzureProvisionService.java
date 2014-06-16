package com.sequenceiq.cloudbreak.service.azure;

import java.io.File;
import java.io.FileNotFoundException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.controller.InternalServerException;
import com.sequenceiq.cloudbreak.controller.json.JsonHelper;
import com.sequenceiq.cloudbreak.domain.AzureCredential;
import com.sequenceiq.cloudbreak.domain.AzureStackDescription;
import com.sequenceiq.cloudbreak.domain.AzureTemplate;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.DetailedAzureStackDescription;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.StackDescription;
import com.sequenceiq.cloudbreak.domain.Status;
import com.sequenceiq.cloudbreak.domain.User;
import com.sequenceiq.cloudbreak.domain.WebsocketEndPoint;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.stack.ProvisionService;
import com.sequenceiq.cloudbreak.websocket.WebsocketService;
import com.sequenceiq.cloudbreak.websocket.message.StatusMessage;

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
    private static final String PORTS = "ports";
    private static final String ERROR = "\"error\":\"Could not fetch data from azure\"";
    private static final int NOT_FOUND = 404;

    @Autowired
    private JsonHelper jsonHelper;

    @Autowired
    private StackRepository stackRepository;

    @Autowired
    private WebsocketService websocketService;

    @Override
    @Async
    public void createStack(User user, Stack stack, Credential credential) {
        AzureTemplate azureTemplate = (AzureTemplate) stack.getTemplate();
        updatedStackStatus(stack.getId(), Status.REQUESTED);
        String filePath = AzureCredentialService.getUserJksFileName(credential, user.emailAsFolder());
        File file = new File(filePath);
        AzureClient azureClient = new AzureClient(
                ((AzureCredential) credential).getSubscriptionId(),
                file.getAbsolutePath(),
                ((AzureCredential) credential).getJks()
        );
        updatedStackStatus(stack.getId(), Status.CREATE_IN_PROGRESS);
        String name = stack.getName().replaceAll("\\s+", "");
        String commonName = credential.getName().replaceAll("\\s+", "");
        createAffinityGroup(azureClient, azureTemplate, commonName);
        createStorageAccount(azureClient, azureTemplate, commonName);
        createVirtualNetwork(azureClient, azureTemplate, name, commonName);

        for (int i = 0; i < stack.getNodeCount(); i++) {
            try {
                String vmName = getVmName(name, i);
                createCloudService(azureClient, azureTemplate, name, vmName, commonName);
                createVirtualMachine(azureClient, azureTemplate, name, vmName, commonName, user);
            } catch (Exception ex) {
                LOGGER.info("Problem with the stack creation: " + ex.getMessage());
                updatedStackStatus(stack.getId(), Status.CREATE_FAILED);
                return;
            }
        }
        Map<String, String> metaData = new HashMap<>();
        for (int i = 0; i < stack.getNodeCount(); i++) {
            String vmName = getVmName(name, i);
            Map<String, Object> props = new HashMap<>();
            props.put(NAME, vmName);
            props.put(SERVICENAME, vmName);
            Object virtualMachine = azureClient.getVirtualMachine(props);
        }
        stack.setMetadata(metaData);
        stackRepository.save(stack);
        updatedStackStatus(stack.getId(), Status.CREATE_COMPLETED);
    }

    private void updatedStackStatus(Long id, Status status) {
        Stack updatedStack = stackRepository.findById(id);
        updatedStack.setStatus(status);
        stackRepository.save(updatedStack);
        websocketService.sendToTopicUser(updatedStack.getUser().getEmail(), WebsocketEndPoint.STACK,
                new StatusMessage(updatedStack.getId(), updatedStack.getName(), status.name()));
    }


    private void createVirtualMachine(AzureClient azureClient, AzureTemplate azureTemplate, String name, String vmName, String commonName, User user) {
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
        props.put(DEPLOYMENTSLOT, azureTemplate.getDeploymentSlot());
        props.put(LABEL, label);
        props.put(IMAGENAME, azureTemplate.getImageName());
        props.put(IMAGESTOREURI,
                String.format("http://%s.blob.core.windows.net/vhd-store/%s.vhd", commonName, vmName)
        );
        props.put(HOSTNAME, vmName);
        props.put(USERNAME, azureTemplate.getUserName());
        if (azureTemplate.getPassword() != null) {
            props.put(PASSWORD, azureTemplate.getPassword());
        } else {
            try {
                X509Certificate sshCert = new X509Certificate(AzureCredentialService.getCerFile(user.emailAsFolder(), azureTemplate.getId()));
                props.put(SSHPUBLICKEYFINGERPRINT, sshCert.getSha1Fingerprint());
                props.put(SSHPUBLICKEYPATH, String.format("/home/%s/.ssh/authorized_keys", azureTemplate.getUserName()));
            } catch (FileNotFoundException e) {
                LOGGER.info("Problem with the ssh file because not found: " + e.getMessage());
                updatedStackStatus(azureTemplate.getId(), Status.CREATE_FAILED);
                return;
            } catch (CertificateException e) {
                LOGGER.info("Problem wiht the certificate file: " + e.getMessage());
                updatedStackStatus(azureTemplate.getId(), Status.CREATE_FAILED);
                return;
            } catch (NoSuchAlgorithmException e) {
                LOGGER.info("Problem wiht the fingerprint: " + e.getMessage());
                updatedStackStatus(azureTemplate.getId(), Status.CREATE_FAILED);
                return;
            }

        }
        props.put(SERVICENAME, vmName);
        props.put(SUBNETNAME, name);
        props.put(VIRTUALNETWORKNAME, name);
        props.put(PORTS, ports);
        props.put(VMTYPE, AzureVmType.valueOf(azureTemplate.getVmType()).vmType().replaceAll(" ", ""));
        HttpResponseDecorator virtualMachineResponse = (HttpResponseDecorator) azureClient.createVirtualMachine(props);
        String requestId = (String) azureClient.getRequestId(virtualMachineResponse);
        azureClient.waitUntilComplete(requestId);
    }

    private void createCloudService(AzureClient azureClient, AzureTemplate azureTemplate, String name, String vmName, String commonName) {
        Map<String, String> props = new HashMap<>();
        props.put(NAME, vmName);
        props.put(DESCRIPTION, azureTemplate.getDescription());
        props.put(AFFINITYGROUP, commonName);
        HttpResponseDecorator cloudServiceResponse = (HttpResponseDecorator) azureClient.createCloudService(props);
        String requestId = (String) azureClient.getRequestId(cloudServiceResponse);
        azureClient.waitUntilComplete(requestId);
    }


    private void createVirtualNetwork(AzureClient azureClient, AzureTemplate azureTemplate, String name, String commonName) {
        Map<String, String> props = new HashMap<>();
        props.put(NAME, name);
        props.put(AFFINITYGROUP, commonName);
        props.put(SUBNETNAME, name);
        props.put(ADDRESSPREFIX, azureTemplate.getAddressPrefix());
        props.put(SUBNETADDRESSPREFIX, azureTemplate.getSubnetAddressPrefix());
        HttpResponseDecorator virtualNetworkResponse = (HttpResponseDecorator) azureClient.createVirtualNetwork(props);
        String requestId = (String) azureClient.getRequestId(virtualNetworkResponse);
        azureClient.waitUntilComplete(requestId);
    }

    private void createStorageAccount(AzureClient azureClient, AzureTemplate azureTemplate, String commonName) {
        try {
            azureClient.getStorageAccount(commonName);
        } catch (Exception ex) {
            Map<String, String> props = new HashMap<>();
            props.put(NAME, commonName);
            props.put(DESCRIPTION, azureTemplate.getDescription());
            props.put(AFFINITYGROUP, commonName);
            HttpResponseDecorator storageResponse = (HttpResponseDecorator) azureClient.createStorageAccount(props);
            String requestId = (String) azureClient.getRequestId(storageResponse);
            azureClient.waitUntilComplete(requestId);
        }
    }

    private void createAffinityGroup(AzureClient azureClient, AzureTemplate azureTemplate, String name) {
        try {
            azureClient.getAffinityGroup(name);
        } catch (Exception ex) {
            Map<String, String> props = new HashMap<>();
            props.put(NAME, name);
            props.put(LOCATION, AzureLocation.valueOf(azureTemplate.getLocation()).location());
            props.put(DESCRIPTION, azureTemplate.getDescription());
            HttpResponseDecorator affinityResponse = (HttpResponseDecorator) azureClient.createAffinityGroup(props);
            String requestId = (String) azureClient.getRequestId(affinityResponse);
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
        String templateName = stack.getName();
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
        String templateName = stack.getName();
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
        String templateName = stack.getName();
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
