package com.sequenceiq.cloudbreak.cloud.azure.client;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.microsoft.azure.AzureEnvironment;
import com.microsoft.azure.PagedList;
import com.microsoft.azure.credentials.ApplicationTokenCredentials;
import com.microsoft.azure.credentials.AzureTokenCredentials;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.compute.AvailabilitySet;
import com.microsoft.azure.management.compute.OperatingSystemStateTypes;
import com.microsoft.azure.management.compute.PowerState;
import com.microsoft.azure.management.compute.VirtualMachine;
import com.microsoft.azure.management.compute.VirtualMachineCustomImage;
import com.microsoft.azure.management.compute.VirtualMachineInstanceView;
import com.microsoft.azure.management.network.LoadBalancer;
import com.microsoft.azure.management.network.NetworkInterface;
import com.microsoft.azure.management.network.NetworkInterfaces;
import com.microsoft.azure.management.network.NetworkSecurityGroup;
import com.microsoft.azure.management.network.PublicIPAddress;
import com.microsoft.azure.management.network.Subnet;
import com.microsoft.azure.management.resources.Deployment;
import com.microsoft.azure.management.resources.DeploymentMode;
import com.microsoft.azure.management.resources.DeploymentOperations;
import com.microsoft.azure.management.resources.Deployments;
import com.microsoft.azure.management.resources.ResourceGroup;
import com.microsoft.azure.management.resources.ResourceGroups;
import com.microsoft.azure.management.storage.ProvisioningState;
import com.microsoft.azure.management.storage.SkuName;
import com.microsoft.azure.management.storage.StorageAccount;
import com.microsoft.azure.management.storage.StorageAccountKey;
import com.microsoft.azure.management.storage.StorageAccounts;
import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.BlobContainerPermissions;
import com.microsoft.azure.storage.blob.BlobContainerPublicAccessType;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import com.microsoft.azure.storage.blob.CloudPageBlob;
import com.microsoft.azure.storage.blob.CopyState;
import com.microsoft.azure.storage.blob.ListBlobItem;
import com.microsoft.rest.LogLevel;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;

public class AzureClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureClient.class);

    private Azure azure;

    private String tenantId;

    private String clientId;

    private String secretKey;

    private String subscriptionId;

    public AzureClient(String tenantId, String clientId, String secretKey, String subscriptionId) throws IOException {
        this.tenantId = tenantId;
        this.clientId = clientId;
        this.secretKey = secretKey;
        this.subscriptionId = subscriptionId;
        connect();
    }

    private void connect() throws IOException {
        AzureTokenCredentials creds = new ApplicationTokenCredentials(clientId, tenantId, secretKey, AzureEnvironment.AZURE)
                .withDefaultSubscriptionId(subscriptionId);
        azure = Azure
                .configure()
                .withLogLevel(LogLevel.BASIC)
                .authenticate(creds)
                .withSubscription(subscriptionId);
    }

    public ResourceGroup getResourceGroup(String name) {
        return azure.resourceGroups().getByName(name);
    }

    public ResourceGroups getResourceGroups() {
        return azure.resourceGroups();
    }

    public boolean resourceGroupExists(String name) {
        return getResourceGroups().checkExistence(name);
    }

    public void deleteResourceGroup(String name) {
        azure.resourceGroups().deleteByName(name);
    }

    public ResourceGroup createResourceGroup(String name, String region) {
        return azure.resourceGroups().define(name)
                .withRegion(region)
                .create();
    }

    public Deployment createTemplateDeployment(String resourceGroupName, String deploymentName, String templateContent, String parameterContent)
            throws IOException {
        return azure.deployments().define(deploymentName)
                .withExistingResourceGroup(resourceGroupName)
                .withTemplate(templateContent)
                .withParameters(parameterContent)
                .withMode(DeploymentMode.INCREMENTAL)
                .create();
    }

    public boolean templateDeploymentExists(String resourceGroupName, String deploymentName) {
        return azure.deployments().checkExistence(resourceGroupName, deploymentName);
    }

    public void deleteTemplateDeployment(String resourceGroupName, String deploymentName) {
        azure.deployments().deleteByResourceGroup(resourceGroupName, deploymentName);
    }

    public Deployment getTemplateDeployment(String resourceGroupName, String deploymentName) {
        return azure.deployments().getByResourceGroup(resourceGroupName, deploymentName);
    }

    public DeploymentOperations getTemplateDeploymentOperations(String resourceGroupName, String deploymentName) {
        return azure.deployments().getByResourceGroup(resourceGroupName, deploymentName).deploymentOperations();
    }

    public void cancelTemplateDeployments(String resourceGroupName, String deploymentName) {
        azure.deployments().getByResourceGroup(resourceGroupName, deploymentName).cancel();
    }

    public Deployments getTemplateDeployments(String resourceGroupName) {
        return azure.deployments();
    }

    public StorageAccounts getStorageAccounts() {
        return azure.storageAccounts();
    }

    public PagedList<StorageAccount> getStorageAccountsForResourceGroup(String resourceGroup) {
        return azure.storageAccounts().listByResourceGroup(resourceGroup);
    }

    public void deleteStorageAccount(String resourceGroup, String storageName) {
        azure.storageAccounts().deleteByResourceGroup(resourceGroup, storageName);
    }

    public StorageAccount createStorageAccount(String resourceGroup, String storageName, String storageLocation, SkuName accType) {

        return azure.storageAccounts()
                .define(storageName)
                .withRegion(storageLocation)
                .withExistingResourceGroup(resourceGroup)
                .withSku(accType)
                .create();
    }

    public List<StorageAccountKey> getStorageAccountKeys(String resourceGroup, String storageName) {
        return getStorageAccountByGroup(resourceGroup, storageName).getKeys();
    }

    public ProvisioningState getStorageStatus(String resourceGroup, String storageName) {
        return getStorageAccountByGroup(resourceGroup, storageName).provisioningState();
    }

    public StorageAccount getStorageAccountByGroup(String resourceGroup, String storageName) {
        return azure.storageAccounts().getByResourceGroup(resourceGroup, storageName);
    }

    public void deleteContainerInStorage(String resourceGroup, String storageName, String containerName) {
        LOGGER.debug("delete container: RG={}, storageName={}, containerName={}", resourceGroup, storageName, containerName);
        CloudBlobContainer container = getBlobContainer(resourceGroup, storageName, containerName);
        try {
            boolean existed = container.deleteIfExists();
            LOGGER.info("is container existed: " + existed);
        } catch (StorageException e) {
            throw new CloudConnectorException("can't delete container in storage, storage service error occurred", e);
        }
    }

    public void deleteBlobInStorageContainer(String resourceGroup, String storageName, String containerName, String blobName) {
        LOGGER.debug("delete blob: RG={}, storageName={}, containerName={}, blobName={}", resourceGroup, storageName, containerName, blobName);
        CloudBlobContainer container = getBlobContainer(resourceGroup, storageName, containerName);
        try {
            CloudBlockBlob blob = container.getBlockBlobReference(blobName);
            boolean wasDeleted = blob.deleteIfExists();
            LOGGER.info("blob was deleted: " + wasDeleted);
        } catch (URISyntaxException e) {
            throw new CloudConnectorException("can't delete blob in storage container, URI is not valid", e);
        } catch (StorageException e) {
            throw new CloudConnectorException("can't delete blob in storage container, storage service error occurred", e);
        }
    }

    public void createContainerInStorage(String resourceGroup, String storageName, String containerName) {
        LOGGER.debug("create container: RG={}, storageName={}, containerName={}", resourceGroup, storageName, containerName);
        CloudBlobContainer container = getBlobContainer(resourceGroup, storageName, containerName);
        try {
            boolean created = container.createIfNotExists();
            LOGGER.info("container created: " + created);
        } catch (StorageException e) {
            throw new CloudConnectorException("can't create container in storage, storage service error occurred", e);
        }
        setPublicPermissionOnContainer(resourceGroup, storageName, containerName);
    }

    public void setPublicPermissionOnContainer(String resourceGroup, String storageName, String containerName) {
        LOGGER.debug("set public permission on container: RG={}, storageName={}, containerName={}", resourceGroup, storageName, containerName);
        CloudBlobContainer container = getBlobContainer(resourceGroup, storageName, containerName);
        BlobContainerPermissions containerPermissions = new BlobContainerPermissions();
        containerPermissions.setPublicAccess(BlobContainerPublicAccessType.CONTAINER);
        try {
            container.uploadPermissions(containerPermissions);
        } catch (StorageException e) {
            throw new CloudConnectorException("can't set public permission on container, storage service error occurred", e);
        }
    }

    public void copyImageBlobInStorageContainer(String resourceGroup, String storageName, String containerName, String sourceBlob) {
        LOGGER.debug("copy image in storage container: RG={}, storageName={}, containerName={}, sourceBlob={}",
                resourceGroup, storageName, containerName, sourceBlob);
        CloudBlobContainer container = getBlobContainer(resourceGroup, storageName, containerName);
        try {
            CloudPageBlob cloudPageBlob = container.getPageBlobReference(sourceBlob.substring(sourceBlob.lastIndexOf("/") + 1));
            String copyId = cloudPageBlob.startCopy(new URI(sourceBlob));
            LOGGER.info("image copy started, copy id: %s", copyId);
        } catch (URISyntaxException e) {
            throw new CloudConnectorException("can't copy image blob, URI is not valid", e);
        } catch (StorageException e) {
            throw new CloudConnectorException("can't copy image blob, storage service error occurred", e);
        }
    }

    public CopyState getCopyStatus(String resourceGroup, String storageName, String containerName, String sourceBlob) {
        LOGGER.debug("get image copy status: RG={}, storageName={}, containerName={}, sourceBlob={}",
                resourceGroup, storageName, containerName, sourceBlob);
        CloudBlobContainer container = getBlobContainer(resourceGroup, storageName, containerName);
        try {
            CloudPageBlob cloudPageBlob = container.getPageBlobReference(sourceBlob.substring(sourceBlob.lastIndexOf("/") + 1));
            container.downloadAttributes();
            cloudPageBlob.downloadAttributes();
            return cloudPageBlob.getCopyState();
        } catch (URISyntaxException e) {
            throw new CloudConnectorException("can't get copy status, URI is not valid", e);
        } catch (StorageException e) {
            throw new CloudConnectorException("can't get copy status, storage service error occurred", e);
        }
    }

    public String getImageBlobUri(String resourceGroup, String storageName, String containerName, String sourceBlob) {
        CloudBlobContainer blobContainer = getBlobContainer(resourceGroup, storageName, containerName);
        String vhdName = sourceBlob.substring(sourceBlob.lastIndexOf('/') + 1);
        try {
            CloudPageBlob pageBlobReference = blobContainer.getPageBlobReference(vhdName);
            return pageBlobReference.getUri().toString();
        } catch (URISyntaxException e) {
            throw new CloudConnectorException("can't get image blob uri, URI is not valid", e);
        } catch (StorageException e) {
            throw new CloudConnectorException("can't get image blob uri, storage service error occurred", e);
        }
    }

    public List<ListBlobItem> listBlobInStorage(String resourceGroup, String storageName, String containerName) {
        CloudBlobContainer container = getBlobContainer(resourceGroup, storageName, containerName);
        List<ListBlobItem> targetCollection = new ArrayList<>();
        container.listBlobs().iterator().forEachRemaining(targetCollection::add);
        return targetCollection;
    }

    public CloudBlobContainer getBlobContainer(String resourceGroup, String storageName, String containerName) {
        LOGGER.debug("get blob container: RG={}, storageName={}, containerName={}", resourceGroup, storageName, containerName);
        List<StorageAccountKey> keys = getStorageAccountKeys(resourceGroup, storageName);
        String storageConnectionString = String.format("DefaultEndpointsProtocol=http;AccountName=%s;AccountKey=%s", storageName, keys.get(0).value());
        try {
            CloudStorageAccount storageAccount = CloudStorageAccount.parse(storageConnectionString);
            CloudBlobClient blobClient = storageAccount.createCloudBlobClient();
            return blobClient.getContainerReference(containerName);
        } catch (URISyntaxException e) {
            throw new CloudConnectorException("can't get blob container, URI is not valid", e);
        } catch (InvalidKeyException e) {
            throw new CloudConnectorException("can't get blob container, credentials in the connection string contain an invalid key", e);
        } catch (StorageException e) {
            throw new CloudConnectorException("can't get blob container, storage service error occurred", e);
        }
    }

    public PagedList<VirtualMachine> getVirtualMachines(String resourceGroup) {
        return azure.virtualMachines().listByResourceGroup(resourceGroup);
    }

    public VirtualMachine getVirtualMachine(String resourceGroup, String vmName) {
        return azure.virtualMachines().getByResourceGroup(resourceGroup, vmName);
    }

    public PowerState getVirtualMachinePowerState(String resourceGroup, String vmName) {
        return getVirtualMachine(resourceGroup, vmName).powerState();
    }

    public VirtualMachineInstanceView getVirtualMachineInstanceView(String resourceGroup, String vmName) {
        return getVirtualMachine(resourceGroup, vmName).instanceView();
    }

    public Integer getFaultDomainNumber(String resourceGroup, String vmName) {
        return getVirtualMachineInstanceView(resourceGroup, vmName).platformFaultDomain();
    }

    public Integer getUpdateDomainNumber(String resourceGroup, String vmName) {
        return getVirtualMachineInstanceView(resourceGroup, vmName).platformUpdateDomain();
    }

    public AvailabilitySet getAvailabilitySet(String resourceGroup, String asName) {
        return azure.availabilitySets().getByResourceGroup(resourceGroup, asName);
    }

    public void deleteAvailabilitySet(String resourceGroup, String asName) {
        azure.availabilitySets().deleteByResourceGroup(resourceGroup, asName);
    }

    public void deallocateVirtualMachine(String resourceGroup, String vmName) {
        azure.virtualMachines().deallocate(resourceGroup, vmName);
    }

    public boolean isVirtualMachineExists(String resourceGroup, String vmName) {
        Iterable<VirtualMachine> vmIterable = () -> azure.virtualMachines().listByResourceGroup(resourceGroup).iterator();
        Stream<VirtualMachine> vmStream = StreamSupport.stream(vmIterable.spliterator(), false);
        Optional<VirtualMachine> vm = vmStream.filter(virtualMachine -> vmName.equals(virtualMachine.name())).findFirst();
        return vm.isPresent();
    }

    public void deleteVirtualMachine(String resourceGroup, String vmName) {
        String id = getVirtualMachine(resourceGroup, vmName).id();
        azure.virtualMachines().deleteById(id);
    }

    public void startVirtualMachine(String resourceGroup, String vmName) {
        azure.virtualMachines().start(resourceGroup, vmName);
    }

    public void stopVirtualMachine(String resourceGroup, String vmName) {
        azure.virtualMachines().powerOff(resourceGroup, vmName);
    }

    public void deletePublicIpAddressByName(String resourceGroup, String ipName) {
        String id = getPublicIpAddress(resourceGroup, ipName).id();
        azure.publicIPAddresses().deleteById(id);
    }

    public void deletePublicIpAddressById(String ipId) {
        azure.publicIPAddresses().deleteById(ipId);
    }

    public PublicIPAddress getPublicIpAddress(String resourceGroup, String ipName) {
        return azure.publicIPAddresses().getByResourceGroup(resourceGroup, ipName);
    }

    public String getCustomImageId(String resourceGroup, String fromVhdUri, String region) {
        String vhdName = fromVhdUri.substring(fromVhdUri.lastIndexOf('/') + 1);
        String imageName = vhdName + "-" + region.toLowerCase().replaceAll("\\s", "");
        Optional<VirtualMachineCustomImage> virtualMachineCustomImage = getCustomImageList(resourceGroup).stream()
                .filter(customImage -> customImage.name().equals(imageName) && customImage.region().name().equals(region)).findFirst();
        if (virtualMachineCustomImage.isPresent()) {
            LOGGER.info("custom image found in '{}' resource group with name '{}'", resourceGroup, imageName);
            return virtualMachineCustomImage.get().id();
        } else {
            LOGGER.info("custom image NOT found in '{}' resource group with name '{}'", resourceGroup, imageName);
            VirtualMachineCustomImage customImage = createCustomImage(imageName, resourceGroup, fromVhdUri, region);
            return customImage.id();
        }
    }

    private PagedList<VirtualMachineCustomImage> getCustomImageList(String resourceGroup) {
        return azure.virtualMachineCustomImages()
                .listByResourceGroup(resourceGroup);
    }

    private VirtualMachineCustomImage createCustomImage(String imageName, String resourceGroup, String fromVhdUri, String region) {
        LOGGER.info("create custom image from '{}' with name '{}' into '{}' resource group (Region: {})",
                fromVhdUri, imageName, resourceGroup, region);
        if (!azure.resourceGroups().checkExistence(resourceGroup)) {
            azure.resourceGroups().define(resourceGroup).withRegion(region).create();
        }
        return azure.virtualMachineCustomImages()
                .define(imageName)
                .withRegion(region)
                .withExistingResourceGroup(resourceGroup)
                .withLinuxFromVhd(fromVhdUri, OperatingSystemStateTypes.GENERALIZED)
                .create();
    }

    public PagedList<PublicIPAddress> getPublicIpAddresses(String resourceGroup) {
        return azure.publicIPAddresses().listByResourceGroup(resourceGroup);
    }

    public PublicIPAddress getPublicIpAddressById(String ipId) {
        return azure.publicIPAddresses().getById(ipId);
    }

    public void deleteNetworkInterface(String resourceGroup, String networkInterfaceName) {
        azure.networkInterfaces().deleteByResourceGroup(resourceGroup, networkInterfaceName);
    }

    public NetworkInterface getNetworkInterface(String resourceGroup, String networkInterfaceName) {
        return azure.networkInterfaces().getByResourceGroup(resourceGroup, networkInterfaceName);
    }

    public NetworkInterface getNetworkInterfaceById(String networkInterfaceId) {
        return azure.networkInterfaces().getById(networkInterfaceId);
    }

    public NetworkInterfaces getNetworkInterfaces(String resourceGroup) {
        return azure.networkInterfaces();
    }

    public Subnet getSubnetProperties(String resourceGroup, String virtualNetwork, String subnet) {
        return azure.networks().getByResourceGroup(resourceGroup, virtualNetwork).subnets().get(subnet);
    }

    public NetworkSecurityGroup getSecurityGroupProperties(String resourceGroup, String securityGroup) {
        return azure.networkSecurityGroups().getByResourceGroup(resourceGroup, securityGroup);
    }

    public LoadBalancer getLoadBalancer(String name, String loadBalancerName) {
        return azure.loadBalancers().getByResourceGroup(name, loadBalancerName);
    }

    public List<String> getLoadBalancerIps(String name, String loadBalancerName) {
        List<String> ipList = new ArrayList<String>();
        List<String> publicIpAddressIds = getLoadBalancer(name, loadBalancerName).publicIPAddressIds();
        for (String publicIpAddressId : publicIpAddressIds) {
            PublicIPAddress publicIpAddress = getPublicIpAddressById(publicIpAddressId);
            ipList.add(publicIpAddress.ipAddress());
        }
        return ipList;
    }

}
