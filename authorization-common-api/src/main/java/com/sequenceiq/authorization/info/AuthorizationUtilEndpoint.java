package com.sequenceiq.authorization.info;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.authorization.info.model.CheckResourceRightsV4Request;
import com.sequenceiq.authorization.info.model.CheckResourceRightsV4Response;
import com.sequenceiq.authorization.info.model.CheckRightOnResourcesV4Request;
import com.sequenceiq.authorization.info.model.CheckRightOnResourcesV4Response;
import com.sequenceiq.authorization.info.model.CheckRightV4Request;
import com.sequenceiq.authorization.info.model.CheckRightV4Response;
import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;
import com.sequenceiq.common.api.util.UtilControllerDescription;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Path("/v4/utils")
@RetryAndMetrics
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "/v4/utils", description = UtilControllerDescription.UTIL_DESCRIPTION)
public interface AuthorizationUtilEndpoint {

    @POST
    @Path("check_right")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary =  "Checking rights from UI in account", description =  "Check right from UI",
            operationId = "checkRightInAccount")
    CheckRightV4Response checkRightInAccount(CheckRightV4Request checkRightV4Request);

    @POST
    @Path("check_right_by_crn")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary =  "Checking rights from UI by resource CRN", description =  "Check right from UI",
            operationId = "checkRightByCrn")
    CheckResourceRightsV4Response checkRightByCrn(CheckResourceRightsV4Request checkRightByCrnV4Request);

    @POST
    @Path("check_right_on_resources")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary =  "Checking right from Uluwatu by resource CRNs", description =  "Check right from Uluwatu",
            operationId = "checkRightOnResources")
    CheckRightOnResourcesV4Response checkRightOnResources(CheckRightOnResourcesV4Request checkRightOnResourcesV4Request);

}
