package com.sequenceiq.cloudbreak.api.endpoint

import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

import com.sequenceiq.cloudbreak.doc.ContentType
import com.sequenceiq.cloudbreak.doc.ControllerDescription
import com.sequenceiq.cloudbreak.doc.Notes
import com.sequenceiq.cloudbreak.doc.OperationDescriptions
import com.sequenceiq.cloudbreak.api.model.CloudbreakUsageJson

import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation

@Path("/usages")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/usages", description = ControllerDescription.USAGES_DESCRIPTION, position = 6)
interface UsageEndpoint {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.UsagesOpDescription.GET_ALL, produces = ContentType.JSON, notes = Notes.USAGE_NOTES)
    fun getDeployer(
            @QueryParam("since") since: Long?,
            @QueryParam("filterenddate") filterEndDate: Long?,
            @QueryParam("user") userId: String,
            @QueryParam("account") accountId: String,
            @QueryParam("cloud") cloud: String,
            @QueryParam("zone") zone: String): List<CloudbreakUsageJson>

    @GET
    @Path("account")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.UsagesOpDescription.GET_PUBLIC, produces = ContentType.JSON, notes = Notes.USAGE_NOTES)
    fun getAccount(
            @QueryParam("since") since: Long?,
            @QueryParam("filterenddate") filterEndDate: Long?,
            @QueryParam("user") userId: String,
            @QueryParam("cloud") cloud: String,
            @QueryParam("zone") zone: String): List<CloudbreakUsageJson>

    @GET
    @Path("user")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.UsagesOpDescription.GET_PRIVATE, produces = ContentType.JSON, notes = Notes.USAGE_NOTES)
    fun getUser(
            @QueryParam("since") since: Long?,
            @QueryParam("filterenddate") filterEndDate: Long?,
            @QueryParam("cloud") cloud: String,
            @QueryParam("zone") zone: String): List<CloudbreakUsageJson>

    @GET
    @Path("generate")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.UsagesOpDescription.GENERATE, produces = ContentType.JSON, notes = Notes.USAGE_NOTES)
    fun generate(): Response
}
