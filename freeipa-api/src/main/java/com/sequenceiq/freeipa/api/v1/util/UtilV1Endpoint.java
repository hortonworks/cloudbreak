package com.sequenceiq.freeipa.api.v1.util;

import java.util.List;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

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
    List<String> usedRecipes(@PathParam("accountId") String accountId);
}
