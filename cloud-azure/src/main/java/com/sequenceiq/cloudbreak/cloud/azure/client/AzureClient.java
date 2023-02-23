package com.sequenceiq.cloudbreak.cloud.azure.client;

import static com.azure.resourcemanager.compute.models.DiskSkuTypes.PREMIUM_LRS;
import static com.azure.resourcemanager.compute.models.DiskSkuTypes.STANDARD_LRS;
import static com.azure.resourcemanager.compute.models.DiskSkuTypes.STANDARD_SSD_LRS;
import static com.azure.resourcemanager.compute.models.DiskSkuTypes.ULTRA_SSD_LRS;
import static com.azure.resourcemanager.privatedns.models.ProvisioningState.SUCCEEDED;
import static com.sequenceiq.cloudbreak.util.Benchmark.measure;
import static java.util.Collections.emptyMap;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.azure.core.http.rest.PagedIterable;
import com.azure.core.management.Region;
import com.azure.core.management.exception.ManagementException;
import com.azure.resourcemanager.AzureResourceManager;
import com.azure.resourcemanager.authorization.fluent.models.RoleAssignmentInner;
import com.azure.resourcemanager.authorization.models.RoleAssignment;
import com.azure.resourcemanager.authorization.models.RoleAssignments;
import com.azure.resourcemanager.compute.ComputeManager;
import com.azure.resourcemanager.compute.fluent.models.DiskEncryptionSetInner;
import com.azure.resourcemanager.compute.fluent.models.DiskInner;
import com.azure.resourcemanager.compute.models.AvailabilitySet;
import com.azure.resourcemanager.compute.models.CachingTypes;
import com.azure.resourcemanager.compute.models.Disk;
import com.azure.resourcemanager.compute.models.DiskEncryptionSetIdentityType;
import com.azure.resourcemanager.compute.models.DiskEncryptionSetType;
import com.azure.resourcemanager.compute.models.DiskSkuTypes;
import com.azure.resourcemanager.compute.models.DiskStorageAccountTypes;
import com.azure.resourcemanager.compute.models.Encryption;
import com.azure.resourcemanager.compute.models.EncryptionSetIdentity;
import com.azure.resourcemanager.compute.models.KeyForDiskEncryptionSet;
import com.azure.resourcemanager.compute.models.OperatingSystemStateTypes;
import com.azure.resourcemanager.compute.models.SourceVault;
import com.azure.resourcemanager.compute.models.VirtualMachine;
import com.azure.resourcemanager.compute.models.VirtualMachineCustomImage;
import com.azure.resourcemanager.compute.models.VirtualMachineDataDisk;
import com.azure.resourcemanager.compute.models.VirtualMachineInstanceView;
import com.azure.resourcemanager.compute.models.VirtualMachineSize;
import com.azure.resourcemanager.keyvault.models.AccessPolicy;
import com.azure.resourcemanager.keyvault.models.AccessPolicyEntry;
import com.azure.resourcemanager.keyvault.models.KeyPermissions;
import com.azure.resourcemanager.keyvault.models.Vault;
import com.azure.resourcemanager.marketplaceordering.MarketplaceOrderingManager;
import com.azure.resourcemanager.marketplaceordering.models.AgreementTerms;
import com.azure.resourcemanager.msi.models.Identity;
import com.azure.resourcemanager.network.models.LoadBalancer;
import com.azure.resourcemanager.network.models.LoadBalancerFrontend;
import com.azure.resourcemanager.network.models.LoadBalancingRule;
import com.azure.resourcemanager.network.models.Network;
import com.azure.resourcemanager.network.models.NetworkInterface;
import com.azure.resourcemanager.network.models.NetworkInterfaces;
import com.azure.resourcemanager.network.models.NetworkSecurityGroups;
import com.azure.resourcemanager.network.models.PublicIpAddress;
import com.azure.resourcemanager.network.models.Subnet;
import com.azure.resourcemanager.privatedns.PrivateDnsZoneManager;
import com.azure.resourcemanager.privatedns.fluent.models.VirtualNetworkLinkInner;
import com.azure.resourcemanager.privatedns.models.PrivateDnsZone;
import com.azure.resourcemanager.privatedns.models.VirtualNetworkLinkState;
import com.azure.resourcemanager.resources.fluentcore.arm.AvailabilityZoneId;
import com.azure.resourcemanager.resources.fluentcore.model.HasInnerModel;
import com.azure.resourcemanager.resources.fluentcore.model.implementation.IndexableRefreshableWrapperImpl;
import com.azure.resourcemanager.resources.models.Deployment;
import com.azure.resourcemanager.resources.models.DeploymentMode;
import com.azure.resourcemanager.resources.models.DeploymentOperations;
import com.azure.resourcemanager.resources.models.ResourceGroup;
import com.azure.resourcemanager.resources.models.ResourceGroups;
import com.azure.resourcemanager.resources.models.Subscription;
import com.azure.resourcemanager.storage.fluent.models.StorageAccountInner;
import com.azure.resourcemanager.storage.models.Kind;
import com.azure.resourcemanager.storage.models.StorageAccount;
import com.azure.resourcemanager.storage.models.StorageAccountKey;
import com.azure.resourcemanager.storage.models.StorageAccounts;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobClientBuilder;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobContainerClientBuilder;
import com.azure.storage.blob.models.BlobItem;
import com.azure.storage.blob.models.BlobProperties;
import com.azure.storage.blob.models.BlobStorageException;
import com.azure.storage.blob.options.BlobBeginCopyOptions;
import com.azure.storage.common.StorageSharedKeyCredential;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.client.ProviderAuthenticationFailedException;
import com.sequenceiq.cloudbreak.cloud.azure.AzureDiskType;
import com.sequenceiq.cloudbreak.cloud.azure.AzureLoadBalancerFrontend;
import com.sequenceiq.cloudbreak.cloud.azure.AzurePrivateDnsZoneServiceEnum;
import com.sequenceiq.cloudbreak.cloud.azure.image.marketplace.AzureMarketplaceImage;
import com.sequenceiq.cloudbreak.cloud.azure.status.AzureStatusMapper;
import com.sequenceiq.cloudbreak.cloud.azure.util.AzureExceptionHandler;
import com.sequenceiq.cloudbreak.cloud.azure.util.RegionUtil;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.LoadBalancerType;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class AzureClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureClient.class);

    private static final int MAX_AZURE_MANAGED_DISK_SIZE_WITH_CACHE = 4095;

    private static final Pattern ENCRYPTION_KEY_URL_VAULT_NAME = Pattern.compile("https://([^.]+)\\.vault.*");

    private final AzureResourceManager azure;

    private final PrivateDnsZoneManager privateDnsZoneManager;

    private final MarketplaceOrderingManager marketplaceOrderingManager;

    private final AzureClientFactory azureClientFactory;

    private final AzureExceptionHandler azureExceptionHandler;

    private final ComputeManager computeManager;

    public AzureClient(AzureClientFactory azureClientCredentials, AzureExceptionHandler azureExceptionHandler) {
        this.azureClientFactory = azureClientCredentials;
        azure = azureClientCredentials.getAzureResourceManager();
        privateDnsZoneManager = azureClientCredentials.getPrivateDnsManager();
        marketplaceOrderingManager = azureClientCredentials.getMarketplaceOrderingManager();
        computeManager = azureClientCredentials.getComputeManager();
        this.azureExceptionHandler = azureExceptionHandler;
    }

    public AzureResourceManager getAzure() {
        return azure;
    }

    public ResourceGroup getResourceGroup(String name) {
        return handleException(() -> azure.resourceGroups().getByName(name));
    }

    public ResourceGroups getResourceGroups() {
        return handleException(azure::resourceGroups);
    }

    public AzureListResult<Network> getNetworks() {
        return handleException(() -> new AzureListResult<>(azure.networks().list()));
    }

    public boolean resourceGroupExists(String name) {
        try {
            return getResourceGroups().contain(name);
        } catch (ManagementException e) {
            if (azureExceptionHandler.isForbidden(e)) {
                LOGGER.info("Resource group {} does not exist or insufficient permission to access it, exception: {}", name, e);
                return false;
            }
            throw e;
        }
    }

    public void deleteResourceGroup(String name) {
        handleException(() -> azure.resourceGroups().deleteByName(name));
    }

    public ResourceGroup createResourceGroup(String name, String region, Map<String, String> tags) {
        return handleException(() -> azure.resourceGroups().define(name)
                .withRegion(region)
                .withTags(tags)
                .create());
    }

    public Deployment createTemplateDeployment(String resourceGroupName, String deploymentName, String templateContent, String parameterContent) {
        return handleException(() -> {
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
        return handleException(() -> azure.deployments().checkExistence(resourceGroupName, deploymentName));
    }

    public ResourceStatus getTemplateDeploymentStatus(String resourceGroupName, String deploymentName) {
        return handleException(() -> Optional.ofNullable(getTemplateDeployment(resourceGroupName, deploymentName)))
                .map(Deployment::provisioningState)
                .map(AzureStatusMapper::mapResourceStatus)
                .orElse(ResourceStatus.DELETED);
    }

    public CommonStatus getTemplateDeploymentCommonStatus(String resourceGroupName, String deploymentName) {
        return handleException(() -> Optional.ofNullable(getTemplateDeployment(resourceGroupName, deploymentName)))
                .map(Deployment::provisioningState)
                .map(AzureStatusMapper::mapCommonStatus)
                .orElse(CommonStatus.DETACHED);
    }

    public void deleteTemplateDeployment(String resourceGroupName, String deploymentName) {
        handleException(() -> azure.deployments().deleteByResourceGroup(resourceGroupName, deploymentName));
    }

    public Deployment getTemplateDeployment(String resourceGroupName, String deploymentName) {
        return handleException(() -> azure.deployments().getByResourceGroup(resourceGroupName, deploymentName));
    }

    public DeploymentOperations getTemplateDeploymentOperations(String resourceGroupName, String deploymentName) {
        return handleException(() -> getTemplateDeployment(resourceGroupName, deploymentName).deploymentOperations());
    }

    public StorageAccounts getStorageAccounts() {
        return handleException(azure::storageAccounts);
    }

    public StorageAccountKey getStorageAccountKey(String resourceGroup, String storageName) {
        List<StorageAccountKey> keys = getStorageAccountByGroup(resourceGroup, storageName).getKeys();
        if (CollectionUtils.isEmpty(keys)) {
            throw new CloudConnectorException("Not found access key for " + storageName + " storage account.");
        }
        return keys.get(0);
    }

    public StorageAccount getStorageAccountByGroup(String resourceGroup, String storageName) {
        return handleException(() -> azure.storageAccounts().getByResourceGroup(resourceGroup, storageName));
    }

    public Optional<StorageAccount> getStorageAccount(String storageName, Kind accountKind) {
        return handleException(() -> azure.storageAccounts().list().stream()
                .filter(account -> account.kind().equals(accountKind)
                        && account.name().equalsIgnoreCase(storageName))
                .findAny());
    }

    public Optional<StorageAccountInner> getStorageAccountBySubscription(String storageName, String subscriptionId, Kind accountKind) {
        return azureClientFactory.getAzureResourceManager(subscriptionId)
                .storageAccounts()
                .list()
                .stream()
                .filter(account -> account.kind().equals(accountKind) && account.name().equalsIgnoreCase(storageName))
                .map(HasInnerModel::innerModel)
                .findAny();
    }

    public Disk createManagedDisk(
            String diskName, int diskSize, AzureDiskType diskType, String region, String resourceGroupName, Map<String, String> tags,
            String diskEncryptionSetId) {
        LOGGER.debug("create managed disk with name={}", diskName);
        Disk.DefinitionStages.WithCreate withCreate = azure.disks().define(diskName)
                .withRegion(RegionUtil.findByLabelOrName(region))
                .withExistingResourceGroup(resourceGroupName)
                .withData()
                .withSizeInGB(diskSize)
                .withTags(tags)
                .withSku(convertAzureDiskTypeToDiskSkuTypes(diskType));
        setupDiskEncryptionWithDesIfNeeded(diskEncryptionSetId, withCreate);
        return withCreate.create();
    }

    @VisibleForTesting
    void setupDiskEncryptionWithDesIfNeeded(String diskEncryptionSetId, Disk.DefinitionStages.WithCreate withCreate) {
        if (!Strings.isNullOrEmpty(diskEncryptionSetId)) {
            // This is nasty. The DES setter is not exposed in WithCreate, so have to rely on the direct tweaking of the underlying DiskInner.
            Encryption encryption = new Encryption();
            encryption.withDiskEncryptionSetId(diskEncryptionSetId);
            // WithCreate is actually a DiskImpl instance, but that type is not visible.
            DiskInner inner = (DiskInner) ((IndexableRefreshableWrapperImpl) withCreate).innerModel();
            inner.withEncryption(encryption);
        }
    }

    public void attachDiskToVm(Disk disk, VirtualMachine vm) {
        LOGGER.debug("attach managed disk {} to VM {}", disk, vm);
        CachingTypes cachingTypes = CachingTypes.READ_WRITE;
        if (disk.sizeInGB() > MAX_AZURE_MANAGED_DISK_SIZE_WITH_CACHE) {
            cachingTypes = CachingTypes.NONE;
        } else if (ULTRA_SSD_LRS.equals(disk.sku())
                || PREMIUM_LRS.equals(disk.sku())
                || STANDARD_LRS.equals(disk.sku())
                || STANDARD_SSD_LRS.equals(disk.sku())) {
            cachingTypes = CachingTypes.READ_ONLY;
        }
        // This is needed because of bug https://github.com/Azure/azure-libraries-for-java/issues/632
        // It affects the VM-s launched from Azure Marketplace images
        vm.innerModel().withPlan(null);
        vm.update()
                .withExistingDataDisk(disk)
                .withDataDiskDefaultCachingType(cachingTypes)
                .apply();
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

    public AzureListResult<Disk> listDisksByResourceGroup(String resourceGroupName) {
        return AzureListResult.listByResourceGroup(azure.disks(), resourceGroupName);
    }

    public Disk getDiskById(String id) {
        return azure.disks().getById(id);
    }

    public Disk getDiskByName(String resourceGroupName, String diskName) {
        return handleException(() -> azure.disks().getByResourceGroup(resourceGroupName, diskName));
    }

    public Flux<String> deleteManagedDisksAsync(Collection<String> ids) {
        LOGGER.debug("delete managed disk: id={}", ids);
        return handleException(() -> azure.disks().deleteByIdsAsync(ids));
    }

    public Mono<Void> deleteManagedDiskAsync(String resourceGroup, String name) {
        return handleException(() -> azure.disks().deleteByResourceGroupAsync(resourceGroup, name));
    }

    public DiskSkuTypes convertAzureDiskTypeToDiskSkuTypes(AzureDiskType diskType) {
        return Objects.nonNull(diskType) ? DiskSkuTypes.fromStorageAccountType(DiskStorageAccountTypes.fromString(diskType.value()))
                : STANDARD_LRS;
    }

    public void createContainerInStorage(String resourceGroup, String storageName, String containerName) {
        try {
            LOGGER.debug("create container: RG={}, storageName={}, containerName={}", resourceGroup, storageName, containerName);
            boolean created = getBlobContainerClient(resourceGroup, storageName, containerName).createIfNotExists();
            LOGGER.debug("Container created: " + created);
        } catch (BlobStorageException e) {
            throw new CloudConnectorException("can't create container in storage, storage service error occurred", e);
        }
    }

    public VirtualMachineCustomImage findImage(String resourceGroup, String imageName) {
        LOGGER.debug("Searching custom image {} in resource group {}", imageName, resourceGroup);
        return azureExceptionHandler.handleException(() -> azure
                .virtualMachineCustomImages()
                .getByResourceGroup(resourceGroup, imageName));
    }

    public VirtualMachineCustomImage createImage(String imageName, String resourceGroup, String fromVhdUri, String region) {
        return handleException(() -> {
            LOGGER.info("check the existence of resource group '{}', creating if it doesn't exist on Azure side", resourceGroup);
            if (!azure.resourceGroups().contain(resourceGroup)) {
                LOGGER.info("Creating resource group: {}", resourceGroup);
                azure.resourceGroups()
                        .define(resourceGroup)
                        .withRegion(region)
                        .create();
            }
            LOGGER.debug("Create custom image from '{}' with name '{}' into '{}' resource group (Region: {})",
                    fromVhdUri, imageName, resourceGroup, region);
            return measure(() -> azure.virtualMachineCustomImages()
                            .define(imageName)
                            .withRegion(region)
                            .withExistingResourceGroup(resourceGroup)
                            .withLinuxFromVhd(fromVhdUri, OperatingSystemStateTypes.GENERALIZED)
                            .create(),
                    LOGGER, "Custom image has been created under {} ms with name {}", imageName);
        });
    }

    public AgreementTerms signImageConsent(AzureMarketplaceImage azureMarketplaceImage) {
        return marketplaceOrderingManager.marketplaceAgreements()
                .sign(azureMarketplaceImage.getPublisherId(), azureMarketplaceImage.getOfferId(), azureMarketplaceImage.getPlanId());
    }

    public void copyImageBlobInStorageContainer(String resourceGroup, String storageName, String containerName, String sourceBlob, String sourceBlobName) {
        try {
            LOGGER.debug("copy image in storage container: RG={}, storageName={}, containerName={}, sourceBlob={}",
                    resourceGroup, storageName, containerName, sourceBlob);
            String copyId = getBlobClient(resourceGroup, storageName, containerName, sourceBlobName)
                    .getPageBlobClient()
                    .beginCopy(new BlobBeginCopyOptions(sourceBlob))
                    .poll()
                    .getValue()
                    .getCopyId();
            LOGGER.debug("Image copy started, copy id: {}", copyId);
        } catch (BlobStorageException e) {
            throw new CloudConnectorException("can't copy image blob, storage service error occurred", e);
        }
    }

    public CopyState getCopyStatus(String resourceGroup, String storageName, String containerName, String sourceBlobName) {
        try {
            LOGGER.debug("get image copy status: RG={}, storageName={}, containerName={}, sourceBlob={}",
                    resourceGroup, storageName, containerName, sourceBlobName);
            BlobClient blobClient = getBlobClient(resourceGroup, storageName, containerName, sourceBlobName);
            BlobProperties blobProperties = blobClient.getProperties();
            CopyState copyState = CopyState.of(blobProperties.getCopyStatus(), blobProperties.getCopyProgress());
            LOGGER.info("Copy state: {}", copyState);
            return copyState;
        } catch (BlobStorageException e) {
            throw new CloudConnectorException("can't get copy status, storage service error occurred", e);
        }
    }

    public String getImageBlobUri(String resourceGroup, String storageName, String containerName, String vhdName) {
        try {
            return getBlobClient(resourceGroup, storageName, containerName, vhdName).getBlobUrl();
        } catch (BlobStorageException e) {
            throw new CloudConnectorException("can't get image blob uri, storage service error occurred", e);
        }
    }

    public List<BlobItem> listBlobInStorage(String resourceGroup, String storageName, String containerName) {
        try {
            return new AzureListResult<>(getBlobContainerClient(resourceGroup, storageName, containerName).listBlobs()).getAll();
        } catch (Exception e) {
            LOGGER.warn("Failed to list blobs in storage: {}.", storageName);
            throw new CloudConnectorException(e);
        }
    }

    public BlobContainerClient getBlobContainerClient(String resourceGroup, String storageName, String containerName) {
        LOGGER.debug("get blob container: RG={}, storageName={}, containerName={}", resourceGroup, storageName, containerName);
        StorageAccountKey key = getStorageAccountKey(resourceGroup, storageName);
        try {
            return azureClientFactory.configureDefault(new BlobContainerClientBuilder())
                    .endpoint("https://" + storageName + ".blob.core.windows.net")
                    .containerName(containerName)
                    .credential(new StorageSharedKeyCredential(storageName, key.value()))
                    .buildClient();
        } catch (BlobStorageException e) {
            throw new CloudConnectorException("can't get blob container, storage service error occurred", e);
        }
    }

    public BlobClient getBlobClient(String resourceGroup, String storageName, String containerName, String blobName) {
        LOGGER.debug("get blob container: RG={}, storageName={}, containerName={}", resourceGroup, storageName, containerName);
        StorageAccountKey key = getStorageAccountKey(resourceGroup, storageName);
        try {
            return azureClientFactory.configureDefault(new BlobClientBuilder())
                    .endpoint("https://" + storageName + ".blob.core.windows.net")
                    .containerName(containerName)
                    .blobName(blobName)
                    .credential(new StorageSharedKeyCredential(storageName, key.value()))
                    .buildClient();
        } catch (BlobStorageException e) {
            throw new CloudConnectorException("can't get blob client, storage service error occurred", e);
        }
    }

    public AzureListResult<VirtualMachine> getVirtualMachines(String resourceGroup) {
        return handleException(() -> AzureListResult.listByResourceGroup(azure.virtualMachines(), resourceGroup));
    }

    public VirtualMachine getVirtualMachineByResourceGroup(String resourceGroup, String vmName) {
        return handleException(() -> azure.virtualMachines().getByResourceGroup(resourceGroup, vmName));
    }

    public VirtualMachine getVirtualMachine(String vmId) {
        return handleException(() -> azure.virtualMachines().getById(vmId));
    }

    public VirtualMachineInstanceView getVirtualMachineInstanceView(String resourceGroup, String vmName) {
        return getVirtualMachineByResourceGroup(resourceGroup, vmName).instanceView();
    }

    public Set<AvailabilityZoneId> getAvailabilityZone(String resourceGroup, String vmName) {
        return handleException(() -> {
            VirtualMachine vm = azure.virtualMachines().getByResourceGroup(resourceGroup, vmName);
            return Objects.nonNull(vm) ? vm.availabilityZones() : Collections.emptySet();
        });
    }

    public Integer getFaultDomainNumber(String resourceGroup, String vmName) {
        return getVirtualMachineInstanceView(resourceGroup, vmName).platformFaultDomain();
    }

    public AvailabilitySet getAvailabilitySet(String resourceGroup, String asName) {
        return handleException(() -> azure.availabilitySets().getByResourceGroup(resourceGroup, asName));
    }

    public Mono<Void> deleteLoadBalancerAsync(String resourceGroup, String loadBalancerName) {
        return handleException(() -> azure.loadBalancers().deleteByResourceGroupAsync(resourceGroup, loadBalancerName));
    }

    public Mono<Void> deleteAvailabilitySetAsync(String resourceGroup, String asName) {
        return handleException(() -> azure.availabilitySets().deleteByResourceGroupAsync(resourceGroup, asName));
    }

    public Mono<Void> deallocateVirtualMachineAsync(String resourceGroup, String vmName) {
        return handleException(() -> azure.virtualMachines().deallocateAsync(resourceGroup, vmName));
    }

    public Mono<Void> deleteVirtualMachine(String resourceGroup, String vmName) {
        return handleException(() -> azure.virtualMachines().deleteByResourceGroupAsync(resourceGroup, vmName));
    }

    public Mono<Void> startVirtualMachineAsync(String resourceGroup, String vmName) {
        return handleException(() -> azure.virtualMachines().startAsync(resourceGroup, vmName));
    }

    public Mono<Void> stopVirtualMachineAsync(String resourceGroup, String vmName) {
        return handleException(() -> azure.virtualMachines().powerOffAsync(resourceGroup, vmName));
    }

    public Mono<Void> deletePublicIpAddressByNameAsync(String resourceGroup, String ipName) {
        return handleException(() -> azure.publicIpAddresses().deleteByResourceGroupAsync(resourceGroup, ipName));
    }

    public Flux<String> deleteSecurityGroupsAsnyc(Collection<String> ids) {
        return handleException(() -> azure.networkSecurityGroups().deleteByIdsAsync(ids));
    }

    public AzureListResult<PublicIpAddress> getPublicIpAddresses(String resourceGroup) {
        return handleException(() -> AzureListResult.listByResourceGroup(azure.publicIpAddresses(), resourceGroup));
    }

    public Mono<Void> deleteNetworkInterfaceAsync(String resourceGroup, String networkInterfaceName) {
        return handleException(() -> azure.networkInterfaces().deleteByResourceGroupAsync(resourceGroup, networkInterfaceName));
    }

    public AzureListResult<NetworkInterface> getNetworkInterfaces(String resourceGroup) {
        return handleException(() -> AzureListResult.listByResourceGroup(azure.networkInterfaces(), resourceGroup));
    }

    public List<NetworkInterface> getNetworkInterfaceListByNames(String resourceGroup, Collection<String> attachedNetworkInterfaces) {
        return getNetworkInterfaces(resourceGroup)
                .getStream()
                .filter(networkInterface -> attachedNetworkInterfaces
                        .contains(networkInterface.name()))
                .collect(Collectors.toList());
    }

    public NetworkInterfaces getNetworkInterfaces() {
        return handleException(azure::networkInterfaces);
    }

    public Subnet getSubnetProperties(String resourceGroup, String virtualNetwork, String subnet) {
        return handleException(() -> {
            Network networkByResourceGroup = getNetworkByResourceGroup(resourceGroup, virtualNetwork);
            return networkByResourceGroup == null ? null : networkByResourceGroup.subnets().get(subnet);
        });
    }

    public Network getNetworkByResourceGroup(String resourceGroup, String virtualNetwork) {
        return handleException(() -> azure.networks().getByResourceGroup(resourceGroup, virtualNetwork));
    }

    public Map<String, Subnet> getSubnets(String resourceGroup, String virtualNetwork) {
        return handleException(() -> {
            Network network = getNetworkByResourceGroup(resourceGroup, virtualNetwork);
            return network == null ? emptyMap() : network.subnets();
        });
    }

    public Flux<String> deleteNetworksAsync(Collection<String> networkIds) {
        return handleException(() -> azure.networks().deleteByIdsAsync(networkIds));
    }

    public void deleteNetworkInResourceGroup(String resourceGroup, String networkId) {
        handleException(() -> azure.networks().deleteByResourceGroup(resourceGroup, networkId));
    }

    public Flux<String> deleteStorageAccountsAsync(Collection<String> accountIds) {
        return handleException(() -> azure.storageAccounts().deleteByIdsAsync(accountIds));
    }

    public Flux<String> deleteImagesAsync(Collection<String> imageIds) {
        return handleException(() -> azure.virtualMachineCustomImages().deleteByIdsAsync(imageIds));
    }

    public NetworkSecurityGroups getSecurityGroups() {
        return handleException(azure::networkSecurityGroups);
    }

    public Set<VirtualMachineSize> getVmTypes(String region) throws ProviderAuthenticationFailedException {
        return handleException(() -> {
            Set<VirtualMachineSize> resultList = new HashSet<>();
            if (region == null) {
                for (Region tmpRegion : Region.values()) {
                    resultList.addAll(AzureListResult.listByRegion(azure.virtualMachines().sizes(), tmpRegion.label()).getAll());
                }
            } else {
                resultList.addAll(AzureListResult.listByRegion(azure.virtualMachines().sizes(), region).getAll());
            }
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

    public LoadBalancer getLoadBalancer(String resourceGroupName, String loadBalancerName) {
        return handleException(() -> azure.loadBalancers().getByResourceGroup(resourceGroupName, loadBalancerName));
    }

    /**
     * Returns the Frontends (frontend name + IP address) associated with a particular Load Balancer in a particular Azure Resource Group.
     *
     * Load balancer type is used to determine whether to return private or public IP addresses for the frontends, it's possible for a
     * load balancer to have both private and public IP addresses.
     *
     * @param resourceGroupName the name of the resource group containing the load balancer
     * @param loadBalancerName the name of the load balancer
     * @param loadBalancerType corresponds to load balancer IP address types to retrieve.
     * @return Frontends (frontend name + IP addresses)
     */
    public List<AzureLoadBalancerFrontend> getLoadBalancerFrontends(String resourceGroupName, String loadBalancerName, LoadBalancerType loadBalancerType) {
        switch (loadBalancerType) {
            case PRIVATE:
            case GATEWAY_PRIVATE:
                return getLoadBalancerPrivateFrontends(resourceGroupName, loadBalancerName);
            case PUBLIC:
                return getLoadBalancerFrontends(resourceGroupName, loadBalancerName);
            default:
                LOGGER.warn("Cannot get IPs for load balancer {}, it has an unknown type {}. Using an empty list instead.", loadBalancerName, loadBalancerType);
                return List.of();

        }
    }

    private List<AzureLoadBalancerFrontend> getLoadBalancerFrontends(String resourceGroupName, String loadBalancerName) {
        List<String> idsAssociatedWithLoadBalancerPublicIps = getLoadBalancer(resourceGroupName, loadBalancerName).publicIpAddressIds();

        List<AzureLoadBalancerFrontend> frontends = getPublicIpAddresses(resourceGroupName)
                .getAll()
                .stream()
                .filter(ipAddress -> idsAssociatedWithLoadBalancerPublicIps.contains(ipAddress.id()))
                .map(ipAddress ->
                        new AzureLoadBalancerFrontend(ipAddress.getAssignedLoadBalancerFrontend().name(), ipAddress.ipAddress(), LoadBalancerType.PUBLIC))
                .collect(Collectors.toList());

        LOGGER.info("Frontends for public load balancer {} retrieved: {}", loadBalancerName, frontends);
        return frontends;
    }

    private List<AzureLoadBalancerFrontend> getLoadBalancerPrivateFrontends(String resourceGroupName, String loadBalancerName) {
        // The keys in this map are the names of the frontend load balancers.
        Map<String, LoadBalancerFrontend> providerFrontends = getLoadBalancer(resourceGroupName, loadBalancerName).frontends();

        List<AzureLoadBalancerFrontend> frontends = providerFrontends.entrySet().stream()
                .filter(fe -> Objects.nonNull(fe.getValue().innerModel())
                        && Objects.nonNull(fe.getValue().innerModel().privateIpAddress())
                        && !fe.getValue().isPublic())
                // The LoadBalancerType pseudo-type is matched based on the frontend name. It is unfortunate, I know.
                .map(fe ->
                        new AzureLoadBalancerFrontend(fe.getKey(), fe.getValue().innerModel().privateIpAddress(),
                                fe.getKey().endsWith("gateway") ? LoadBalancerType.GATEWAY_PRIVATE : LoadBalancerType.PRIVATE))
                .collect(Collectors.toList());

        LOGGER.info("Frontends for private load balancer {} retrieved: {}", loadBalancerName, frontends);
        return frontends;
    }

    public Map<String, LoadBalancingRule> getLoadBalancerRules(String resourceGroupName, String loadBalancerName) {
        return getLoadBalancer(resourceGroupName, loadBalancerName).loadBalancingRules();
    }

    public AzureListResult<Identity> listIdentities() {
        return handleException(() -> AzureListResult.list(azure.identities()));
    }

    public List<Identity> listIdentitiesByRegion(String region) {
        return listIdentities()
                .getAll()
                .stream()
                .filter(identity -> identity.region().label().equalsIgnoreCase(region)
                        || identity.region().name().equalsIgnoreCase(region))
                .collect(Collectors.toList());
    }

    public Identity getIdentityById(String id) {
        return handleException(() -> azure.identities().getById(id));
    }

    public RoleAssignments getRoleAssignments() {
        return getRoleAssignments(azure);
    }

    public RoleAssignments getRoleAssignments(AzureResourceManager azure) {
        return handleException(() -> azure.identities().manager()).authorizationManager().roleAssignments();
    }

    public AzureListResult<RoleAssignment> listRoleAssignmentsByScope(String scope) {
        return handleException(() -> new AzureListResult<>(getRoleAssignments().listByScope(scope)));
    }

    public List<RoleAssignmentInner> listRoleAssignmentsByScopeInner(String scope) {
        return listRoleAssignmentsByScope(scope)
                .getStream()
                .map(HasInnerModel::innerModel)
                .collect(Collectors.toList());
    }

    public AzureListResult<RoleAssignmentInner> listRoleAssignments() {
        return new AzureListResult<>(listRoleAssignmentsBySubscription(getCurrentSubscription().subscriptionId()));
    }

    public PagedIterable<RoleAssignmentInner> listRoleAssignmentsBySubscription(String subscriptionId) {
        return handleException(() -> getRoleAssignments(azureClientFactory.getAzureResourceManager(subscriptionId))
                .manager()
                .roleServiceClient()
                .getRoleAssignments()
                .list());
    }

    public Subscription getCurrentSubscription() {
        return azure.getCurrentSubscription();
    }

    public AzureListResult<Subscription> listSubscriptions() {
        return AzureListResult.list(azure.subscriptions());
    }

    public void deleteGenericResourceById(String id) {
        handleException(() -> azure.genericResources().deleteById(id));
    }

    public String getServicePrincipalForResourceById(String referenceId) {
        return handleException(() -> azure.genericResources().getById(referenceId).identity().principalId());
    }

    public AzureListResult<PrivateDnsZone> getPrivateDnsZoneList() {
        return AzureListResult.list(privateDnsZoneManager.privateZones());
    }

    public AzureListResult<PrivateDnsZone> getPrivateDnsZonesByResourceGroup(String subscriptionId, String resourceGroupName) {
        return AzureListResult.listByResourceGroup(
                azureClientFactory.getPrivateDnsManagerWithAnotherSubscription(subscriptionId).privateZones(),
                resourceGroupName);
    }

    public List<PrivateDnsZone> getPrivateDnsZoneListFromAllSubscriptions() {
        return azure.subscriptions()
                .list()
                .stream()
                .map(s -> getPrivateDnsZones(s.subscriptionId()))
                .flatMap(AzureListResult::getStream)
                .collect(Collectors.toList());
    }

    private AzureListResult<PrivateDnsZone> getPrivateDnsZones(String subscriptionId) {
        return AzureListResult.list(azureClientFactory.getPrivateDnsManagerWithAnotherSubscription(subscriptionId).privateZones());
    }

    public AzureListResult<PrivateDnsZone> listPrivateDnsZonesByResourceGroup(String resourceGroupName) {
        return AzureListResult.listByResourceGroup(privateDnsZoneManager.privateZones(), resourceGroupName);
    }

    public AzureListResult<VirtualNetworkLinkInner> listNetworkLinksByPrivateDnsZoneName(String resourceGroupName, String dnsZoneName) {
        return new AzureListResult<>(privateDnsZoneManager.serviceClient().getVirtualNetworkLinks().list(resourceGroupName, dnsZoneName));
    }

    public AzureListResult<VirtualNetworkLinkInner> listNetworkLinksByPrivateDnsZoneName(String subscriptionId, String resourceGroupName, String dnsZoneName) {
        return new AzureListResult<>(azureClientFactory.getPrivateDnsManagerWithAnotherSubscription(subscriptionId)
                .serviceClient()
                .getVirtualNetworkLinks()
                .list(resourceGroupName, dnsZoneName));
    }

    public VirtualNetworkLinkInner getNetworkLinkByPrivateDnsZone(String resourceGroupName, String dnsZoneName, String virtualNetworkLinkName) {
        return virtualNetworkLinkName == null ? null : handleException(() ->
                privateDnsZoneManager.serviceClient().getVirtualNetworkLinks().get(resourceGroupName, dnsZoneName, virtualNetworkLinkName));
    }

    public boolean checkIfDnsZonesDeployed(String resourceGroupName, List<AzurePrivateDnsZoneServiceEnum> services) {
        LOGGER.debug("Checking DNS Zones for services {}", services.stream()
                .map(AzurePrivateDnsZoneServiceEnum::getDnsZoneName)
                .collect(Collectors.toList()));

        List<PrivateDnsZone> dnsZones = listPrivateDnsZonesByResourceGroup(resourceGroupName).getAll();
        for (AzurePrivateDnsZoneServiceEnum service : services) {
            String dnsZoneName = service.getDnsZoneName();
            boolean dnsZoneFound = dnsZones.stream()
                    .filter(dnsZone -> dnsZone.name().equals(dnsZoneName))
                    .anyMatch(dnsZone -> dnsZone.provisioningState().equals(SUCCEEDED));
            if (!dnsZoneFound) {
                LOGGER.info("DNS Zone {} is not provisioned successfully yet!", dnsZoneName);
                return false;
            }
        }
        return true;
    }

    public boolean checkIfNetworkLinksDeployed(String resourceGroupName, String networkId, List<AzurePrivateDnsZoneServiceEnum> services) {
        LOGGER.debug("Checking Network link between network and services {} and network link {}", networkId,
                services.stream()
                        .map(AzurePrivateDnsZoneServiceEnum::getDnsZoneName)
                        .collect(Collectors.toList()));
        for (AzurePrivateDnsZoneServiceEnum service : services) {
            String dnsZoneName = service.getDnsZoneName();
            AzureListResult<VirtualNetworkLinkInner> virtualNetworkLinks = listNetworkLinksByPrivateDnsZoneName(resourceGroupName, dnsZoneName);
            if (!isNetworkLinkCreated(networkId, virtualNetworkLinks)) {
                LOGGER.info("Network link for network {} and DNS Zone {} is not provisioned successfully yet!", networkId, dnsZoneName);
                return false;
            }
        }
        return true;
    }

    public boolean isNetworkLinkCreated(String networkId, AzureListResult<VirtualNetworkLinkInner> virtualNetworkLinks) {
        return virtualNetworkLinks
                .getStream()
                .filter(networkLink -> networkId.equals(networkLink.name()))
                .anyMatch(networkLink -> networkLink.provisioningState().equals(SUCCEEDED)
                        && networkLink.virtualNetworkLinkState().equals(VirtualNetworkLinkState.COMPLETED));
    }

    private <T> T handleException(Supplier<T> function) {
        return azureExceptionHandler.handleException(function);
    }

    private void handleException(Runnable function) {
        azureExceptionHandler.handleException(function);
    }

    public Mono<Void> deleteGenericResourceByIdAsync(String id) {
        return handleException(() -> azure.genericResources().deleteByIdAsync(id));
    }

    private DiskEncryptionSetInner createDiskEncryptionSetInner(String sourceVaultId, String encryptionKeyUrl, String location, Map<String, String> tags) {
        SourceVault sourceVault = new SourceVault().withId(sourceVaultId);
        KeyForDiskEncryptionSet keyForDiskEncryptionSet = new KeyForDiskEncryptionSet().withKeyUrl(encryptionKeyUrl).withSourceVault(sourceVault);
        EncryptionSetIdentity eSetId = new EncryptionSetIdentity().withType(DiskEncryptionSetIdentityType.SYSTEM_ASSIGNED);
        return new DiskEncryptionSetInner()
                .withEncryptionType(DiskEncryptionSetType.ENCRYPTION_AT_REST_WITH_CUSTOMER_KEY)
                .withActiveKey(keyForDiskEncryptionSet)
                .withIdentity(eSetId)
                .withLocation(location)
                .withTags(tags);
    }

    public DiskEncryptionSetInner getDiskEncryptionSetByName(String resourceGroupName, String diskEncryptionSetName) {
        return handleException(() ->
                computeManager.serviceClient()
                        .getDiskEncryptionSets()
                        .getByResourceGroup(resourceGroupName, diskEncryptionSetName));
    }

    public DiskEncryptionSetInner createDiskEncryptionSet(String diskEncryptionSetName, String encryptionKeyUrl, String location,
            String resourceGroupName, String sourceVaultId, Map<String, String> tags) {
        return handleException(() -> {
            DiskEncryptionSetInner encryptionSet = createDiskEncryptionSetInner(sourceVaultId, encryptionKeyUrl, location, tags);
            return computeManager.serviceClient()
                    .getDiskEncryptionSets()
                    .createOrUpdate(resourceGroupName, diskEncryptionSetName, encryptionSet);
        });
    }

    public boolean keyVaultExists(String resourceGroupName, String vaultName) {
        return handleException(() -> {
            Vault keyVault = azure.vaults()
                    .getByResourceGroup(resourceGroupName, vaultName);
            return keyVault != null;
        });
    }

    public void grantKeyVaultAccessPolicyToServicePrincipal(String resourceGroupName, String vaultName, String principalObjectId) {
        handleException(() -> {
            azure.vaults()
                    .getByResourceGroup(resourceGroupName, vaultName)
                    .update()
                    .defineAccessPolicy()
                    .forObjectId(principalObjectId)
                    .allowKeyPermissions(List.of(KeyPermissions.WRAP_KEY, KeyPermissions.UNWRAP_KEY, KeyPermissions.GET))
                    .attach()
                    .apply();
        });
    }

    public void modifyInstanceType(String resourceGroupName, String instanceName, String instanceType) {
        handleException(() -> {
            LOGGER.info("Updating instance {} in {} resourcegroup to {} instancetype",
                    instanceName, resourceGroupName, instanceName);
            azure.virtualMachines()
                    .getByResourceGroup(resourceGroupName, instanceName)
                    .update()
                    .withSize(instanceType)
                    .apply();
        });
    }

    public boolean checkKeyVaultAccessPolicyForServicePrincipal(String resourceGroupName, String vaultName, String principalObjectId) {
        return handleException(() -> {
            List<AccessPolicy> accessPolicies = azure.vaults()
                    .getByResourceGroup(resourceGroupName, vaultName)
                    .accessPolicies();
            return checkKeyVaultAccessPolicyListForServicePrincipal(accessPolicies, principalObjectId);
        });
    }

    @VisibleForTesting
    boolean checkKeyVaultAccessPolicyListForServicePrincipal(List<AccessPolicy> accessPolicies, String principalObjectId) {
        if (accessPolicies != null) {
            for (int i = accessPolicies.size() - 1; i >= 0; i--) {
                AccessPolicyEntry accessPolicyEntry = accessPolicies.get(i).innerModel();
                if (principalObjectId.equals(accessPolicyEntry.objectId())) {
                    return accessPolicyEntry.permissions().keys()
                            .containsAll(List.of(KeyPermissions.WRAP_KEY, KeyPermissions.UNWRAP_KEY, KeyPermissions.GET));
                }
            }
        }
        return false;
    }

    public void removeKeyVaultAccessPolicyForServicePrincipal(String resourceGroupName, String vaultName, String principalObjectId) {
        handleException(() -> {
            azure.vaults()
                    .getByResourceGroup(resourceGroupName, vaultName)
                    .update()
                    .updateAccessPolicy(principalObjectId)
                    .disallowKeyPermissions(List.of(KeyPermissions.WRAP_KEY, KeyPermissions.UNWRAP_KEY, KeyPermissions.GET))
                    .parent()
                    .apply();
        });
    }

    public void deleteDiskEncryptionSet(String resourceGroup, String diskEncryptionSetName) {
        handleException(() -> computeManager.diskEncryptionSets().deleteByResourceGroup(resourceGroup, diskEncryptionSetName));
    }

    public Optional<String> getAccessToken() {
        return azureClientFactory.getAccessToken();
    }

    public String getVaultNameFromEncryptionKeyUrl(String encryptionKeyUrl) {
        Matcher matcher = ENCRYPTION_KEY_URL_VAULT_NAME.matcher(encryptionKeyUrl);
        if (matcher.matches()) {
            return matcher.group(1);
        }
        return null;
    }

}
