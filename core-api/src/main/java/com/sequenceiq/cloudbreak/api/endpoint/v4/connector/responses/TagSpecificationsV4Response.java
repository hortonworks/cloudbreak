package com.sequenceiq.cloudbreak.api.endpoint.v4.connector.responses;

import java.util.Map;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.ConnectorModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@NotNull
@JsonIgnoreProperties(ignoreUnknown = true)
public class TagSpecificationsV4Response {

    @ApiModelProperty(ConnectorModelDescription.TAG_SPECIFICATIONS)
    private Map<String, Map<String, Object>> specifications;

    public Map<String, Map<String, Object>> getSpecifications() {
        return specifications;
    }

    public void setSpecifications(Map<String, Map<String, Object>> specifications) {
        this.specifications = specifications;
    }

}
