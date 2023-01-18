package com.sequenceiq.environment.api.v1.marketplace.endpoint;

import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.auth.security.internal.AccountId;
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
    AzureMarketplaceTermsResponse getInAccount(@AccountId @PathParam("accountId") String accountId);

    @PUT
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = MarketplaceDescription.PUT, description = MarketplaceDescription.PUT_NOTES, operationId = "putTermsSettingV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    AzureMarketplaceTermsResponse put(@Valid AzureMarketplaceTermsRequest request);
}
