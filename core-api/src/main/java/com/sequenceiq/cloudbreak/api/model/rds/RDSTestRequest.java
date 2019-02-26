package com.sequenceiq.cloudbreak.api.model.rds;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.api.model.JsonEntity;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.RDSConfig;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("RdsTestRequest")
@JsonIgnoreProperties(ignoreUnknown = true)
public class RDSTestRequest implements JsonEntity {

    @ApiModelProperty(RDSConfig.NAME)
    private String name;

    @Valid
    @ApiModelProperty(RDSConfig.RDS_CONFIG_REQUEST)
    private RDSConfigRequest rdsConfig;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public RDSConfigRequest getRdsConfig() {
        return rdsConfig;
    }

    public void setRdsConfig(RDSConfigRequest rdsConfig) {
        this.rdsConfig = rdsConfig;
    }
}
