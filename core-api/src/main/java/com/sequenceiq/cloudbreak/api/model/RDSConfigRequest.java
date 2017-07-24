package com.sequenceiq.cloudbreak.api.model;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("RDSConfig")
@JsonIgnoreProperties(ignoreUnknown = true)
public class RDSConfigRequest extends RDSConfigJson {

    @ApiModelProperty(value = ModelDescriptions.RDSConfig.NAME, required = true)
    private String name;

    @NotNull
    @ApiModelProperty(value = ModelDescriptions.RDSConfig.USERNAME, required = true)
    private String connectionUserName;

    @NotNull
    @ApiModelProperty(value = ModelDescriptions.RDSConfig.PASSWORD, required = true)
    private String connectionPassword;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getConnectionUserName() {
        return connectionUserName;
    }

    public void setConnectionUserName(String connectionUserName) {
        this.connectionUserName = connectionUserName;
    }

    public String getConnectionPassword() {
        return connectionPassword;
    }

    public void setConnectionPassword(String connectionPassword) {
        this.connectionPassword = connectionPassword;
    }
}
