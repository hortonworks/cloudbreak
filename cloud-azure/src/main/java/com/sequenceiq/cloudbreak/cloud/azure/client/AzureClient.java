package com.sequenceiq.cloudbreak.cloud.azure.client;

import static java.util.Collections.emptyMap;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.microsoft.aad.adal4j.AuthenticationException;
import com.microsoft.azure.PagedList;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.compute.AvailabilitySet;
import com.microsoft.azure.management.compute.Disk;
import com.microsoft.azure.management.compute.DiskSkuTypes;
import com.microsoft.azure.management.compute.DiskStorageAccountTypes;
import com.microsoft.azure.management.compute.OperatingSystemStateTypes;
import com.microsoft.azure.management.compute.PowerState;
import com.microsoft.azure.management.compute.VirtualMachine;
import com.microsoft.azure.management.compute.VirtualMachineCustomImage;
import com.microsoft.azure.management.compute.VirtualMachineDataDisk;
import com.microsoft.azure.management.compute.VirtualMachineInstanceView;
import com.microsoft.azure.management.compute.VirtualMachineSize;
import com.microsoft.azure.management.graphrbac.RoleAssignment;
import com.microsoft.azure.management.graphrbac.RoleAssignments;
import com.microsoft.azure.management.graphrbac.implementation.RoleAssignmentInner;
import com.microsoft.azure.management.msi.Identity;
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
import com.microsoft.azure.management.resources.ResourceGroup;
import com.microsoft.azure.management.resources.ResourceGroups;
import com.microsoft.azure.management.resources.Subscription;
import com.microsoft.azure.management.resources.fluentcore.arm.AvailabilityZoneId;
import com.microsoft.azure.management.resources.fluentcore.arm.Region;
import com.microsoft.azure.management.resources.fluentcore.arm.models.HasId;
import com.microsoft.azure.management.storage.Kind;
import com.microsoft.azure.management.storage.ProvisioningState;
import com.microsoft.azure.management.storage.StorageAccount;
import com.microsoft.azure.management.storage.StorageAccount.DefinitionStages.WithCreate;
import com.microsoft.azure.management.storage.StorageAccountKey;
import com.microsoft.azure.management.storage.StorageAccountSkuType;
import com.microsoft.azure.management.storage.StorageAccounts;
import com.microsoft.azure.management.storage.implementation.StorageAccountInner;
import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import com.microsoft.azure.storage.blob.CloudPageBlob;
import com.microsoft.azure.storage.blob.CopyState;
import com.microsoft.azure.storage.blob.ListBlobItem;
import com.sequenceiq.cloudbreak.client.ProviderAuthenticationFailedException;
import com.sequenceiq.cloudbreak.cloud.azure.AzureDiskType;
import com.sequenceiq.cloudbreak.cloud.azure.util.CustomVMImageNameProvider;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;

import rx.Completable;
import rx.Observable;

public class AzureClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureClient.class);

    private final Azure azure;

    private final AzureClientCredentials azureClientCredentials;

    public AzureClient(AzureClientCredentials azureClientCredentials) {
        this.azureClientCredentials = azureClientCredentials;
        azure = azureClientCredentials.getAzure();
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

    public Optional<String> getRefreshToken() {
        return azureClientCredentials.getRefreshToken();
    }

    public ResourceGroup getResourceGroup(String name) {
        return handleAuthException(() -> azure.resourceGroups().getByName(name));
    }

    public ResourceGroups getResourceGroups() {
        return handleAuthException(azure::resourceGroups);
    }

    public PagedList<Network> getNetworks() {
        return handleAuthException(() -> azure.networks().list());
    }

    public boolean resourceGroupExists(String name) {
        return getResourceGroups().contain(name);
    }

    public boolean isResourceGroupEmpty(String name) {
        return handleAuthException(() -> azure.genericResources().listByResourceGroup(name).isEmpty());
    }

    public void deleteResourceGroup(String name) {
        handleAuthException(() -> azure.resourceGroups().deleteByName(name));
    }

    public ResourceGroup createResourceGroup(String name, String region, Map<String, String> tags) {
        return handleAuthException(() ->
                azure.resourceGroups().define(name)
                        .withRegion(region)
                        .withTags(tags)
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

    public StorageAccounts getStorageAccounts() {
        return handleAuthException(azure::storageAccounts);
    }

    public void deleteStorageAccount(String resourceGroup, String storageName) {
        handleAuthException(() -> azure.storageAccounts().deleteByResourceGroup(resourceGroup, storageName));
    }

    public StorageAccount createStorageAccount(String resourceGroup, String storageName, String storageLocation, StorageAccountSkuType accType, Boolean encryted,
            Map<String, String> tags) {
        return handleAuthException(() -> {
            WithCreate withCreate = azure.storageAccounts()
                    .define(storageName)
                    .withRegion(storageLocation)
                    .withExistingResourceGroup(resourceGroup)
                    .withTags(tags)
                    .withSku(accType)
                    .withOnlyHttpsTraffic();
            if (encryted) {
                withCreate.withBlobEncryption();
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

    public Optional<StorageAccount> getStorageAccount(String storageName, Kind accountKind) {
        return handleAuthException(() -> azure.storageAccounts().list().stream()
                .filter(account -> account.kind().equals(accountKind)
                        && account.name().equalsIgnoreCase(storageName))
                .findAny());
    }

    public Optional<StorageAccountInner> getStorageAccountBySubscription(String storageName, String subscriptionId, Kind accountKind) {
        return
                azure.storageAccounts().manager().inner().withSubscriptionId(subscriptionId).storageAccounts().list().stream()
                        .filter(account -> account.kind().equals(accountKind) && account.name().equalsIgnoreCase(storageName))
                        .findAny();
    }

    public void deleteContainerInStorage(String resourceGroup, String storageName, String containerName) {
        LOGGER.debug("delete container: RG={}, storageName={}, containerName={}", resourceGroup, storageName, containerName);
        CloudBlobContainer container = getBlobContainer(resourceGroup, storageName, containerName);
        try {
            boolean existed = container.deleteIfExists();
            LOGGER.debug("Is container existed: " + existed);
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
            LOGGER.debug("Blob was deleted: " + wasDeleted);
        } catch (URISyntaxException e) {
            throw new CloudConnectorException("can't delete blob in storage container, URI is not valid", e);
        } catch (StorageException e) {
            throw new CloudConnectorException("can't delete blob in storage container, storage service error occurred", e);
        }
    }

    public Disk createManagedDisk(String diskName, int diskSize, AzureDiskType diskType, String region, String resourceGroupName, Map<String, String> tags) {
        LOGGER.debug("create managed disk with name={}", diskName);
        return azure.disks().define(diskName)
                .withRegion(Region.findByLabelOrName(region))
                .withExistingResourceGroup(resourceGroupName)
                .withData()
                .withSizeInGB(diskSize)
                .withTags(tags)
                .withSku(convertAzureDiskTypeToDiskSkuTypes(diskType))
                .create();
    }

    public void attachDiskToVm(Disk disk, VirtualMachine vm) {
        LOGGER.debug("attach managed disk {} to VM {}", disk, vm);
        vm.update().withExistingDataDisk(disk).apply();
    }

    public void detachDiskFromVm(String id, VirtualMachine vm) {
        LOGGER.debug("detach managed disk with id={}", id);
        VirtualMachineDataDisk dataDisk = vm.dataDisks()
                .values()
                .stream()
                .filter(virtualMachineDataDisk -> virtualMachineDataDisk.id().equals(id))
                .findFirst()
                .orElseThrow(() -> new CloudConnectorException(String.format("Virtual machine does not have attached data disk with id %s", id)));
        int lun = dataDisk.lun();
        LOGGER.debug("detaches a managed data disk with LUN {} from the virtual machine {}", lun, vm);
        vm.update().withoutDataDisk(lun).apply();
    }

    public PagedList<Disk> listDisksByResourceGroup(String resourceGroupName) {
        return azure.disks().listByResourceGroup(resourceGroupName);
    }

    public Disk getDiskById(String id) {
        return azure.disks().getById(id);
    }

    public Disk getDiskByName(String resourceGroupName, String diskName) {
        return azure.disks().getByResourceGroup(resourceGroupName, diskName);
    }

    public Completable deleteManagedDiskAsync(String id) {
        LOGGER.debug("delete managed disk: id={}", id);
        return handleAuthException(() -> azure.disks().deleteByIdAsync(id));
    }

    public DiskSkuTypes convertAzureDiskTypeToDiskSkuTypes(AzureDiskType diskType) {
        return Objects.nonNull(diskType) ? DiskSkuTypes.fromStorageAccountType(DiskStorageAccountTypes.fromString(diskType.value())) : DiskSkuTypes.STANDARD_LRS;
    }

    public void createContainerInStorage(String resourceGroup, String storageName, String containerName) {
        LOGGER.debug("create container: RG={}, storageName={}, containerName={}", resourceGroup, storageName, containerName);
        CloudBlobContainer container = getBlobContainer(resourceGroup, storageName, containerName);
        try {
            boolean created = container.createIfNotExists();
            LOGGER.debug("Container created: " + created);
        } catch (StorageException e) {
            throw new CloudConnectorException("can't create container in storage, storage service error occurred", e);
        }
    }

    public void copyImageBlobInStorageContainer(String resourceGroup, String storageName, String containerName, String sourceBlob) {
        LOGGER.debug("copy image in storage container: RG={}, storageName={}, containerName={}, sourceBlob={}",
                resourceGroup, storageName, containerName, sourceBlob);
        CloudBlobContainer container = getBlobContainer(resourceGroup, storageName, containerName);
        try {
            CloudPageBlob cloudPageBlob = container.getPageBlobReference(sourceBlob.substring(sourceBlob.lastIndexOf('/') + 1));
            String copyId = cloudPageBlob.startCopy(new URI(sourceBlob));
            LOGGER.debug("Image copy started, copy id: {}", copyId);
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
        String storageConnectionString = String.format("DefaultEndpointsProtocol=https;AccountName=%s;AccountKey=%s", storageName, keys.get(0).value());
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

    public Observable<VirtualMachine> getVirtualMachineAsync(String resourceGroup, String vmName) {
        return handleAuthException(() -> azure.virtualMachines().getByResourceGroupAsync(resourceGroup, vmName));
    }

    public PowerState getVirtualMachinePowerState(String resourceGroup, String vmName) {
        return getVirtualMachine(resourceGroup, vmName).powerState();
    }

    public VirtualMachineInstanceView getVirtualMachineInstanceView(String resourceGroup, String vmName) {
        return getVirtualMachine(resourceGroup, vmName).instanceView();
    }

    public Set<AvailabilityZoneId> getAvailabilityZone(String resourceGroup, String vmName) {
        return handleAuthException(() -> {
            VirtualMachine vm = azure.virtualMachines().getByResourceGroup(resourceGroup, vmName);
            return Objects.nonNull(vm) ? vm.availabilityZones() : Collections.emptySet();
        });
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

    public Completable deallocateVirtualMachineAsync(String resourceGroup, String vmName) {
        return handleAuthException(() -> azure.virtualMachines().deallocateAsync(resourceGroup, vmName));
    }

    public boolean isVirtualMachineExists(String resourceGroup, String vmName) {
        return handleAuthException(() -> {
            Optional<VirtualMachine> vm = azure.virtualMachines().listByResourceGroup(resourceGroup).stream()
                    .filter(virtualMachine -> vmName.equals(virtualMachine.name()))
                    .findFirst();
            return vm.isPresent();
        });
    }

    public Completable deleteVirtualMachine(String resourceGroup, String vmName) {
        return handleAuthException(() -> azure.virtualMachines().deleteByResourceGroupAsync(resourceGroup, vmName));
    }

    public Completable startVirtualMachineAsync(String resourceGroup, String vmName) {
        return handleAuthException(() -> azure.virtualMachines().startAsync(resourceGroup, vmName));
    }

    public Completable rebootVirtualMachineAsync(String resourceGroup, String vmName) {
        return handleAuthException(() -> azure.virtualMachines().restartAsync(resourceGroup, vmName));
    }

    public void stopVirtualMachine(String resourceGroup, String vmName) {
        handleAuthException(() -> azure.virtualMachines().powerOff(resourceGroup, vmName));
    }

    public Completable deletePublicIpAddressByNameAsync(String resourceGroup, String ipName) {
        return handleAuthException(() -> azure.publicIPAddresses().deleteByResourceGroupAsync(resourceGroup, ipName));
    }

    public void deletePublicIpAddressById(String ipId) {
        handleAuthException(() -> azure.publicIPAddresses().deleteById(ipId));
    }

    public HasId getPublicIpAddress(String resourceGroup, String ipName) {
        return handleAuthException(() -> azure.publicIPAddresses().getByResourceGroup(resourceGroup, ipName));
    }

    public String getCustomImageId(String resourceGroup, String fromVhdUri, String region) {
        String vhdName = fromVhdUri.substring(fromVhdUri.lastIndexOf('/') + 1);
        String imageName = CustomVMImageNameProvider.get(region, vhdName);
        PagedList<VirtualMachineCustomImage> customImageList = getCustomImageList(resourceGroup);
        Optional<VirtualMachineCustomImage> virtualMachineCustomImage = customImageList.stream()
                .filter(customImage -> customImage.name().equals(imageName)
                        && (customImage.region().name().equals(region)
                        || customImage.region().label().equals(region))).findFirst();
        if (virtualMachineCustomImage.isPresent()) {
            LOGGER.debug("Custom image found in '{}' resource group with name '{}'", resourceGroup, imageName);
            return virtualMachineCustomImage.get().id();
        } else {
            LOGGER.debug("Custom image NOT found in '{}' resource group with name '{}'", resourceGroup, imageName);
            VirtualMachineCustomImage customImage = createCustomImage(imageName, resourceGroup, fromVhdUri, region);
            return customImage.id();
        }
    }

    private PagedList<VirtualMachineCustomImage> getCustomImageList(String resourceGroup) {
        return handleAuthException(() -> azure.virtualMachineCustomImages().listByResourceGroup(resourceGroup));
    }

    private VirtualMachineCustomImage createCustomImage(String imageName, String resourceGroup, String fromVhdUri, String region) {
        return handleAuthException(() -> {
            LOGGER.info("check the existence of resource group '{}', creating if it doesn't exist on Azure side", resourceGroup);
            if (!azure.resourceGroups().contain(resourceGroup)) {
                azure.resourceGroups().define(resourceGroup).withRegion(region).create();
            }
            LOGGER.debug("Create custom image from '{}' with name '{}' into '{}' resource group (Region: {})",
                    fromVhdUri, imageName, resourceGroup, region);
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

    public Completable deleteNetworkInterfaceAsync(String resourceGroup, String networkInterfaceName) {
        return handleAuthException(() -> azure.networkInterfaces().deleteByResourceGroupAsync(resourceGroup, networkInterfaceName));
    }

    public NetworkInterface getNetworkInterface(String resourceGroup, String networkInterfaceName) {
        return handleAuthException(() -> azure.networkInterfaces().getByResourceGroup(resourceGroup, networkInterfaceName));
    }

    public Observable<NetworkInterface> getNetworkInterfaceAsync(String resourceGroup, String networkInterfaceName) {
        return handleAuthException(() -> azure.networkInterfaces().getByResourceGroupAsync(resourceGroup, networkInterfaceName));
    }

    public NetworkInterface getNetworkInterfaceById(String networkInterfaceId) {
        return handleAuthException(() -> azure.networkInterfaces().getById(networkInterfaceId));
    }

    public NetworkInterfaces getNetworkInterfaces() {
        return handleAuthException(azure::networkInterfaces);
    }

    public Subnet getSubnetProperties(String resourceGroup, String virtualNetwork, String subnet) {
        return handleAuthException(() -> {
            Network networkByResourceGroup = getNetworkByResourceGroup(resourceGroup, virtualNetwork);
            return networkByResourceGroup == null ? null : networkByResourceGroup.subnets().get(subnet);
        });
    }

    public Network getNetworkByResourceGroup(String resourceGroup, String virtualNetwork) {
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

    public Collection<Region> getRegion(com.sequenceiq.cloudbreak.cloud.model.Region region) {
        Collection<Region> resultList = new HashSet<>();
        for (Region tmpRegion : Region.values()) {
            if (region == null || Strings.isNullOrEmpty(region.value())
                    || tmpRegion.name().equals(region.value()) || tmpRegion.label().equals(region.value())) {
                resultList.add(tmpRegion);
            }
        }
        return resultList;
    }

    private Set<VirtualMachineSize> getAllElement(Collection<VirtualMachineSize> virtualMachineSizes, Set<VirtualMachineSize> resultList) {
        resultList.addAll(virtualMachineSizes);
        return resultList;
    }

    public NetworkSecurityGroups getSecurityGroups() {
        return handleAuthException(azure::networkSecurityGroups);
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

    public PagedList<Identity> listIdentities() {
        return handleAuthException(() -> azure.identities().list());
    }

    public List<Identity> listIdentitiesByRegion(String region) {
        return listIdentities()
                .stream()
                .filter(identity -> identity.region().label().equalsIgnoreCase(region)
                        || identity.region().name().equalsIgnoreCase(region))
                .collect(Collectors.toList());
    }

    public List<Identity> filterIdentitiesByRoleAssignement(List<Identity> identityList, String roleAssignmentScope) {
        PagedList<RoleAssignment> roleAssignments = listRoleAssignmentsByScope(roleAssignmentScope);
        return identityList.stream().filter(
                identity -> roleAssignments.stream()
                        .anyMatch(roleAssignment -> roleAssignment.principalId() != null
                                && roleAssignment.principalId().equalsIgnoreCase(identity.principalId())))
                .collect(Collectors.toList());
    }

    public Identity getIdentityById(String id) {
        return handleAuthException(() -> azure.identities().getById(id));
    }

    public RoleAssignments getRoleAssignments() {
        return handleAuthException(() -> azure.identities().manager()).graphRbacManager().roleAssignments();
    }

    public PagedList<RoleAssignment> listRoleAssignmentsByScope(String scope) {
        return handleAuthException(() -> getRoleAssignments().listByScope(scope));
    }

    public PagedList<RoleAssignmentInner> listRoleAssignments() {
        return listRoleAssignmentsBySubscription(getCurrentSubscription().subscriptionId());
    }

    public PagedList<RoleAssignmentInner> listRoleAssignmentsBySubscription(String subscriptionId) {
        return handleAuthException(() ->
                getRoleAssignments().manager().roleInner().withSubscriptionId(subscriptionId).roleAssignments().list());
    }

    public PagedList<RoleAssignmentInner> listRoleAssignmentsByPrincipalId(String principalId) {
        return handleAuthException(() -> getRoleAssignments().inner().list(String.format("$filter=principalId eq %s", principalId)));
    }

    public boolean checkIdentityRoleAssignement(String identityId, String scopeId) {
        Identity identity = getIdentityById(identityId);
        PagedList<RoleAssignment> roleAssignments = listRoleAssignmentsByScope(scopeId);
        return roleAssignments.stream().anyMatch(roleAssignment -> roleAssignment.principalId() != null &&
                roleAssignment.principalId().equalsIgnoreCase(identity.principalId()));
    }

    public Subscription getCurrentSubscription() {
        return azure.getCurrentSubscription();
    }

    public PagedList<Subscription> listSubscriptions() {
        return azure.subscriptions().list();
    }

    public void deleteDatabaseServer(String databaseServerId) {
        handleAuthException(() -> azure.genericResources().deleteById(databaseServerId));
    }
}
