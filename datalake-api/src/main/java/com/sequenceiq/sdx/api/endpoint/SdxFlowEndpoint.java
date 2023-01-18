package com.sequenceiq.sdx.api.endpoint;

import javax.ws.rs.Consumes;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;
import com.sequenceiq.flow.api.FlowEndpoint;

import io.swagger.v3.oas.annotations.tags.Tag;

@Path("/flow")
@RetryAndMetrics
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "/flow", description = "Operations on flow logs")
public interface SdxFlowEndpoint extends FlowEndpoint {
}
