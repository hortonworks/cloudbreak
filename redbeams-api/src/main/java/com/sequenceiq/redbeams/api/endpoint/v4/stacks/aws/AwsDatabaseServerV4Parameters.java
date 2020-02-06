package com.sequenceiq.redbeams.api.endpoint.v4.stacks.aws;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.mappable.MappableBase;
import com.sequenceiq.redbeams.doc.ModelDescriptions.AwsDatabaseServerModelDescriptions;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class AwsDatabaseServerV4Parameters extends MappableBase {

    public static final String MULTI_AZ = "multiAZ";

    @ApiModelProperty(AwsDatabaseServerModelDescriptions.BACKUP_RETENTION_PERIOD)
    private Integer backupRetentionPeriod;

    @ApiModelProperty(AwsDatabaseServerModelDescriptions.ENGINE_VERSION)
    private String engineVersion;

    // This is a String because of https://github.com/swagger-api/swagger-codegen/issues/7391
    @ApiModelProperty(AwsDatabaseServerModelDescriptions.MULTI_AZ)
    private String multiAZ;

    @ApiModelProperty(AwsDatabaseServerModelDescriptions.STORAGE_TYPE)
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
    @ApiModelProperty(hidden = true)
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
}
