package com.sequenceiq.cloudbreak.api.endpoint.v3;

import java.util.Set;

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

import com.sequenceiq.cloudbreak.api.model.KubernetesConfigRequest;
import com.sequenceiq.cloudbreak.api.model.KubernetesConfigResponse;
import com.sequenceiq.cloudbreak.doc.ContentType;
import com.sequenceiq.cloudbreak.doc.ControllerDescription;
import com.sequenceiq.cloudbreak.doc.Notes;
import com.sequenceiq.cloudbreak.doc.OperationDescriptions.KubernetesConfigOpDescription;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/v3/{workspaceId}/kubernetesconfigs")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v3/{workspaceId}/kubernetesconfigs", description = ControllerDescription.KUBERNETESCONFIGS_V3_DESCRIPTION, protocols = "http,https")
public interface KubernetesConfigV3Endpoint {

    @GET
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = KubernetesConfigOpDescription.LIST_BY_WORKSPACE, produces = ContentType.JSON, notes = Notes.KUBERNETESCONFIG_NOTES,
            nickname = "listKubernetesConfigsByWorkspace")
    Set<KubernetesConfigResponse> listByWorkspace(@PathParam("workspaceId") Long workspaceId, @QueryParam("environment") String environment,
            @QueryParam("attachGlobal") Boolean attachGlobal);

    @POST
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = KubernetesConfigOpDescription.CREATE_IN_WORKSPACE, produces = ContentType.JSON, notes = Notes.KUBERNETESCONFIG_NOTES,
            nickname = "createKubernetesConfigInWorkspace")
    KubernetesConfigResponse createInWorkspace(@PathParam("workspaceId") Long workspaceId, KubernetesConfigRequest request);

    @PUT
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = KubernetesConfigOpDescription.PUT_IN_WORKSPACE, produces = ContentType.JSON, notes = Notes.KUBERNETESCONFIG_NOTES,
            nickname = "putKubernetesConfigInWorkspace")
    KubernetesConfigResponse putInWorkspace(@PathParam("workspaceId") Long workspaceId, KubernetesConfigRequest request);

    @GET
    @Path("{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = KubernetesConfigOpDescription.GET_BY_NAME_IN_WORKSPACE, produces = ContentType.JSON, notes = Notes.KUBERNETESCONFIG_NOTES,
            nickname = "getKubernetesConfigInWorkspace")
    KubernetesConfigResponse getByNameInWorkspace(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name);

    @DELETE
    @Path("{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = KubernetesConfigOpDescription.DELETE_BY_NAME_IN_WORKSPACE, produces = ContentType.JSON, notes = Notes.KUBERNETESCONFIG_NOTES,
            nickname = "deleteKubernetesConfigInWorkspace")
    KubernetesConfigResponse deleteInWorkspace(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name);

    @PUT
    @Path("{name}/attach")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = KubernetesConfigOpDescription.ATTACH_TO_ENVIRONMENTS, produces = ContentType.JSON, notes = Notes.KUBERNETESCONFIG_NOTES,
            nickname = "attachKubernetesResourceToEnvironments")
    KubernetesConfigResponse attachToEnvironments(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name,
            @NotEmpty Set<String> environmentNames);

    @PUT
    @Path("{name}/detach")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = KubernetesConfigOpDescription.DETACH_FROM_ENVIRONMENTS, produces = ContentType.JSON, notes = Notes.KUBERNETESCONFIG_NOTES,
            nickname = "detachKubernetesResourceFromEnvironments")
    KubernetesConfigResponse detachFromEnvironments(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name,
            @NotEmpty Set<String> environmentNames);
}
