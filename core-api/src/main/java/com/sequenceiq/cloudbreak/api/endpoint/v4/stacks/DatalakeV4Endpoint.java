package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks;

import static com.sequenceiq.cloudbreak.doc.OperationDescriptions.StackOpDescription.LIST_BY_WORKSPACE;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Responses;
import com.sequenceiq.cloudbreak.doc.Notes;
import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RetryAndMetrics
@Path("/v4/datalake")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "/v4/datalake")
public interface DatalakeV4Endpoint {

    @GET
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = LIST_BY_WORKSPACE, description = Notes.STACK_NOTES, operationId = "listDatalakes",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    StackViewV4Responses list(@Deprecated @QueryParam("environment") String environment, @QueryParam("environmentCrn") String environmentCrn);
}
