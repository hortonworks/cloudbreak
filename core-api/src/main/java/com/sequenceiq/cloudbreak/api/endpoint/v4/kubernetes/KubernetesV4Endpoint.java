package com.sequenceiq.cloudbreak.api.endpoint.v4.kubernetes;

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
import com.sequenceiq.cloudbreak.api.endpoint.v4.kubernetes.requests.KubernetesV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.kubernetes.responses.KubernetesV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.kubernetes.responses.KubernetesV4Responses;
import com.sequenceiq.cloudbreak.doc.ContentType;
import com.sequenceiq.cloudbreak.doc.ControllerDescription;
import com.sequenceiq.cloudbreak.doc.Notes;
import com.sequenceiq.cloudbreak.doc.OperationDescriptions.KubernetesConfigOpDescription;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/v4/{workspaceId}/kubernetes")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v4/{workspaceId}/kubernetes", description = ControllerDescription.KUBERNETESCONFIGS_V4_DESCRIPTION, protocols = "http,https")
public interface KubernetesV4Endpoint {

    @GET
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = KubernetesConfigOpDescription.LIST_BY_WORKSPACE, produces = ContentType.JSON, notes = Notes.KUBERNETESCONFIG_NOTES,
            nickname = "listKubernetesConfigsByWorkspace")
    KubernetesV4Responses list(@PathParam("workspaceId") Long workspaceId, @QueryParam("environment") String environment,
            @QueryParam("attachGlobal") Boolean attachGlobal);

    @POST
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = KubernetesConfigOpDescription.CREATE_IN_WORKSPACE, produces = ContentType.JSON, notes = Notes.KUBERNETESCONFIG_NOTES,
            nickname = "createKubernetesConfigInWorkspace")
    KubernetesV4Response post(@PathParam("workspaceId") Long workspaceId, @Valid KubernetesV4Request request);

    @PUT
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = KubernetesConfigOpDescription.PUT_IN_WORKSPACE, produces = ContentType.JSON, notes = Notes.KUBERNETESCONFIG_NOTES,
            nickname = "putKubernetesConfigInWorkspace")
    KubernetesV4Response put(@PathParam("workspaceId") Long workspaceId, @Valid KubernetesV4Request request);

    @GET
    @Path("{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = KubernetesConfigOpDescription.GET_BY_NAME_IN_WORKSPACE, produces = ContentType.JSON, notes = Notes.KUBERNETESCONFIG_NOTES,
            nickname = "getKubernetesConfigInWorkspace")
    KubernetesV4Response get(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name);

    @DELETE
    @Path("{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = KubernetesConfigOpDescription.DELETE_BY_NAME_IN_WORKSPACE, produces = ContentType.JSON, notes = Notes.KUBERNETESCONFIG_NOTES,
            nickname = "deleteKubernetesConfigInWorkspace")
    KubernetesV4Response delete(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name);

    @DELETE
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = KubernetesConfigOpDescription.DELETE_MULTIPLE_BY_NAME_IN_WORKSPACE, produces = ContentType.JSON, notes = Notes.KUBERNETESCONFIG_NOTES,
            nickname = "deleteKubernetesConfigsInWorkspace")
    KubernetesV4Responses deleteMultiple(@PathParam("workspaceId") Long workspaceId, Set<String> names);

    @PUT
    @Path("{name}/attach")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = KubernetesConfigOpDescription.ATTACH_TO_ENVIRONMENTS, produces = ContentType.JSON, notes = Notes.KUBERNETESCONFIG_NOTES,
            nickname = "attachKubernetesResourceToEnvironments")
    KubernetesV4Response attach(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name,
        @Valid @NotNull EnvironmentNames environmentNames);

    @PUT
    @Path("{name}/detach")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = KubernetesConfigOpDescription.DETACH_FROM_ENVIRONMENTS, produces = ContentType.JSON, notes = Notes.KUBERNETESCONFIG_NOTES,
            nickname = "detachKubernetesResourceFromEnvironments")
    KubernetesV4Response detach(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name,
        @Valid @NotNull EnvironmentNames environmentNames);
}
