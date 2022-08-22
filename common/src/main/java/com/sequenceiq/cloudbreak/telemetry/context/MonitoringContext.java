package com.sequenceiq.cloudbreak.telemetry.context;

import com.sequenceiq.cloudbreak.telemetry.monitoring.MonitoringAuthConfig;
import com.sequenceiq.cloudbreak.telemetry.monitoring.MonitoringClusterType;
import com.sequenceiq.cloudbreak.telemetry.monitoring.MonitoringServiceType;
import com.sequenceiq.common.api.telemetry.model.MonitoringCredential;

public class MonitoringContext {

    private final boolean enabled;

    private final MonitoringServiceType serviceType;

    private final String remoteWriteUrl;

    private final MonitoringCredential credential;

    private final MonitoringClusterType clusterType;

    private final MonitoringAuthConfig cmAuth;

    private final boolean cmAutoTls;

    private final char[] sharedPassword;

    private MonitoringContext(Builder builder) {
        this.enabled = builder.enabled;
        this.serviceType = builder.serviceType;
        this.remoteWriteUrl = builder.remoteWriteUrl;
        this.credential = builder.credential;
        this.clusterType = builder.clusterType;
        this.cmAuth = builder.cmAuth;
        this.cmAutoTls = builder.cmAutoTls;
        this.sharedPassword = builder.sharedPassword;
    }

    public static Builder builder() {
        return new Builder();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getRemoteWriteUrl() {
        return remoteWriteUrl;
    }

    public MonitoringServiceType getServiceType() {
        return serviceType;
    }

    public MonitoringClusterType getClusterType() {
        return clusterType;
    }

    public MonitoringAuthConfig getCmAuth() {
        return cmAuth;
    }

    public char[] getSharedPassword() {
        return sharedPassword;
    }

    public boolean isCmAutoTls() {
        return cmAutoTls;
    }

    public MonitoringCredential getCredential() {
        return credential;
    }

    @Override
    public String toString() {
        return "MonitoringContext{" +
                "enabled=" + enabled +
                ", serviceType=" + serviceType +
                ", remoteWriteUrl='" + remoteWriteUrl + '\'' +
                ", credential=" + credential +
                ", clusterType=" + clusterType +
                ", cmAuth=" + cmAuth +
                ", cmAutoTls=" + cmAutoTls +
                ", sharedPassword=*****" +
                '}';
    }

    public static class Builder {

        private boolean enabled;

        private MonitoringServiceType serviceType = MonitoringServiceType.PAAS;

        private String remoteWriteUrl;

        private MonitoringCredential credential;

        private MonitoringAuthConfig cmAuth;

        private char[] sharedPassword;

        private MonitoringClusterType clusterType;

        private boolean cmAutoTls;

        private Builder() {
        }

        public MonitoringContext build() {
            return new MonitoringContext(this);
        }

        public Builder enabled() {
            this.enabled = true;
            return this;
        }

        public Builder withServiceType(MonitoringServiceType serviceType) {
            this.serviceType = serviceType;
            return this;
        }

        public Builder withClusterType(MonitoringClusterType clusterType) {
            this.clusterType = clusterType;
            return this;
        }

        public Builder withRemoteWriteUrl(String remoteWriteUrl) {
            this.remoteWriteUrl = remoteWriteUrl;
            return this;
        }

        public Builder withCredential(MonitoringCredential credential) {
            this.credential = credential;
            return this;
        }

        public Builder withCmAuth(MonitoringAuthConfig cmAuth) {
            this.cmAuth = cmAuth;
            return this;
        }

        public Builder withSharedPassword(char[] sharedPassword) {
            this.sharedPassword = sharedPassword;
            return this;
        }

        public Builder withCmAutoTls(boolean cmAutoTls) {
            this.cmAutoTls = cmAutoTls;
            return this;
        }

    }
}
