package com.sequenceiq.cloudbreak.api.endpoint.v4.database.requests;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.Database;
import com.sequenceiq.common.model.JsonEntity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
public class DatabaseV4BuildRequest implements JsonEntity {

    @NotNull
    @Schema(description = Database.DATABASE_REQUEST, required = true)
    private DatabaseV4Request rdsConfigRequest;

    @NotNull
    @Schema(description = Database.DATABASE_REQUEST_CLUSTER_NAME, required = true)
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
