package com.sequenceiq.cloudbreak.api.endpoint.v4.blueprint;

import static com.sequenceiq.cloudbreak.doc.ContentType.JSON;

import java.util.Set;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprint.responses.GeneratedCmTemplateV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprint.responses.ServiceDependencyMatrixV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprint.responses.SupportedVersionsV4Response;
import com.sequenceiq.cloudbreak.doc.ControllerDescription;
import com.sequenceiq.cloudbreak.doc.Notes;
import com.sequenceiq.cloudbreak.doc.OperationDescriptions.ConnectorOpDescription;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/v4/{workspaceId}/blueprints-util")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v4/{workspaceId}/blueprints-util", description = ControllerDescription.BLUEPRINT_V4_DESCRIPTION, protocols = "http,https")
public interface BlueprintUtilV4Endpoint {

    @GET
    @Path("service_dependencies")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ConnectorOpDescription.GET_RECOMMENDATION, produces = JSON, notes = Notes.CONNECTOR_NOTES,
            nickname = "getServiceAndDependencies")
    ServiceDependencyMatrixV4Response getServiceAndDependencies(@PathParam("workspaceId") Long workspaceId,
        @QueryParam("services") Set<String> services, @QueryParam("platform") String platform);

    @GET
    @Path("services")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ConnectorOpDescription.GET_RECOMMENDATION, produces = JSON, notes = Notes.CONNECTOR_NOTES,
            nickname = "getServiceList")
    SupportedVersionsV4Response getServiceList(@PathParam("workspaceId") Long workspaceId);

    @GET
    @Path("generate")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ConnectorOpDescription.GET_RECOMMENDATION, produces = JSON, notes = Notes.CONNECTOR_NOTES,
            nickname = "getGeneratedTemplate")
    GeneratedCmTemplateV4Response getGeneratedTemplate(@PathParam("workspaceId") Long workspaceId,
        @QueryParam("services") Set<String> services, @QueryParam("platform") String platform);

}
