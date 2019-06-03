package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.telemetry.workload;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.WorkloadAnalyticsV4Base;

import io.swagger.annotations.ApiModel;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
public class WorkloadAnalyticsV4Response extends WorkloadAnalyticsV4Base {
}
