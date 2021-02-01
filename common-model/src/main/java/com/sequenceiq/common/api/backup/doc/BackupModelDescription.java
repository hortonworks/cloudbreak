package com.sequenceiq.common.api.backup.doc;

public class BackupModelDescription {

    public static final String BACKUP_S3_ATTRIBUTES = "backup s3 attributes";
    public static final String BACKUP_ADLS_GEN_2_ATTRIBUTES = "backup adls gen2 attributes";
    public static final String BACKUP_GCS_ATTRIBUTES = "backup gcs attributes";
    public static final String BACKUP_CLOUDWATCH_ATTRIBUTES = "backup cloudwatch attributes";
    public static final String BACKUP_STORAGE_LOCATION = "backup storage location / container";
    public static final String CLOUDWATCH_PARAMS = "CloudWatch releated parameters";
    public static final String CLOUDWATCH_PARAMS_REGION = "CloudWatch related AWS region (should be used only outside of AWS platform)";

    private BackupModelDescription() {
    }

}
