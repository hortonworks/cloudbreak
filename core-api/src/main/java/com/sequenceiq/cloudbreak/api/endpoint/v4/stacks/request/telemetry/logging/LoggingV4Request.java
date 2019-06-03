package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.telemetry.logging;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.LoggingV4Base;

import io.swagger.annotations.ApiModel;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
public class LoggingV4Request extends LoggingV4Base {
}
