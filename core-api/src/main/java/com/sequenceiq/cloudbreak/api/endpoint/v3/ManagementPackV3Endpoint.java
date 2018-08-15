package com.sequenceiq.cloudbreak.api.endpoint.v3;

import java.util.Set;

import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.api.model.mpack.ManagementPackRequest;
import com.sequenceiq.cloudbreak.api.model.mpack.ManagementPackResponse;
import com.sequenceiq.cloudbreak.doc.ContentType;
import com.sequenceiq.cloudbreak.doc.ControllerDescription;
import com.sequenceiq.cloudbreak.doc.Notes;
import com.sequenceiq.cloudbreak.doc.OperationDescriptions.ManagementPackOpDescription;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/v3/{organizationId}/mpacks")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v3/{organizationId}/mpacks", description = ControllerDescription.MANAGEMENT_PACK_V3_DESCRIPTION, protocols = "http,https")
public interface ManagementPackV3Endpoint {

    @GET
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ManagementPackOpDescription.LIST_BY_ORGANIZATION, produces = ContentType.JSON, notes = Notes.MANAGEMENT_PACK_NOTES,
            nickname = "listManagementPacksByOrganization")
    Set<ManagementPackResponse> listByOrganization(@PathParam("organizationId") Long organizationId);

    @GET
    @Path("/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ManagementPackOpDescription.GET_BY_NAME_IN_ORG, produces = ContentType.JSON, notes = Notes.MANAGEMENT_PACK_NOTES,
            nickname = "getManagementPackInOrganization")
    ManagementPackResponse getByNameInOrganization(@PathParam("organizationId") Long organizationId, @PathParam("name") String name);

    @POST
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ManagementPackOpDescription.CREATE_IN_ORG, produces = ContentType.JSON, notes = Notes.MANAGEMENT_PACK_NOTES,
            nickname = "createManagementPackInOrganization")
    ManagementPackResponse createInOrganization(@PathParam("organizationId") Long organizationId, @Valid ManagementPackRequest request);

    @DELETE
    @Path("/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ManagementPackOpDescription.DELETE_BY_NAME_IN_ORG, produces = ContentType.JSON, notes = Notes.MANAGEMENT_PACK_NOTES,
            nickname = "deleteManagementPackInOrganization")
    ManagementPackResponse deleteInOrganization(@PathParam("organizationId") Long organizationId, @PathParam("name") String name);
}
