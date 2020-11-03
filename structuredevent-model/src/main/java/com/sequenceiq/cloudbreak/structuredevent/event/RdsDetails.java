package com.sequenceiq.cloudbreak.structuredevent.event;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class RdsDetails implements Serializable {

    /**
     * @deprecated this is value is not set anymore, since this is considered as sensitive information
     */
    @Deprecated
    private Long id;

    /**
     * @deprecated this is value is not set anymore, since this is considered as sensitive information
     */
    @Deprecated
    private String name;

    /**
     * @deprecated this is value is not set anymore, since this is considered as sensitive information
     */
    @Deprecated
    private String description;

    /**
     * @deprecated this is value is not set anymore, since this is considered as sensitive information
     */
    @Deprecated
    private String connectionURL;

    private String sslMode;

    private String databaseEngine;

    /**
     * @deprecated this is value is not set anymore, since this is considered as useless information
     */
    @Deprecated
    private String connectionDriver;

    private Long creationDate;

    private String stackVersion;

    private String status;

    private String type;

    /**
     * @deprecated this is value is not set anymore, since this is considered as useless information
     */
    @Deprecated
    private String connectorJarUrl;

    /**
     * @deprecated this is value is not set anymore, since this is considered as sensitive information
     */
    @Deprecated
    private Long workspaceId;

    /**
     * @deprecated this is value is not set anymore, since this is considered as sensitive information
     */
    @Deprecated
    private String userId;

    /**
     * @deprecated this is value is not set anymore, since this is considered as sensitive information
     */
    @Deprecated
    private String userName;

    /**
     * @deprecated this is value is not set anymore, since this is considered as duplicated data
     */
    @Deprecated
    private String tenantName;

    private Boolean externalDatabase;

    public String getDatabaseEngine() {
        return databaseEngine;
    }

    public void setDatabaseEngine(String databaseEngine) {
        this.databaseEngine = databaseEngine;
    }

    public Long getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Long creationDate) {
        this.creationDate = creationDate;
    }

    public String getStackVersion() {
        return stackVersion;
    }

    public void setStackVersion(String stackVersion) {
        this.stackVersion = stackVersion;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Boolean getExternal() {
        return externalDatabase;
    }

    public void setExternal(Boolean external) {
        externalDatabase = external;
    }

    public String getSslMode() {
        return sslMode;
    }

    public void setSslMode(String sslMode) {
        this.sslMode = sslMode;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getConnectionURL() {
        return connectionURL;
    }

    public void setConnectionURL(String connectionURL) {
        this.connectionURL = connectionURL;
    }

    public String getConnectionDriver() {
        return connectionDriver;
    }

    public void setConnectionDriver(String connectionDriver) {
        this.connectionDriver = connectionDriver;
    }

    public String getConnectorJarUrl() {
        return connectorJarUrl;
    }

    public void setConnectorJarUrl(String connectorJarUrl) {
        this.connectorJarUrl = connectorJarUrl;
    }

    public Long getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(Long workspaceId) {
        this.workspaceId = workspaceId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getTenantName() {
        return tenantName;
    }

    public void setTenantName(String tenantName) {
        this.tenantName = tenantName;
    }
}
