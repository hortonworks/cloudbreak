package com.sequenceiq.cloudbreak.api.model;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
public class SecurityRulesResponse {

    @ApiModelProperty(ModelDescriptions.SecurityRulesModelDescription.CORE)
    private List<SecurityRuleResponse> core = new ArrayList<>();

    @ApiModelProperty(ModelDescriptions.SecurityRulesModelDescription.GATEWAY)
    private List<SecurityRuleResponse> gateway = new ArrayList<>();

    public List<SecurityRuleResponse> getCore() {
        return core;
    }

    public void setCore(List<SecurityRuleResponse> core) {
        this.core = core;
    }

    public List<SecurityRuleResponse> getGateway() {
        return gateway;
    }

    public void setGateway(List<SecurityRuleResponse> gateway) {
        this.gateway = gateway;
    }
}
