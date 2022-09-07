package com.sequenceiq.cloudbreak.telemetry.monitoring;

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sequenceiq.cloudbreak.telemetry.TelemetryClusterDetails;
import com.sequenceiq.cloudbreak.telemetry.TelemetryConfigView;

public class MonitoringConfigView implements TelemetryConfigView {

    private static final String EMPTY_CONFIG_DEFAULT = "";

    private static final String AGENT_MAX_DISK_USAGE_DEFAULT = "4GB";

    private static final String RETENTION_MIN_TIME = "5m";

    private static final String RETENTION_MAX_TIME = "4h";

    private static final String MIN_BACKOFF = "1s";

    private static final String MAX_BACKOFF = "20m";

    private static final String WAL_TRUNCATE_FREQUENCY = "2h";

    private static final Integer DEFAULT_CM_SMON_PORT = 61010;

    private final boolean enabled;

    private final String type;

    private final String cmUsername;

    private final char[] cmPassword;

    private final char[] localPassword;

    private final Integer cmMetricsExporterPort;

    private final boolean cmAutoTls;

    private final String nodeExporterUser;

    private final Integer nodeExporterPort;

    private final List<String> nodeExporterCollectors;

    private final String blackboxExporterUser;

    private final Integer blackboxExporterPort;

    private final Integer blackboxCloudInvervalSeconds;

    private final Integer blackboxClouderaIntervalSeconds;

    private final boolean blackboxCheckOnAllNodes;

    private final Integer agentPort;

    private final String agentUser;

    private final String agentMaxDiskUsage;

    private final String retentionMinTime;

    private final String retentionMaxTime;

    private final String minBackoff;

    private final String maxBackoff;

    private final String walTruncateFrequency;

    private final String remoteWriteUrl;

    private final Integer scrapeIntervalSeconds;

    private final boolean useDevStack;

    private final String username;

    private final char[] password;

    private final char[] token;

    private final String accessKeyId;

    private final char[] privateKey;

    private final String accessKeyType;

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
        this.blackboxCloudInvervalSeconds = builder.cloudIntervalSeconds;
        this.blackboxClouderaIntervalSeconds = builder.clouderaIntervalSeconds;
        this.blackboxCheckOnAllNodes = builder.checkOnAllNodes;
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
        this.retentionMinTime = builder.retentionMinTime;
        this.retentionMaxTime = builder.retentionMaxTime;
        this.minBackoff = builder.minBackoff;
        this.maxBackoff = builder.maxBackoff;
        this.walTruncateFrequency = builder.walTruncateFrequency;
        this.accessKeyId = builder.accessKeyId;
        this.privateKey = builder.privateKey;
        this.accessKeyType = builder.accessKeyType;
        this.cmAutoTls = builder.cmAutoTls;
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

    public boolean isCmAutoTls() {
        return cmAutoTls;
    }

    public String getRetentionMinTime() {
        return retentionMinTime;
    }

    public String getRetentionMaxTime() {
        return retentionMaxTime;
    }

    public String getWalTruncateFrequency() {
        return walTruncateFrequency;
    }

    public String getAccessKeyId() {
        return accessKeyId;
    }

    public char[] getPrivateKey() {
        return privateKey;
    }

    public String getAccessKeyType() {
        return accessKeyType;
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
        map.put("type", defaultIfNull(this.type, EMPTY_CONFIG_DEFAULT));
        map.put("cmUsername", defaultIfNull(this.cmUsername, EMPTY_CONFIG_DEFAULT));
        map.put("cmPassword", defaultIfNull(this.cmPassword, EMPTY_CONFIG_DEFAULT));
        map.put("cmMetricsExporterPort", defaultIfNull(this.cmMetricsExporterPort, DEFAULT_CM_SMON_PORT));
        map.put("cmAutoTls", defaultIfNull(this.cmAutoTls, true));
        map.put("localPassword", this.localPassword != null ? new String(this.localPassword) : EMPTY_CONFIG_DEFAULT);
        map.put("nodeExporterUser", defaultIfNull(this.nodeExporterUser, EMPTY_CONFIG_DEFAULT));
        map.put("nodeExporterPort", this.nodeExporterPort);
        map.put("nodeExporterCollectors", defaultIfNull(this.nodeExporterCollectors, new ArrayList<>()));
        map.put("blackboxExporterUser", defaultIfNull(this.blackboxExporterUser, EMPTY_CONFIG_DEFAULT));
        map.put("blackboxExporterPort", this.blackboxExporterPort);
        map.put("blackboxExporterCloudIntervalSeconds", this.blackboxCloudInvervalSeconds);
        map.put("blackboxExporterClouderaIntervalSeconds", this.blackboxClouderaIntervalSeconds);
        map.put("blackboxExporterCheckOnAllNodes", this.blackboxCheckOnAllNodes);
        map.put("agentUser", defaultIfNull(this.agentUser, EMPTY_CONFIG_DEFAULT));
        map.put("agentPort", this.agentPort);
        map.put("agentMaxDiskUsage", defaultIfNull(this.agentMaxDiskUsage, AGENT_MAX_DISK_USAGE_DEFAULT));
        map.put("retentionMinTime", defaultIfNull(this.retentionMinTime, RETENTION_MIN_TIME));
        map.put("retentionMaxTime", defaultIfNull(this.retentionMaxTime, RETENTION_MAX_TIME));
        map.put("minBackoff", defaultIfNull(this.minBackoff, MIN_BACKOFF));
        map.put("maxBackoff", defaultIfNull(this.maxBackoff, MAX_BACKOFF));
        map.put("walTruncateFrequency", defaultIfNull(this.walTruncateFrequency, WAL_TRUNCATE_FREQUENCY));
        map.put("username", defaultIfNull(this.username, EMPTY_CONFIG_DEFAULT));
        map.put("password", this.password != null ? new String(this.password) : EMPTY_CONFIG_DEFAULT);
        map.put("token", this.token != null ? new String(this.token) : EMPTY_CONFIG_DEFAULT);
        map.put("monitoringAccessKeyId", defaultIfNull(this.accessKeyId, EMPTY_CONFIG_DEFAULT));
        map.put("monitoringPrivateKey", this.privateKey != null ? new String(this.privateKey) : EMPTY_CONFIG_DEFAULT);
        map.put("monitoringAccessKeyType", defaultIfNull(this.accessKeyType, EMPTY_CONFIG_DEFAULT));
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

        private boolean cmAutoTls;

        private String nodeExporterUser;

        private Integer nodeExporterPort;

        private List<String> nodeExporterCollectors;

        private String blackboxExporterUser;

        private Integer blackboxExporterPort;

        private Integer clouderaIntervalSeconds;

        private Integer cloudIntervalSeconds;

        private boolean checkOnAllNodes;

        private String agentUser;

        private Integer agentPort;

        private String agentMaxDiskUsage;

        private String retentionMinTime;

        private String retentionMaxTime;

        private String minBackoff;

        private String maxBackoff;

        private String walTruncateFrequency;

        private String accessKeyId;

        private char[] privateKey;

        private String accessKeyType;

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

        public Builder withBlackboxExporterCloudIntervalSeconds(Integer cloudIntervalSeconds) {
            this.cloudIntervalSeconds = cloudIntervalSeconds;
            return this;
        }

        public Builder withBlackboxExporterClouderaIntervalSeconds(Integer clouderaIntervalSeconds) {
            this.clouderaIntervalSeconds = clouderaIntervalSeconds;
            return this;
        }

        public Builder withBlackboxExporterCheckOnAllNodes(boolean checkOnAllNodes) {
            this.checkOnAllNodes = checkOnAllNodes;
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

        public Builder withRetentionMinTime(String retentionMinTime) {
            this.retentionMinTime = retentionMinTime;
            return this;
        }

        public Builder withRetentionMaxTime(String retentionMaxTime) {
            this.retentionMaxTime = retentionMaxTime;
            return this;
        }

        public Builder withWalTruncateFrequency(String walTruncateFrequency) {
            this.walTruncateFrequency = walTruncateFrequency;
            return this;
        }

        public Builder withMinBackoff(String minBackoff) {
            this.minBackoff = minBackoff;
            return this;
        }

        public Builder withMaxBackoff(String maxBackoff) {
            this.maxBackoff = maxBackoff;
            return this;
        }

        public Builder withAccessKeyId(String accessKeyId) {
            this.accessKeyId = accessKeyId;
            return this;
        }

        public Builder withPrivateKey(char[] privateKey) {
            this.privateKey = privateKey;
            return this;
        }

        public Builder withAccessKeyType(String accessKeyType) {
            this.accessKeyType = accessKeyType;
            return this;
        }

        public Builder withCmAutoTls(boolean cmAutoTls) {
            this.cmAutoTls = cmAutoTls;
            return this;
        }
    }
}
