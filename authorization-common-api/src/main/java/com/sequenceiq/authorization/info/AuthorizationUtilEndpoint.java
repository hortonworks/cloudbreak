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

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/v4/utils")
@RetryAndMetrics
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v4/utils", description = UtilControllerDescription.UTIL_DESCRIPTION, protocols = "http,https", consumes = MediaType.APPLICATION_JSON)
public interface AuthorizationUtilEndpoint {

    @POST
    @Path("check_right")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Checking rights from UI in account", produces = MediaType.APPLICATION_JSON, notes = "Check right from UI",
            nickname = "checkRightInAccount")
    CheckRightV4Response checkRightInAccount(CheckRightV4Request checkRightV4Request);

    @POST
    @Path("check_right_by_crn")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Checking rights from UI by resource CRN", produces = MediaType.APPLICATION_JSON, notes = "Check right from UI",
            nickname = "checkRightByCrn")
    CheckResourceRightsV4Response checkRightByCrn(CheckResourceRightsV4Request checkRightByCrnV4Request);

    @POST
    @Path("check_right_on_resources")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Checking right from Uluwatu by resource CRNs", produces = MediaType.APPLICATION_JSON, notes = "Check right from Uluwatu",
            nickname = "checkRightOnResources")
    CheckRightOnResourcesV4Response checkRightOnResources(CheckRightOnResourcesV4Request checkRightOnResourcesV4Request);

}
