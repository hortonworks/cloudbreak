package com.sequenceiq.distrox.api.v1.distrox.model.telemetry.logging;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.LoggingV4Base;

import io.swagger.annotations.ApiModel;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
public class LoggingV1Request extends LoggingV4Base {
}
