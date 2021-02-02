package com.sequenceiq.common.api.backup.base;

import java.io.Serializable;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.common.api.cloudstorage.old.GcsCloudStorageV1Parameters;
import com.sequenceiq.common.api.cloudstorage.old.S3CloudStorageV1Parameters;
import com.sequenceiq.common.api.cloudstorage.old.AdlsGen2CloudStorageV1Parameters;
import com.sequenceiq.common.api.backup.doc.BackupModelDescription;
import com.sequenceiq.common.api.backup.model.BackupCloudwatchParams;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public abstract class BackupBase implements Serializable {

    @NotNull
    @ApiModelProperty(BackupModelDescription.BACKUP_STORAGE_LOCATION)
    private String storageLocation;

    @Valid
    @ApiModelProperty(BackupModelDescription.BACKUP_S3_ATTRIBUTES)
    private S3CloudStorageV1Parameters s3;

    @ApiModelProperty(BackupModelDescription.BACKUP_ADLS_GEN_2_ATTRIBUTES)
    private AdlsGen2CloudStorageV1Parameters adlsGen2;

    @ApiModelProperty(BackupModelDescription.BACKUP_GCS_ATTRIBUTES)
    private @Valid GcsCloudStorageV1Parameters gcs;

    @Valid
    @ApiModelProperty(BackupModelDescription.BACKUP_CLOUDWATCH_ATTRIBUTES)
    private BackupCloudwatchParams cloudwatch;

    public String getStorageLocation() {
        return storageLocation;
    }

    public void setStorageLocation(String storageLocation) {
        this.storageLocation = storageLocation;
    }

    public S3CloudStorageV1Parameters getS3() {
        return s3;
    }

    public void setS3(S3CloudStorageV1Parameters s3) {
        this.s3 = s3;
    }

    public AdlsGen2CloudStorageV1Parameters getAdlsGen2() {
        return adlsGen2;
    }

    public void setAdlsGen2(AdlsGen2CloudStorageV1Parameters adlsGen2) {
        this.adlsGen2 = adlsGen2;
    }

    public GcsCloudStorageV1Parameters getGcs() {
        return gcs;
    }

    public void setGcs(GcsCloudStorageV1Parameters gcs) {
        this.gcs = gcs;
    }

    public BackupCloudwatchParams getCloudwatch() {
        return cloudwatch;
    }

    public void setCloudwatch(BackupCloudwatchParams cloudwatch) {
        this.cloudwatch = cloudwatch;
    }

    @Override
    public String toString() {
        return "BackupBase{" +
                "storageLocation='" + storageLocation + '\'' +
                ", s3=" + s3 +
                ", adlsGen2=" + adlsGen2 +
                ", gcs=" + gcs +
                ", cloudwatch=" + cloudwatch +
                '}';
    }
}
