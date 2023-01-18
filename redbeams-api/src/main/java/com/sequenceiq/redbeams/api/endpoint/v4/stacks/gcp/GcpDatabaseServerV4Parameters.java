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

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class GcpDatabaseServerV4Parameters extends MappableBase {

    private static final String BACKUP_RETENTION_DAYS = "backupRetentionDays";

    private static final String ENGINE_VERSION = "engineVersion";

    @Min(value = 7, message = "backupRetentionDays must be 7 or higher")
    @Schema(description = GcpDatabaseServerModelDescriptions.BACKUP_RETENTION_DAYS)
    private Integer backupRetentionDays;

    @Pattern(regexp = "\\d+(?:\\.\\d)?")
    @Schema(description = GcpDatabaseServerModelDescriptions.DB_VERSION)
    private String engineVersion;

    public Integer getBackupRetentionDays() {
        return backupRetentionDays;
    }

    public void setBackupRetentionDays(Integer backupRetentionDays) {
        this.backupRetentionDays = backupRetentionDays;
    }

    public String getEngineVersion() {
        return engineVersion;
    }

    public void setEngineVersion(String engineVersion) {
        this.engineVersion = engineVersion;
    }

    @Override
    public Map<String, Object> asMap() {
        Map<String, Object> map = super.asMap();
        putIfValueNotNull(map, BACKUP_RETENTION_DAYS, backupRetentionDays);
        putIfValueNotNull(map, ENGINE_VERSION, engineVersion);
        return map;
    }

    @Override
    @JsonIgnore
    @Schema(hidden = true)
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.GCP;
    }

    @Override
    public void parse(Map<String, Object> parameters) {
        backupRetentionDays = getInt(parameters, BACKUP_RETENTION_DAYS);
        engineVersion = getParameterOrNull(parameters, ENGINE_VERSION);
    }

    @Override
    public String toString() {
        return "GcpDatabaseServerV4Parameters{" +
                "backupRetentionDays=" + backupRetentionDays +
                ", engineVersion='" + engineVersion + '\'' +
                "} " + super.toString();
    }
}
