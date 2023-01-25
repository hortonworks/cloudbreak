package com.sequenceiq.freeipa.service.freeipa.config;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FreeIpaBackupConfigView {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaBackupConfigView.class);

    private static final String EMPTY_CONFIG_DEFAULT = "";

    private final boolean enabled;

    private final boolean monthlyFullEnabled;

    private final boolean hourlyEnabled;

    private final boolean initialFullEnabled;

    private final String platform;

    private final String location;

    private final String azureInstanceMsi;

    private final String proxyUrl;

    private final String awsRegion;

    private final String awsEndpoint;

    private String gcpServiceAccount;

    @SuppressWarnings("ExecutableStatementCount")
    private FreeIpaBackupConfigView(FreeIpaBackupConfigView.Builder builder) {
        enabled = builder.enabled;
        monthlyFullEnabled = builder.monthlyFullEnabled;
        hourlyEnabled = builder.hourlyEnabled;
        initialFullEnabled = builder.initialFullEnabled;
        location = builder.location;
        platform = builder.platform;
        azureInstanceMsi = builder.azureInstanceMsi;
        proxyUrl = builder.proxyUrl;
        awsRegion = builder.awsRegion;
        awsEndpoint = builder.awsEndpoint;
        gcpServiceAccount = builder.gcpServiceAccount;
    }

    public String getLocation() {
        return location;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isMonthlyFullEnabled() {
        return monthlyFullEnabled;
    }

    public boolean isHourlyEnabled() {
        return hourlyEnabled;
    }

    public boolean isInitialFullEnabled() {
        return initialFullEnabled;
    }

    public String getPlatform() {
        return platform;
    }

    public String getAzureInstanceMsi() {
        return azureInstanceMsi;
    }

    public String getProxyUrl() {
        return proxyUrl;
    }

    public String getAwsRegion() {
        return awsRegion;
    }

    public String getAwsEndpoint() {
        return awsEndpoint;
    }

    public String getGcpServiceAccount() {
        return gcpServiceAccount;
    }

    public void setGcpServiceAccount(String gcpServiceAccount) {
        this.gcpServiceAccount = gcpServiceAccount;
    }

    @SuppressWarnings("ExecutableStatementCount")
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("enabled", enabled);
        map.put("location", ObjectUtils.defaultIfNull(location, EMPTY_CONFIG_DEFAULT));
        map.put("monthly_full_enabled", monthlyFullEnabled);
        map.put("hourly_enabled", hourlyEnabled);
        map.put("initial_full_enabled", initialFullEnabled);
        map.put("platform", ObjectUtils.defaultIfNull(platform, EMPTY_CONFIG_DEFAULT));
        map.put("azure_instance_msi", ObjectUtils.defaultIfNull(azureInstanceMsi, EMPTY_CONFIG_DEFAULT));
        map.put("gcp_service_account", ObjectUtils.defaultIfNull(gcpServiceAccount, EMPTY_CONFIG_DEFAULT));
        map.put("http_proxy", ObjectUtils.defaultIfNull(proxyUrl, EMPTY_CONFIG_DEFAULT));
        map.put("aws_region", ObjectUtils.defaultIfNull(awsRegion, EMPTY_CONFIG_DEFAULT));
        map.put("aws_endpoint", ObjectUtils.defaultIfNull(awsEndpoint, EMPTY_CONFIG_DEFAULT));
        return map;
    }

    public static final class Builder {

        private boolean enabled;

        private boolean monthlyFullEnabled;

        private boolean hourlyEnabled;

        private boolean initialFullEnabled;

        private String location;

        private String platform;

        private String azureInstanceMsi;

        private String gcpServiceAccount;

        private String proxyUrl;

        private String awsRegion;

        private String awsEndpoint;

        public FreeIpaBackupConfigView build() {
            return new FreeIpaBackupConfigView(this);
        }

        public Builder withEnabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public Builder withMonthlyFullEnabled(boolean monthlyFullEnabled) {
            this.monthlyFullEnabled = monthlyFullEnabled;
            return this;
        }

        public Builder withHourlyEnabled(boolean hourlyEnabled) {
            this.hourlyEnabled = hourlyEnabled;
            return this;
        }

        public Builder withInitialFullEnabled(boolean initialFullEnabled) {
            this.initialFullEnabled = initialFullEnabled;
            return this;
        }

        public Builder withLocation(String location) {
            this.location = location;
            return this;
        }

        public Builder withPlatform(String platform) {
            this.platform = platform;
            return this;
        }

        public Builder withAzureInstanceMsi(String azureInstanceMsi) {
            this.azureInstanceMsi = azureInstanceMsi;
            return this;
        }

        public Builder withProxyUrl(String proxyUrl) {
            this.proxyUrl = proxyUrl;
            return this;
        }

        public Builder withAwsRegion(String awsRegion) {
            this.awsRegion = awsRegion;
            return this;
        }

        public Builder withAwsEndpoint(String awsEndpoint) {
            this.awsEndpoint = awsEndpoint;
            return this;
        }

        public Builder withGcpServiceAccount(String gcpServiceAccount) {
            this.gcpServiceAccount = gcpServiceAccount;
            return this;
        }
    }
}
