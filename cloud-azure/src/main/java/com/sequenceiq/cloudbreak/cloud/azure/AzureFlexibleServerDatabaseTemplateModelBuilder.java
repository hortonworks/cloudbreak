package com.sequenceiq.cloudbreak.cloud.azure;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.azure.view.AzureDatabaseServerView;
import com.sequenceiq.cloudbreak.cloud.azure.view.AzureNetworkView;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.common.model.AzureDatabaseType;

@Component
public class AzureFlexibleServerDatabaseTemplateModelBuilder implements AzureDatabaseTemplateModelBuilder {
    private static final Logger LOGGER = LoggerFactory.getLogger(AzureFlexibleServerDatabaseTemplateModelBuilder.class);

    private static final String GENERAL_PURPOSE = "GeneralPurpose";

    private static final String MEMORY_OPTIMIZED = "MemoryOptimized";

    private static final Set<String> GENERAL_PURPOSE_INSTANCE_TYPES = Set.of("Standard_D2ds_v4", "Standard_D4ds_v4");

    private static final Set<String> MEMORY_OPTIMIZED_INSTANCE_TYPES = Set.of("Standard_E2ds_v4", "Standard_E4ds_v4");

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
        model.put("skuName", azureDatabaseServerView.getSkuName());
        model.put("skuSizeGB", azureDatabaseServerView.getStorageSizeInGb());
        model.put("skuTier", getSkuTier(azureDatabaseServerView));
        model.put("useSslEnforcement", azureDatabaseServerView.isUseSslEnforcement());
        model.put("location", azureDatabaseServerView.getLocation());
        model.put("highAvailability", azureDatabaseServerView.getHighAvailabilityMode().templateValue());
        return model;
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
