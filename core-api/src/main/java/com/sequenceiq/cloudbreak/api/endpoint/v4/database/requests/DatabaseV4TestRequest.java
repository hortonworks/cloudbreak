package com.sequenceiq.cloudbreak.api.endpoint.v4.database.requests;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.api.model.JsonEntity;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("RdsTestRequest")
@JsonIgnoreProperties(ignoreUnknown = true)
public class DatabaseV4TestRequest implements JsonEntity {

    @ApiModelProperty(ModelDescriptions.Database.NAME)
    private String name;

    @Valid
    @ApiModelProperty(ModelDescriptions.Database.DATABASE_REQUEST)
    private DatabaseV4Request rdsConfig;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public DatabaseV4Request getRdsConfig() {
        return rdsConfig;
    }

    public void setRdsConfig(DatabaseV4Request rdsConfig) {
        this.rdsConfig = rdsConfig;
    }
}
