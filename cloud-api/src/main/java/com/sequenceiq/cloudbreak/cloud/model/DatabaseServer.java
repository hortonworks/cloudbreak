package com.sequenceiq.cloudbreak.cloud.model;

import com.sequenceiq.cloudbreak.cloud.model.generic.DynamicModel;

import java.util.Map;

public class DatabaseServer extends DynamicModel {

    private final String serverId;

    private final String flavor;

    private final DatabaseEngine engine;

    private final String rootUserName;

    private final String rootPassword;

    private final long storageSize;

    private final Security security;

    private final InstanceStatus status;

    public DatabaseServer(String serverId, String flavor, DatabaseEngine engine, String rootUserName, String rootPassword,
            long storageSize, Security security, InstanceStatus status) {
        this.serverId = serverId;
        this.flavor = flavor;
        this.engine = engine;
        this.rootUserName = rootUserName;
        this.rootPassword = rootPassword;
        this.storageSize = storageSize;
        this.security = security;
        this.status = status;
    }

    public DatabaseServer(String serverId, String flavor, DatabaseEngine engine, String rootUserName, String rootPassword,
            long storageSize, Security security, InstanceStatus status, Map<String, Object> parameters) {
        super(parameters);
        this.serverId = serverId;
        this.flavor = flavor;
        this.engine = engine;
        this.rootUserName = rootUserName;
        this.rootPassword = rootPassword;
        this.storageSize = storageSize;
        this.security = security;
        this.status = status;
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

    public String getRootUserName() {
        return rootUserName;
    }

    public String getRootPassword() {
        return rootPassword;
    }

    public long getStorageSize() {
        return storageSize;
    }

    public Security getSecurity() {
        return security;
    }

    public InstanceStatus getStatus() {
        return status;
    }

    @Override
    public String toString() {
        return "DatabaseServer{"
                + "serverId='" + serverId + '\''
                + ", flavor='" + flavor + '\''
                + ", engine='" + engine + '\''
                + ", rootUserName='" + rootUserName + '\''
                + ", storageSize=" + storageSize
                + ", security=" + security
                + ", status=" + status
                + '}';
    }
}
