package com.sequenceiq.cloudbreak.api.endpoint.v4.database.requests;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.api.endpoint.v4.JsonEntity;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.Database;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
public class DatabaseV4BuildRequest implements JsonEntity {

    @NotNull
    @ApiModelProperty(value = Database.DATABASE_REQUEST, required = true)
    private DatabaseV4Request rdsConfigRequest;

    @NotNull
    @ApiModelProperty(value = Database.DATABASE_REQUEST_CLUSTER_NAME, required = true)
    private String clusterName;

    public DatabaseV4Request getRdsConfigRequest() {
        return rdsConfigRequest;
    }

    public void setRdsConfigRequest(DatabaseV4Request rdsConfigRequest) {
        this.rdsConfigRequest = rdsConfigRequest;
    }

    public String getClusterName() {
        return clusterName;
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }
}
