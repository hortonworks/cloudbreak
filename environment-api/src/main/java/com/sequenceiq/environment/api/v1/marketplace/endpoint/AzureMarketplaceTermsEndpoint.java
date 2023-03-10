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
import com.sequenceiq.environment.api.doc.tag.TagDescription;
import com.sequenceiq.environment.api.v1.marketplace.model.AzureMarketplaceTermsRequest;
import com.sequenceiq.environment.api.v1.marketplace.model.AzureMarketplaceTermsResponse;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@RetryAndMetrics
@Path("/v1/imageterms")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v1/imageterms", protocols = "http,https", consumes = MediaType.APPLICATION_JSON)
public interface AzureMarketplaceTermsEndpoint {

    @GET
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = MarketplaceDescription.GET, produces = MediaType.APPLICATION_JSON, notes = MarketplaceDescription.GET_NOTES,
            nickname = "getTermsSettingV1")
    AzureMarketplaceTermsResponse get();

    @GET
    @Path("{accountId}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = TagDescription.GET, produces = MediaType.APPLICATION_JSON, notes = TagDescription.GET_NOTES, nickname = "getTermsSettingInAccountV1")
    AzureMarketplaceTermsResponse getInAccount(@AccountId @PathParam("accountId") String accountId);

    @PUT
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = MarketplaceDescription.PUT, produces = MediaType.APPLICATION_JSON, notes = MarketplaceDescription.PUT_NOTES,
            nickname = "putTermsSettingV1")
    AzureMarketplaceTermsResponse put(@Valid AzureMarketplaceTermsRequest request);
}
