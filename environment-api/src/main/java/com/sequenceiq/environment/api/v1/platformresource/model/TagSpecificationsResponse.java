package com.sequenceiq.environment.api.v1.platformresource.model;

import java.util.Map;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.environment.api.v1.platformresource.PlatformResourceModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@NotNull
@JsonIgnoreProperties(ignoreUnknown = true)
public class TagSpecificationsResponse {

    @ApiModelProperty(PlatformResourceModelDescription.TAG_SPECIFICATIONS)
    private Map<String, Map<String, Object>> specifications;

    public Map<String, Map<String, Object>> getSpecifications() {
        return specifications;
    }

    public void setSpecifications(Map<String, Map<String, Object>> specifications) {
        this.specifications = specifications;
    }

    @Override
    public String toString() {
        return "TagSpecificationsResponse{" +
                "specifications=" + specifications +
                '}';
    }
}
