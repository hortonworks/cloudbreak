package com.sequenceiq.freeipa.api.v1.dns.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.annotations.ApiModel;

@ApiModel("AddDnsZoneForSubnetsV1Response")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AddDnsZoneForSubnetsResponse {

    private Set<String> success = new HashSet<>();

    private Map<String, String> failed = new HashMap<>();

    public Set<String> getSuccess() {
        return success;
    }

    public void setSuccess(Set<String> success) {
        this.success = success;
    }

    public Map<String, String> getFailed() {
        return failed;
    }

    public void setFailed(Map<String, String> failed) {
        this.failed = failed;
    }
}
