package com.sequenceiq.cloudbreak.api.model;

import java.util.Set;

import com.sequenceiq.cloudbreak.doc.ModelDescriptions;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class RDSConfigResponse extends RDSConfigJson {

    @ApiModelProperty(value = ModelDescriptions.RDSConfig.NAME, required = true)
    private String name;

    @ApiModelProperty(value = ModelDescriptions.ID)
    private Long id;

    @ApiModelProperty(value = ModelDescriptions.CREATED)
    private Long creationDate;

    @ApiModelProperty(value = ModelDescriptions.PUBLIC_IN_ACCOUNT)
    private boolean publicInAccount;

    @ApiModelProperty(value = ModelDescriptions.RDSConfigModelDescription.CLUSTER_NAMES)
    private Set<String> clusterNames;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

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
}
