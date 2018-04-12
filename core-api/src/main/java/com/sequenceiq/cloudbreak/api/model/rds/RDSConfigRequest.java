package com.sequenceiq.cloudbreak.api.model.rds;

import java.util.HashMap;
import java.util.Map;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.RDSConfig;
import com.sequenceiq.cloudbreak.validation.externaldatabase.ValidRds;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ValidRds
@ApiModel("RdsConfig")
@JsonIgnoreProperties(ignoreUnknown = true)
public class RDSConfigRequest extends RDSConfigJson {

    @NotNull
    @ApiModelProperty(value = RDSConfig.USERNAME, required = true)
    private String connectionUserName;

    @NotNull
    @ApiModelProperty(value = RDSConfig.PASSWORD, required = true)
    private String connectionPassword;

    @ApiModelProperty(value = RDSConfig.ORACLE)
    private Map<String, Object> oracleProperties = new HashMap<>();

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

    public Map<String, Object> getOracleProperties() {
        return oracleProperties;
    }

    public void setOracleProperties(Map<String, Object> oracleProperties) {
        this.oracleProperties = oracleProperties;
    }
}
