package com.sequenceiq.cloudbreak.api.endpoint;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.sequenceiq.cloudbreak.api.model.CloudbreakUsageJson;
import com.sequenceiq.cloudbreak.doc.ContentType;
import com.sequenceiq.cloudbreak.doc.ControllerDescription;
import com.sequenceiq.cloudbreak.doc.Notes;
import com.sequenceiq.cloudbreak.doc.OperationDescriptions;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/usages")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/usages", description = ControllerDescription.USAGES_DESCRIPTION, position = 6)
public interface UsageEndpoint {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.UsagesOpDescription.GET_ALL, produces = ContentType.JSON, notes = Notes.USAGE_NOTES)
    List<CloudbreakUsageJson> getDeployer(
            @QueryParam("since") Long since,
            @QueryParam("filterenddate") Long filterEndDate,
            @QueryParam("user") String userId,
            @QueryParam("account") String accountId,
            @QueryParam("cloud") String cloud,
            @QueryParam("zone") String zone);

    @GET
    @Path("account")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.UsagesOpDescription.GET_PUBLIC, produces = ContentType.JSON, notes = Notes.USAGE_NOTES)
    List<CloudbreakUsageJson> getAccount(
            @QueryParam("since") Long since,
            @QueryParam("filterenddate") Long filterEndDate,
            @QueryParam("user") String userId,
            @QueryParam("cloud") String cloud,
            @QueryParam("zone") String zone);

    @GET
    @Path("user")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.UsagesOpDescription.GET_PRIVATE, produces = ContentType.JSON, notes = Notes.USAGE_NOTES)
    List<CloudbreakUsageJson> getUser(
            @QueryParam("since") Long since,
            @QueryParam("filterenddate") Long filterEndDate,
            @QueryParam("cloud") String cloud,
            @QueryParam("zone") String zone);

    @GET
    @Path("generate")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.UsagesOpDescription.GENERATE, produces = ContentType.JSON, notes = Notes.USAGE_NOTES)
    Response generate();
}
