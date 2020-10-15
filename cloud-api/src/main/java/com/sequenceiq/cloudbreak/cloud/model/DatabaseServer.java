package com.sequenceiq.cloudbreak.cloud.model;

import java.util.HashMap;
import java.util.Map;

import com.sequenceiq.cloudbreak.cloud.model.generic.DynamicModel;

public class DatabaseServer extends DynamicModel {

    private final String serverId;

    private final String flavor;

    private final DatabaseEngine engine;

    private final String connectionDriver;

    private final String connectorJarUrl;

    private final String rootUserName;

    private final String rootPassword;

    private final Integer port;

    private final boolean useSslEnforcement;

    private final Long storageSize;

    private final Security security;

    private final InstanceStatus status;

    private final String location;

    private DatabaseServer(String serverId, String flavor, DatabaseEngine engine, String connectionDriver,
            String connectorJarUrl, String rootUserName, String rootPassword,
            Integer port, boolean useSslEnforcement, Long storageSize, Security security, InstanceStatus status, String location,
            Map<String, Object> parameters) {
        super(parameters);
        this.serverId = serverId;
        this.flavor = flavor;
        this.engine = engine;
        this.connectionDriver = connectionDriver;
        this.connectorJarUrl = connectorJarUrl;
        this.rootUserName = rootUserName;
        this.rootPassword = rootPassword;
        this.port = port;
        this.useSslEnforcement = useSslEnforcement;
        this.storageSize = storageSize;
        this.security = security;
        this.status = status;
        this.location = location;
    }

    public String getServerId() {
        return serverId;
    }

    public String getFlavor() {
        return flavor;
    }

    public DatabaseEngine getEngine() {
        return engine;
    }

    public String getConnectionDriver() {
        return connectionDriver;
    }

    public String getConnectorJarUrl() {
        return connectorJarUrl;
    }

    public String getRootUserName() {
        return rootUserName;
    }

    public String getRootPassword() {
        return rootPassword;
    }

    public Integer getPort() {
        return port;
    }

    public boolean isUseSslEnforcement() {
        return useSslEnforcement;
    }

    public Long getStorageSize() {
        return storageSize;
    }

    public Security getSecurity() {
        return security;
    }

    public InstanceStatus getStatus() {
        return status;
    }

    public String getLocation() {
        return location;
    }

    @Override
    public String toString() {
        return "DatabaseServer{"
                + "serverId='" + serverId + '\''
                + ", flavor='" + flavor + '\''
                + ", engine='" + engine + '\''
                + ", connectionDriver='" + connectionDriver + '\''
                + ", connectorJarUrl='" + connectorJarUrl + '\''
                + ", rootUserName='" + rootUserName + '\''
                + ", port='" + port + '\''
                + ", useSslEnforcement='" + useSslEnforcement + '\''
                + ", storageSize=" + storageSize
                + ", security=" + security
                + ", status=" + status
                + '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private String serverId;

        private String flavor;

        private DatabaseEngine engine;

        private String connectionDriver;

        private String connectorJarUrl;

        private String rootUserName;

        private String rootPassword;

        private Integer port;

        private boolean useSslEnforcement;

        private Long storageSize;

        private Security security;

        private InstanceStatus status;

        private String location;

        private Map<String, Object> params = new HashMap<>();

        public Builder serverId(String serverId) {
            this.serverId = serverId;
            return this;
        }

        public Builder flavor(String flavor) {
            this.flavor = flavor;
            return this;
        }

        public Builder engine(DatabaseEngine engine) {
            this.engine = engine;
            return this;
        }

        public Builder connectionDriver(String connectionDriver) {
            this.connectionDriver = connectionDriver;
            return this;
        }

        public Builder connectorJarUrl(String connectorJarUrl) {
            this.connectorJarUrl = connectorJarUrl;
            return this;
        }

        public Builder rootUserName(String rootUserName) {
            this.rootUserName = rootUserName;
            return this;
        }

        public Builder rootPassword(String rootPassword) {
            this.rootPassword = rootPassword;
            return this;
        }

        public Builder port(Integer port) {
            this.port = port;
            return this;
        }

        public Builder useSslEnforcement(boolean useSslEnforcement) {
            this.useSslEnforcement = useSslEnforcement;
            return this;
        }

        public Builder storageSize(Long storageSize) {
            this.storageSize = storageSize;
            return this;
        }

        public Builder security(Security security) {
            this.security = security;
            return this;
        }

        public Builder status(InstanceStatus status) {
            this.status = status;
            return this;
        }

        public Builder location(String location) {
            this.location = location;
            return this;
        }

        public Builder params(Map<String, Object> params) {
            this.params = params;
            return this;
        }

        public DatabaseServer build() {
            return new DatabaseServer(serverId, flavor, engine, connectionDriver, connectorJarUrl, rootUserName, rootPassword,
                port, useSslEnforcement, storageSize, security, status, location, params);
        }

    }

}
