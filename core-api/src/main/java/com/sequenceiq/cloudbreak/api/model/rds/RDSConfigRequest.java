package com.sequenceiq.cloudbreak.api.model.rds;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.RDSConfig;
import com.sequenceiq.cloudbreak.validation.externaldatabase.ValidRds;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ValidRds
@ApiModel("RdsConfig")
@JsonIgnoreProperties(ignoreUnknown = true)
public class RDSConfigRequest extends RDSConfigJson {

    @ApiModelProperty(RDSConfig.ORACLE)
    private OracleParameters oracle;

    public OracleParameters getOracle() {
        return oracle;
    }

    public void setOracle(OracleParameters oracle) {
        this.oracle = oracle;
    }
}
