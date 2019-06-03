package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.telemetry.logging;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.LoggingV4Base;

import io.swagger.annotations.ApiModel;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
public class LoggingV4Response extends LoggingV4Base {
}
