package com.sequenceiq.environment.api.v1.environment.model.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.validation.MutuallyExclusiveNotNull;
import com.sequenceiq.environment.api.v1.environment.model.base.EnvironmentNetworkBase;
import com.sequenceiq.environment.api.v1.environment.validator.ValidOutboundInternetTrafficNetworkRequest;

import io.swagger.annotations.ApiModel;

@ApiModel(value = "EnvironmentNetworkV1Request")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
// The yarn field was added because of Ycloud and Qaas runs where they are sending the yarn queue.
// The message can not contains it because then we are exposing an error to customer which is not a customer facing feature.
@MutuallyExclusiveNotNull(fieldGroups = {"networkCidr", "subnetIds", "yarn"},
        message = "The network CIDR and the Subnet Ids or none of them should not be defined in the same request.")
@ValidOutboundInternetTrafficNetworkRequest
public class EnvironmentNetworkRequest extends EnvironmentNetworkBase {
    @Override
    public String toString() {
        return super.toString() + ", " + "EnvironmentNetworkRequest{}";
    }
}
