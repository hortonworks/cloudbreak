package com.sequenceiq.freeipa.api.v2.freeipa.model.rebuild;

import static com.sequenceiq.freeipa.api.v2.freeipa.doc.FreeIpaV2ModelDescriptions.DATA_BACKUP_STORAGE_PATH;
import static com.sequenceiq.freeipa.api.v2.freeipa.doc.FreeIpaV2ModelDescriptions.FULL_BACKUP_STORAGE_PATH;
import static com.sequenceiq.freeipa.api.v2.freeipa.doc.FreeIpaV2ModelDescriptions.INSTANCE_TO_REPAIR_FQDN;

import jakarta.validation.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.validation.ValidCrn;
import com.sequenceiq.service.api.doc.ModelDescriptions;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "RebuildV2Request")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RebuildV2Request {
    @ValidCrn(resource = CrnResourceDescriptor.ENVIRONMENT)
    @NotEmpty
    @Schema(description = ModelDescriptions.ENVIRONMENT_CRN, required = true)
    private String environmentCrn;

    @ValidCrn(resource = CrnResourceDescriptor.FREEIPA)
    @NotEmpty
    @Schema(description = ModelDescriptions.CRN, required = true)
    private String resourceCrn;

    @NotEmpty
    @Schema(description = INSTANCE_TO_REPAIR_FQDN, required = true)
    private String instanceToRestoreFqdn;

    @NotEmpty
    @Schema(description = FULL_BACKUP_STORAGE_PATH, required = true)
    private String fullBackupStorageLocation;

    @NotEmpty
    @Schema(description = DATA_BACKUP_STORAGE_PATH, required = true)
    private String dataBackupStorageLocation;

    public String getEnvironmentCrn() {
        return environmentCrn;
    }

    public void setEnvironmentCrn(String environmentCrn) {
        this.environmentCrn = environmentCrn;
    }

    public String getResourceCrn() {
        return resourceCrn;
    }

    public void setResourceCrn(String resourceCrn) {
        this.resourceCrn = resourceCrn;
    }

    public String getInstanceToRestoreFqdn() {
        return instanceToRestoreFqdn;
    }

    public void setInstanceToRestoreFqdn(String instanceToRestoreFqdn) {
        this.instanceToRestoreFqdn = instanceToRestoreFqdn;
    }

    public String getFullBackupStorageLocation() {
        return fullBackupStorageLocation;
    }

    public void setFullBackupStorageLocation(String fullBackupStorageLocation) {
        this.fullBackupStorageLocation = fullBackupStorageLocation;
    }

    public String getDataBackupStorageLocation() {
        return dataBackupStorageLocation;
    }

    public void setDataBackupStorageLocation(String dataBackupStorageLocation) {
        this.dataBackupStorageLocation = dataBackupStorageLocation;
    }

    @Override
    public String toString() {
        return "RebuildV2Request{" +
                "environmentCrn='" + environmentCrn + '\'' +
                ", resourceCrn='" + resourceCrn + '\'' +
                ", instanceToRestoreFqdn='" + instanceToRestoreFqdn + '\'' +
                ", fullBackupStorageLocation='" + fullBackupStorageLocation + '\'' +
                ", dataBackupStorageLocation='" + dataBackupStorageLocation + '\'' +
                '}';
    }
}
