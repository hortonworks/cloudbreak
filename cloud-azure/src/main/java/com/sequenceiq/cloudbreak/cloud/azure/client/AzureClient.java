package com.sequenceiq.cloudbreak.cloud.azure.client;

import static com.microsoft.azure.management.compute.DiskSkuTypes.PREMIUM_LRS;
import static com.microsoft.azure.management.compute.DiskSkuTypes.STANDARD_LRS;
import static com.microsoft.azure.management.compute.DiskSkuTypes.STANDARD_SSD_LRS;
import static com.microsoft.azure.management.compute.DiskSkuTypes.ULTRA_SSD_LRS;
import static com.microsoft.azure.management.privatedns.v2018_09_01.ProvisioningState.SUCCEEDED;
import static com.sequenceiq.cloudbreak.util.Benchmark.measure;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.microsoft.azure.CloudException;
import com.microsoft.azure.PagedList;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.appservice.implementation.AppServicePlanInner;
import com.microsoft.azure.management.compute.AvailabilitySet;
import com.microsoft.azure.management.compute.CachingTypes;
import com.microsoft.azure.management.compute.Disk;
import com.microsoft.azure.management.compute.DiskEncryptionSetIdentityType;
import com.microsoft.azure.management.compute.DiskEncryptionSetType;
import com.microsoft.azure.management.compute.DiskSkuTypes;
import com.microsoft.azure.management.compute.DiskStorageAccountTypes;
import com.microsoft.azure.management.compute.Encryption;
import com.microsoft.azure.management.compute.EncryptionSetIdentity;
import com.microsoft.azure.management.compute.KeyVaultAndKeyReference;
import com.microsoft.azure.management.compute.OperatingSystemStateTypes;
import com.microsoft.azure.management.compute.SourceVault;
import com.microsoft.azure.management.compute.VirtualMachine;
import com.microsoft.azure.management.compute.VirtualMachineCustomImage;
import com.microsoft.azure.management.compute.VirtualMachineDataDisk;
import com.microsoft.azure.management.compute.VirtualMachineInstanceView;
import com.microsoft.azure.management.compute.VirtualMachineSize;
import com.microsoft.azure.management.compute.implementation.ComputeManager;
import com.microsoft.azure.management.compute.implementation.DiskEncryptionSetInner;
import com.microsoft.azure.management.compute.implementation.DiskEncryptionSetsInner;
import com.microsoft.azure.management.compute.implementation.DiskInner;
import com.microsoft.azure.management.graphrbac.RoleAssignment;
import com.microsoft.azure.management.graphrbac.RoleAssignments;
import com.microsoft.azure.management.graphrbac.implementation.RoleAssignmentInner;
import com.microsoft.azure.management.graphrbac.implementation.ServicePrincipalCreateParametersInner;
import com.microsoft.azure.management.keyvault.KeyPermissions;
import com.microsoft.azure.management.marketplaceordering.v2015_06_01.AgreementTerms;
import com.microsoft.azure.management.marketplaceordering.v2015_06_01.implementation.MarketplaceOrderingManager;
import com.microsoft.azure.management.msi.Identity;
import com.microsoft.azure.management.network.LoadBalancer;
import com.microsoft.azure.management.network.LoadBalancerFrontend;
import com.microsoft.azure.management.network.LoadBalancingRule;
import com.microsoft.azure.management.network.Network;
import com.microsoft.azure.management.network.NetworkInterface;
import com.microsoft.azure.management.network.NetworkInterfaces;
import com.microsoft.azure.management.network.NetworkSecurityGroup;
import com.microsoft.azure.management.network.NetworkSecurityGroups;
import com.microsoft.azure.management.network.PublicIPAddress;
import com.microsoft.azure.management.network.Subnet;
import com.microsoft.azure.management.network.implementation.FrontendIPConfigurationInner;
import com.microsoft.azure.management.privatedns.v2018_09_01.PrivateZone;
import com.microsoft.azure.management.privatedns.v2018_09_01.VirtualNetworkLinkState;
import com.microsoft.azure.management.privatedns.v2018_09_01.implementation.VirtualNetworkLinkInner;
import com.microsoft.azure.management.privatedns.v2018_09_01.implementation.privatednsManager;
import com.microsoft.azure.management.resources.Deployment;
import com.microsoft.azure.management.resources.DeploymentMode;
import com.microsoft.azure.management.resources.DeploymentOperations;
import com.microsoft.azure.management.resources.ResourceGroup;
import com.microsoft.azure.management.resources.ResourceGroups;
import com.microsoft.azure.management.resources.Subscription;
import com.microsoft.azure.management.resources.fluentcore.arm.AvailabilityZoneId;
import com.microsoft.azure.management.resources.fluentcore.arm.Region;
import com.microsoft.azure.management.resources.fluentcore.arm.models.HasId;
import com.microsoft.azure.management.resources.fluentcore.model.implementation.IndexableRefreshableWrapperImpl;
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
import com.sequenceiq.cloudbreak.cloud.azure.image.marketplace.AzureMarketplaceImage;
import com.sequenceiq.cloudbreak.cloud.azure.AzurePrivateDnsZoneServiceEnum;
import com.sequenceiq.cloudbreak.cloud.azure.status.AzureStatusMapper;
import com.sequenceiq.cloudbreak.cloud.azure.util.AzureAuthExceptionHandler;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.cloudbreak.validation.ValidationResult.ValidationResultBuilder;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.LoadBalancerType;

import rx.Completable;
import rx.Observable;

public class AzureClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureClient.class);

    private static final int MAX_AZURE_MANAGED_DISK_SIZE_WITH_CACHE = 4095;

    private final Azure azure;

    private final privatednsManager privatednsManager;

    private final MarketplaceOrderingManager marketplaceOrderingManager;

    private final AzureClientCredentials azureClientCredentials;

    private final AzureAuthExceptionHandler azureAuthExceptionHandler;

    private final ComputeManager computeManager;

    public AzureClient(AzureClientCredentials azureClientCredentials, AzureAuthExceptionHandler azureAuthExceptionHandler) {
        this.azureClientCredentials = azureClientCredentials;
        azure = azureClientCredentials.getAzure();
        privatednsManager = azureClientCredentials.getPrivateDnsManager();
        marketplaceOrderingManager = azureClientCredentials.getMarketplaceOrderingManager();
        computeManager = azureClientCredentials.getComputeManager();
        this.azureAuthExceptionHandler = azureAuthExceptionHandler;
    }

    public Azure getAzure() {
        return azure;
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
        try {
            return getResourceGroups().contain(name);
        } catch (CloudException e) {
            if (e.getMessage().contains("Status code 403")) {
                LOGGER.info("Resource group {} does not exist or insufficient permission to access it, exception: {}", name, e);
                return false;
            }
            throw e;
        }
    }

    public void deleteResourceGroup(String name) {
        handleAuthException(() -> azure.resourceGroups().deleteByName(name));
    }

    public ResourceGroup createResourceGroup(String name, String region, Map<String, String> tags) {
        return handleAuthException(() -> azure.resourceGroups().define(name)
                .withRegion(region)
                .withTags(tags)
                .create());
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

    public void createServicePrincipal(String resourceGroupName, String deploymentName, String templateContent, String parameterContent) {
//        return handleAuthException(() -> {
//            try {
//                return azure.appServices().appServicePlans().inner()
//                        .createOrUpdate("", "", new ServicePrincipalCreateParametersInner()
//                                .withAppId(""))
//                        .withTemplate(templateContent)
//                        .withParameters(parameterContent)
//                        .withMode(DeploymentMode.INCREMENTAL)
//                        .create();
//            } catch (IOException e) {
//                throw new RuntimeException(e);
//            }
//        });
    }

    public boolean templateDeploymentExists(String resourceGroupName, String deploymentName) {
        return handleAuthException(() -> azure.deployments().checkExistence(resourceGroupName, deploymentName));
    }

    public ResourceStatus getTemplateDeploymentStatus(String resourceGroupName, String deploymentName) {
        return handleAuthException(() -> Optional.ofNullable(getTemplateDeployment(resourceGroupName, deploymentName)))
                .map(Deployment::provisioningState)
                .map(AzureStatusMapper::mapResourceStatus)
                .orElse(ResourceStatus.DELETED);
    }

    public CommonStatus getTemplateDeploymentCommonStatus(String resourceGroupName, String deploymentName) {
        return handleAuthException(() -> Optional.ofNullable(getTemplateDeployment(resourceGroupName, deploymentName)))
                .map(Deployment::provisioningState)
                .map(AzureStatusMapper::mapCommonStatus)
                .orElse(CommonStatus.DETACHED);
    }

    public void deleteTemplateDeployment(String resourceGroupName, String deploymentName) {
        handleAuthException(() -> azure.deployments().deleteByResourceGroup(resourceGroupName, deploymentName));
    }

    public Deployment getTemplateDeployment(String resourceGroupName, String deploymentName) {
        return handleAuthException(() -> azure.deployments().getByResourceGroup(resourceGroupName, deploymentName));
    }

    public DeploymentOperations getTemplateDeploymentOperations(String resourceGroupName, String deploymentName) {
        return handleAuthException(() -> getTemplateDeployment(resourceGroupName, deploymentName).deploymentOperations());
    }

    public void cancelTemplateDeployment(String resourceGroupName, String deploymentName) {
        handleAuthException(() -> getTemplateDeployment(resourceGroupName, deploymentName).cancel());
    }

    public StorageAccounts getStorageAccounts() {
        return handleAuthException(azure::storageAccounts);
    }

    public void deleteStorageAccount(String resourceGroup, String storageName) {
        handleAuthException(() -> azure.storageAccounts().deleteByResourceGroup(resourceGroup, storageName));
    }

    public StorageAccount createStorageAccount(String resourceGroup, String storageName, String storageLocation, StorageAccountSkuType accType,
            Boolean encryted,
            Map<String, String> tags) {
        return handleAuthException(() -> {
            WithCreate withCreate = azure.storageAccounts()
                    .define(storageName)
                    .withRegion(storageLocation)
                    .withExistingResourceGroup(resourceGroup)
                    .withTags(tags)
                    .withSku(accType)
                    .withOnlyHttpsTraffic()
                    .withGeneralPurposeAccountKindV2();
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
        return azure.storageAccounts().manager().inner().withSubscriptionId(subscriptionId).storageAccounts().list().stream()
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

    public Disk createManagedDisk(String diskName, int diskSize, AzureDiskType diskType, String region, String resourceGroupName, Map<String, String> tags,
            String diskEncryptionSetId) {
        LOGGER.debug("create managed disk with name={}", diskName);
        Disk.DefinitionStages.WithCreate withCreate = azure.disks().define(diskName)
                .withRegion(Region.findByLabelOrName(region))
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
            DiskInner inner = (DiskInner) ((IndexableRefreshableWrapperImpl) withCreate).inner();
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
        vm.inner().withPlan(null);
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

    public PagedList<Disk> listDisksByResourceGroup(String resourceGroupName) {
        return azure.disks().listByResourceGroup(resourceGroupName);
    }

    public Disk getDiskById(String id) {
        return azure.disks().getById(id);
    }

    public Disk getDiskByName(String resourceGroupName, String diskName) {
        return azure.disks().getByResourceGroup(resourceGroupName, diskName);
    }

    public Observable<String> deleteManagedDisksAsync(Collection<String> ids) {
        LOGGER.debug("delete managed disk: id={}", ids);
        return handleAuthException(() -> azure.disks().deleteByIdsAsync(ids));
    }

    public Completable deleteManagedDiskAsync(String resourceGroup, String name) {
        return handleAuthException(() -> azure.disks().deleteByResourceGroupAsync(resourceGroup, name));
    }

    public DiskSkuTypes convertAzureDiskTypeToDiskSkuTypes(AzureDiskType diskType) {
        return Objects.nonNull(diskType) ? DiskSkuTypes.fromStorageAccountType(DiskStorageAccountTypes.fromString(diskType.value()))
                : DiskSkuTypes.STANDARD_LRS;
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

    public VirtualMachineCustomImage findImage(String resourceGroup, String imageName) {
        LOGGER.debug("Searching custom image {} in resource group {}", imageName, resourceGroup);
        return azureAuthExceptionHandler.handleAuthException(() -> azure
                .virtualMachineCustomImages()
                .getByResourceGroup(resourceGroup, imageName));
    }

    public VirtualMachineCustomImage createImage(String imageName, String resourceGroup, String fromVhdUri, String region) {
        return handleAuthException(() -> {
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

    public Observable<AgreementTerms> signImageConsent(AzureMarketplaceImage azureMarketplaceImage) {
        return marketplaceOrderingManager.marketplaceAgreements()
                .signAsync(azureMarketplaceImage.getPublisherId(), azureMarketplaceImage.getOfferId(), azureMarketplaceImage.getPlanId());
    }

    public void copyImageBlobInStorageContainer(String resourceGroup, String storageName, String containerName, String sourceBlob, String sourceBlobName) {
        LOGGER.debug("copy image in storage container: RG={}, storageName={}, containerName={}, sourceBlob={}",
                resourceGroup, storageName, containerName, sourceBlob);
        CloudBlobContainer container = getBlobContainer(resourceGroup, storageName, containerName);
        try {
            CloudPageBlob cloudPageBlob = container.getPageBlobReference(sourceBlobName);
            String copyId = cloudPageBlob.startCopy(new URI(sourceBlob));
            LOGGER.debug("Image copy started, copy id: {}", copyId);
        } catch (URISyntaxException e) {
            throw new CloudConnectorException("can't copy image blob, URI is not valid", e);
        } catch (StorageException e) {
            throw new CloudConnectorException("can't copy image blob, storage service error occurred", e);
        }
    }

    public CopyState getCopyStatus(String resourceGroup, String storageName, String containerName, String sourceBlobName) {
        LOGGER.debug("get image copy status: RG={}, storageName={}, containerName={}, sourceBlob={}",
                resourceGroup, storageName, containerName, sourceBlobName);
        CloudBlobContainer container = getBlobContainer(resourceGroup, storageName, containerName);
        try {
            CloudPageBlob cloudPageBlob = container.getPageBlobReference(sourceBlobName);
            LOGGER.debug("Downloading {} container attributes.", container.getName());
            container.downloadAttributes();
            LOGGER.debug("Downloading {} cloudPageBlob attributes.", cloudPageBlob.getName());
            cloudPageBlob.downloadAttributes();
            return cloudPageBlob.getCopyState();
        } catch (URISyntaxException e) {
            throw new CloudConnectorException("can't get copy status, URI is not valid", e);
        } catch (StorageException e) {
            throw new CloudConnectorException("can't get copy status, storage service error occurred", e);
        }
    }

    public String getImageBlobUri(String resourceGroup, String storageName, String containerName, String vhdName) {
        CloudBlobContainer blobContainer = getBlobContainer(resourceGroup, storageName, containerName);
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
        try {
            container.downloadAttributes();
            container.listBlobs().iterator().forEachRemaining(targetCollection::add);
        } catch (Exception e) {
            LOGGER.warn("Failed to list blobs in storage: {}.", storageName);
            throw new CloudConnectorException(e);
        }
        return targetCollection;
    }

    public CloudBlobContainer getBlobContainer(String resourceGroup, String storageName, String containerName) {
        LOGGER.debug("get blob container: RG={}, storageName={}, containerName={}", resourceGroup, storageName, containerName);
        List<StorageAccountKey> keys = getStorageAccountKeys(resourceGroup, storageName);
        String storageConnectionString = String.format("DefaultEndpointsProtocol=https;AccountName=%s;AccountKey=%s", storageName, keys.get(0).value());
        try {
            CloudStorageAccount storageAccount = CloudStorageAccount.parse(storageConnectionString);
            CloudBlobClient blobClient = storageAccount.createCloudBlobClient();
            CloudBlobContainer containerReference = blobClient.getContainerReference(containerName);
            LOGGER.debug("Blob container {} reference retrieved.", containerReference.getName());
            return containerReference;
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

    public VirtualMachine getVirtualMachineByResourceGroup(String resourceGroup, String vmName) {
        return handleAuthException(() -> azure.virtualMachines().getByResourceGroup(resourceGroup, vmName));
    }

    public VirtualMachine getVirtualMachine(String vmId) {
        return handleAuthException(() -> azure.virtualMachines().getById(vmId));
    }

    public Observable<VirtualMachine> getVirtualMachineAsync(String resourceGroup, String vmName) {
        return handleAuthException(() -> azure.virtualMachines().getByResourceGroupAsync(resourceGroup, vmName));
    }

    public VirtualMachineInstanceView getVirtualMachineInstanceView(String resourceGroup, String vmName) {
        return getVirtualMachineByResourceGroup(resourceGroup, vmName).instanceView();
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

    public Completable deleteLoadBalancerAsync(String resourceGroup, String loadBalancerName) {
        return handleAuthException(() -> azure.loadBalancers().deleteByResourceGroupAsync(resourceGroup, loadBalancerName));
    }

    public void deleteAvailabilitySet(String resourceGroup, String asName) {
        handleAuthException(() -> azure.availabilitySets().deleteByResourceGroup(resourceGroup, asName));
    }

    public Completable deleteAvailabilitySetAsync(String resourceGroup, String asName) {
        return handleAuthException(() -> azure.availabilitySets().deleteByResourceGroupAsync(resourceGroup, asName));
    }

    public Completable deallocateVirtualMachineAsync(String resourceGroup, String vmName) {
        return handleAuthException(() -> azure.virtualMachines().deallocateAsync(resourceGroup, vmName));
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

    public Completable stopVirtualMachineAsync(String resourceGroup, String vmName) {
        return handleAuthException(() -> azure.virtualMachines().powerOffAsync(resourceGroup, vmName));
    }

    public Completable deletePublicIpAddressByNameAsync(String resourceGroup, String ipName) {
        return handleAuthException(() -> azure.publicIPAddresses().deleteByResourceGroupAsync(resourceGroup, ipName));
    }

    public Observable<String> deleteSecurityGroupsAsnyc(Collection<String> ids) {
        return handleAuthException(() -> azure.networkSecurityGroups().deleteByIdsAsync(ids));
    }

    public void deletePublicIpAddressById(String ipId) {
        handleAuthException(() -> azure.publicIPAddresses().deleteById(ipId));
    }

    public HasId getPublicIpAddress(String resourceGroup, String ipName) {
        return handleAuthException(() -> azure.publicIPAddresses().getByResourceGroup(resourceGroup, ipName));
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

    public PagedList<NetworkInterface> getNetworkInterfaces(String resourceGroup) {
        return handleAuthException(() -> azure.networkInterfaces().listByResourceGroup(resourceGroup));
    }

    public List<NetworkInterface> getNetworkInterfaceListByNames(String resourceGroup, Collection<String> attachedNetworkInterfaces) {
        PagedList<NetworkInterface> networkInterfaces = getNetworkInterfaces(resourceGroup);
        networkInterfaces.loadAll();
        return networkInterfaces.stream()
                .filter(networkInterface -> attachedNetworkInterfaces
                        .contains(networkInterface.name()))
                .collect(Collectors.toList());
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

    public Observable<String> deleteNetworksAsync(Collection<String> networkIds) {
        return handleAuthException(() -> azure.networks().deleteByIdsAsync(networkIds));
    }

    public void deleteNetworkInResourceGroup(String resourceGroup, String networkId) {
        handleAuthException(() -> azure.networks().deleteByResourceGroup(resourceGroup, networkId));
    }

    public Observable<String> deleteStorageAccountsAsync(Collection<String> accountIds) {
        return handleAuthException(() -> azure.storageAccounts().deleteByIdsAsync(accountIds));
    }

    public Observable<String> deleteImagesAsync(Collection<String> imageIds) {
        return handleAuthException(() -> azure.virtualMachineCustomImages().deleteByIdsAsync(imageIds));
    }

    public NetworkSecurityGroups getSecurityGroups() {
        return handleAuthException(azure::networkSecurityGroups);
    }

    public NetworkSecurityGroup getSecurityGroupProperties(String resourceGroup, String securityGroup) {
        return handleAuthException(() -> azure.networkSecurityGroups().getByResourceGroup(resourceGroup, securityGroup));
    }

    public Set<VirtualMachineSize> getVmTypes(String region) throws ProviderAuthenticationFailedException {
        return handleAuthException(() -> {
            Set<VirtualMachineSize> resultList = new HashSet<>();
            if (region == null) {
                for (Region tmpRegion : Region.values()) {
                    PagedList<VirtualMachineSize> virtualMachineSizes = azure.virtualMachines().sizes()
                            .listByRegion(Region.findByLabelOrName(tmpRegion.label()));
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

    public LoadBalancer getLoadBalancer(String resourceGroupName, String loadBalancerName) {
        return handleAuthException(() -> azure.loadBalancers().getByResourceGroup(resourceGroupName, loadBalancerName));
    }

    /**
     * Returns the IP addresses associated with a particular Load Balancer in a particular Azure Resource Group.
     *
     * Load balancer type is used to determine whether to return private or public IP addresses, it's possible for a
     * load balancer to have both private and public IP addresses.
     *
     * @param resourceGroupName the name of the resource group containing the load balancer
     * @param loadBalancerName the name of the load balancer
     * @param loadBalancerType corresponds to load balancer IP address types to retrieve.
     * @return IP addresses
     */
    public List<String> getLoadBalancerIps(String resourceGroupName, String loadBalancerName, LoadBalancerType loadBalancerType) {
        switch (loadBalancerType) {
            case PRIVATE:
                return getLoadBalancerPrivateIps(resourceGroupName, loadBalancerName);
            case PUBLIC:
                return getLoadBalancerIps(resourceGroupName, loadBalancerName);
            default:
                LOGGER.warn("Cannot get IPs for load balancer {}, it has an unknown type {}. Using an empty list instead.", loadBalancerName, loadBalancerType);
                return List.of();

        }
    }

    private List<String> getLoadBalancerIps(String resourceGroupName, String loadBalancerName) {
        List<String> idsAssociatedWithLoadBalancerPublicIps = getLoadBalancer(resourceGroupName, loadBalancerName).publicIPAddressIds();
        List<PublicIPAddress> publicIpAddressesInResourceGroup = getPublicIpAddresses(resourceGroupName);

        List<String> loadBalancerIps = publicIpAddressesInResourceGroup.stream()
                .filter(ipAddress -> idsAssociatedWithLoadBalancerPublicIps.contains(ipAddress.id()))
                .map(PublicIPAddress::ipAddress)
                .sorted()
                .collect(Collectors.toList());

        LOGGER.info("IPs for load balancer {} retrieved: {}", loadBalancerName, loadBalancerIps);
        return loadBalancerIps;
    }

    private List<String> getLoadBalancerPrivateIps(String resourceGroupName, String loadBalancerName) {
        // The keys in this map are the names of the frontend load balancers. We don't use them however.
        Map<String, LoadBalancerFrontend> frontends = getLoadBalancer(resourceGroupName, loadBalancerName).frontends();

        List<String> loadbalancerPrivateIps = frontends.values().stream()
                .map(LoadBalancerFrontend::inner)
                .map(FrontendIPConfigurationInner::privateIPAddress)
                .filter(Objects::nonNull)
                .sorted()
                .collect(Collectors.toList());

        LOGGER.info("Private IPs for load balancer {} retrieved: {}", loadBalancerName, loadbalancerPrivateIps);
        return loadbalancerPrivateIps;
    }

    public Map<String, LoadBalancingRule> getLoadBalancerRules(String resourceGroupName, String loadBalancerName) {
        return getLoadBalancer(resourceGroupName, loadBalancerName).loadBalancingRules();
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

    public List<RoleAssignmentInner> listRoleAssignmentsByScopeInner(String scope) {
        List<RoleAssignmentInner> roleAssignmentInners = new ArrayList<>();
        for (RoleAssignment ra : listRoleAssignmentsByScope(scope)) {
            roleAssignmentInners.add(ra.inner());
        }
        return roleAssignmentInners;
    }

    public PagedList<RoleAssignmentInner> listRoleAssignments() {
        return listRoleAssignmentsBySubscription(getCurrentSubscription().subscriptionId());
    }

    public PagedList<RoleAssignmentInner> listRoleAssignmentsBySubscription(String subscriptionId) {
        return handleAuthException(() -> getRoleAssignments().manager().roleInner().withSubscriptionId(subscriptionId).roleAssignments().list());
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

    public void deleteGenericResourceById(String databaseServerId) {
        handleAuthException(() -> azure.genericResources().deleteById(databaseServerId));
    }

    public PagedList<PrivateZone> getPrivateDnsZoneList() {
        return privatednsManager.privateZones().list();
    }

    public ValidationResult validateNetworkLinkExistenceForDnsZones(String networkLinkId, List<AzurePrivateDnsZoneServiceEnum> services,
            String resourceGroupName) {
        ValidationResultBuilder resultBuilder = new ValidationResultBuilder();
        PagedList<PrivateZone> privateDnsZoneList = getPrivateDnsZoneList();
        for (AzurePrivateDnsZoneServiceEnum service : services) {
            String dnsZoneName = service.getDnsZoneName();
            Optional<PrivateZone> privateZoneWithNetworkLink = privateDnsZoneList.stream()
                    .filter(privateZone -> !privateZone.resourceGroupName().equalsIgnoreCase(resourceGroupName))
                    .filter(privateZone -> privateZone.name().equalsIgnoreCase(dnsZoneName))
                    .filter(privateZone -> privateZone.provisioningState().equals(SUCCEEDED))
                    .filter(privateZone -> Objects.nonNull(getNetworkLinkByPrivateDnsZone(privateZone.resourceGroupName(), dnsZoneName, networkLinkId)))
                    .findFirst();
            if (privateZoneWithNetworkLink.isPresent()) {
                PrivateZone privateZone = privateZoneWithNetworkLink.get();
                String validationMessage = String.format("Network link for the network %s already exists for Private DNS Zone %s in resource group %s. "
                            + "Please ensure that there is no existing network link and try again!",
                    networkLinkId, dnsZoneName, privateZone.resourceGroupName());
                LOGGER.warn(validationMessage);
                resultBuilder.error(validationMessage);
            }
        }
        return resultBuilder.build();
    }

    public PagedList<PrivateZone> listPrivateDnsZonesByResourceGroup(String resourceGroupName) {
        return privatednsManager.privateZones().listByResourceGroup(resourceGroupName);
    }

    public PagedList<VirtualNetworkLinkInner> listNetworkLinksByPrivateDnsZoneName(String resourceGroupName, String dnsZoneName) {
        return privatednsManager.virtualNetworkLinks().inner().list(resourceGroupName, dnsZoneName);
    }

    private VirtualNetworkLinkInner getNetworkLinkByPrivateDnsZone(String resourceGroupName, String dnsZoneName, String virtualNetworkLinkName) {
        return virtualNetworkLinkName == null ? null
                : privatednsManager.virtualNetworkLinks().inner().get(resourceGroupName, dnsZoneName, virtualNetworkLinkName);
    }

    public boolean checkIfDnsZonesDeployed(String resourceGroupName, List<AzurePrivateDnsZoneServiceEnum> services) {
        LOGGER.debug("Checking DNS Zones for services {}", services.stream()
                .map(AzurePrivateDnsZoneServiceEnum::getDnsZoneName)
                .collect(Collectors.toList()));

        PagedList<PrivateZone> dnsZones = listPrivateDnsZonesByResourceGroup(resourceGroupName);
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
            PagedList<VirtualNetworkLinkInner> virtualNetworkLinks = listNetworkLinksByPrivateDnsZoneName(resourceGroupName, dnsZoneName);
            if (virtualNetworkLinks.isEmpty()) {
                LOGGER.info("Network link for network {} not found for DNS zone {}!", networkId, dnsZoneName);
                return false;
            } else if (!isNetworkLinkCreated(networkId, virtualNetworkLinks)) {
                LOGGER.info("Network link for network {} and DNS Zone {} is not provisioned successfully yet!", networkId, dnsZoneName);
                return false;
            }
        }
        return true;
    }

    public boolean isNetworkLinkCreated(String networkId, PagedList<VirtualNetworkLinkInner> virtualNetworkLinks) {
        return virtualNetworkLinks.stream()
                .filter(networkLink -> networkId.equals(networkLink.name()))
                .anyMatch(networkLink -> networkLink.provisioningState().equals(SUCCEEDED)
                        && networkLink.virtualNetworkLinkState().equals(VirtualNetworkLinkState.COMPLETED));
    }

    private <T> T handleAuthException(Supplier<T> function) {
        return azureAuthExceptionHandler.handleAuthException(function);
    }

    private void handleAuthException(Runnable function) {
        azureAuthExceptionHandler.handleAuthException(function);
    }

    public Completable deleteGenericResourceByIdAsync(String databaseServerId) {
        return handleAuthException(() -> azure.genericResources().deleteByIdAsync(databaseServerId));
    }

    private DiskEncryptionSetInner createDiskEncryptionSetInner(String sourceVaultId, String encryptionKeyUrl, String location, Map<String, String> tags) {
        SourceVault sourceVault = new SourceVault().withId(sourceVaultId);
        KeyVaultAndKeyReference keyUrl = new KeyVaultAndKeyReference().withKeyUrl(encryptionKeyUrl).withSourceVault(sourceVault);
        EncryptionSetIdentity eSetId = new EncryptionSetIdentity().withType(DiskEncryptionSetIdentityType.SYSTEM_ASSIGNED);
        return (DiskEncryptionSetInner) new DiskEncryptionSetInner()
                .withEncryptionType(DiskEncryptionSetType.ENCRYPTION_AT_REST_WITH_CUSTOMER_KEY)
                .withActiveKey(keyUrl)
                .withIdentity(eSetId)
                .withLocation(location)
                .withTags(tags);
    }

    public DiskEncryptionSetInner getDiskEncryptionSetByName(String resourceGroupName, String diskEncryptionSetName) {
        return handleAuthException(() -> {
            // The Disk encryption set operations are not exposed in public API, so have to rely on underlying DiskEncryptionSetsInner
            DiskEncryptionSetsInner dSetsIn = computeManager.inner().diskEncryptionSets();
            return dSetsIn.getByResourceGroup(resourceGroupName, diskEncryptionSetName);
        });
    }

    public DiskEncryptionSetInner createDiskEncryptionSet(String diskEncryptionSetName, String encryptionKeyUrl, String location,
            String resourceGroupName, String sourceVaultId, Map<String, String> tags) {
        return handleAuthException(() -> {
            // The Disk encryption set operations are not exposed in public API, so have to rely on underlying DiskEncryptionSetsInner
            DiskEncryptionSetInner desIn = createDiskEncryptionSetInner(sourceVaultId, encryptionKeyUrl, location, tags);
            DiskEncryptionSetsInner dSetsIn = computeManager.inner().diskEncryptionSets();
            return dSetsIn.createOrUpdate(resourceGroupName, diskEncryptionSetName, desIn);
        });
    }

    public void grantKeyVaultAccessPolicyToServicePrincipal(String resourceGroupName, String vaultName, String principalObjectId) {
        handleAuthException(() -> {
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

    public void removeKeyVaultAccessPolicyFromServicePrincipal(String resourceGroupName, String vaultName, String principalObjectId) {
        handleAuthException(() -> {
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
        handleAuthException(() -> {
            // The Disk encryption set operations are not exposed in public API, so have to rely on underlying DiskEncryptionSetsInner
            DiskEncryptionSetsInner dSetsIn = computeManager.inner().diskEncryptionSets();
            dSetsIn.delete(resourceGroup, diskEncryptionSetName);
        });
    }

    public Optional<String> getAccessToken() {
        return azureClientCredentials.getAccesToken();
    }

}
