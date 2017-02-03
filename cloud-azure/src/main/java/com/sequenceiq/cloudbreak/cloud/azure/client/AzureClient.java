package com.sequenceiq.cloudbreak.cloud.azure.client;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import com.microsoft.azure.AzureEnvironment;
import com.microsoft.azure.PagedList;
import com.microsoft.azure.credentials.ApplicationTokenCredentials;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.compute.VirtualMachine;
import com.microsoft.azure.management.compute.VirtualMachineInstanceView;
import com.microsoft.azure.management.network.LoadBalancer;
import com.microsoft.azure.management.network.NetworkInterface;
import com.microsoft.azure.management.network.NetworkInterfaces;
import com.microsoft.azure.management.network.NetworkSecurityGroup;
import com.microsoft.azure.management.network.PublicIpAddress;
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
import com.microsoft.azure.storage.blob.BlobContainerPermissions;
import com.microsoft.azure.storage.blob.BlobContainerPublicAccessType;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import com.microsoft.azure.storage.blob.CloudPageBlob;
import com.microsoft.azure.storage.blob.CopyState;
import com.microsoft.azure.storage.blob.ListBlobItem;

import okhttp3.logging.HttpLoggingInterceptor;

public class AzureClient {

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
        ApplicationTokenCredentials creds = new ApplicationTokenCredentials(clientId, tenantId, secretKey, AzureEnvironment.AZURE)
                .withDefaultSubscriptionId(subscriptionId);
        azure = Azure
                .configure()
                .withLogLevel(HttpLoggingInterceptor.Level.BASIC)
                .authenticate(creds)
                .withDefaultSubscription();
    }

    public ResourceGroup getResourceGroup(String name) {
        return azure.resourceGroups().getByName(name);
    }

    public ResourceGroups getResourceGroups()  {
        return azure.resourceGroups();
    }

    public boolean resourceGroupExists(String name) {
        return getResourceGroups().checkExistence(name);
    }

    public void deleteResourceGroup(String name) {
        azure.resourceGroups().delete(name);
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

    public void deleteTemplateDeployment(String resourceGroupName, String deploymentName) {
        azure.deployments().delete(resourceGroupName, deploymentName);
    }

    public Deployment getTemplateDeployment(String resourceGroupName, String deploymentName) {
        return azure.deployments().getByGroup(resourceGroupName, deploymentName);
    }

    public DeploymentOperations getTemplateDeploymentOperations(String resourceGroupName, String deploymentName) {
        return azure.deployments().getByGroup(resourceGroupName, deploymentName).deploymentOperations();
    }

    public void cancelTemplateDeployments(String resourceGroupName, String deploymentName) {
        azure.deployments().getByGroup(resourceGroupName, deploymentName).cancel();
    }

    public Deployments getTemplateDeployments(String resourceGroupName) {
        return azure.deployments();
    }

    public StorageAccounts getStorageAccounts() {
        return azure.storageAccounts();
    }

    public PagedList<StorageAccount> getStorageAccountsForResourceGroup(String resourceGroup) {
        return azure.storageAccounts().listByGroup(resourceGroup);
    }

    public void deleteStorageAccount(String resourceGroup, String storageName) {
        azure.storageAccounts().delete(resourceGroup, storageName);
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
        return azure.storageAccounts().getByGroup(resourceGroup, storageName);
    }

    public void deleteContainerInStorage(String resourceGroup, String storageName, String containerName) throws Exception {
        CloudBlobContainer container = getBlobContainer(resourceGroup, storageName, containerName);
        container.deleteIfExists();
    }

    public void deleteBlobInStorageContainer(String resourceGroup, String storageName, String containerName, String blobName) throws Exception {
        CloudBlobContainer container = getBlobContainer(resourceGroup, storageName, containerName);
        CloudBlockBlob blob = container.getBlockBlobReference(blobName);
        blob.deleteIfExists();
    }

    public void createContainerInStorage(String resourceGroup, String storageName, String containerName) throws Exception {
        CloudBlobContainer container = getBlobContainer(resourceGroup, storageName, containerName);
        container.createIfNotExists();
        setPublicPermissionOnContainer(resourceGroup, storageName, containerName);
    }

    public void setPublicPermissionOnContainer(String resourceGroup, String storageName, String containerName) throws Exception {
        CloudBlobContainer container = getBlobContainer(resourceGroup, storageName, containerName);
        BlobContainerPermissions containerPermissions = new BlobContainerPermissions();
        containerPermissions.setPublicAccess(BlobContainerPublicAccessType.CONTAINER);
        container.uploadPermissions(containerPermissions);
    }

    public String copyImageBlobInStorageContainer(String resourceGroup, String storageName, String containerName, String sourceBlob) throws Exception {
        CloudBlobContainer container = getBlobContainer(resourceGroup, storageName, containerName);
        CloudPageBlob cloudPageBlob = container.getPageBlobReference(sourceBlob.substring(sourceBlob.lastIndexOf("/") + 1));
        return cloudPageBlob.startCopy(new URI(sourceBlob));
    }

    public CopyState getCopyStatus(String resourceGroup, String storageName, String containerName, String sourceBlob) throws Exception {
        CloudBlobContainer container = getBlobContainer(resourceGroup, storageName, containerName);
        CloudPageBlob cloudPageBlob = container.getPageBlobReference(sourceBlob.substring(sourceBlob.lastIndexOf("/") + 1));
        container.downloadAttributes();
        cloudPageBlob.downloadAttributes();
        return cloudPageBlob.getCopyState();
    }

    public List<ListBlobItem> listBlobInStorage(String resourceGroup, String storageName, String containerName) throws Exception {
        CloudBlobContainer container = getBlobContainer(resourceGroup, storageName, containerName);
        List<ListBlobItem> targetCollection = new ArrayList<ListBlobItem>();
        container.listBlobs().iterator().forEachRemaining(targetCollection::add);
        return targetCollection;
    }

    public CloudBlobContainer getBlobContainer(String resourceGroup, String storageName, String containerName) throws Exception {
        List<StorageAccountKey> keys = getStorageAccountKeys(resourceGroup, storageName);
        String storageConnectionString = String.format("DefaultEndpointsProtocol=http;AccountName=%s;AccountKey=%s", storageName, keys.get(0).value());
        CloudStorageAccount storageAccount = CloudStorageAccount.parse(storageConnectionString);
        CloudBlobClient blobClient = storageAccount.createCloudBlobClient();
        CloudBlobContainer container = blobClient.getContainerReference(containerName);
        return container;
    }

    public PagedList<VirtualMachine> getVirtualMachines(String resourceGroup) {
        return azure.virtualMachines().listByGroup(resourceGroup);
    }

    public VirtualMachine getVirtualMachine(String resourceGroup, String vmName) {
        return azure.virtualMachines().getByGroup(resourceGroup, vmName);
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

    public void deallocateVirtualMachine(String resourceGroup, String vmName) {
        getVirtualMachine(resourceGroup, vmName).deallocate();
    }

    public void deleteVirtualMachine(String resourceGroup, String vmName) {
        String id = getVirtualMachine(resourceGroup, vmName).id();
        azure.virtualMachines().delete(id);
    }

    public void startVirtualMachine(String resourceGroup, String vmName) {
        azure.virtualMachines().start(resourceGroup, vmName);
    }

    public void stopVirtualMachine(String resourceGroup, String vmName) {
        azure.virtualMachines().powerOff(resourceGroup, vmName);
    }

    public void deletePublicIpAddressByName(String resourceGroup, String ipName) {
        String id = getPublicIpAddress(resourceGroup, ipName).id();
        azure.publicIpAddresses().delete(id);
    }

    public void deletePublicIpAddressById(String ipId) {
        azure.publicIpAddresses().delete(ipId);
    }

    public PublicIpAddress getPublicIpAddress(String resourceGroup, String ipName) {
        return azure.publicIpAddresses().getByGroup(resourceGroup, ipName);
    }

    public PagedList<PublicIpAddress> getPublicIpAddresses(String resourceGroup) {
        return azure.publicIpAddresses().listByGroup(resourceGroup);
    }

    public PublicIpAddress getPublicIpAddressById(String ipId) {
        return azure.publicIpAddresses().getById(ipId);
    }

    public void deleteNetworkInterface(String resourceGroup, String networkInterfaceName) {
        azure.networkInterfaces().delete(resourceGroup, networkInterfaceName);
    }

    public NetworkInterface getNetworkInterface(String resourceGroup, String networkInterfaceName) {
        return azure.networkInterfaces().getByGroup(resourceGroup, networkInterfaceName);
    }

    public NetworkInterface getNetworkInterfaceById(String networkInterfaceId) {
        return azure.networkInterfaces().getById(networkInterfaceId);
    }

    public NetworkInterfaces getNetworkInterfaces(String resourceGroup) {
        return azure.networkInterfaces();
    }

    public Subnet getSubnetProperties(String resourceGroup, String virtualNetwork, String subnet) {
        return azure.networks().getByGroup(resourceGroup, virtualNetwork).subnets().get(subnet);
    }

    public NetworkSecurityGroup getSecurityGroupProperties(String resourceGroup, String securityGroup) {
        return azure.networkSecurityGroups().getByGroup(resourceGroup, securityGroup);
    }

    public LoadBalancer getLoadBalancer(String name, String loadBalancerName) {
        return azure.loadBalancers().getByGroup(name, loadBalancerName);
    }

    public List<String> getLoadBalancerIps(String name, String loadBalancerName) {
        List<String> ipList = new ArrayList<String>();
        List<String> publicIpAddressIds = getLoadBalancer(name, loadBalancerName).publicIpAddressIds();
        for (String publicIpAddressId : publicIpAddressIds) {
            PublicIpAddress publicIpAddress = getPublicIpAddressById(publicIpAddressId);
            ipList.add(publicIpAddress.ipAddress());
        }
        return ipList;
    }

}
