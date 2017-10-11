package com.sequenceiq.cloudbreak.api.endpoint.v2;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.api.model.PlatformResourceRequestJson;
import com.sequenceiq.cloudbreak.api.model.PlatformVmtypesResponse;
import com.sequenceiq.cloudbreak.api.model.RegionResponse;
import com.sequenceiq.cloudbreak.doc.ContentType;
import com.sequenceiq.cloudbreak.doc.ControllerDescription;
import com.sequenceiq.cloudbreak.doc.Notes;
import com.sequenceiq.cloudbreak.doc.OperationDescriptions.ConnectorOpDescription;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/v2/connectors")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v2/connectors", description = ControllerDescription.CONNECTOR_DESCRIPTION, protocols = "http,https")
public interface ConnectorV2Endpoint {

    @POST
    @Path("vmtypes")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ConnectorOpDescription.GET_VMTYPES_BY_CREDENTIAL, produces = ContentType.JSON, notes = Notes.CONNECTOR_NOTES,
            nickname = "getVmTypesByCredentialId")
    PlatformVmtypesResponse getVmTypesByCredentialId(PlatformResourceRequestJson resourceRequestJson);

    @POST
    @Path("regions")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ConnectorOpDescription.GET_REGION_R_BY_TYPE, produces = ContentType.JSON, notes = Notes.CONNECTOR_NOTES,
            nickname = "getRegionsByCredentialId")
    RegionResponse getRegionsByCredentialId(PlatformResourceRequestJson resourceRequestJson);
}
