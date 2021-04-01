package com.sequenceiq.cloudbreak.cloud.azure;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.microsoft.azure.AzureEnvironment;
import com.microsoft.azure.credentials.ApplicationTokenCredentials;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.compute.DiskEncryptionSetIdentityType;
import com.microsoft.azure.management.compute.DiskEncryptionSetType;
import com.microsoft.azure.management.compute.EncryptionSetIdentity;
import com.microsoft.azure.management.compute.KeyVaultAndKeyReference;
import com.microsoft.azure.management.compute.SourceVault;
import com.microsoft.azure.management.compute.implementation.ComputeManager;
import com.microsoft.azure.management.compute.implementation.DiskEncryptionSetInner;
import com.microsoft.azure.management.compute.implementation.DiskEncryptionSetsInner;
import com.microsoft.azure.management.keyvault.KeyPermissions;
import com.microsoft.azure.management.resources.ResourceGroup;
import com.sequenceiq.cloudbreak.cloud.EncryptionResources;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClientService;
import com.sequenceiq.cloudbreak.cloud.azure.view.AzureCredentialView;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.cloud.model.encryption.CreatedEncryptionResources;
import com.sequenceiq.cloudbreak.cloud.model.encryption.EncryptionResourcesCreationRequest;

@Service
public class AzureEncryptionResources implements EncryptionResources {
    private static final Logger LOGGER = LoggerFactory.getLogger(AzureEncryptionResources.class);

    @Inject
    private AzureClientService azureClientService;



    @Inject
    private AzureUtils azureUtils;

    @Override
    public Platform platform() {
        return AzureConstants.PLATFORM;
    }

    @Override
    public Variant variant() {
        return AzureConstants.VARIANT;
    }

    @Override
    public CreatedEncryptionResources createDiskEncryptionSet(EncryptionResourcesCreationRequest encryptionResourcesCreationRequest) {
        CloudCredential cloudCredential = encryptionResourcesCreationRequest.getCloudCredential();
        AzureCredentialView azureCredentialView = new AzureCredentialView(cloudCredential);
        String subscriptionId = azureCredentialView.getSubscriptionId();
        ApplicationTokenCredentials applicationTokenCredentials = new ApplicationTokenCredentials(azureCredentialView.getAccessKey(),
                azureCredentialView.getTenantId(), azureCredentialView.getSecretKey(), AzureEnvironment.AZURE);

        CreatedEncryptionResources diskEncryptionSet = createResourceGroupAndDes(encryptionResourcesCreationRequest,
                applicationTokenCredentials, subscriptionId);
        grantAccessPolicyToDes(applicationTokenCredentials, diskEncryptionSet.getEncryptionResourcePrincipalId(), subscriptionId);

        return diskEncryptionSet;
    }

    private void grantAccessPolicyToDes(ApplicationTokenCredentials applicationTokenCredentials, String principalId, String subscriptionId) {
        Azure azure = Azure.authenticate(applicationTokenCredentials).withSubscription(subscriptionId);
        azure.vaults().getByResourceGroup("juhig", "juhig").update().defineAccessPolicy().forObjectId(principalId)
                .allowKeyPermissions(List.of(KeyPermissions.WRAP_KEY, KeyPermissions.UNWRAP_KEY, KeyPermissions.GET)).attach().apply();
    }

    private CreatedEncryptionResources createResourceGroupAndDes(EncryptionResourcesCreationRequest encryptionResourcesCreationRequest,
            ApplicationTokenCredentials applicationTokenCredentials, String subscriptionId) {
        String resourceGroupName = (String) getOrCreateResourceGroup(encryptionResourcesCreationRequest);
        String location = encryptionResourcesCreationRequest.getRegion();
        String encryptionKeyUrl = encryptionResourcesCreationRequest.getEncryptionKeyUrl();
        String sourceVaultId = createSourceVaultId(subscriptionId, resourceGroupName, encryptionKeyUrl);
        SourceVault sourceVault = new SourceVault().withId(sourceVaultId);
        KeyVaultAndKeyReference keyUrl = new KeyVaultAndKeyReference().withKeyUrl(encryptionKeyUrl).withSourceVault(sourceVault);
        DiskEncryptionSetIdentityType desIdType = new DiskEncryptionSetIdentityType().fromString("SystemAssigned");
        EncryptionSetIdentity eSetId = new EncryptionSetIdentity().withType(desIdType);
        DiskEncryptionSetType desType = new DiskEncryptionSetType().fromString("EncryptionAtRestWithCustomerKey");
        DiskEncryptionSetInner desIn = (DiskEncryptionSetInner) new DiskEncryptionSetInner()
                .withEncryptionType(desType).withActiveKey(keyUrl).withIdentity(eSetId).withLocation(location);

        DiskEncryptionSetsInner dSetsIn = ComputeManager.authenticate(applicationTokenCredentials, subscriptionId).inner().diskEncryptionSets();
        String desNameForCreation = azureUtils.generateDESNameByNameAndId(
                String.format("%s-DES-", encryptionResourcesCreationRequest.getEnvName()),
                UUID.randomUUID().toString());
        LOGGER.debug("Creating Disk Encryption Set {}", desNameForCreation);
        DiskEncryptionSetInner createdSet = dSetsIn.createOrUpdate(resourceGroupName, desNameForCreation, desIn);

        return new CreatedEncryptionResources(createdSet.id(), createdSet.identity().principalId(),
                createdSet.location(), createdSet.getTags(), createdSet.name());
    }

    private String getOrCreateResourceGroup(EncryptionResourcesCreationRequest encryptionResourcesCreationRequest) {
        if (encryptionResourcesCreationRequest.isSingleResourceGroup()) {
            return encryptionResourcesCreationRequest.getResourceGroup();
        } else {
            CloudCredential cloudCredential = encryptionResourcesCreationRequest.getCloudCredential();
            AzureClient azureClient = azureClientService.getClient(cloudCredential);
            String region = encryptionResourcesCreationRequest.getRegion();
            Map<String, String> tags = Collections.unmodifiableMap(encryptionResourcesCreationRequest.getTags());
            ResourceGroup resourceGroup;
            String resourceGroupNameForCreation = azureUtils.generateResourceGroupNameByNameAndId(
                    String.format("%s-", encryptionResourcesCreationRequest.getEnvName()),
                    UUID.randomUUID().toString());
            LOGGER.debug("Creating resource group {}", resourceGroupNameForCreation);
            resourceGroup = azureClient.createResourceGroup(resourceGroupNameForCreation, region, tags);
            return resourceGroup.name();
        }
    }

    private String createSourceVaultId(String subscriptionId, String resourceGroupName, String encryptionKeyUrl) {
        String vaultName = encryptionKeyUrl.substring(encryptionKeyUrl.indexOf("https://") + "https://".length(), encryptionKeyUrl.indexOf("."));
        String sourceVaultId = "/subscriptions/" + subscriptionId + "/resourceGroups/"
                + resourceGroupName + "/providers/Microsoft.KeyVault/vaults/" + vaultName;
        return sourceVaultId;
    }
}