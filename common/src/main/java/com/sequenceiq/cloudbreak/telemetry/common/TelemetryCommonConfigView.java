package com.sequenceiq.cloudbreak.telemetry.common;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;

import com.sequenceiq.cloudbreak.telemetry.TelemetryClusterDetails;
import com.sequenceiq.cloudbreak.telemetry.TelemetryConfigView;
import com.sequenceiq.common.api.telemetry.model.VmLog;
import com.sequenceiq.common.api.telemetry.model.AnonymizationRule;

public class TelemetryCommonConfigView implements TelemetryConfigView {

    public static final String DESIRED_CDP_LOGGING_AGENT_VERSION = "desiredCdpLoggingAgentVersion";

    public static final String DESIRED_CDP_TELEMETRY_VERSION = "desiredCdpTelemetryVersion";

    private final TelemetryClusterDetails clusterDetails;

    private final List<AnonymizationRule> rules;

    private final List<VmLog> vmLogs;

    private final Integer databusConnectMaxTime;

    private final Integer databusConnectRetryTimes;

    private final Integer databusConnectRetryDelay;

    private final Integer databusConnectRetryMaxTime;

    private final String desiredCdpLoggingAgentVersion;

    private final String desiredCdpTelemetryVersion;

    private final String repoName;

    private final String repoBaseUrl;

    private final String repoGpgKey;

    private final Integer repoGpgCheck;

    private TelemetryCommonConfigView(Builder builder) {
        clusterDetails = builder.clusterDetails;
        rules = builder.rules;
        vmLogs = builder.vmLogs;
        databusConnectMaxTime = builder.databusConnectMaxTime;
        databusConnectRetryTimes = builder.databusConnectRetryTimes;
        databusConnectRetryDelay = builder.databusConnectRetryDelay;
        databusConnectRetryMaxTime = builder.databusConnectRetryMaxTime;
        desiredCdpLoggingAgentVersion = builder.desiredCdpLoggingAgentVersion;
        desiredCdpTelemetryVersion = builder.desiredCdpTelemetryVersion;
        repoName = builder.repoName;
        repoBaseUrl = builder.repoBaseUrl;
        repoGpgKey = builder.repoGpgKey;
        repoGpgCheck = builder.repoGpgCheck;
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        if (this.clusterDetails != null) {
            map.putAll(clusterDetails.toMap());
        }
        if (CollectionUtils.isNotEmpty(this.rules)) {
            map.put("anonymizationRules", this.rules);
        }
        if (CollectionUtils.isNotEmpty(vmLogs)) {
            map.put("logs", vmLogs);
        }
        map.put("databusConnectMaxTime", databusConnectMaxTime);
        map.put("databusConnectRetryTimes", databusConnectRetryTimes);
        map.put("databusConnectRetryDelay", databusConnectRetryDelay);
        map.put("databusConnectRetryMaxTime", databusConnectRetryMaxTime);
        map.put(DESIRED_CDP_LOGGING_AGENT_VERSION, desiredCdpLoggingAgentVersion);
        map.put(DESIRED_CDP_TELEMETRY_VERSION, desiredCdpTelemetryVersion);
        map.put("repoName", repoName);
        map.put("repoBaseUrl", repoBaseUrl);
        map.put("repoGpgCheck", repoGpgCheck);
        map.put("repoGpgKey", repoGpgKey);
        return map;
    }

    public static final class Builder {

        private TelemetryClusterDetails clusterDetails;

        private List<AnonymizationRule> rules;

        private List<VmLog> vmLogs;

        private Integer databusConnectMaxTime;

        private Integer databusConnectRetryTimes;

        private Integer databusConnectRetryDelay;

        private Integer databusConnectRetryMaxTime;

        private String desiredCdpLoggingAgentVersion;

        private String desiredCdpTelemetryVersion;

        private String repoName;

        private String repoBaseUrl;

        private String repoGpgKey;

        private Integer repoGpgCheck;

        public TelemetryCommonConfigView build() {
            return new TelemetryCommonConfigView(this);
        }

        public Builder withClusterDetails(TelemetryClusterDetails clusterDetails) {
            this.clusterDetails = clusterDetails;
            return this;
        }

        public Builder withRules(List<AnonymizationRule> rules) {
            this.rules = rules;
            return this;
        }

        public Builder withVmLogs(List<VmLog> vmLogs) {
            this.vmLogs = vmLogs;
            return this;
        }

        public Builder withDatabusConnectRetryTimes(Integer databusConnectRetryTimes) {
            this.databusConnectRetryTimes = databusConnectRetryTimes;
            return this;
        }

        public Builder withDatabusConnectMaxTimeSeconds(Integer databusConnectMaxTime) {
            this.databusConnectMaxTime = databusConnectMaxTime;
            return this;
        }

        public Builder withDatabusConnectRetryDelay(Integer databusConnectRetryDelay) {
            this.databusConnectRetryDelay = databusConnectRetryDelay;
            return this;
        }

        public Builder withDatabusConnectRetryMaxTime(Integer databusConnectRetryMaxTime) {
            this.databusConnectRetryMaxTime = databusConnectRetryMaxTime;
            return this;
        }

        public Builder withDesiredCdpLoggingAgentVersion(String desiredCdpLoggingAgentVersion) {
            this.desiredCdpLoggingAgentVersion = desiredCdpLoggingAgentVersion;
            return this;
        }

        public Builder withDesiredCdpTelemetryVersion(String desiredCdpTelemetryVersion) {
            this.desiredCdpTelemetryVersion = desiredCdpTelemetryVersion;
            return this;
        }

        public Builder withRepoName(String repoName) {
            this.repoName = repoName;
            return this;
        }

        public Builder withRepoBaseUrl(String repoBaseUrl) {
            this.repoBaseUrl = repoBaseUrl;
            return this;
        }

        public Builder withRepoGpgKey(String repoGpgKey) {
            this.repoGpgKey = repoGpgKey;
            return this;
        }

        public Builder withRepoGpgCheck(Integer repoGpgCheck) {
            this.repoGpgCheck = repoGpgCheck;
            return this;
        }
    }
}
