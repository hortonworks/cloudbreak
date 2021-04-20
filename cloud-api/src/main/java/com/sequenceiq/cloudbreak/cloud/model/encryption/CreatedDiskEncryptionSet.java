package com.sequenceiq.cloudbreak.cloud.model.encryption;

import java.util.Map;

public class CreatedDiskEncryptionSet {
    private String diskEncryptionSetId;

    private String diskEncryptionSetPrincipalId;

    private String diskEncryptionSetLocation;

    private String diskEncryptionSetName;

    private Map<String, String> tags;

    private String diskEncryptionSetResourceGroup;

    public CreatedDiskEncryptionSet(String diskEncryptionSetId) {
        this.diskEncryptionSetId = diskEncryptionSetId;
    }

    private CreatedDiskEncryptionSet(CreatedDiskEncryptionSet.Builder builder) {
        this.diskEncryptionSetId = builder.diskEncryptionSetId;
        this.diskEncryptionSetPrincipalId = builder.diskEncryptionSetPrincipalId;
        this.diskEncryptionSetLocation = builder.diskEncryptionSetLocation;
        this.diskEncryptionSetName = builder.diskEncryptionSetName;
        this.diskEncryptionSetResourceGroup = builder.diskEncryptionSetResourceGroup;
        this.tags = builder.tags;
    }

    public String getDiskEncryptionSetId() {
        return diskEncryptionSetId;
    }

    public String getDiskEncryptionSetPrincipalId() {
        return diskEncryptionSetPrincipalId;
    }

    public String getDiskEncryptionSetLocation() {
        return diskEncryptionSetLocation;
    }

    public String getDiskEncryptionSetName() {
        return diskEncryptionSetName;
    }

    public String getDiskEncryptionSetResourceGroup() {
        return diskEncryptionSetResourceGroup;
    }

    public Map<String, String> getTags() {
        return tags;
    }

    public static final class Builder {

        private String diskEncryptionSetId;

        private String diskEncryptionSetPrincipalId;

        private String diskEncryptionSetLocation;

        private String diskEncryptionSetName;

        private String diskEncryptionSetResourceGroup;

        private Map<String, String> tags;

        public Builder() {
        }

        public CreatedDiskEncryptionSet.Builder withDiskEncryptionSetId(String diskEncryptionSetId) {
            this.diskEncryptionSetId = diskEncryptionSetId;
            return this;
        }

        public CreatedDiskEncryptionSet.Builder withDiskEncryptionSetPrincipalId(String diskEncryptionSetPrincipalId) {
            this.diskEncryptionSetPrincipalId = diskEncryptionSetPrincipalId;
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

        public CreatedDiskEncryptionSet.Builder withDiskEncryptionSetResourceGroup(String diskEncryptionSetResourceGroup) {
            this.diskEncryptionSetResourceGroup = diskEncryptionSetResourceGroup;
            return this;
        }

        public CreatedDiskEncryptionSet build() {
            return new CreatedDiskEncryptionSet(this);
        }
    }
}