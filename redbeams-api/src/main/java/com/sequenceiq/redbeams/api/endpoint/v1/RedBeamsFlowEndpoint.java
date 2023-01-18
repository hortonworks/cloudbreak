package com.sequenceiq.redbeams.api.endpoint.v1;

import javax.ws.rs.Consumes;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;
import com.sequenceiq.flow.api.FlowEndpoint;

import io.swagger.v3.oas.annotations.tags.Tag;

@Path("/flow")
@RetryAndMetrics
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "/flow")
public interface RedBeamsFlowEndpoint extends FlowEndpoint {
}
