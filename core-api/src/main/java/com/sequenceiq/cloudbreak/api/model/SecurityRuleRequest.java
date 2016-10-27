package com.sequenceiq.cloudbreak.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.swagger.annotations.ApiModel;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
public class SecurityRuleRequest extends SecurityRuleBase {

    public SecurityRuleRequest() {
    }

    public SecurityRuleRequest(String subnet) {
        super(subnet);
    }
}
