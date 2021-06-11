package com.sequenceiq.cloudbreak.cloud.aws.common.efs;

import static com.sequenceiq.cloudbreak.cloud.model.filesystem.efs.CloudEfsConfiguration.KEY_BACKUP_POLICY_STATUS;
import static com.sequenceiq.cloudbreak.cloud.model.filesystem.efs.CloudEfsConfiguration.KEY_ENCRYPTED;
import static com.sequenceiq.cloudbreak.cloud.model.filesystem.efs.CloudEfsConfiguration.KEY_FILESYSTEM_POLICY;
import static com.sequenceiq.cloudbreak.cloud.model.filesystem.efs.CloudEfsConfiguration.KEY_FILESYSTEM_TAGS;
import static com.sequenceiq.cloudbreak.cloud.model.filesystem.efs.CloudEfsConfiguration.KEY_KMSKEYID;
import static com.sequenceiq.cloudbreak.cloud.model.filesystem.efs.CloudEfsConfiguration.KEY_LIFECYCLE_POLICIES;
import static com.sequenceiq.cloudbreak.cloud.model.filesystem.efs.CloudEfsConfiguration.KEY_PERFORMANCE_MODE;
import static com.sequenceiq.cloudbreak.cloud.model.filesystem.efs.CloudEfsConfiguration.KEY_PROVISIONED_THROUGHPUT_INMIBPS;
import static com.sequenceiq.cloudbreak.cloud.model.filesystem.efs.CloudEfsConfiguration.KEY_THROUGHPUT_MODE;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sequenceiq.cloudbreak.cloud.model.SpiFileSystem;
import com.sequenceiq.cloudbreak.cloud.model.filesystem.efs.BackupPolicy;
import com.sequenceiq.cloudbreak.cloud.model.filesystem.efs.PerformanceMode;
import com.sequenceiq.cloudbreak.cloud.model.filesystem.efs.ThroughputMode;
import com.sequenceiq.common.model.FileSystemType;

public class AwsEfsFileSystem {
    private static final String BACKUP_POLICY_STATUS_DEFAULT = BackupPolicy.DISABLED.toString();

    private static final String PERFORMANCE_MODE_DEFAULT = PerformanceMode.GENERALPURPOSE.toString();

    private static final String THROUGHPUT_MODE_BURSTING = ThroughputMode.BURSTING.toString();

    private static final String THROUGHPUT_MODE_PROVISIONED = ThroughputMode.PROVISIONED.toString();

    private static final String THROUGHPUT_MODE_DEFAULT = THROUGHPUT_MODE_BURSTING;

    private static final Double PROVISIONED_THROUGHPUT_MIBPS_DEFAULT = 1.0;

    // When this value is null, do not create backup policy
    private String backupPolicyStatus;

    private Boolean encrypted;

    private String fileSystemPolicy;

    private Map<String, String> fileSystemTags;

    private String kmsKeyId;

    private List<String> lifeCyclePolicies;

    private String performanceMode;

    private Double provisionedThroughputInMibps;

    private String throughputMode;

    public AwsEfsFileSystem(String backupPolicyStatus, boolean encrypted, String fileSystemPolicy, Map<String, String> fileSystemTags, String kmsKeyId,
            List<String> lifeCyclePolicies, String performanceMode, Double provisionedThroughputInMibps, String throughputMode) {
        this.backupPolicyStatus = backupPolicyStatus != null ? backupPolicyStatus : BACKUP_POLICY_STATUS_DEFAULT;
        this.encrypted = encrypted;
        this.fileSystemPolicy = fileSystemPolicy;

        if (fileSystemTags != null) {
            this.fileSystemTags = new HashMap<>(fileSystemTags);
        }

        this.kmsKeyId = kmsKeyId;

        if (lifeCyclePolicies != null) {
            this.lifeCyclePolicies = new ArrayList<>(lifeCyclePolicies);
        }

        this.performanceMode = performanceMode != null ? performanceMode : PERFORMANCE_MODE_DEFAULT;
        this.provisionedThroughputInMibps = provisionedThroughputInMibps;
        this.throughputMode = throughputMode != null ? throughputMode : THROUGHPUT_MODE_DEFAULT;

        if (this.throughputMode.equalsIgnoreCase(THROUGHPUT_MODE_PROVISIONED) && this.provisionedThroughputInMibps == null) {
            this.provisionedThroughputInMibps = PROVISIONED_THROUGHPUT_MIBPS_DEFAULT;
        }
    }

    public static AwsEfsFileSystem toAwsEfsFileSystem(SpiFileSystem inFileSystem) {
        if (inFileSystem == null) {
            return null;
        }

        if (!FileSystemType.EFS.equals(inFileSystem.getType())) {
            return null;
        }

        AwsEfsFileSystem newEfs = new AwsEfsFileSystem(
                inFileSystem.getStringParameter(KEY_BACKUP_POLICY_STATUS),
                inFileSystem.getParameter(KEY_ENCRYPTED, Boolean.class),
                inFileSystem.getStringParameter(KEY_FILESYSTEM_POLICY),
                inFileSystem.getParameter(KEY_FILESYSTEM_TAGS, Map.class),
                inFileSystem.getStringParameter(KEY_KMSKEYID),
                inFileSystem.getParameter(KEY_LIFECYCLE_POLICIES, List.class),
                inFileSystem.getStringParameter(KEY_PERFORMANCE_MODE),
                inFileSystem.getParameter(KEY_PROVISIONED_THROUGHPUT_INMIBPS, Double.class),
                inFileSystem.getStringParameter(KEY_THROUGHPUT_MODE)
        );

        return newEfs;
    }

    public String getBackupPolicyStatus() {
        return backupPolicyStatus;
    }

    public void setbackupPolicyStatus(String backupPolicyStatus) {
        this.backupPolicyStatus = backupPolicyStatus;
    }

    public Boolean getencrypted() {
        return encrypted;
    }

    public void setEncrypted(Boolean encrypted) {
        this.encrypted = encrypted;
    }

    public String getFileSystemPolicy() {
        return fileSystemPolicy;
    }

    public void setFileSystemPolicy(String fileSystemPolicy) {
        this.fileSystemPolicy = fileSystemPolicy;
    }

    public Map<String, String> getFileSystemTags() {
        return fileSystemTags;
    }

    public void setFileSystemTags(Map<String, String> fileSystemTags) {
        this.fileSystemTags = new HashMap<>(fileSystemTags);
    }

    public String getKmsKeyId() {
        return kmsKeyId;
    }

    public void setKmsKeyId(String kmsKeyId) {
        this.kmsKeyId = kmsKeyId;
    }

    public List<String> getLifeCyclePolicies() {
        return lifeCyclePolicies;
    }

    public void setLifeCyclePolicies(List<String> lifeCyclePolicies) {
        this.lifeCyclePolicies = new ArrayList<>(lifeCyclePolicies);
    }

    public String getPerformanceMode() {
        return performanceMode;
    }

    public void setPerformanceMode(String performanceMode) {
        this.performanceMode = performanceMode;
    }

    public Double getProvisionedThroughputInMibps() {
        return provisionedThroughputInMibps;
    }

    public void setProvisionedThroughputInMibps(Double provisionedThroughputInMibps) {
        this.provisionedThroughputInMibps = provisionedThroughputInMibps;
    }

    public String getThroughputMode() {
        return throughputMode;
    }

    public void setThroughputMode(String throughputMode) {
        this.throughputMode = throughputMode;
    }
}
