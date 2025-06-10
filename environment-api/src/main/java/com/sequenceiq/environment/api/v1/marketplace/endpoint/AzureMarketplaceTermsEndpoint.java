package com.sequenceiq.environment.api.v1.marketplace.endpoint;

import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;
import com.sequenceiq.environment.api.doc.marketplace.MarketplaceDescription;
import com.sequenceiq.environment.api.v1.marketplace.model.AzureMarketplaceTermsRequest;
import com.sequenceiq.environment.api.v1.marketplace.model.AzureMarketplaceTermsResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RetryAndMetrics
@Path("/v1/imageterms")
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "/v1/imageterms")
public interface AzureMarketplaceTermsEndpoint {

    @GET
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = MarketplaceDescription.GET, description = MarketplaceDescription.GET_NOTES, operationId = "getTermsSettingV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    AzureMarketplaceTermsResponse get();

    @GET
    @Path("{accountId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = MarketplaceDescription.GET, description = MarketplaceDescription.GET_NOTES, operationId = "getTermsSettingInAccountV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    AzureMarketplaceTermsResponse getInAccount(@PathParam("accountId") String accountId);

    @PUT
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = MarketplaceDescription.PUT, description = MarketplaceDescription.PUT_NOTES, operationId = "putTermsSettingV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    AzureMarketplaceTermsResponse put(@Valid AzureMarketplaceTermsRequest request);
}
