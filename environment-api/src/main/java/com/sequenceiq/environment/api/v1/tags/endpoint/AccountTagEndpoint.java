package com.sequenceiq.environment.api.v1.tags.endpoint;

import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;
import com.sequenceiq.environment.api.doc.tag.TagDescription;
import com.sequenceiq.environment.api.v1.tags.model.request.AccountTagRequests;
import com.sequenceiq.environment.api.v1.tags.model.response.AccountTagResponses;
import com.sequenceiq.environment.api.v1.tags.model.response.GeneratedAccountTagResponses;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RetryAndMetrics
@Path("/v1/tags")
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "/v1/tags")
public interface AccountTagEndpoint {

    @GET
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = TagDescription.GET, description = TagDescription.GET_NOTES, operationId = "listTagsV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    AccountTagResponses list();

    @GET
    @Path("{accountId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = TagDescription.GET, description = TagDescription.GET_NOTES, operationId = "listTagsInAccountV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    AccountTagResponses listInAccount(@PathParam("accountId") String accountId);

    @PUT
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = TagDescription.PUT, description = TagDescription.PUT_NOTES, operationId = "putTagsV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    AccountTagResponses put(@Valid AccountTagRequests request);

    @GET
    @Path("generate")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = TagDescription.GET_GENERATED, description = TagDescription.GET_GENERATED_NOTES,
            operationId = "getGeneratedTagsV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    GeneratedAccountTagResponses generate(@QueryParam("environmentName") String environmentName, @QueryParam("environmentCrn") String environmentCrn);
}
