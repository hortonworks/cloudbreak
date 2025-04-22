package com.sequenceiq.cloudbreak.telemetry.fluent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;

import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.telemetry.TelemetryClusterDetails;
import com.sequenceiq.cloudbreak.telemetry.TelemetryConfigView;
import com.sequenceiq.common.api.telemetry.model.AnonymizationRule;

public class FluentConfigView implements TelemetryConfigView {

    public static final String AZURE_IDBROKER_INSTANCE_MSI = "azureIdBrokerInstanceMsi";

    private static final String LOG_FOLDER_DEFAULT = "/var/log";

    private static final String TD_AGENT_USER_DEFAULT = "root";

    private static final String TD_AGENT_GROUP_DEFAULT = "root";

    private static final String PROVIDER_PREFIX_DEFAULT = "stdout";

    private static final String EMPTY_CONFIG_DEFAULT = "";

    private static final Boolean DBUS_INCLUDE_SALT_LOGS_DEFAULT = false;

    private static final Integer PARTITION_INTERVAL_DEFAULT = 5;

    private final boolean enabled;

    private final boolean cloudStorageLoggingEnabled;

    private final boolean monitoringEnabled;

    private final TelemetryClusterDetails clusterDetails;

    private final String user;

    private final String group;

    private final String serverLogFolderPrefix;

    private final String agentLogFolderPrefix;

    private final String serviceLogFolderPrefix;

    private final String region;

    private final String providerPrefix;

    private final String environmentRegion;

    private final Integer partitionIntervalMin;

    private final String azureStorageAccount;

    private final String azureContainer;

    private final String azureInstanceMsi;

    private final String azureIdBrokerInstanceMsi;

    private final String azureStorageAccessKey;

    private final String s3LogArchiveBucketName;

    private final String gcsBucket;

    private final String gcsProjectId;

    private final String logFolderName;

    private final List<AnonymizationRule> anonymizationRules;

    private final Map<String, Object> overrideAttributes;

    private FluentConfigView(Builder builder) {
        this.enabled = builder.enabled;
        this.cloudStorageLoggingEnabled = builder.cloudStorageLoggingEnabled;
        this.monitoringEnabled = builder.monitoringEnabled;
        this.clusterDetails = builder.clusterDetails;
        this.user = builder.user;
        this.group = builder.group;
        this.serverLogFolderPrefix = builder.serverLogFolderPrefix;
        this.agentLogFolderPrefix = builder.agentLogFolderPrefix;
        this.serviceLogFolderPrefix = builder.serviceLogFolderPrefix;
        this.region = builder.region;
        this.providerPrefix = builder.providerPrefix;
        this.environmentRegion = builder.environmentRegion;
        this.partitionIntervalMin = builder.partitionIntervalMin;
        this.logFolderName = builder.logFolderName;
        this.s3LogArchiveBucketName = builder.s3LogArchiveBucketName;
        this.azureContainer = builder.azureContainer;
        this.azureStorageAccount = builder.azureStorageAccount;
        this.azureInstanceMsi = builder.azureInstanceMsi;
        this.azureIdBrokerInstanceMsi = builder.azureIdBrokerInstanceMsi;
        this.azureStorageAccessKey = builder.azureStorageAccessKey;
        this.gcsBucket = builder.gcsBucket;
        this.gcsProjectId = builder.gcsProjectId;
        this.anonymizationRules = builder.anonymizationRules;
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

    public String getRegion() {
        return region;
    }

    public TelemetryClusterDetails getClusterDetails() {
        return clusterDetails;
    }

    public String getProviderPrefix() {
        return providerPrefix;
    }

    public String getEnvironmentRegion() {
        return environmentRegion;
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

    public String getAzureIdBrokerInstanceMsi() {
        return azureIdBrokerInstanceMsi;
    }

    public String getAzureStorageAccessKey() {
        return azureStorageAccessKey;
    }

    public String getGcsBucket() {
        return gcsBucket;
    }

    public String getGcsProjectId() {
        return gcsProjectId;
    }

    public Boolean isEnabled() {
        return enabled;
    }

    public boolean isCloudStorageLoggingEnabled() {
        return cloudStorageLoggingEnabled;
    }

    public Map<String, Object> getOverrideAttributes() {
        return this.overrideAttributes;
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("enabled", this.enabled);
        map.put("cloudStorageLoggingEnabled", this.cloudStorageLoggingEnabled);
        map.put("dbusIncludeSaltLogs", DBUS_INCLUDE_SALT_LOGS_DEFAULT);
        map.put("user", ObjectUtils.defaultIfNull(this.user, TD_AGENT_USER_DEFAULT));
        map.put("group", ObjectUtils.defaultIfNull(this.group, TD_AGENT_GROUP_DEFAULT));
        map.put("environmentRegion", ObjectUtils.defaultIfNull(this.environmentRegion, Crn.Region.US_WEST_1.getName()));
        map.put("providerPrefix", ObjectUtils.defaultIfNull(this.providerPrefix, PROVIDER_PREFIX_DEFAULT));
        map.put("region", ObjectUtils.defaultIfNull(this.region, EMPTY_CONFIG_DEFAULT));
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
        map.put("gcsBucket", ObjectUtils.defaultIfNull(this.gcsBucket, EMPTY_CONFIG_DEFAULT));
        map.put("gcsProjectId", ObjectUtils.defaultIfNull(this.gcsProjectId, EMPTY_CONFIG_DEFAULT));
        map.put(AZURE_IDBROKER_INSTANCE_MSI, ObjectUtils.defaultIfNull(this.azureIdBrokerInstanceMsi, EMPTY_CONFIG_DEFAULT));
        if (this.clusterDetails != null) {
            map.putAll(clusterDetails.toMap());
        }
        if (CollectionUtils.isNotEmpty(this.anonymizationRules)) {
            map.put("anonymizationRules", this.anonymizationRules);
        }
        if (this.overrideAttributes != null) {
            fillOverrideAttributes(map);
        }
        return map;
    }

    private void fillOverrideAttributes(Map<String, Object> map) {
        for (Map.Entry<String, Object> entry : this.overrideAttributes.entrySet()) {
            if (!"enabled".equalsIgnoreCase(entry.getKey())
                    && map.containsKey(entry.getKey())) {
                map.put(entry.getKey(), entry.getValue());
            }
        }
    }

    public static final class Builder {

        private boolean enabled;

        private boolean cloudStorageLoggingEnabled;

        private boolean monitoringEnabled;

        private TelemetryClusterDetails clusterDetails;

        private String user;

        private String group;

        private String serverLogFolderPrefix;

        private String agentLogFolderPrefix;

        private String serviceLogFolderPrefix;

        private String region;

        private String providerPrefix;

        private String environmentRegion;

        private Integer partitionIntervalMin;

        private String logFolderName;

        private String s3LogArchiveBucketName;

        private String azureStorageAccount;

        private String azureContainer;

        private String azureInstanceMsi;

        private String azureIdBrokerInstanceMsi;

        private String azureStorageAccessKey;

        private String gcsBucket;

        private String gcsProjectId;

        private List<AnonymizationRule> anonymizationRules;

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

        public Builder withRegion(String region) {
            this.region = region;
            return this;
        }

        public Builder withProviderPrefix(String providerPrefix) {
            this.providerPrefix = providerPrefix;
            return this;
        }

        public Builder withEnvironmentRegion(String environmentRegion) {
            this.environmentRegion = environmentRegion;
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

        public Builder withAzureIdBrokerInstanceMsi(String azureIdBrokerInstanceMsi) {
            this.azureIdBrokerInstanceMsi = azureIdBrokerInstanceMsi;
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

        public Builder withMonitoringEnabled(boolean monitoringEnabled) {
            this.monitoringEnabled = monitoringEnabled;
            return this;
        }

        public Builder withOverrideAttributes(Map<String, Object> overrideAttributes) {
            this.overrideAttributes = overrideAttributes;
            return this;
        }

        public Builder withCloudStorageLoggingEnabled(boolean cloudStorageLoggingEnabled) {
            this.cloudStorageLoggingEnabled = cloudStorageLoggingEnabled;
            return this;
        }

        public Builder withClusterDetails(TelemetryClusterDetails clusterDetails) {
            this.clusterDetails = clusterDetails;
            return this;
        }

        public Builder withAnonymizationRules(List<AnonymizationRule> anonymizationRules) {
            this.anonymizationRules = anonymizationRules;
            return this;
        }

        public Builder withGcsBucket(String gcsBucket) {
            this.gcsBucket = gcsBucket;
            return this;
        }

        public Builder withGcsProjectId(String gcsProjectId) {
            this.gcsProjectId = gcsProjectId;
            return this;
        }

    }
}
