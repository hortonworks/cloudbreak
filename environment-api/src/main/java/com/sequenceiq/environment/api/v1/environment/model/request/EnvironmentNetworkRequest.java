package com.sequenceiq.environment.api.v1.environment.model.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.validation.MutuallyExclusiveNotNull;
import com.sequenceiq.environment.api.v1.environment.model.base.EnvironmentNetworkBase;

import io.swagger.annotations.ApiModel;

@ApiModel(value = "EnvironmentNetworkV1Request")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
//the yarn field aws added because of Ycloud and Qaas runs where they are sending the yarn queue
@MutuallyExclusiveNotNull(fieldGroups = {"networkCidr", "subnetIds", "yarn"},
        message = "The network CIDR and the Subnet Ids or none of them should not be defined in the same request.")
public class EnvironmentNetworkRequest extends EnvironmentNetworkBase {
}
