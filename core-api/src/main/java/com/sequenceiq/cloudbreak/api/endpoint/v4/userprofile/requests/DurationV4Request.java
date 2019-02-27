package com.sequenceiq.cloudbreak.api.endpoint.v4.userprofile.requests;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.api.endpoint.v4.JsonEntity;

import io.swagger.annotations.ApiModel;

@JsonIgnoreProperties(ignoreUnknown = true)
@ApiModel
public class DurationV4Request extends DurationV4Base implements JsonEntity {
}
