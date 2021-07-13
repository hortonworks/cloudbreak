package com.sequenceiq.redbeams.api.endpoint.v4.operation;

import static com.sequenceiq.redbeams.doc.OperationDescriptions.OperationOpDescriptions.GET_OPERATIONS;
import static com.sequenceiq.redbeams.doc.OperationDescriptions.OperationOpDescriptions.NOTES;

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

@Path("/v4/operation")
@RetryAndMetrics
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v4/operation", description = "Get flow step progression", protocols = "http,https", consumes = MediaType.APPLICATION_JSON)
public interface OperationV4Endpoint {

    @GET
    @Path("/resource/crn/{resourceCrn}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = GET_OPERATIONS, produces = "application/json", notes = NOTES,
            nickname = "getRedbeamsOperationProgressByResourceCrn")
    OperationView getRedbeamsOperationProgressByResourceCrn(@PathParam("resourceCrn") String resourceCrn,
            @DefaultValue("false") @QueryParam("detailed") boolean detailed);
}
