package com.sequenceiq.cloudbreak.api.endpoint.v3;

import java.util.Set;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.api.model.rds.RDSConfigRequest;
import com.sequenceiq.cloudbreak.api.model.rds.RDSConfigResponse;
import com.sequenceiq.cloudbreak.api.model.rds.RDSTestRequest;
import com.sequenceiq.cloudbreak.api.model.rds.RdsTestResult;
import com.sequenceiq.cloudbreak.doc.ContentType;
import com.sequenceiq.cloudbreak.doc.ControllerDescription;
import com.sequenceiq.cloudbreak.doc.Notes;
import com.sequenceiq.cloudbreak.doc.OperationDescriptions.RdsConfigOpDescription;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/v3/{workspaceId}/rdsconfigs")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v3/{workspaceId}/rdsconfigs", description = ControllerDescription.RDSCONFIGS_V3_DESCRIPTION, protocols = "http,https")
public interface RdsConfigV3Endpoint {

    @GET
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = RdsConfigOpDescription.LIST_BY_WORKSPACE, produces = ContentType.JSON, notes = Notes.RDSCONFIG_NOTES,
            nickname = "listRdsConfigsByWorkspace")
    Set<RDSConfigResponse> listByWorkspace(@PathParam("workspaceId") Long workspaceId, @QueryParam("environment") String environment,
            @QueryParam("attachGlobal") Boolean attachGlobal);

    @POST
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = RdsConfigOpDescription.CREATE_IN_WORKSPACE, produces = ContentType.JSON, notes = Notes.RDSCONFIG_NOTES,
            nickname = "createRdsConfigInWorkspace")
    RDSConfigResponse createInWorkspace(@PathParam("workspaceId") Long workspaceId, @Valid RDSConfigRequest request);

    @GET
    @Path("{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = RdsConfigOpDescription.GET_BY_NAME_IN_WORKSPACE, produces = ContentType.JSON, notes = Notes.RDSCONFIG_NOTES,
            nickname = "getRdsConfigInWorkspace")
    RDSConfigResponse getByNameInWorkspace(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name);

    @DELETE
    @Path("{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = RdsConfigOpDescription.DELETE_BY_NAME_IN_WORKSPACE, produces = ContentType.JSON, notes = Notes.RDSCONFIG_NOTES,
            nickname = "deleteRdsConfigInWorkspace")
    RDSConfigResponse deleteInWorkspace(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name);

    @GET
    @Path("{name}/request")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = RdsConfigOpDescription.GET_REQUEST_IN_WORKSPACE, produces = ContentType.JSON, notes = Notes.RDSCONFIG_NOTES,
            nickname = "getRdsRequestFromNameInWorkspace")
    RDSConfigRequest getRequestFromName(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name);

    @POST
    @Path("testconnect")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = RdsConfigOpDescription.POST_CONNECTION_TEST, produces = ContentType.JSON, notes = Notes.RDSCONFIG_NOTES,
            nickname = "testRdsConnectionInWorkspace")
    RdsTestResult testRdsConnection(@PathParam("workspaceId") Long workspaceId, @Valid RDSTestRequest rdsTestRequest);

    @PUT
    @Path("{name}/attach")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = RdsConfigOpDescription.ATTACH_TO_ENVIRONMENTS, produces = ContentType.JSON, notes = Notes.RDSCONFIG_NOTES,
            nickname = "attachRdsResourceToEnvironments")
    RDSConfigResponse attachToEnvironments(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name, @NotEmpty Set<String> environmentNames);

    @PUT
    @Path("{name}/detach")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = RdsConfigOpDescription.DETACH_FROM_ENVIRONMENTS, produces = ContentType.JSON, notes = Notes.RDSCONFIG_NOTES,
            nickname = "detachRdsResourceFromEnvironments")
    RDSConfigResponse detachFromEnvironments(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name,
            @NotEmpty Set<String> environmentNames);
}
