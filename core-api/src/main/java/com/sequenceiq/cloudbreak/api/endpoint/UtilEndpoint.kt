package com.sequenceiq.cloudbreak.api.endpoint

import javax.validation.Valid
import javax.ws.rs.Consumes
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

import com.sequenceiq.cloudbreak.api.model.RDSConfigJson
import com.sequenceiq.cloudbreak.api.model.RdsTestResult
import com.sequenceiq.cloudbreak.doc.ContentType
import com.sequenceiq.cloudbreak.doc.ControllerDescription
import com.sequenceiq.cloudbreak.doc.OperationDescriptions

import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation

@Path("/util")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/util", description = ControllerDescription.UTIL_DESCRIPTION, position = 10)
interface UtilEndpoint {

    @POST
    @Path("rds")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.UtilityOpDescription.TEST_CONNECTION, produces = ContentType.JSON)
    fun testRdsConnection(@Valid rdsConfigJson: RDSConfigJson): RdsTestResult

}
