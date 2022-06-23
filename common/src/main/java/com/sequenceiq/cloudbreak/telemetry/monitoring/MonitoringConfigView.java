package com.sequenceiq.cloudbreak.telemetry.monitoring;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.ObjectUtils;

import com.sequenceiq.cloudbreak.telemetry.TelemetryClusterDetails;
import com.sequenceiq.cloudbreak.telemetry.TelemetryConfigView;

public class MonitoringConfigView implements TelemetryConfigView {

    private static final String EMPTY_CONFIG_DEFAULT = "";

    private static final String AGENT_MAX_DISK_USAGE_DEFAULT = "4GB";

    private static final Integer DEFAULT_CM_SMON_PORT = 61010;

    private final boolean enabled;

    private final String type;

    private final String cmUsername;

    private final char[] cmPassword;

    private final char[] localPassword;

    private final Integer cmMetricsExporterPort;

    private final String nodeExporterUser;

    private final Integer nodeExporterPort;

    private final List<String> nodeExporterCollectors;

    private final String blackboxExporterUser;

    private final Integer blackboxExporterPort;

    private final Integer agentPort;

    private final String agentUser;

    private final String agentMaxDiskUsage;

    private final String remoteWriteUrl;

    private final Integer scrapeIntervalSeconds;

    private final boolean useDevStack;

    private final String username;

    private final char[] password;

    private final char[] token;

    private final TelemetryClusterDetails clusterDetails;

    private final RequestSignerConfigView requestSigner;

    private MonitoringConfigView(Builder builder) {
        this.enabled = builder.enabled;
        this.type = builder.type;
        this.cmUsername = builder.cmUsername;
        this.cmPassword = builder.cmPassword;
        this.cmMetricsExporterPort = builder.cmMetricsExporterPort;
        this.localPassword = builder.localPassword;
        this.nodeExporterUser = builder.nodeExporterUser;
        this.nodeExporterPort = builder.nodeExporterPort;
        this.nodeExporterCollectors = builder.nodeExporterCollectors;
        this.blackboxExporterUser = builder.blackboxExporterUser;
        this.blackboxExporterPort = builder.blackboxExporterPort;
        this.agentUser = builder.agentUser;
        this.agentPort = builder.agentPort;
        this.agentMaxDiskUsage = builder.agentMaxDiskUsage;
        this.clusterDetails = builder.clusterDetails;
        this.remoteWriteUrl = builder.remoteWriteUrl;
        this.scrapeIntervalSeconds = builder.scrapeIntervalSeconds;
        this.useDevStack = builder.useDevStack;
        this.username = builder.username;
        this.password = builder.password;
        this.token = builder.token;
        this.requestSigner = builder.requestSigner;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getCmUsername() {
        return cmUsername;
    }

    public char[] getCmPassword() {
        return cmPassword;
    }

    public Integer getCmMetricsExporterPort() {
        return cmMetricsExporterPort;
    }

    public String getType() {
        return type;
    }

    public TelemetryClusterDetails getClusterDetails() {
        return clusterDetails;
    }

    public String getRemoteWriteUrl() {
        return remoteWriteUrl;
    }

    public Integer getScrapeIntervalSeconds() {
        return scrapeIntervalSeconds;
    }

    public char[] getLocalPassword() {
        return localPassword;
    }

    public String getNodeExporterUser() {
        return nodeExporterUser;
    }

    public Integer getNodeExporterPort() {
        return nodeExporterPort;
    }

    public List<String> getNodeExporterCollectors() {
        return nodeExporterCollectors;
    }

    public String getBlackboxExporterUser() {
        return blackboxExporterUser;
    }

    public Integer getBlackboxExporterPort() {
        return blackboxExporterPort;
    }

    public String getAgentUser() {
        return agentUser;
    }

    public Integer getAgentPort() {
        return agentPort;
    }

    public String getAgentMaxDiskUsage() {
        return agentMaxDiskUsage;
    }

    public boolean isUseDevStack() {
        return useDevStack;
    }

    public String getUsername() {
        return username;
    }

    public char[] getPassword() {
        return password;
    }

    public char[] getToken() {
        return token;
    }

    public RequestSignerConfigView getRequestSigner() {
        return requestSigner;
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("enabled", this.enabled);
        map.put("remoteWriteUrl", this.remoteWriteUrl);
        map.put("scrapeIntervalSeconds", this.scrapeIntervalSeconds);
        map.put("useDevStack", this.useDevStack);
        map.put("type", ObjectUtils.defaultIfNull(this.type, EMPTY_CONFIG_DEFAULT));
        map.put("cmUsername", ObjectUtils.defaultIfNull(this.cmUsername, EMPTY_CONFIG_DEFAULT));
        map.put("cmPassword", ObjectUtils.defaultIfNull(this.cmPassword, EMPTY_CONFIG_DEFAULT));
        map.put("cmMetricsExporterPort", ObjectUtils.defaultIfNull(this.cmMetricsExporterPort, DEFAULT_CM_SMON_PORT));
        map.put("localPassword", this.localPassword != null ? new String(this.localPassword) : EMPTY_CONFIG_DEFAULT);
        map.put("nodeExporterUser", ObjectUtils.defaultIfNull(this.nodeExporterUser, EMPTY_CONFIG_DEFAULT));
        map.put("nodeExporterPort", this.nodeExporterPort);
        map.put("nodeExporterCollectors", ObjectUtils.defaultIfNull(this.nodeExporterCollectors, new ArrayList<>()));
        map.put("blackboxExporterUser", ObjectUtils.defaultIfNull(this.blackboxExporterUser, EMPTY_CONFIG_DEFAULT));
        map.put("blackboxExporterPort", this.blackboxExporterPort);
        map.put("agentUser", ObjectUtils.defaultIfNull(this.agentUser, EMPTY_CONFIG_DEFAULT));
        map.put("agentPort", this.agentPort);
        map.put("agentMaxDiskUsage", ObjectUtils.defaultIfNull(this.agentMaxDiskUsage, AGENT_MAX_DISK_USAGE_DEFAULT));
        map.put("username", ObjectUtils.defaultIfNull(this.username, EMPTY_CONFIG_DEFAULT));
        map.put("password", this.password != null ? new String(this.password) : EMPTY_CONFIG_DEFAULT);
        map.put("token", this.token != null ? new String(this.token) : EMPTY_CONFIG_DEFAULT);
        if (this.clusterDetails != null) {
            map.putAll(clusterDetails.toMap());
        }
        if (this.requestSigner != null) {
            map.putAll(requestSigner.toMap());
        }
        return map;
    }

    public static final class Builder {

        private boolean enabled;

        private String remoteWriteUrl;

        private Integer scrapeIntervalSeconds;

        private boolean useDevStack;

        private String type;

        private String username;

        private char[] password;

        private char[] token;

        private String cmUsername;

        private char[] cmPassword;

        private char[] localPassword;

        private Integer cmMetricsExporterPort;

        private String nodeExporterUser;

        private Integer nodeExporterPort;

        private List<String> nodeExporterCollectors;

        private String blackboxExporterUser;

        private Integer blackboxExporterPort;

        private String agentUser;

        private Integer agentPort;

        private String agentMaxDiskUsage;

        private TelemetryClusterDetails clusterDetails;

        private RequestSignerConfigView requestSigner;

        public MonitoringConfigView build() {
            return new MonitoringConfigView(this);
        }

        public Builder withEnabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public Builder withRemoteWriteUrl(String remoteWriteUrl) {
            this.remoteWriteUrl = remoteWriteUrl;
            return this;
        }

        public Builder withScrapeIntervalSeconds(Integer scrapeIntervalSeconds) {
            this.scrapeIntervalSeconds = scrapeIntervalSeconds;
            return this;
        }

        public Builder withUseDevStack(boolean useDevStack) {
            this.useDevStack = useDevStack;
            return this;
        }

        public Builder withType(String type) {
            this.type = type;
            return this;
        }

        public Builder withUsername(String username) {
            this.username = username;
            return this;
        }

        public Builder withPassword(char[] password) {
            this.password = password;
            return this;
        }

        public Builder withToken(char[] token) {
            this.token = token;
            return this;
        }

        public Builder withCMUsername(String cmUsername) {
            this.cmUsername = cmUsername;
            return this;
        }

        public Builder withCMPassword(char[] cmPassword) {
            this.cmPassword = cmPassword;
            return this;
        }

        public Builder withCMMetricsExporterPort(Integer cmMetricsExporterPort) {
            this.cmMetricsExporterPort = cmMetricsExporterPort;
            return this;
        }

        public Builder withClusterDetails(TelemetryClusterDetails clusterDetails) {
            this.clusterDetails = clusterDetails;
            return this;
        }

        public Builder withRequestSigner(RequestSignerConfigView requestSigner) {
            this.requestSigner = requestSigner;
            return this;
        }

        public Builder withLocalPassword(char[] localPassword) {
            this.localPassword = localPassword;
            return this;
        }

        public Builder withNodeExporterUser(String nodeExporterUser) {
            this.nodeExporterUser = nodeExporterUser;
            return this;
        }

        public Builder withNodeExporterPort(Integer nodeExporterPort) {
            this.nodeExporterPort = nodeExporterPort;
            return this;
        }

        public Builder withNodeExporterCollectors(List<String> nodeExporterCollectors) {
            this.nodeExporterCollectors = nodeExporterCollectors;
            return this;
        }

        public Builder withBlackboxExporterUser(String blackboxExporterUser) {
            this.blackboxExporterUser = blackboxExporterUser;
            return this;
        }

        public Builder withBlackboxExporterPort(Integer blackboxExporterPort) {
            this.blackboxExporterPort = blackboxExporterPort;
            return this;
        }

        public Builder withAgentPort(Integer agentPort) {
            this.agentPort = agentPort;
            return this;
        }

        public Builder withAgentUser(String agentUser) {
            this.agentUser = agentUser;
            return this;
        }

        public Builder withAgentMaxDiskUsage(String maxDiskUsage) {
            this.agentMaxDiskUsage = maxDiskUsage;
            return this;
        }
    }
}
