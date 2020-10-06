package com.sequenceiq.distrox.api.v1.distrox.model.cluster;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.annotations.ApiModel;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DistroXMultiDeleteV1Request {

    private Set<String> names = new HashSet<>();

    private Set<String> crns = new HashSet<>();

    public Set<String> getNames() {
        return names;
    }

    public void setNames(Set<String> names) {
        if (names != null) {
            this.names = names;
        }
    }

    public Set<String> getCrns() {
        return crns;
    }

    public void setCrns(Set<String> crns) {
        if (crns != null) {
            this.crns = crns;
        }
    }
}
