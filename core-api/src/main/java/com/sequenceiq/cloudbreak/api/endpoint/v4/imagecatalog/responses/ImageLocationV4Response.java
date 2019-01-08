package com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses;

import java.util.Set;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.api.model.JsonEntity;

import io.swagger.annotations.ApiModel;

@ApiModel
@NotNull
@JsonIgnoreProperties(ignoreUnknown = true)
public class ImageLocationV4Response implements JsonEntity {

    @JsonProperty("cloudProvider")
    private String cloudProvider;

    @JsonProperty("regions")
    private Set<RegionAwareImageLocationV4Response> regions;

    public String getCloudProvider() {
        return cloudProvider;
    }

    public void setCloudProvider(String cloudProvider) {
        this.cloudProvider = cloudProvider;
    }

    public Set<RegionAwareImageLocationV4Response> getRegions() {
        return regions;
    }

    public void setRegions(Set<RegionAwareImageLocationV4Response> regions) {
        this.regions = regions;
    }
}
