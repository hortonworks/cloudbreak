package com.sequenceiq.redbeams.api.endpoint.v4.stacks.gcp;

import java.util.Map;

import javax.validation.constraints.Min;
import javax.validation.constraints.Pattern;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.mappable.MappableBase;
import com.sequenceiq.redbeams.doc.ModelDescriptions.GcpDatabaseServerModelDescriptions;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class GcpDatabaseServerV4Parameters extends MappableBase {

    private static final String BACKUP_RETENTION_DAYS = "backupRetentionDays";

    private static final String DB_VERSION = "dbVersion";

    @Min(value = 7, message = "backupRetentionDays must be 7 or higher")
    @ApiModelProperty(GcpDatabaseServerModelDescriptions.BACKUP_RETENTION_DAYS)
    private Integer backupRetentionDays;

    @Pattern(regexp = "\\d+(?:\\.\\d)?")
    @ApiModelProperty(GcpDatabaseServerModelDescriptions.DB_VERSION)
    private String dbVersion;

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

    @Override
    public Map<String, Object> asMap() {
        Map<String, Object> map = super.asMap();
        putIfValueNotNull(map, BACKUP_RETENTION_DAYS, backupRetentionDays);
        putIfValueNotNull(map, DB_VERSION, dbVersion);
        return map;
    }

    @Override
    @JsonIgnore
    @ApiModelProperty(hidden = true)
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.GCP;
    }

    @Override
    public void parse(Map<String, Object> parameters) {
        backupRetentionDays = getInt(parameters, BACKUP_RETENTION_DAYS);
        dbVersion = getParameterOrNull(parameters, DB_VERSION);
    }
}
