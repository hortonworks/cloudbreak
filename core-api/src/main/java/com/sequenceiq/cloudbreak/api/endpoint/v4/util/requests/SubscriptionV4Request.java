package com.sequenceiq.cloudbreak.api.endpoint.v4.util.requests;


import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import com.sequenceiq.cloudbreak.doc.ModelDescriptions.SubscriptionModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class SubscriptionV4Request {

    static final String SIMPLE_URL_PATTERN = "^(https?:\\/\\/)((([\\da-z\\.-]+)\\.([a-z]{2,6}))|localhost|[1-9][0-9]{0,2}.[0-9]{1,3}.[0-9]{1,3}.[0-9]{1,3})"
            + "(:[1-9][0-9]{1,4})?\\/([\\/\\w\\.-]*)\\/?$";

    @NotNull
    @Pattern(regexp = SIMPLE_URL_PATTERN,
            message = "The notification hook URL must be proper and valid!")
    @ApiModelProperty(value = SubscriptionModelDescription.ENDPOINT, required = true)
    private String endpointUrl;

    public String getEndpointUrl() {
        return endpointUrl;
    }

    public void setEndpointUrl(String endpointUrl) {
        this.endpointUrl = endpointUrl;
    }
}
