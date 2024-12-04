package com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.SecurityRulesModelDescription;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
public class SecurityRulesV4Response {

    @Schema(description = SecurityRulesModelDescription.CORE, requiredMode = Schema.RequiredMode.REQUIRED)
    private List<SecurityRuleV4Response> core = new ArrayList<>();

    @Schema(description = SecurityRulesModelDescription.GATEWAY, requiredMode = Schema.RequiredMode.REQUIRED)
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
