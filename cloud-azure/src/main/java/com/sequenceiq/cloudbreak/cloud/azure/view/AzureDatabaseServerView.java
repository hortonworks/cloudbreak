package com.sequenceiq.cloudbreak.cloud.azure.view;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseServer;
import com.sequenceiq.common.model.AzureDatabaseType;
import com.sequenceiq.common.model.AzureHighAvailabiltyMode;

public class AzureDatabaseServerView {

    public static final String DB_VERSION = "dbVersion";

    @VisibleForTesting
    static final String BACKUP_RETENTION_DAYS = "backupRetentionDays";

    @VisibleForTesting
    static final String GEO_REDUNDANT_BACKUP = "geoRedundantBackup";

    @VisibleForTesting
    static final String SKU_CAPACITY = "skuCapacity";

    @VisibleForTesting
    static final String SKU_FAMILY = "skuFamily";

    @VisibleForTesting
    static final String SKU_TIER = "skuTier";

    @VisibleForTesting
    static final String STORAGE_AUTO_GROW = "storageAutoGrow";

    @VisibleForTesting
    static final String KEY_VAULT_URL = "keyVaultUrl";

    @VisibleForTesting
    static final String KEY_VAULT_RESOURCE_GROUP_NAME = "keyVaultResourceGroupName";

    private static final String STAND_BY_AVAILABILITY_ZONE = "standbyAvailabilityZone";

    private static final String AVAILABILITY_ZONE = "availabilityZone";

    private static final int NUM_MB_IN_GB = 1024;

    private static final String ZONE_1 = "1";

    private final DatabaseServer databaseServer;

    public AzureDatabaseServerView(DatabaseServer databaseServer) {
        this.databaseServer = databaseServer;
    }

    public Long getStorageSizeInGb() {
        return databaseServer.getStorageSize();
    }

    public Long getAllocatedStorageInMb() {
        return databaseServer.getStorageSize() != null ? databaseServer.getStorageSize() * NUM_MB_IN_GB : null;
    }

    public Integer getBackupRetentionDays() {
        return databaseServer.getParameter(BACKUP_RETENTION_DAYS, Integer.class);
    }

    public Integer getSkuCapacity() {
        return databaseServer.getParameter(SKU_CAPACITY, Integer.class);
    }

    public String getSkuFamily() {
        return databaseServer.getStringParameter(SKU_FAMILY);
    }

    public String getSkuName() {
        return databaseServer.getFlavor();
    }

    public String getSkuTier() {
        return databaseServer.getStringParameter(SKU_TIER);
    }

    public String getDbServerName() {
        return databaseServer.getServerId();
    }

    public String getDatabaseType() {
        if (databaseServer.getEngine() == null) {
            return null;
        }
        switch (databaseServer.getEngine()) {
            case POSTGRESQL:
                return "postgres";
            default:
                throw new IllegalStateException("Unsupported Azure Database Server engine " + databaseServer.getEngine());
        }
    }

    public String getDbVersion() {
        return databaseServer.getStringParameter(DB_VERSION);
    }

    public Boolean getGeoRedundantBackup() {
        return databaseServer.getParameter(GEO_REDUNDANT_BACKUP, Boolean.class);
    }

    public Boolean getStorageAutoGrow() {
        return databaseServer.getParameter(STORAGE_AUTO_GROW, Boolean.class);
    }

    public String getStandbyAvailabilityZone() {
        return databaseServer.getParameter(STAND_BY_AVAILABILITY_ZONE, String.class);
    }

    public boolean useStandbyAvailabilityZone() {
        return !Strings.isNullOrEmpty(getStandbyAvailabilityZone());
    }

    public String getAvailabilityZone() {
        String zone = databaseServer.getParameter(AVAILABILITY_ZONE, String.class);
        return Strings.isNullOrEmpty(zone) ? ZONE_1 : zone;
    }

    public String getAdminLoginName() {
        return databaseServer.getRootUserName();
    }

    public String getAdminPassword() {
        return databaseServer.getRootPassword();
    }

    public Integer getPort() {
        return databaseServer.getPort();
    }

    public boolean isUseSslEnforcement() {
        return databaseServer.isUseSslEnforcement();
    }

    public String getLocation() {
        return databaseServer.getLocation();
    }

    public String getKeyVaultUrl() {
        return databaseServer.getStringParameter(KEY_VAULT_URL);
    }

    public String getKeyVaultResourceGroupName() {
        return databaseServer.getStringParameter(KEY_VAULT_RESOURCE_GROUP_NAME);
    }

    public AzureDatabaseType getAzureDatabaseType() {
        return AzureDatabaseType.safeValueOf(databaseServer.getStringParameter(AzureDatabaseType.AZURE_DATABASE_TYPE_KEY));
    }

    public AzureHighAvailabiltyMode getHighAvailabilityMode() {
        return AzureHighAvailabiltyMode.safeValueOf(databaseServer.getStringParameter(AzureHighAvailabiltyMode.AZURE_HA_MODE_KEY));
    }
}
