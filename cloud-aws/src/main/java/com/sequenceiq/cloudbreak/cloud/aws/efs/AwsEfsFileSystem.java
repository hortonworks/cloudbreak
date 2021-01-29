package com.sequenceiq.cloudbreak.cloud.aws.efs;

import static com.sequenceiq.cloudbreak.cloud.model.filesystem.efs.CloudEfsConfiguration.KEY_ASSOCIATED_INSTANCE_GROUP_NAMES;
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
    private final String backupPolicyStatus;

    private final Boolean encrypted;

    private final String fileSystemPolicy;

    private final Map<String, String> fileSystemTags;

    private final String kmsKeyId;

    private final List<String> lifeCyclePolicies;

    private final String performanceMode;

    private final Double provisionedThroughputInMibps;

    private final String throughputMode;

    private final List<String> associatedInstanceGroupNames;

    private AwsEfsFileSystem(String backupPolicyStatus, boolean encrypted, String fileSystemPolicy, Map<String, String> fileSystemTags, String kmsKeyId,
            List<String> lifeCyclePolicies, String performanceMode, Double provisionedThroughputInMibps, String throughputMode,
            List<String> associatedInstanceGroupNames) {

        if (associatedInstanceGroupNames == null || associatedInstanceGroupNames.size() == 0) {
            throw new IllegalArgumentException("EFS associated instance group names are not set");
        }

        this.backupPolicyStatus = backupPolicyStatus;
        this.encrypted = encrypted;
        this.fileSystemPolicy = fileSystemPolicy;

        if (fileSystemTags != null) {
            this.fileSystemTags = new HashMap<>(fileSystemTags);
        } else {
            this.fileSystemTags = new HashMap<>();
        }

        this.kmsKeyId = kmsKeyId;

        if (lifeCyclePolicies != null) {
            this.lifeCyclePolicies = new ArrayList<>(lifeCyclePolicies);
        } else {
            this.lifeCyclePolicies = new ArrayList<>();
        }

        this.performanceMode = performanceMode;
        this.provisionedThroughputInMibps = provisionedThroughputInMibps;
        this.throughputMode = throughputMode;
        this.associatedInstanceGroupNames = associatedInstanceGroupNames;
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
                inFileSystem.getStringParameter(KEY_THROUGHPUT_MODE),
                inFileSystem.getParameter(KEY_ASSOCIATED_INSTANCE_GROUP_NAMES, List.class)
        );

        return newEfs;
    }

    public String getBackupPolicyStatus() {
        return backupPolicyStatus;
    }

    public Boolean getEncrypted() {
        return encrypted;
    }

    public String getFileSystemPolicy() {
        return fileSystemPolicy;
    }

    public Map<String, String> getFileSystemTags() {
        return fileSystemTags;
    }

    public String getKmsKeyId() {
        return kmsKeyId;
    }

    public List<String> getLifeCyclePolicies() {
        return lifeCyclePolicies;
    }

    public String getPerformanceMode() {
        return performanceMode;
    }

    public Double getProvisionedThroughputInMibps() {
        return provisionedThroughputInMibps;
    }

    public String getThroughputMode() {
        return throughputMode;
    }

    public List<String> getAssociatedInstanceGroupNames() {
        return associatedInstanceGroupNames;
    }

    public static class Builder {
        private String backupPolicyStatus;

        private Boolean encrypted;

        private String fileSystemPolicy;

        private Map<String, String> fileSystemTags;

        private String kmsKeyId;

        private List<String> lifeCyclePolicies;

        private String performanceMode;

        private Double provisionedThroughputInMibps;

        private String throughputMode;

        private List<String> associatedInstanceGroupNames;

        public Builder withBackupPolicyStatus(String backupPolicyStatus) {
            this.backupPolicyStatus = backupPolicyStatus;
            return this;
        }

        public Builder withEncrypted(Boolean encrypted) {
            this.encrypted = encrypted;
            return this;
        }

        public Builder withFileSystemPolicy(String fileSystemPolicy) {
            this.fileSystemPolicy = fileSystemPolicy;
            return this;
        }

        public Builder withFileSystemTags(Map<String, String> fileSystemTags) {
            this.fileSystemTags = fileSystemTags;
            return this;
        }

        public Builder withKmsKeyId(String kmsKeyId) {
            this.kmsKeyId = kmsKeyId;
            return this;
        }

        public Builder withLifeCyclePolicies(List<String> lifeCyclePolicies) {
            this.lifeCyclePolicies = lifeCyclePolicies;
            return this;
        }

        public Builder withPerformanceMode(String performanceMode) {
            this.performanceMode = performanceMode;
            return this;
        }

        public Builder withProvisionedThroughputInMibps(Double provisionedThroughputInMibps) {
            this.provisionedThroughputInMibps = provisionedThroughputInMibps;
            return this;
        }

        public Builder withThroughputMode(String throughputMode) {
            this.throughputMode = throughputMode;
            return this;
        }

        public Builder withAssociatedInstanceGroupNames(List<String> associatedInstanceGroupNames) {
            this.associatedInstanceGroupNames = associatedInstanceGroupNames;
            return this;
        }

        public static Builder builder() {
            return new Builder();
        }

        public AwsEfsFileSystem build() {
            setDefaultIfNeeded();
            return new AwsEfsFileSystem(
                    backupPolicyStatus,
                    encrypted,
                    fileSystemPolicy,
                    fileSystemTags,
                    kmsKeyId,
                    lifeCyclePolicies,
                    performanceMode,
                    provisionedThroughputInMibps,
                    throughputMode,
                    associatedInstanceGroupNames
            );
        }

        private void setDefaultIfNeeded() {
            if (backupPolicyStatus == null) {
                backupPolicyStatus = BACKUP_POLICY_STATUS_DEFAULT;
            }

            if (performanceMode == null) {
                performanceMode = PERFORMANCE_MODE_DEFAULT;
            }

            if (throughputMode == null) {
                throughputMode = THROUGHPUT_MODE_DEFAULT;
            }

            if (throughputMode.equalsIgnoreCase(THROUGHPUT_MODE_PROVISIONED) && provisionedThroughputInMibps == null) {
                provisionedThroughputInMibps = PROVISIONED_THROUGHPUT_MIBPS_DEFAULT;
            }
        }
    }
}
