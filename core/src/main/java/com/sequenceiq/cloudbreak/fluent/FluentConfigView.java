package com.sequenceiq.cloudbreak.fluent;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.ObjectUtils;

public class FluentConfigView {

    private static final String LOG_FOLDER_DEFAULT = "/var/log";

    private static final String TD_AGENT_USER_DEFAULT = "root";

    private static final String TD_AGENT_GROUP_DEFAULT = "root";

    private static final String PROVIDER_PREFIX_DEFAULT = "stdout";

    private static final String EMPTY_CONFIG_DEFAULT = "";

    private static final Integer PARTITION_INTERVAL_DEFAULT = 5;

    private final boolean enabled;

    private final String user;

    private final String group;

    private final String serverLogFolderPrefix;

    private final String agentLogFolderPrefix;

    private final String serviceLogFolderPrefix;

    private final String platform;

    private final String providerPrefix;

    private final Integer partitionIntervalMin;

    private final String azureStorageAccount;

    private final String azureContainer;

    private final String azureInstanceMsi;

    private final String azureStorageAccessKey;

    private final String s3LogArchiveBucketName;

    private final String logFolderName;

    private final Map<String, Object> overrideAttributes;

    private FluentConfigView(Builder builder) {
        this.enabled = builder.enabled;
        this.user = builder.user;
        this.group = builder.group;
        this.serverLogFolderPrefix = builder.serverLogFolderPrefix;
        this.agentLogFolderPrefix = builder.agentLogFolderPrefix;
        this.serviceLogFolderPrefix = builder.serviceLogFolderPrefix;
        this.platform = builder.platform;
        this.providerPrefix = builder.providerPrefix;
        this.partitionIntervalMin = builder.partitionIntervalMin;
        this.logFolderName = builder.logFolderName;
        this.s3LogArchiveBucketName = builder.s3LogArchiveBucketName;
        this.azureContainer = builder.azureContainer;
        this.azureStorageAccount = builder.azureStorageAccount;
        this.azureInstanceMsi = builder.azureInstanceMsi;
        this.azureStorageAccessKey = builder.azureStorageAccessKey;
        this.overrideAttributes = builder.overrideAttributes;
    }

    public String getUser() {
        return user;
    }

    public String getGroup() {
        return group;
    }

    public String getServerLogFolderPrefix() {
        return serverLogFolderPrefix;
    }

    public String getAgentLogFolderPrefix() {
        return agentLogFolderPrefix;
    }

    public String getServiceLogFolderPrefix() {
        return serviceLogFolderPrefix;
    }

    public String getPlatform() {
        return platform;
    }

    public String getProviderPrefix() {
        return providerPrefix;
    }

    public Integer getPartitionIntervalMin() {
        return partitionIntervalMin;
    }

    public String getLogFolderName() {
        return logFolderName;
    }

    public String getS3LogArchiveBucketName() {
        return s3LogArchiveBucketName;
    }

    public String getAzureStorageAccount() {
        return azureStorageAccount;
    }

    public String getAzureContainer() {
        return azureContainer;
    }

    public String getAzureInstanceMsi() {
        return azureInstanceMsi;
    }

    public String getAzureStorageAccessKey() {
        return azureStorageAccessKey;
    }

    public Boolean isEnabled() {
        return enabled;
    }

    public Map<String, Object> getOverrideAttributes() {
        return this.overrideAttributes;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("enabled", this.enabled);
        map.put("user", ObjectUtils.defaultIfNull(this.user, TD_AGENT_USER_DEFAULT));
        map.put("group", ObjectUtils.defaultIfNull(this.group, TD_AGENT_GROUP_DEFAULT));
        map.put("providerPrefix", ObjectUtils.defaultIfNull(this.providerPrefix, PROVIDER_PREFIX_DEFAULT));
        map.put("platform", ObjectUtils.defaultIfNull(this.platform, EMPTY_CONFIG_DEFAULT));
        map.put("serverLogFolderPrefix", ObjectUtils.defaultIfNull(this.serverLogFolderPrefix, LOG_FOLDER_DEFAULT));
        map.put("agentLogFolderPrefix", ObjectUtils.defaultIfNull(this.agentLogFolderPrefix, LOG_FOLDER_DEFAULT));
        map.put("serviceLogFolderPrefix", ObjectUtils.defaultIfNull(this.serviceLogFolderPrefix, LOG_FOLDER_DEFAULT));
        map.put("partitionIntervalMin", ObjectUtils.defaultIfNull(this.partitionIntervalMin, PARTITION_INTERVAL_DEFAULT));
        map.put("logFolderName", ObjectUtils.defaultIfNull(this.logFolderName, EMPTY_CONFIG_DEFAULT));
        map.put("s3LogArchiveBucketName", ObjectUtils.defaultIfNull(this.s3LogArchiveBucketName, EMPTY_CONFIG_DEFAULT));
        map.put("azureContainer", ObjectUtils.defaultIfNull(this.azureContainer, EMPTY_CONFIG_DEFAULT));
        map.put("azureStorageAccount", ObjectUtils.defaultIfNull(this.azureStorageAccount, EMPTY_CONFIG_DEFAULT));
        map.put("azureStorageAccessKey", ObjectUtils.defaultIfNull(this.azureStorageAccessKey, EMPTY_CONFIG_DEFAULT));
        map.put("azureInstanceMsi", ObjectUtils.defaultIfNull(this.azureInstanceMsi, EMPTY_CONFIG_DEFAULT));
        return map;
    }

    public static final class Builder {

        private boolean enabled;

        private String user;

        private String group;

        private String serverLogFolderPrefix;

        private String agentLogFolderPrefix;

        private String serviceLogFolderPrefix;

        private String platform;

        private String providerPrefix;

        private Integer partitionIntervalMin;

        private String logFolderName;

        private String s3LogArchiveBucketName;

        private String azureStorageAccount;

        private String azureContainer;

        private String azureInstanceMsi;

        private String azureStorageAccessKey;

        private Map<String, Object> overrideAttributes;

        public FluentConfigView build() {
            return new FluentConfigView(this);
        }

        public Builder withEnabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public Builder withUser(String user) {
            this.user = user;
            return this;
        }

        public Builder withGroup(String group) {
            this.group = group;
            return this;
        }

        public Builder withServerLogFolderPrefix(String serverLogFolderPrefix) {
            this.serverLogFolderPrefix = serverLogFolderPrefix;
            return this;
        }

        public Builder withAgentLogFolderPrefix(String agentLogFolderPrefix) {
            this.agentLogFolderPrefix = agentLogFolderPrefix;
            return this;
        }

        public Builder withServiceLogFolderPrefix(String serviceLogFolderPrefix) {
            this.serviceLogFolderPrefix = serviceLogFolderPrefix;
            return this;
        }

        public Builder withPlatform(String platform) {
            this.platform = platform;
            return this;
        }

        public Builder withProviderPrefix(String providerPrefix) {
            this.providerPrefix = providerPrefix;
            return this;
        }

        public Builder withPartitionIntervalMin(Integer partitionIntervalMin) {
            this.partitionIntervalMin = partitionIntervalMin;
            return this;
        }

        public Builder withLogFolderName(String logFolderName) {
            this.logFolderName = logFolderName;
            return this;
        }

        public Builder withS3LogArchiveBucketName(String s3LogArchiveBucketName) {
            this.s3LogArchiveBucketName = s3LogArchiveBucketName;
            return this;
        }

        public Builder withAzureInstanceMsi(String azureInstanceMsi) {
            this.azureInstanceMsi = azureInstanceMsi;
            return this;
        }

        public Builder withAzureStorageAccount(String azureStorageAccount) {
            this.azureStorageAccount = azureStorageAccount;
            return this;
        }

        public Builder withAzureContainer(String azureContainer) {
            this.azureContainer = azureContainer;
            return this;
        }

        public Builder withAzureStorageAccessKey(String azureStorageAccessKey) {
            this.azureStorageAccessKey = azureStorageAccessKey;
            return this;
        }

        public Builder withOverrideAttributes(Map<String, Object> overrideAttributes) {
            this.overrideAttributes = overrideAttributes;
            return this;
        }
    }
}
