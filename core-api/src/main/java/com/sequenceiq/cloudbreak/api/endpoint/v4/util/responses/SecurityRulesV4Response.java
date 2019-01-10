package com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.SecurityRulesModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
public class SecurityRulesV4Response {

    @ApiModelProperty(SecurityRulesModelDescription.CORE)
    private List<SecurityRuleV4Response> core = new ArrayList<>();

    @ApiModelProperty(SecurityRulesModelDescription.GATEWAY)
    private List<SecurityRuleV4Response> gateway = new ArrayList<>();

    public List<SecurityRuleV4Response> getCore() {
        return core;
    }

    public void setCore(List<SecurityRuleV4Response> core) {
        this.core = core;
    }

    public List<SecurityRuleV4Response> getGateway() {
        return gateway;
    }

    public void setGateway(List<SecurityRuleV4Response> gateway) {
        this.gateway = gateway;
    }
}
