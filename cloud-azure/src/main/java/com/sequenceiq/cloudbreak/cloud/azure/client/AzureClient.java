package com.sequenceiq.cloudbreak.cloud.azure.client;

import static java.util.Collections.emptyMap;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.microsoft.aad.adal4j.AuthenticationException;
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
import com.microsoft.azure.management.compute.VirtualMachineSize;
import com.microsoft.azure.management.network.LoadBalancer;
import com.microsoft.azure.management.network.Network;
import com.microsoft.azure.management.network.NetworkInterface;
import com.microsoft.azure.management.network.NetworkInterfaces;
import com.microsoft.azure.management.network.NetworkSecurityGroup;
import com.microsoft.azure.management.network.NetworkSecurityGroups;
import com.microsoft.azure.management.network.PublicIPAddress;
import com.microsoft.azure.management.network.Subnet;
import com.microsoft.azure.management.resources.Deployment;
import com.microsoft.azure.management.resources.DeploymentMode;
import com.microsoft.azure.management.resources.DeploymentOperations;
import com.microsoft.azure.management.resources.Deployments;
import com.microsoft.azure.management.resources.ResourceGroup;
import com.microsoft.azure.management.resources.ResourceGroups;
import com.microsoft.azure.management.resources.fluentcore.arm.Region;
import com.microsoft.azure.management.resources.fluentcore.arm.models.HasId;
import com.microsoft.azure.management.storage.ProvisioningState;
import com.microsoft.azure.management.storage.SkuName;
import com.microsoft.azure.management.storage.StorageAccount;
import com.microsoft.azure.management.storage.StorageAccount.DefinitionStages.WithCreate;
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
import com.sequenceiq.cloudbreak.client.ProviderAuthenticationFailedException;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.common.type.ResourceType;

import okhttp3.JavaNetAuthenticator;

public class AzureClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureClient.class);

    private Azure azure;

    private final String tenantId;

    private final String clientId;

    private final String secretKey;

    private final String subscriptionId;

    public AzureClient(String tenantId, String clientId, String secretKey, String subscriptionId, LogLevel logLevel) {
        this.tenantId = tenantId;
        this.clientId = clientId;
        this.secretKey = secretKey;
        this.subscriptionId = subscriptionId;
        connect(logLevel);
    }

    private void connect(LogLevel logLevel) {
        AzureTokenCredentials creds = new ApplicationTokenCredentials(clientId, tenantId, secretKey, AzureEnvironment.AZURE)
                .withDefaultSubscriptionId(subscriptionId);
        azure = Azure
                .configure()
                .withProxyAuthenticator(new JavaNetAuthenticator())
                .withLogLevel(logLevel)
                .authenticate(creds)
                .withSubscription(subscriptionId);
    }

    private <T> T handleAuthException(Supplier<T> function) {
        try {
            return function.get();
        } catch (RuntimeException e) {
            if (ExceptionUtils.indexOfThrowable(e, AuthenticationException.class) != -1) {
                throw new ProviderAuthenticationFailedException(e);
            } else {
                throw e;
            }
        }
    }

    private void handleAuthException(Runnable function) {
        try {
            function.run();
        } catch (RuntimeException e) {
            if (ExceptionUtils.indexOfThrowable(e, AuthenticationException.class) != -1) {
                throw new ProviderAuthenticationFailedException(e);
            } else {
                throw e;
            }
        }
    }

    public ResourceGroup getResourceGroup(String name) {
        return handleAuthException(() -> azure.resourceGroups().getByName(name));
    }

    public ResourceGroups getResourceGroups() {
        return handleAuthException(() -> azure.resourceGroups());
    }

    public PagedList<Network> getNetworks() {
        return handleAuthException(() -> azure.networks().list());
    }

    public boolean resourceGroupExists(String name) {
        return getResourceGroups().contain(name);
    }

    public void deleteResourceGroup(String name) {
        azure.resourceGroups().deleteByName(name);
    }

    public ResourceGroup createResourceGroup(String name, String region, Map<String, String> tags, Map<String, String> costFollowerTags) {
        Map<String, String> resultTags = new HashMap<>();
        for (Entry<String, String> entry : costFollowerTags.entrySet()) {
            resultTags.put(entry.getKey(), entry.getValue());
        }
        resultTags.putAll(tags);
        return handleAuthException(() ->
                azure.resourceGroups().define(name)
                        .withRegion(region)
                        .withTags(resultTags)
                        .create()
        );
    }

    public Deployment createTemplateDeployment(String resourceGroupName, String deploymentName, String templateContent, String parameterContent) {
        return handleAuthException(() -> {
            try {
                return azure.deployments().define(deploymentName)
                        .withExistingResourceGroup(resourceGroupName)
                        .withTemplate(templateContent)
                        .withParameters(parameterContent)
                        .withMode(DeploymentMode.INCREMENTAL)
                        .create();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public boolean templateDeploymentExists(String resourceGroupName, String deploymentName) {
        return handleAuthException(() -> azure.deployments().checkExistence(resourceGroupName, deploymentName));
    }

    public void deleteTemplateDeployment(String resourceGroupName, String deploymentName) {
        handleAuthException(() -> azure.deployments().deleteByResourceGroup(resourceGroupName, deploymentName));
    }

    public Deployment getTemplateDeployment(String resourceGroupName, String deploymentName) {
        return handleAuthException(() -> azure.deployments().getByResourceGroup(resourceGroupName, deploymentName));
    }

    public DeploymentOperations getTemplateDeploymentOperations(String resourceGroupName, String deploymentName) {
        return handleAuthException(() -> azure.deployments().getByResourceGroup(resourceGroupName, deploymentName).deploymentOperations());
    }

    public void cancelTemplateDeployments(String resourceGroupName, String deploymentName) {
        handleAuthException(() -> azure.deployments().getByResourceGroup(resourceGroupName, deploymentName).cancel());
    }

    public Deployments getTemplateDeployments(String resourceGroupName) {
        return handleAuthException(() -> azure.deployments());
    }

    public StorageAccounts getStorageAccounts() {
        return handleAuthException(() -> azure.storageAccounts());
    }

    public PagedList<StorageAccount> getStorageAccountsForResourceGroup(String resourceGroup) {
        return handleAuthException(() -> azure.storageAccounts().listByResourceGroup(resourceGroup));
    }

    public void deleteStorageAccount(String resourceGroup, String storageName) {
        handleAuthException(() -> azure.storageAccounts().deleteByResourceGroup(resourceGroup, storageName));
    }

    public StorageAccount createStorageAccount(String resourceGroup, String storageName, String storageLocation, SkuName accType, Boolean encryted,
            Map<String, String> tags, Map<String, String> costFollowerTags) {
        Map<String, String> resultTags = new HashMap<>();
        for (Entry<String, String> entry : costFollowerTags.entrySet()) {
            resultTags.put(entry.getKey(), entry.getValue());
        }
        resultTags.putAll(tags);
        return handleAuthException(() -> {
            WithCreate withCreate = azure.storageAccounts()
                    .define(storageName)
                    .withRegion(storageLocation)
                    .withExistingResourceGroup(resourceGroup)
                    .withTags(resultTags)
                    .withSku(accType);
            if (encryted) {
                withCreate.withEncryption();
            }

            return withCreate.create();
        });
    }

    public List<StorageAccountKey> getStorageAccountKeys(String resourceGroup, String storageName) {
        return getStorageAccountByGroup(resourceGroup, storageName).getKeys();
    }

    public ProvisioningState getStorageStatus(String resourceGroup, String storageName) {
        return getStorageAccountByGroup(resourceGroup, storageName).provisioningState();
    }

    public StorageAccount getStorageAccountByGroup(String resourceGroup, String storageName) {
        return handleAuthException(() -> azure.storageAccounts().getByResourceGroup(resourceGroup, storageName));
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

    public void deleteManagedDisk(String id) {
        LOGGER.debug("delete managed disk: id={}", id);
        handleAuthException(() -> azure.disks().deleteById(id));
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
            CloudPageBlob cloudPageBlob = container.getPageBlobReference(sourceBlob.substring(sourceBlob.lastIndexOf('/') + 1));
            String copyId = cloudPageBlob.startCopy(new URI(sourceBlob));
            LOGGER.info("image copy started, copy id: {}", copyId);
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
            CloudPageBlob cloudPageBlob = container.getPageBlobReference(sourceBlob.substring(sourceBlob.lastIndexOf('/') + 1));
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
        return handleAuthException(() -> azure.virtualMachines().listByResourceGroup(resourceGroup));
    }

    public VirtualMachine getVirtualMachine(String resourceGroup, String vmName) {
        return handleAuthException(() -> azure.virtualMachines().getByResourceGroup(resourceGroup, vmName));
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
        return handleAuthException(() -> azure.availabilitySets().getByResourceGroup(resourceGroup, asName));
    }

    public void deleteAvailabilitySet(String resourceGroup, String asName) {
        handleAuthException(() -> azure.availabilitySets().deleteByResourceGroup(resourceGroup, asName));
    }

    public void deallocateVirtualMachine(String resourceGroup, String vmName) {
        handleAuthException(() -> azure.virtualMachines().deallocate(resourceGroup, vmName));
    }

    public boolean isVirtualMachineExists(String resourceGroup, String vmName) {
        return handleAuthException(() -> {
            Optional<VirtualMachine> vm = azure.virtualMachines().listByResourceGroup(resourceGroup).stream()
                    .filter(virtualMachine -> vmName.equals(virtualMachine.name()))
                    .findFirst();
            return vm.isPresent();
        });
    }

    public void deleteVirtualMachine(String resourceGroup, String vmName) {
        String id = getVirtualMachine(resourceGroup, vmName).id();
        handleAuthException(() -> azure.virtualMachines().deleteById(id));
    }

    public void startVirtualMachine(String resourceGroup, String vmName) {
        handleAuthException(() -> azure.virtualMachines().start(resourceGroup, vmName));
    }

    public void stopVirtualMachine(String resourceGroup, String vmName) {
        handleAuthException(() -> azure.virtualMachines().powerOff(resourceGroup, vmName));
    }

    public void deletePublicIpAddressByName(String resourceGroup, String ipName) {
        String id = getPublicIpAddress(resourceGroup, ipName).id();
        handleAuthException(() -> azure.publicIPAddresses().deleteById(id));
    }

    public void deletePublicIpAddressById(String ipId) {
        handleAuthException(() -> azure.publicIPAddresses().deleteById(ipId));
    }

    public HasId getPublicIpAddress(String resourceGroup, String ipName) {
        return handleAuthException(() -> azure.publicIPAddresses().getByResourceGroup(resourceGroup, ipName));
    }

    public String getCustomImageId(String resourceGroup, String fromVhdUri, String region) {
        String vhdName = fromVhdUri.substring(fromVhdUri.lastIndexOf('/') + 1);
        String imageName = vhdName + '-' + region.toLowerCase().replaceAll("\\s", "");
        PagedList<VirtualMachineCustomImage> customImageList = getCustomImageList(resourceGroup);
        Optional<VirtualMachineCustomImage> virtualMachineCustomImage = customImageList.stream()
                .filter(customImage -> customImage.name().equals(imageName) && customImage.region().label().equals(region)).findFirst();
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
        return handleAuthException(() -> azure.virtualMachineCustomImages().listByResourceGroup(resourceGroup));
    }

    private VirtualMachineCustomImage createCustomImage(String imageName, String resourceGroup, String fromVhdUri, String region) {
        return handleAuthException(() -> {
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
        });
    }

    public PagedList<PublicIPAddress> getPublicIpAddresses(String resourceGroup) {
        return handleAuthException(() -> azure.publicIPAddresses().listByResourceGroup(resourceGroup));
    }

    public PublicIPAddress getPublicIpAddressById(String ipId) {
        return handleAuthException(() -> azure.publicIPAddresses().getById(ipId));
    }

    public void deleteNetworkInterface(String resourceGroup, String networkInterfaceName) {
        handleAuthException(() -> azure.networkInterfaces().deleteByResourceGroup(resourceGroup, networkInterfaceName));
    }

    public NetworkInterface getNetworkInterface(String resourceGroup, String networkInterfaceName) {
        return handleAuthException(() -> azure.networkInterfaces().getByResourceGroup(resourceGroup, networkInterfaceName));
    }

    public NetworkInterface getNetworkInterfaceById(String networkInterfaceId) {
        return handleAuthException(() -> azure.networkInterfaces().getById(networkInterfaceId));
    }

    public NetworkInterfaces getNetworkInterfaces() {
        return handleAuthException(() -> azure.networkInterfaces());
    }

    public Subnet getSubnetProperties(String resourceGroup, String virtualNetwork, String subnet) {
        return handleAuthException(() -> {
            Network networkByResourceGroup = getNetworkByResourceGroup(resourceGroup, virtualNetwork);
            return networkByResourceGroup == null ? null : networkByResourceGroup.subnets().get(subnet);
        });
    }

    private Network getNetworkByResourceGroup(String resourceGroup, String virtualNetwork) {
        return azure.networks().getByResourceGroup(resourceGroup, virtualNetwork);
    }

    public Map<String, Subnet> getSubnets(String resourceGroup, String virtualNetwork) {
        return handleAuthException(() -> {
            Network network = getNetworkByResourceGroup(resourceGroup, virtualNetwork);
            return network == null ? emptyMap() : network.subnets();
        });
    }

    public NetworkSecurityGroup getSecurityGroupProperties(String resourceGroup, String securityGroup) {
        return handleAuthException(() -> azure.networkSecurityGroups().getByResourceGroup(resourceGroup, securityGroup));
    }

    public Set<VirtualMachineSize> getVmTypes(String region) throws ProviderAuthenticationFailedException {
        return handleAuthException(() -> {
            Set<VirtualMachineSize> resultList = new HashSet<>();
            if (region == null) {
                for (Region tmpRegion : Region.values()) {
                    PagedList<VirtualMachineSize> virtualMachineSizes =
                            azure.virtualMachines().sizes().listByRegion(Region.findByLabelOrName(tmpRegion.label()));
                    getAllElement(virtualMachineSizes, resultList);
                }
            }
            PagedList<VirtualMachineSize> virtualMachineSizes = azure.virtualMachines().sizes().listByRegion(Region.findByLabelOrName(region));
            getAllElement(virtualMachineSizes, resultList);
            return resultList;
        });
    }

    public Iterable<Region> getRegion(com.sequenceiq.cloudbreak.cloud.model.Region region) {
        Collection<Region> resultList = new HashSet<>();
        for (Region tmpRegion : Region.values()) {
            if (region == null || Strings.isNullOrEmpty(region.value())
                    || tmpRegion.name().equals(region.value()) || tmpRegion.label().equals(region.value())) {
                resultList.add(tmpRegion);
            }
        }
        return resultList;
    }

    public void collectAndSaveNetworkAndSubnet(String resourceGroupName, String virtualNetwork, PersistenceNotifier notifier, CloudContext cloudContext) {
        Optional<Subnet> first = getSubnets(resourceGroupName, virtualNetwork).values().stream().findFirst();
        if (first.isPresent()) {
            Subnet subnet = first.get();
            String subnetName = subnet.name();
            String networkName = subnet.parent().name();

            notifier.notifyAllocation(CloudResource.builder().name(networkName).type(ResourceType.AZURE_NETWORK).build(), cloudContext);
            notifier.notifyAllocation(CloudResource.builder().name(subnetName).type(ResourceType.AZURE_SUBNET).build(), cloudContext);
        }
    }

    private Set<VirtualMachineSize> getAllElement(Collection<VirtualMachineSize> virtualMachineSizes, Set<VirtualMachineSize> resultList) {
        resultList.addAll(virtualMachineSizes);
        return resultList;
    }

    public NetworkSecurityGroups getSecurityGroups() {
        return handleAuthException(() -> azure.networkSecurityGroups());
    }

    public LoadBalancer getLoadBalancer(String resourceGroupName, String loadBalancerName) {
        return handleAuthException(() -> azure.loadBalancers().getByResourceGroup(resourceGroupName, loadBalancerName));
    }

    public List<String> getLoadBalancerIps(String resourceGroupName, String loadBalancerName) {
        List<String> ipList = new ArrayList<>();
        List<String> publicIpAddressIds = getLoadBalancer(resourceGroupName, loadBalancerName).publicIPAddressIds();
        for (String publicIpAddressId : publicIpAddressIds) {
            PublicIPAddress publicIpAddress = getPublicIpAddressById(publicIpAddressId);
            ipList.add(publicIpAddress.ipAddress());
        }
        return ipList;
    }

}
