package com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.providers.CloudPlatform;

import io.swagger.annotations.ApiModelProperty;

@JsonIgnoreProperties(ignoreUnknown = true, value = "provider")
public interface CredentialV4Parameters extends Mappable {

    @ApiModelProperty(hidden = true)
    CloudPlatform getCloudPlatform();

}
