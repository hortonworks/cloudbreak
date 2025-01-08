package com.sequenceiq.cloudbreak.cloud.azure;

import static com.sequenceiq.common.model.PrivateEndpointType.USE_PRIVATE_ENDPOINT;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.cloud.azure.view.AzureDatabaseServerView;
import com.sequenceiq.cloudbreak.cloud.azure.view.AzureNetworkView;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.common.model.AzureDatabaseType;

import io.micrometer.common.util.StringUtils;

@Component
public class AzureFlexibleServerDatabaseTemplateModelBuilder implements AzureDatabaseTemplateModelBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureFlexibleServerDatabaseTemplateModelBuilder.class);

    private static final String GENERAL_PURPOSE = "GeneralPurpose";

    private static final String MEMORY_OPTIMIZED = "MemoryOptimized";

    private static final Set<String> GENERAL_PURPOSE_INSTANCE_TYPES = Set.of("Standard_D2s_v3", "Standard_D4s_v3", "Standard_D2ds_v4", "Standard_D4ds_v4",
            "Standard_D2ds_v5", "Standard_D4ds_v5");

    private static final Set<String> MEMORY_OPTIMIZED_INSTANCE_TYPES = Set.of("Standard_E2s_v3", "Standard_E4s_v3", "Standard_E2ds_v4", "Standard_E4ds_v4",
            "Standard_E2ds_v5", "Standard_E4ds_v5");

    @Inject
    private AzureUtils azureUtils;

    @Override
    public Map<String, Object> buildModel(AzureDatabaseServerView azureDatabaseServerView, AzureNetworkView azureNetworkView, DatabaseStack databaseStack) {
        Map<String, Object> model = new HashMap<>();
        model.put("adminLoginName", azureDatabaseServerView.getAdminLoginName());
        model.put("adminPassword", azureDatabaseServerView.getAdminPassword());
        model.put("backupRetentionDays", azureDatabaseServerView.getBackupRetentionDays());
        model.put("dbServerName", azureDatabaseServerView.getDbServerName());
        model.put("dbVersion", azureDatabaseServerView.getDbVersion());
        model.put("geoRedundantBackup", azureDatabaseServerView.getGeoRedundantBackup());
        if (azureDatabaseServerView.getPort() != null) {
            LOGGER.warn("Found port {} in database stack, but Azure ignores it", azureDatabaseServerView.getPort());
        }
        model.put("serverTags", databaseStack.getTags());
        model.put("usePrivateEndpoints", USE_PRIVATE_ENDPOINT.equals(azureNetworkView.getEndpointType()));
        model.put("subnetIdForPrivateEndpoint", azureNetworkView.getSubnetIdForPrivateEndpoint());
        model.put("existingDatabasePrivateDnsZoneId", azureNetworkView.getExistingDatabasePrivateDnsZoneId());
        model.put("flexibleServerDelegatedSubnetId", azureNetworkView.getFlexibleServerDelegatedSubnetId());
        model.put("useDelegatedSubnet", StringUtils.isNotEmpty(azureNetworkView.getExistingDatabasePrivateDnsZoneId()) &&
                StringUtils.isNotEmpty(azureNetworkView.getFlexibleServerDelegatedSubnetId()));
        model.put("skuName", azureDatabaseServerView.getSkuName());
        model.put("skuSizeGB", azureDatabaseServerView.getStorageSizeInGb());
        model.put("skuTier", getSkuTier(azureDatabaseServerView));
        model.put("useSslEnforcement", azureDatabaseServerView.isUseSslEnforcement());
        model.put("location", azureDatabaseServerView.getLocation());
        model.put("highAvailability", azureDatabaseServerView.getHighAvailabilityMode().templateValue());
        model.put("availabilityZone", azureDatabaseServerView.getAvailabilityZone());
        model.put("useAvailabilityZone", azureDatabaseServerView.useAvailabilityZone());
        model.put("standbyAvailabilityZone", azureDatabaseServerView.getStandbyAvailabilityZone());
        model.put("useStandbyAvailabilityZone", azureDatabaseServerView.useStandbyAvailabilityZone());
        model.put("privateEndpointName", String.format("pe-%s-to-%s",
                azureUtils.encodeString(azureUtils.getResourceName(azureNetworkView.getSubnetList().getFirst())), azureDatabaseServerView.getDbServerName()));
        model.put("deploymentType", databaseStack.getDeploymentType());
        addEncryptionParameters(model, azureDatabaseServerView);
        return model;
    }

    private void addEncryptionParameters(Map<String, Object> model, AzureDatabaseServerView azureDatabaseServerView) {
        String encryptionUserManagedIdentity = azureDatabaseServerView.getEncryptionUserManagedIdentity();
        String keyVaultUrl = azureDatabaseServerView.getKeyVaultUrl();
        if (!Strings.isNullOrEmpty(encryptionUserManagedIdentity) && !Strings.isNullOrEmpty(keyVaultUrl)) {
            model.put("dataEncryption", true);
            model.put("encryptionKeyName", keyVaultUrl);
            model.put("encryptionUserManagedIdentity", encryptionUserManagedIdentity);
        } else {
            model.put("dataEncryption", false);
        }
    }

    @Override
    public AzureDatabaseType azureDatabaseType() {
        return AzureDatabaseType.FLEXIBLE_SERVER;
    }

    private String getSkuTier(AzureDatabaseServerView azureDatabaseServerView) {
        String skuTier = azureDatabaseServerView.getSkuTier();
        String skuName = azureDatabaseServerView.getSkuName();
        if (Objects.isNull(skuTier)) {
            if (Objects.isNull(skuName)) {
                return null;
            } else {
                return getSkuTierFromSkuName(skuName);
            }
        } else {
            return azureDatabaseServerView.getSkuTier();
        }
    }

    private String getSkuTierFromSkuName(String skuName) {
        if (GENERAL_PURPOSE_INSTANCE_TYPES.contains(skuName)) {
            return GENERAL_PURPOSE;
        } else if (MEMORY_OPTIMIZED_INSTANCE_TYPES.contains(skuName)) {
            return MEMORY_OPTIMIZED;
        } else {
            return null;
        }
    }
}