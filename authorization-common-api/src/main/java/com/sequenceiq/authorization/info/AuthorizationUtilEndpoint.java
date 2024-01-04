package com.sequenceiq.authorization.info;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import com.sequenceiq.authorization.info.model.CheckResourceRightsV4Request;
import com.sequenceiq.authorization.info.model.CheckResourceRightsV4Response;
import com.sequenceiq.authorization.info.model.CheckRightOnResourcesV4Request;
import com.sequenceiq.authorization.info.model.CheckRightOnResourcesV4Response;
import com.sequenceiq.authorization.info.model.CheckRightV4Request;
import com.sequenceiq.authorization.info.model.CheckRightV4Response;
import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;
import com.sequenceiq.common.api.util.UtilControllerDescription;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Path("/v4/utils")
@RetryAndMetrics
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "/v4/utils", description = UtilControllerDescription.UTIL_DESCRIPTION)
public interface AuthorizationUtilEndpoint {

    @POST
    @Path("check_right")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Checking rights from UI in account", description = "Check right from UI",
            operationId = "checkRightInAccount",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    CheckRightV4Response checkRightInAccount(CheckRightV4Request checkRightV4Request);

    @POST
    @Path("check_right_by_crn")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Checking rights from UI by resource CRN", description = "Check right from UI",
            operationId = "checkRightByCrn",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    CheckResourceRightsV4Response checkRightByCrn(CheckResourceRightsV4Request checkRightByCrnV4Request);

    @POST
    @Path("check_right_on_resources")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Checking right from Uluwatu by resource CRNs", description = "Check right from Uluwatu",
            operationId = "checkRightOnResources",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    CheckRightOnResourcesV4Response checkRightOnResources(CheckRightOnResourcesV4Request checkRightOnResourcesV4Request);

}
