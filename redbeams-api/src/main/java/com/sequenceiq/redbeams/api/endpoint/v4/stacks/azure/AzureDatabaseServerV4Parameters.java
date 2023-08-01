package com.sequenceiq.redbeams.api.endpoint.v4.stacks.azure;

import java.util.Map;

import javax.validation.constraints.Min;
import javax.validation.constraints.Pattern;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.mappable.MappableBase;
import com.sequenceiq.common.model.AzureDatabaseType;
import com.sequenceiq.common.model.AzureHighAvailabiltyMode;
import com.sequenceiq.redbeams.doc.ModelDescriptions.AzureDatabaseServerModelDescriptions;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class AzureDatabaseServerV4Parameters extends MappableBase {

    private static final String BACKUP_RETENTION_DAYS = "backupRetentionDays";

    private static final String DB_VERSION = "dbVersion";

    private static final String GEO_REDUNDANT_BACKUP = "geoRedundantBackup";

    private static final String SKU_CAPACITY = "skuCapacity";

    private static final String SKU_FAMILY = "skuFamily";

    private static final String SKU_TIER = "skuTier";

    private static final String STORAGE_AUTO_GROW = "storageAutoGrow";

    private static final String AZURE_DATABASE_TYPE = AzureDatabaseType.AZURE_DATABASE_TYPE_KEY;

    private static final String HIGH_AVAILABILITY = AzureHighAvailabiltyMode.AZURE_HA_MODE_KEY;

    @Min(value = 7, message = "backupRetentionDays must be 7 or higher")
    @ApiModelProperty(AzureDatabaseServerModelDescriptions.BACKUP_RETENTION_DAYS)
    private Integer backupRetentionDays;

    @Pattern(regexp = "\\d+(?:\\.\\d)?")
    @ApiModelProperty(AzureDatabaseServerModelDescriptions.DB_VERSION)
    private String dbVersion;

    @ApiModelProperty(AzureDatabaseServerModelDescriptions.GEO_REDUNDANT_BACKUPS)
    private Boolean geoRedundantBackup;

    @Min(value = 2, message = "skuCapacity must be 2 or higher")
    @ApiModelProperty(AzureDatabaseServerModelDescriptions.SKU_CAPACITY)
    private Integer skuCapacity;

    @ApiModelProperty(AzureDatabaseServerModelDescriptions.SKU_FAMILY)
    private String skuFamily;

    @Pattern(regexp = "Basic|GeneralPurpose|MemoryOptimized")
    @ApiModelProperty(AzureDatabaseServerModelDescriptions.SKU_TIER)
    private String skuTier;

    @ApiModelProperty(AzureDatabaseServerModelDescriptions.STORAGE_AUTO_GROW)
    private Boolean storageAutoGrow;

    @ApiModelProperty(AzureDatabaseServerModelDescriptions.AZURE_DATABASE_TYPE)
    private AzureDatabaseType azureDatabaseType;

    @ApiModelProperty(AzureDatabaseServerModelDescriptions.HIGH_AVAILABILITY)
    private AzureHighAvailabiltyMode highAvailabilityMode;

    public Integer getBackupRetentionDays() {
        return backupRetentionDays;
    }

    public void setBackupRetentionDays(Integer backupRetentionDays) {
        this.backupRetentionDays = backupRetentionDays;
    }

    public String getDbVersion() {
        return dbVersion;
    }

    public void setDbVersion(String dbVersion) {
        this.dbVersion = dbVersion;
    }

    public Boolean getGeoRedundantBackup() {
        return geoRedundantBackup;
    }

    public void setGeoRedundantBackup(Boolean geoRedundantBackup) {
        this.geoRedundantBackup = geoRedundantBackup;
    }

    public Integer getSkuCapacity() {
        return skuCapacity;
    }

    public void setSkuCapacity(Integer skuCapacity) {
        this.skuCapacity = skuCapacity;
    }

    public String getSkuFamily() {
        return skuFamily;
    }

    public void setSkuFamily(String skuFamily) {
        this.skuFamily = skuFamily;
    }

    public String getSkuTier() {
        return skuTier;
    }

    public void setSkuTier(String skuTier) {
        this.skuTier = skuTier;
    }

    public Boolean getStorageAutoGrow() {
        return storageAutoGrow;
    }

    public void setStorageAutoGrow(Boolean storageAutoGrow) {
        this.storageAutoGrow = storageAutoGrow;
    }

    public AzureDatabaseType getAzureDatabaseType() {
        return azureDatabaseType;
    }

    public void setAzureDatabaseType(AzureDatabaseType azureDatabaseType) {
        this.azureDatabaseType = azureDatabaseType;
    }

    public AzureHighAvailabiltyMode getHighAvailabilityMode() {
        return highAvailabilityMode;
    }

    public void setHighAvailabilityMode(AzureHighAvailabiltyMode highAvailabilityMode) {
        this.highAvailabilityMode = highAvailabilityMode;
    }

    @Override
    public Map<String, Object> asMap() {
        Map<String, Object> map = super.asMap();
        putIfValueNotNull(map, BACKUP_RETENTION_DAYS, backupRetentionDays);
        putIfValueNotNull(map, DB_VERSION, dbVersion);
        putIfValueNotNull(map, GEO_REDUNDANT_BACKUP, geoRedundantBackup);
        putIfValueNotNull(map, SKU_CAPACITY, skuCapacity);
        putIfValueNotNull(map, SKU_FAMILY, skuFamily);
        putIfValueNotNull(map, SKU_TIER, skuTier);
        putIfValueNotNull(map, STORAGE_AUTO_GROW, storageAutoGrow);
        putIfValueNotNull(map, AZURE_DATABASE_TYPE, azureDatabaseType == null ? AzureDatabaseType.SINGLE_SERVER.name() : azureDatabaseType.name());
        putIfValueNotNull(map, HIGH_AVAILABILITY, highAvailabilityMode == null ? AzureHighAvailabiltyMode.DISABLED.name() : highAvailabilityMode.name());
        return map;
    }

    @Override
    @JsonIgnore
    @ApiModelProperty(hidden = true)
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.AZURE;
    }

    @Override
    public void parse(Map<String, Object> parameters) {
        backupRetentionDays = getInt(parameters, BACKUP_RETENTION_DAYS);
        dbVersion = getParameterOrNull(parameters, DB_VERSION);
        geoRedundantBackup = Boolean.valueOf(getParameterOrNull(parameters, GEO_REDUNDANT_BACKUP));
        skuCapacity = getInt(parameters, SKU_CAPACITY);
        skuFamily = getParameterOrNull(parameters, SKU_FAMILY);
        skuTier = getParameterOrNull(parameters, SKU_TIER);
        storageAutoGrow = Boolean.valueOf(getParameterOrNull(parameters, STORAGE_AUTO_GROW));
        azureDatabaseType = AzureDatabaseType.safeValueOf(getParameterOrNull(parameters, AZURE_DATABASE_TYPE));
        highAvailabilityMode = AzureHighAvailabiltyMode.safeValueOf(getParameterOrNull(parameters, HIGH_AVAILABILITY));
    }

    @Override
    public String toString() {
        return "AzureDatabaseServerV4Parameters{" +
                "backupRetentionDays=" + backupRetentionDays +
                ", dbVersion='" + dbVersion + '\'' +
                ", geoRedundantBackup=" + geoRedundantBackup +
                ", skuCapacity=" + skuCapacity +
                ", skuFamily='" + skuFamily + '\'' +
                ", skuTier='" + skuTier + '\'' +
                ", storageAutoGrow=" + storageAutoGrow +
                ", azureDatabaseType=" + azureDatabaseType +
                ", highAvailabilityMode=" + highAvailabilityMode +
                "} " + super.toString();
    }
}
