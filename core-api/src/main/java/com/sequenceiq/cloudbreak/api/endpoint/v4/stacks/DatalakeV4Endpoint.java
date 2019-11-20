package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks;

import static com.sequenceiq.cloudbreak.doc.OperationDescriptions.StackOpDescription.LIST_BY_WORKSPACE;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Responses;
import com.sequenceiq.cloudbreak.doc.ContentType;
import com.sequenceiq.cloudbreak.doc.Notes;
import com.sequenceiq.cloudbreak.jerseyclient.retry.RetryingRestClient;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@RetryingRestClient
@Path("/v4/datalake")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Api(value = "/v4/datalake", protocols = "http,https")
public interface DatalakeV4Endpoint {

    @GET
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = LIST_BY_WORKSPACE, produces = ContentType.JSON, notes = Notes.STACK_NOTES, nickname = "listDatalakes")
    StackViewV4Responses list(@QueryParam("environment") String environmentName);

}
