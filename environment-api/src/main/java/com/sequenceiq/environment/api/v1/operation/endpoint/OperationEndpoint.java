package com.sequenceiq.environment.api.v1.operation.endpoint;

import static com.sequenceiq.environment.api.doc.environment.EnvironmentDescription.ENVIRONMENT_NOTES;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;
import com.sequenceiq.environment.api.doc.environment.EnvironmentOpDescription;
import com.sequenceiq.flow.api.model.operation.OperationView;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/v1/operation")
@RetryAndMetrics
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v1/operation", protocols = "http,https", consumes = MediaType.APPLICATION_JSON)
public interface OperationEndpoint {

    @GET
    @Path("/resource/crn/{resourceCrn}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = EnvironmentOpDescription.GET_OPERATION, produces = "application/json", notes = ENVIRONMENT_NOTES,
            nickname = "getOperationProgressByResourceCrn")
    OperationView getOperationProgressByResourceCrn(@PathParam("resourceCrn") String resourceCrn,
            @DefaultValue("false") @QueryParam("detailed") boolean detailed);
}
