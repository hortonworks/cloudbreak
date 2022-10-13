package com.sequenceiq.sdx.api.endpoint;

import javax.ws.rs.Consumes;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;
import com.sequenceiq.flow.api.FlowEndpoint;

import io.swagger.annotations.Api;

@Path("/flow")
@RetryAndMetrics
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/flow", description = "Operations on flow logs", protocols = "http,https",
        consumes = MediaType.APPLICATION_JSON)
public interface SdxFlowEndpoint extends FlowEndpoint {
}
