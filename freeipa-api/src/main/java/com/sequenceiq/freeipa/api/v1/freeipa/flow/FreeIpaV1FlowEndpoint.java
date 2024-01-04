package com.sequenceiq.freeipa.api.v1.freeipa.flow;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;
import com.sequenceiq.flow.api.FlowEndpoint;

import io.swagger.v3.oas.annotations.tags.Tag;

@Path("/flow")
@RetryAndMetrics
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "/flow", description = "Operations on flow logs")
public interface FreeIpaV1FlowEndpoint extends FlowEndpoint {
}
