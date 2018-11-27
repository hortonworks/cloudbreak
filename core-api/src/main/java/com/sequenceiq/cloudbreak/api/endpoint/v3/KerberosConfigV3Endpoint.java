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

import com.sequenceiq.cloudbreak.api.model.KerberosResponse;
import com.sequenceiq.cloudbreak.api.model.kerberos.KerberosViewResponse;
import com.sequenceiq.cloudbreak.api.model.kerberos.KerberosRequest;
import com.sequenceiq.cloudbreak.doc.ContentType;
import com.sequenceiq.cloudbreak.doc.ControllerDescription;
import com.sequenceiq.cloudbreak.doc.Notes;
import com.sequenceiq.cloudbreak.doc.OperationDescriptions.KerberosOpDescription;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/v3/{workspaceId}/kerberos")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v3/{workspaceId}/kerberos", description = ControllerDescription.KERBEROS_CONFIG_V3_DESCRIPTION, protocols = "http,https")
public interface KerberosConfigV3Endpoint {

    @GET
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = KerberosOpDescription.LIST_BY_WORKSPACE, produces = ContentType.JSON, notes = Notes.KERBEROS_CONFIG_NOTES,
            nickname = "listKerberosConfigByWorkspace")
    Set<KerberosViewResponse> listByWorkspace(@PathParam("workspaceId") Long workspaceId, @QueryParam("environment") String environment,
            @QueryParam("attachGlobal") Boolean attachGlobal);

    @GET
    @Path("{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = KerberosOpDescription.GET_BY_NAME_IN_WORKSPACE, produces = ContentType.JSON, notes = Notes.KERBEROS_CONFIG_NOTES,
            nickname = "getKerberosConfigInWorkspace")
    KerberosResponse getByNameInWorkspace(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name);

    @POST
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = KerberosOpDescription.CREATE_IN_WORKSPACE, produces = ContentType.JSON, notes = Notes.KERBEROS_CONFIG_NOTES,
            nickname = "createKerberosConfigInWorkspace")
    KerberosResponse createInWorkspace(@PathParam("workspaceId") Long workspaceId, @Valid KerberosRequest request);

    @DELETE
    @Path("{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = KerberosOpDescription.DELETE_BY_NAME_IN_WORKSPACE, produces = ContentType.JSON,
            notes = Notes.KERBEROS_CONFIG_NOTES, nickname = "deleteKerberosConfigInWorkspace")
    KerberosResponse deleteInWorkspace(@PathParam("workspaceId") Long workspaceId,  @PathParam("name") String name);

    @PUT
    @Path("{name}/attach")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = KerberosOpDescription.ATTACH_TO_ENVIRONMENTS, produces = ContentType.JSON, notes = Notes.KERBEROS_CONFIG_NOTES,
            nickname = "attachKerberosConfigToEnvironments")
    KerberosResponse attachToEnvironments(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name, @NotEmpty Set<String> environmentNames);

    @PUT
    @Path("{name}/detach")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = KerberosOpDescription.DETACH_FROM_ENVIRONMENTS, produces = ContentType.JSON, notes = Notes.KERBEROS_CONFIG_NOTES,
            nickname = "detachKerberosConfigFromEnvironments")
    KerberosResponse detachFromEnvironments(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name,
            @NotEmpty Set<String> environmentNames);

    @GET
    @Path("{name}/request")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = KerberosOpDescription.GET_REQUEST, produces = ContentType.JSON, notes = Notes.KERBEROS_CONFIG_NOTES,
            nickname = "getKerberosRequestByNameAndWorkspaceId")
    KerberosRequest getRequestFromName(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name);

}
