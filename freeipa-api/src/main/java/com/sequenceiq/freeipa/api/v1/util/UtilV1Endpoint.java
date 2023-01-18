package com.sequenceiq.freeipa.api.v1.util;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.auth.security.internal.AccountId;
import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;
import com.sequenceiq.freeipa.api.v1.util.doc.UtilDescriptions;
import com.sequenceiq.freeipa.api.v1.util.model.UsedImagesListV1Response;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RetryAndMetrics
@Path("/v1/utils")
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "/v1/utils", description = UtilDescriptions.NOTES)
public interface UtilV1Endpoint {

    @GET
    @Path("used_images")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = UtilDescriptions.USED_IMAGES, operationId = "usedImagesV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    UsedImagesListV1Response usedImages(@QueryParam("thresholdInDays") Integer thresholdInDays);

    @GET
    @Path("used_recipes/{accountId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = UtilDescriptions.USED_IMAGES, operationId = "usedRecipesV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    List<String> usedRecipes(@AccountId @PathParam("accountId") String accountId);
}
