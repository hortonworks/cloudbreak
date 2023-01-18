package com.sequenceiq.redbeams.api.endpoint.v4.stacks.aws;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.mappable.MappableBase;
import com.sequenceiq.redbeams.doc.ModelDescriptions.AwsDatabaseServerModelDescriptions;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class AwsDatabaseServerV4Parameters extends MappableBase {

    public static final String MULTI_AZ = "multiAZ";

    @Schema(description = AwsDatabaseServerModelDescriptions.BACKUP_RETENTION_PERIOD)
    private Integer backupRetentionPeriod;

    @Schema(description = AwsDatabaseServerModelDescriptions.ENGINE_VERSION)
    private String engineVersion;

    // This is a String because of https://github.com/swagger-api/swagger-codegen/issues/7391
    @Schema(description = AwsDatabaseServerModelDescriptions.MULTI_AZ)
    private String multiAZ;

    @Schema(description = AwsDatabaseServerModelDescriptions.STORAGE_TYPE)
    private String storageType;

    public Integer getBackupRetentionPeriod() {
        return backupRetentionPeriod;
    }

    public void setBackupRetentionPeriod(Integer backupRetentionPeriod) {
        this.backupRetentionPeriod = backupRetentionPeriod;
    }

    public String getEngineVersion() {
        return engineVersion;
    }

    public void setEngineVersion(String engineVersion) {
        this.engineVersion = engineVersion;
    }

    public String getMultiAZ() {
        return multiAZ;
    }

    public void setMultiAZ(String multiAZ) {
        this.multiAZ = multiAZ;
    }

    public String getStorageType() {
        return storageType;
    }

    public void setStorageType(String storageType) {
        this.storageType = storageType;
    }

    @Override
    public Map<String, Object> asMap() {
        Map<String, Object> map = super.asMap();
        putIfValueNotNull(map, "backupRetentionPeriod", backupRetentionPeriod);
        putIfValueNotNull(map, "engineVersion", engineVersion);
        putIfValueNotNull(map, MULTI_AZ, multiAZ);
        putIfValueNotNull(map, "storageType", storageType);
        return map;
    }

    @Override
    @JsonIgnore
    @Schema(hidden = true)
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.AWS;
    }

    @Override
    public void parse(Map<String, Object> parameters) {
        backupRetentionPeriod = getInt(parameters, "backupRetentionPeriod");
        engineVersion = getParameterOrNull(parameters, "engineVersion");
        multiAZ = getParameterOrNull(parameters, MULTI_AZ);
        storageType = getParameterOrNull(parameters, "storageType");
    }

    @Override
    public String toString() {
        return "AwsDatabaseServerV4Parameters{" +
                "backupRetentionPeriod=" + backupRetentionPeriod +
                ", engineVersion='" + engineVersion + '\'' +
                ", multiAZ='" + multiAZ + '\'' +
                ", storageType='" + storageType + '\'' +
                "} " + super.toString();
    }
}
