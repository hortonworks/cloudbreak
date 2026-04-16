package com.sequenceiq.cloudbreak.api.service;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ExposedService {

    private String name;

    private String displayName;

    private String serviceName;

    private String knoxService;

    private String knoxUrl;

    private String iconKey;

    private boolean ssoSupported;

    private Integer port;

    private Integer tlsPort;

    private boolean apiOnly;

    private boolean cmProxied;

    private boolean apiIncluded;

    private boolean visibleForDatalake;

    private boolean visibleForDatahub;

    private String entitlement;

    private String minVersion;

    private String maxVersion;

    private boolean withoutProxyPath;

    private String minHttpsVersion;

    private Set<String> roleTypes = new HashSet<>();

    private ExposedService(Builder builder) {
        this.name = builder.name;
        this.displayName = builder.displayName;
        this.serviceName = builder.serviceName;
        this.knoxService = builder.knoxService;
        this.knoxUrl = builder.knoxUrl;
        this.iconKey = builder.iconKey;
        this.ssoSupported = builder.ssoSupported;
        this.port = builder.port;
        this.tlsPort = builder.tlsPort;
        this.apiOnly = builder.apiOnly;
        this.cmProxied = builder.cmProxied;
        this.apiIncluded = builder.apiIncluded;
        this.visibleForDatalake = builder.visibleForDatalake;
        this.visibleForDatahub = builder.visibleForDatahub;
        this.entitlement = builder.entitlement;
        this.minVersion = builder.minVersion;
        this.maxVersion = builder.maxVersion;
        this.withoutProxyPath = builder.withoutProxyPath;
        this.minHttpsVersion = builder.minHttpsVersion;
        this.roleTypes = builder.roleTypes;
    }

    @JsonCreator
    public ExposedService(
            @JsonProperty("name") String name,
            @JsonProperty("displayName") String displayName,
            @JsonProperty("serviceName") String serviceName,
            @JsonProperty("knoxService") String knoxService,
            @JsonProperty("knoxUrl") String knoxUrl,
            @JsonProperty("iconKey") String iconKey,
            @JsonProperty("ssoSupported") boolean ssoSupported,
            @JsonProperty("port") Integer port,
            @JsonProperty("tlsPort") Integer tlsPort,
            @JsonProperty("apiOnly") boolean apiOnly,
            @JsonProperty("cmProxied") boolean cmProxied,
            @JsonProperty("apiIncluded") boolean apiIncluded,
            @JsonProperty("visibleForDatalake") boolean visibleForDatalake,
            @JsonProperty("visibleForDatahub") boolean visibleForDatahub,
            @JsonProperty("entitlement") String entitlement,
            @JsonProperty("minVersion") String minVersion,
            @JsonProperty("maxVersion") String maxVersion,
            @JsonProperty("withoutProxyPath") boolean withoutProxyPath,
            @JsonProperty("minHttpsVersion") String minHttpsVersion,
            @JsonProperty("roleTypes") Set<String> roleTypes
    ) {
        this.name = name;
        this.displayName = displayName;
        this.serviceName = serviceName;
        this.knoxService = knoxService;
        this.knoxUrl = knoxUrl;
        this.iconKey = iconKey;
        this.ssoSupported = ssoSupported;
        this.port = port;
        this.tlsPort = tlsPort;
        this.apiOnly = apiOnly;
        this.cmProxied = cmProxied;
        this.apiIncluded = apiIncluded;
        this.visibleForDatalake = visibleForDatalake;
        this.visibleForDatahub = visibleForDatahub;
        this.entitlement = entitlement;
        this.minVersion = minVersion;
        this.maxVersion = maxVersion;
        this.withoutProxyPath = withoutProxyPath;
        this.minHttpsVersion = minHttpsVersion;
        this.roleTypes = roleTypes;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getKnoxService() {
        return knoxService;
    }

    public void setKnoxService(String knoxService) {
        this.knoxService = knoxService;
    }

    public String getKnoxUrl() {
        return knoxUrl;
    }

    public void setKnoxUrl(String knoxUrl) {
        this.knoxUrl = knoxUrl;
    }

    public boolean isSsoSupported() {
        return ssoSupported;
    }

    public void setSsoSupported(boolean ssoSupported) {
        this.ssoSupported = ssoSupported;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public Integer getTlsPort() {
        return tlsPort;
    }

    public void setTlsPort(Integer tlsPort) {
        this.tlsPort = tlsPort;
    }

    public boolean isApiOnly() {
        return apiOnly;
    }

    public void setApiOnly(boolean apiOnly) {
        this.apiOnly = apiOnly;
    }

    public boolean isApiIncluded() {
        return apiIncluded;
    }

    public boolean isCmProxied() {
        return cmProxied;
    }

    public void setCmProxied(boolean cmProxied) {
        this.cmProxied = cmProxied;
    }

    public void setApiIncluded(boolean apiIncluded) {
        this.apiIncluded = apiIncluded;
    }

    public boolean isVisibleForDatalake() {
        return visibleForDatalake;
    }

    public void setVisibleForDatalake(boolean visibleForDatalake) {
        this.visibleForDatalake = visibleForDatalake;
    }

    public boolean isVisibleForDatahub() {
        return visibleForDatahub;
    }

    public void setVisibleForDatahub(boolean visibleForDatahub) {
        this.visibleForDatahub = visibleForDatahub;
    }

    public boolean isVisible() {
        return isVisibleForDatalake() || isVisibleForDatahub();
    }

    public void setMinVersion(String minVersion) {
        this.minVersion = minVersion;
    }

    public void setMaxVersion(String maxVersion) {
        this.maxVersion = maxVersion;
    }

    public String getMinVersion() {
        return minVersion;
    }

    public String getMaxVersion() {
        return maxVersion;
    }

    public String getEntitlement() {
        return entitlement;
    }

    public void setEntitlement(String entitlement) {
        this.entitlement = entitlement;
    }

    public boolean isWithoutProxyPath() {
        return withoutProxyPath;
    }

    public void setWithoutProxyPath(boolean withoutProxyPath) {
        this.withoutProxyPath = withoutProxyPath;
    }

    public String getMinHttpsVersion() {
        return minHttpsVersion;
    }

    public void setMinHttpsVersion(String minHttpsVersion) {
        this.minHttpsVersion = minHttpsVersion;
    }

    public String getIconKey() {
        return StringUtils.isNotBlank(iconKey) ? iconKey : knoxService;
    }

    public void setIconKey(String iconKey) {
        this.iconKey = iconKey;
    }

    public Set<String> getRoleTypes() {
        return roleTypes;
    }

    public void setRoleTypes(Set<String> roleTypes) {
        this.roleTypes = roleTypes;
    }

    //CHECKSTYLE:OFF: CyclomaticComplexity
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || !getClass().equals(o.getClass())) {
            return false;
        }
        ExposedService that = (ExposedService) o;
        return ssoSupported == that.ssoSupported &&
                apiOnly == that.apiOnly &&
                apiIncluded == that.apiIncluded &&
                visibleForDatalake == that.visibleForDatalake &&
                visibleForDatahub == that.visibleForDatahub &&
                Objects.equals(name, that.name) &&
                Objects.equals(displayName, that.displayName) &&
                Objects.equals(iconKey, that.iconKey) &&
                Objects.equals(serviceName, that.serviceName) &&
                Objects.equals(knoxService, that.knoxService) &&
                Objects.equals(knoxUrl, that.knoxUrl) &&
                Objects.equals(port, that.port) &&
                Objects.equals(minVersion, that.minVersion) &&
                Objects.equals(maxVersion, that.maxVersion) &&
                Objects.equals(entitlement, that.entitlement) &&
                Objects.equals(withoutProxyPath, that.withoutProxyPath) &&
                Objects.equals(minHttpsVersion, that.minHttpsVersion) &&
                Objects.equals(roleTypes, that.roleTypes) &&
                Objects.equals(tlsPort, that.tlsPort);
    }
    //CHECKSTYLE:ON

    @Override
    public int hashCode() {
        return Objects.hash(name, displayName, serviceName, knoxService, knoxUrl, ssoSupported, port, tlsPort,
                apiOnly, apiIncluded, visibleForDatahub, visibleForDatalake, minVersion, minHttpsVersion,
                maxVersion, entitlement, withoutProxyPath, iconKey, roleTypes);
    }

    @Override
    public String toString() {
        return "ExposedService{" +
                "name='" + name + '\'' +
                ", displayName='" + displayName + '\'' +
                ", serviceName='" + serviceName + '\'' +
                ", knoxService='" + knoxService + '\'' +
                ", knoxUrl='" + knoxUrl + '\'' +
                ", iconKey='" + iconKey + '\'' +
                ", ssoSupported=" + ssoSupported +
                ", port=" + port +
                ", tlsPort=" + tlsPort +
                ", apiOnly=" + apiOnly +
                ", apiIncluded=" + apiIncluded +
                ", isVisibleForDatahub=" + visibleForDatahub +
                ", isVisibleForDatalake=" + visibleForDatalake +
                ", minVersion=" + minVersion +
                ", maxVersion=" + maxVersion +
                ", entitlement=" + entitlement +
                ", roleTypes=" + roleTypes +
                ", minHttpsVersion=" + minHttpsVersion +
                ", withoutProxyPath=" + withoutProxyPath +
                '}';
    }

    public static final class Builder {
        private String name;

        private String displayName;

        private String serviceName;

        private String knoxService;

        private String knoxUrl;

        private String iconKey;

        private boolean ssoSupported;

        private Integer port;

        private Integer tlsPort;

        private boolean apiOnly;

        private boolean cmProxied;

        private boolean apiIncluded;

        private boolean visibleForDatalake;

        private boolean visibleForDatahub;

        private String entitlement;

        private String minVersion;

        private String maxVersion;

        private boolean withoutProxyPath;

        private String minHttpsVersion;

        private Set<String> roleTypes = new HashSet<>();

        private Builder() {
        }

        public Builder withName(String name) {
            this.name = name;
            return this;
        }

        public Builder withDisplayName(String displayName) {
            this.displayName = displayName;
            return this;
        }

        public Builder withServiceName(String serviceName) {
            this.serviceName = serviceName;
            return this;
        }

        public Builder withKnoxService(String knoxService) {
            this.knoxService = knoxService;
            return this;
        }

        public Builder withKnoxUrl(String knoxUrl) {
            this.knoxUrl = knoxUrl;
            return this;
        }

        public Builder withIconKey(String iconKey) {
            this.iconKey = iconKey;
            return this;
        }

        public Builder withSsoSupported(boolean ssoSupported) {
            this.ssoSupported = ssoSupported;
            return this;
        }

        public Builder withPort(Integer port) {
            this.port = port;
            return this;
        }

        public Builder withTlsPort(Integer tlsPort) {
            this.tlsPort = tlsPort;
            return this;
        }

        public Builder withApiOnly(boolean apiOnly) {
            this.apiOnly = apiOnly;
            return this;
        }

        public Builder withCmProxied(boolean cmProxied) {
            this.cmProxied = cmProxied;
            return this;
        }

        public Builder withApiIncluded(boolean apiIncluded) {
            this.apiIncluded = apiIncluded;
            return this;
        }

        public Builder withVisibleForDatalake(boolean visibleForDatalake) {
            this.visibleForDatalake = visibleForDatalake;
            return this;
        }

        public Builder withVisibleForDatahub(boolean visibleForDatahub) {
            this.visibleForDatahub = visibleForDatahub;
            return this;
        }

        public Builder withEntitlement(String entitlement) {
            this.entitlement = entitlement;
            return this;
        }

        public Builder withMinVersion(String minVersion) {
            this.minVersion = minVersion;
            return this;
        }

        public Builder withMaxVersion(String maxVersion) {
            this.maxVersion = maxVersion;
            return this;
        }

        public Builder withWithoutProxyPath(boolean withoutProxyPath) {
            this.withoutProxyPath = withoutProxyPath;
            return this;
        }

        public Builder withMinHttpsVersion(String minHttpsVersion) {
            this.minHttpsVersion = minHttpsVersion;
            return this;
        }

        public Builder withRoleTypes(Set<String> roleTypes) {
            this.roleTypes = roleTypes;
            return this;
        }

        public ExposedService build() {
            return new ExposedService(this);
        }
    }
}
