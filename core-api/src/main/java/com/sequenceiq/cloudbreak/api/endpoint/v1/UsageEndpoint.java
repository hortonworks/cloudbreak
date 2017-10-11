package com.sequenceiq.cloudbreak.api.endpoint.v1;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.api.model.CloudbreakUsageJson;
import com.sequenceiq.cloudbreak.api.model.flex.CloudbreakFlexUsageJson;
import com.sequenceiq.cloudbreak.doc.ContentType;
import com.sequenceiq.cloudbreak.doc.ControllerDescription;
import com.sequenceiq.cloudbreak.doc.Notes;
import com.sequenceiq.cloudbreak.doc.OperationDescriptions.UsagesOpDescription;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/v1/usages")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v1/usages", description = ControllerDescription.USAGES_DESCRIPTION, protocols = "http,https")
public interface UsageEndpoint {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = UsagesOpDescription.GET_ALL, produces = ContentType.JSON, notes = Notes.USAGE_NOTES,
            nickname = "getDeployerUsage")
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
    @ApiOperation(value = UsagesOpDescription.GET_PUBLIC, produces = ContentType.JSON, notes = Notes.USAGE_NOTES,
            nickname = "getAccountUsage")
    List<CloudbreakUsageJson> getAccount(
            @QueryParam("since") Long since,
            @QueryParam("filterenddate") Long filterEndDate,
            @QueryParam("user") String userId,
            @QueryParam("cloud") String cloud,
            @QueryParam("zone") String zone);

    @GET
    @Path("user")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = UsagesOpDescription.GET_PRIVATE, produces = ContentType.JSON, notes = Notes.USAGE_NOTES,
            nickname = "getUserUsage")
    List<CloudbreakUsageJson> getUser(
            @QueryParam("since") Long since,
            @QueryParam("filterenddate") Long filterEndDate,
            @QueryParam("cloud") String cloud,
            @QueryParam("zone") String zone);

    @GET
    @Path("flex/daily")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = UsagesOpDescription.GET_FLEX_DAILY, produces = ContentType.JSON, notes = Notes.USAGE_NOTES,
            nickname = "getDailyFlexUsage")
    CloudbreakFlexUsageJson getDailyFlexUsages();

    @GET
    @Path("flex/latest")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = UsagesOpDescription.GET_FLEX_LATEST, produces = ContentType.JSON, notes = Notes.USAGE_NOTES,
            nickname = "getLatestFlexUsage")
    CloudbreakFlexUsageJson getLatestFlexUsages();

}
