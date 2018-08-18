package com.sequenceiq.cloudbreak.api.model.rds;

import java.util.Set;

import com.sequenceiq.cloudbreak.api.model.users.OrganizationResourceResponse;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.RDSConfigModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class RDSConfigResponse extends RDSConfigJson {

    @ApiModelProperty(ModelDescriptions.ID)
    private Long id;

    @ApiModelProperty(ModelDescriptions.CREATED)
    private Long creationDate;

    @ApiModelProperty(ModelDescriptions.PUBLIC_IN_ACCOUNT)
    private boolean publicInAccount;

    @ApiModelProperty(RDSConfigModelDescription.CLUSTER_NAMES)
    private Set<String> clusterNames;

    @ApiModelProperty(value = ModelDescriptions.RDSConfig.STACK_VERSION)
    private String stackVersion;

    @ApiModelProperty(value = ModelDescriptions.RDSConfig.DB_ENGINE, required = true)
    private String databaseEngine;

    @ApiModelProperty(value = ModelDescriptions.RDSConfig.CONNECTION_DRIVER_NAME, required = true)
    private String connectionDriver;

    @ApiModelProperty(value = ModelDescriptions.RDSConfig.DB_ENGINE_DISPLAYNAME, required = true)
    private String databaseEngineDisplayName;

    @ApiModelProperty(ModelDescriptions.ORGANIZATION_OF_THE_RESOURCE)
    private OrganizationResourceResponse organization;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Long creationDate) {
        this.creationDate = creationDate;
    }

    public boolean isPublicInAccount() {
        return publicInAccount;
    }

    public void setPublicInAccount(boolean publicInAccount) {
        this.publicInAccount = publicInAccount;
    }

    public Set<String> getClusterNames() {
        return clusterNames;
    }

    public void setClusterNames(Set<String> clusterNames) {
        this.clusterNames = clusterNames;
    }

    public String getStackVersion() {
        return stackVersion;
    }

    public void setStackVersion(String stackVersion) {
        this.stackVersion = stackVersion;
    }

    public String getDatabaseEngine() {
        return databaseEngine;
    }

    public void setDatabaseEngine(String databaseEngine) {
        this.databaseEngine = databaseEngine;
    }

    public String getConnectionDriver() {
        return connectionDriver;
    }

    public void setConnectionDriver(String connectionDriver) {
        this.connectionDriver = connectionDriver;
    }

    public String getDatabaseEngineDisplayName() {
        return databaseEngineDisplayName;
    }

    public void setDatabaseEngineDisplayName(String databaseEngineDisplayName) {
        this.databaseEngineDisplayName = databaseEngineDisplayName;
    }

    public OrganizationResourceResponse getOrganization() {
        return organization;
    }

    public void setOrganization(OrganizationResourceResponse organization) {
        this.organization = organization;
    }
}
