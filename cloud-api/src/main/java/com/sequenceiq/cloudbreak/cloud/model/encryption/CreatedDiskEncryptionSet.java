package com.sequenceiq.cloudbreak.cloud.model.encryption;

import java.util.Map;

public class CreatedDiskEncryptionSet {
    private final String diskEncryptionSetId;

    private final String diskEncryptionSetPrincipalObjectId;

    private final String diskEncryptionSetLocation;

    private final String diskEncryptionSetName;

    private final Map<String, String> tags;

    private final String diskEncryptionSetResourceGroupName;

    private CreatedDiskEncryptionSet(CreatedDiskEncryptionSet.Builder builder) {
        this.diskEncryptionSetId = builder.diskEncryptionSetId;
        this.diskEncryptionSetPrincipalObjectId = builder.diskEncryptionSetPrincipalObjectId;
        this.diskEncryptionSetLocation = builder.diskEncryptionSetLocation;
        this.diskEncryptionSetName = builder.diskEncryptionSetName;
        this.diskEncryptionSetResourceGroupName = builder.diskEncryptionSetResourceGroupName;
        this.tags = builder.tags;
    }

    public String getDiskEncryptionSetId() {
        return diskEncryptionSetId;
    }

    public String getDiskEncryptionSetPrincipalObjectId() {
        return diskEncryptionSetPrincipalObjectId;
    }

    public String getDiskEncryptionSetLocation() {
        return diskEncryptionSetLocation;
    }

    public String getDiskEncryptionSetName() {
        return diskEncryptionSetName;
    }

    public String getDiskEncryptionSetResourceGroupName() {
        return diskEncryptionSetResourceGroupName;
    }

    public Map<String, String> getTags() {
        return tags;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("CreatedDiskEncryptionSet{");
        sb.append("diskEncryptionSetId='").append(diskEncryptionSetId).append('\'');
        sb.append(", diskEncryptionSetPrincipalObjectId='").append(diskEncryptionSetPrincipalObjectId).append('\'');
        sb.append(", diskEncryptionSetLocation='").append(diskEncryptionSetLocation).append('\'');
        sb.append(", diskEncryptionSetName='").append(diskEncryptionSetName).append('\'');
        sb.append(", tags=").append(tags);
        sb.append(", diskEncryptionSetResourceGroupName='").append(diskEncryptionSetResourceGroupName).append('\'');
        sb.append('}');
        return sb.toString();
    }

    public static final class Builder {

        private String diskEncryptionSetId;

        private String diskEncryptionSetPrincipalObjectId;

        private String diskEncryptionSetLocation;

        private String diskEncryptionSetName;

        private String diskEncryptionSetResourceGroupName;

        private Map<String, String> tags;

        public Builder() {
        }

        public CreatedDiskEncryptionSet.Builder withDiskEncryptionSetId(String diskEncryptionSetId) {
            this.diskEncryptionSetId = diskEncryptionSetId;
            return this;
        }

        public CreatedDiskEncryptionSet.Builder withDiskEncryptionSetPrincipalObjectId(String diskEncryptionSetPrincipalObjectId) {
            this.diskEncryptionSetPrincipalObjectId = diskEncryptionSetPrincipalObjectId;
            return this;
        }

        public CreatedDiskEncryptionSet.Builder withDiskEncryptionSetLocation(String diskEncryptionSetLocation) {
            this.diskEncryptionSetLocation = diskEncryptionSetLocation;
            return this;
        }

        public CreatedDiskEncryptionSet.Builder withDiskEncryptionSetName(String diskEncryptionSetName) {
            this.diskEncryptionSetName = diskEncryptionSetName;
            return this;
        }

        public CreatedDiskEncryptionSet.Builder withTags(Map<String, String> tags) {
            this.tags = tags;
            return this;
        }

        public CreatedDiskEncryptionSet.Builder withDiskEncryptionSetResourceGroupName(String diskEncryptionSetResourceGroupName) {
            this.diskEncryptionSetResourceGroupName = diskEncryptionSetResourceGroupName;
            return this;
        }

        public CreatedDiskEncryptionSet build() {
            return new CreatedDiskEncryptionSet(this);
        }
    }
}