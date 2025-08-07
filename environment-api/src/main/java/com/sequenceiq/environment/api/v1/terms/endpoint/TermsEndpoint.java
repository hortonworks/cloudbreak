package com.sequenceiq.environment.api.v1.terms.endpoint;

import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;
import com.sequenceiq.environment.api.doc.terms.TermsDescription;
import com.sequenceiq.environment.api.v1.terms.model.TermType;
import com.sequenceiq.environment.api.v1.terms.model.TermsRequest;
import com.sequenceiq.environment.api.v1.terms.model.TermsResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RetryAndMetrics
@Path("/v1/terms")
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "/v1/terms")
public interface TermsEndpoint {

    @GET
    @Path("type/{termType}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = TermsDescription.GET, description = TermsDescription.GET_NOTES, operationId = "getTermsSettingV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    TermsResponse get(@PathParam("termType") TermType termType);

    @GET
    @Path("account/{accountId}/type/{termType}/")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = TermsDescription.GET, description = TermsDescription.GET_NOTES, operationId = "getTermsSettingInAccountV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    TermsResponse getInAccount(@PathParam("accountId") String accountId, @PathParam("termType") TermType termType);

    @PUT
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = TermsDescription.PUT, description = TermsDescription.PUT_NOTES, operationId = "putTermsSettingV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    TermsResponse put(@Valid TermsRequest request);
}
