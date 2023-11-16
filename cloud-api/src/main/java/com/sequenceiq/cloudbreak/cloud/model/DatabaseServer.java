package com.sequenceiq.cloudbreak.cloud.model;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.sequenceiq.cloudbreak.cloud.model.generic.DynamicModel;

@JsonDeserialize(builder = DatabaseServer.Builder.class)
public class DatabaseServer extends DynamicModel {

    /**
     * Key of the optional dynamic parameter denoting the cloud provider specific identifier of the SSL root certificate to use for the DB server instance.
     * The exact interpretation of this setting is up to the target cloud provider. Relevant only if {@code useSslEnforcement == true}.
     *
     * <p>
     * When set, the value shall be a nonempty {@link String} containing the SSL root certificate identifier in a cloud provider specific syntax.
     * </p>
     *
     * @see #isUseSslEnforcement()
     * @see #putParameter(String, Object)
     */
    public static final String SSL_CERTIFICATE_IDENTIFIER = "sslCertificateIdentifier";

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

    private final boolean highAvailability;

    /*
     * We need this constructor because flow request objects use this class, and it can not be deserialized to object
     */
    private DatabaseServer(String serverId, String flavor, DatabaseEngine engine, String connectionDriver, String connectorJarUrl, String rootUserName,
            String rootPassword, Integer port, boolean useSslEnforcement, Long storageSize, Security security, InstanceStatus status, String location,
            boolean highAvailability, Map<String, Object> params) {
        super(params);
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
        this.highAvailability = highAvailability;
    }

    private DatabaseServer(Builder builder) {
        super(builder.params);
        this.serverId = builder.serverId;
        this.flavor = builder.flavor;
        this.engine = builder.engine;
        this.connectionDriver = builder.connectionDriver;
        this.connectorJarUrl = builder.connectorJarUrl;
        this.rootUserName = builder.rootUserName;
        this.rootPassword = builder.rootPassword;
        this.port = builder.port;
        this.useSslEnforcement = builder.useSslEnforcement;
        this.storageSize = builder.storageSize;
        this.security = builder.security;
        this.status = builder.status;
        this.location = builder.location;
        this.highAvailability = builder.highAvailability;
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

    public boolean getHighAvailability() {
        return highAvailability;
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
                + ", highAvailability=" + highAvailability
                + ", dynamicModel=" + super.toString()
                + '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(DatabaseServer databaseServer) {
        return new Builder()
                .withServerId(databaseServer.serverId)
                .withFlavor(databaseServer.flavor)
                .withEngine(databaseServer.engine)
                .withConnectionDriver(databaseServer.connectionDriver)
                .withConnectorJarUrl(databaseServer.connectorJarUrl)
                .withRootUserName(databaseServer.rootUserName)
                .withRootPassword(databaseServer.rootPassword)
                .withPort(databaseServer.port)
                .withHighAvailability(databaseServer.highAvailability)
                .withUseSslEnforcement(databaseServer.useSslEnforcement)
                .withStorageSize(databaseServer.storageSize)
                .withSecurity(databaseServer.security)
                .withStatus(databaseServer.status)
                .withLocation(databaseServer.location)
                .withParams(databaseServer.getParameters());
    }

    @JsonPOJOBuilder
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

        private boolean highAvailability;

        private Map<String, Object> params = new HashMap<>();

        public Builder withServerId(String serverId) {
            this.serverId = serverId;
            return this;
        }

        public Builder withFlavor(String flavor) {
            this.flavor = flavor;
            return this;
        }

        public Builder withEngine(DatabaseEngine engine) {
            this.engine = engine;
            return this;
        }

        public Builder withConnectionDriver(String connectionDriver) {
            this.connectionDriver = connectionDriver;
            return this;
        }

        public Builder withConnectorJarUrl(String connectorJarUrl) {
            this.connectorJarUrl = connectorJarUrl;
            return this;
        }

        public Builder withRootUserName(String rootUserName) {
            this.rootUserName = rootUserName;
            return this;
        }

        public Builder withRootPassword(String rootPassword) {
            this.rootPassword = rootPassword;
            return this;
        }

        public Builder withPort(Integer port) {
            this.port = port;
            return this;
        }

        public Builder withHighAvailability(boolean highAvailability) {
            this.highAvailability = highAvailability;
            return this;
        }

        public Builder withUseSslEnforcement(boolean useSslEnforcement) {
            this.useSslEnforcement = useSslEnforcement;
            return this;
        }

        public Builder withStorageSize(Long storageSize) {
            this.storageSize = storageSize;
            return this;
        }

        public Builder withSecurity(Security security) {
            this.security = security;
            return this;
        }

        public Builder withStatus(InstanceStatus status) {
            this.status = status;
            return this;
        }

        public Builder withLocation(String location) {
            this.location = location;
            return this;
        }

        public Builder withParams(Map<String, Object> params) {
            this.params = params;
            return this;
        }

        public DatabaseServer build() {
            return new DatabaseServer(this);
        }

    }

}
