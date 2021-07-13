package com.sequenceiq.sdx.api.endpoint;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;
import com.sequenceiq.flow.api.model.operation.OperationView;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/operation")
@RetryAndMetrics
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/operation", description = "Get progression details of (flow) operations on SDX cluster",
        protocols = "http,https", consumes = MediaType.APPLICATION_JSON)
public interface OperationEndpoint {

    @GET
    @Path("/resource/crn/{resourceCrn}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get provision flow operation progress details for resource by resource crn", produces = "application/json",
            notes = "Flow operations progress", nickname = "getOperationProgressByResourceCrn")
    OperationView getOperationProgressByResourceCrn(@PathParam("resourceCrn") String resourceCrn,
            @DefaultValue("false") @QueryParam("detailed") boolean detailed);
}
