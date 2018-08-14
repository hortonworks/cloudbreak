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

import com.sequenceiq.cloudbreak.api.model.rds.RDSConfigRequest;
import com.sequenceiq.cloudbreak.api.model.rds.RDSConfigResponse;
import com.sequenceiq.cloudbreak.doc.ContentType;
import com.sequenceiq.cloudbreak.doc.ControllerDescription;
import com.sequenceiq.cloudbreak.doc.Notes;
import com.sequenceiq.cloudbreak.doc.OperationDescriptions;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/v3/{organizationId}/rdsconfigs")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v3/{organizationId}/rdsconfigs", description = ControllerDescription.RDSCONFIGS_V3_DESCRIPTION, protocols = "http,https")
public interface RdsConfigV3Endpoint {

    @GET
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.RdsConfigOpDescription.LIST_BY_ORGANIZATION, produces = ContentType.JSON, notes = Notes.RDSCONFIG_NOTES,
            nickname = "listRdsConfigsByOrganization")
    Set<RDSConfigResponse> listByOrganization(@PathParam("organizationId") Long organizationId);

    @GET
    @Path("{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.RdsConfigOpDescription.GET_BY_NAME_IN_ORG, produces = ContentType.JSON, notes = Notes.RDSCONFIG_NOTES,
            nickname = "getRdsConfigInOrganization")
    RDSConfigResponse getByNameInOrganization(@PathParam("organizationId") Long organizationId, @PathParam("name") String name);

    @POST
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.RdsConfigOpDescription.CREATE_IN_ORG, produces = ContentType.JSON, notes = Notes.RDSCONFIG_NOTES,
            nickname = "createRdsConfigInOrganization")
    RDSConfigResponse createInOrganization(@PathParam("organizationId") Long organizationId, @Valid RDSConfigRequest request);

    @DELETE
    @Path("{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.RdsConfigOpDescription.DELETE_BY_NAME_IN_ORG, produces = ContentType.JSON, notes = Notes.RDSCONFIG_NOTES,
            nickname = "deleteRdsConfigInOrganization")
    RDSConfigResponse deleteInOrganization(@PathParam("organizationId") Long organizationId, @PathParam("name") String name);

}