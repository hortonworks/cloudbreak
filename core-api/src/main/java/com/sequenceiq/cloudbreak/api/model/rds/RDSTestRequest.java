package com.sequenceiq.cloudbreak.api.model.rds;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.api.model.JsonEntity;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("RdsTestRequest")
@JsonIgnoreProperties(ignoreUnknown = true)
public class RDSTestRequest implements JsonEntity {

    @ApiModelProperty(ModelDescriptions.RDSConfig.RDS_CONFIG_ID)
    private Long id;

    @Valid
    @ApiModelProperty(ModelDescriptions.RDSConfig.RDS_CONFIG_REQUEST)
    private RDSConfigRequest rdsConfig;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public RDSConfigRequest getRdsConfig() {
        return rdsConfig;
    }

    public void setRdsConfig(RDSConfigRequest rdsConfig) {
        this.rdsConfig = rdsConfig;
    }
}
