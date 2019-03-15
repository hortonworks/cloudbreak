package com.sequenceiq.cloudbreak.api.endpoint.v4.kerberos;

import java.util.Set;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
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

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.EnvironmentNames;
import com.sequenceiq.cloudbreak.api.endpoint.v4.kerberos.requests.KerberosV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.kerberos.responses.KerberosV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.kerberos.responses.KerberosV4Responses;
import com.sequenceiq.cloudbreak.api.endpoint.v4.kerberos.responses.KerberosViewV4Responses;
import com.sequenceiq.cloudbreak.doc.ContentType;
import com.sequenceiq.cloudbreak.doc.ControllerDescription;
import com.sequenceiq.cloudbreak.doc.Notes;
import com.sequenceiq.cloudbreak.doc.OperationDescriptions.KerberosOpDescription;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/v4/{workspaceId}/kerberos")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v4/{workspaceId}/kerberos", description = ControllerDescription.KERBEROS_CONFIG_V4_DESCRIPTION, protocols = "http,https")
public interface KerberosConfigV4Endpoint {

    @GET
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = KerberosOpDescription.LIST_BY_WORKSPACE, produces = ContentType.JSON, notes = Notes.KERBEROS_CONFIG_NOTES,
            nickname = "listKerberosConfigByWorkspace")
    KerberosViewV4Responses list(@PathParam("workspaceId") Long workspaceId, @QueryParam("environment") String environment,
            @QueryParam("attachGlobal") Boolean attachGlobal);

    @GET
    @Path("{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = KerberosOpDescription.GET_BY_NAME_IN_WORKSPACE, produces = ContentType.JSON, notes = Notes.KERBEROS_CONFIG_NOTES,
            nickname = "getKerberosConfigInWorkspace")
    KerberosV4Response get(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name);

    @POST
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = KerberosOpDescription.CREATE_IN_WORKSPACE, produces = ContentType.JSON, notes = Notes.KERBEROS_CONFIG_NOTES,
            nickname = "createKerberosConfigInWorkspace")
    KerberosV4Response create(@PathParam("workspaceId") Long workspaceId, @Valid KerberosV4Request request);

    @DELETE
    @Path("{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = KerberosOpDescription.DELETE_BY_NAME_IN_WORKSPACE, produces = ContentType.JSON,
            notes = Notes.KERBEROS_CONFIG_NOTES, nickname = "deleteKerberosConfigInWorkspace")
    KerberosV4Response delete(@PathParam("workspaceId") Long workspaceId,  @PathParam("name") String name);

    @DELETE
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = KerberosOpDescription.DELETE_MULTIPLE_BY_NAME_IN_WORKSPACE, produces = ContentType.JSON,
            notes = Notes.KERBEROS_CONFIG_NOTES, nickname = "deleteKerberosConfigsInWorkspace")
    KerberosV4Responses deleteMultiple(@PathParam("workspaceId") Long workspaceId, Set<String> names);

    @PUT
    @Path("{name}/attach")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = KerberosOpDescription.ATTACH_TO_ENVIRONMENTS, produces = ContentType.JSON, notes = Notes.KERBEROS_CONFIG_NOTES,
            nickname = "attachKerberosConfigToEnvironments")
    KerberosV4Response attach(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name, @Valid @NotNull EnvironmentNames environmentNames);

    @PUT
    @Path("{name}/detach")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = KerberosOpDescription.DETACH_FROM_ENVIRONMENTS, produces = ContentType.JSON, notes = Notes.KERBEROS_CONFIG_NOTES,
            nickname = "detachKerberosConfigFromEnvironments")
    KerberosV4Response detach(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name,
            @Valid @NotNull EnvironmentNames environmentNames);

    @GET
    @Path("{name}/request")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = KerberosOpDescription.GET_REQUEST, produces = ContentType.JSON, notes = Notes.KERBEROS_CONFIG_NOTES,
            nickname = "getKerberosRequestByNameAndWorkspaceId")
    KerberosV4Request getRequest(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name);

}
