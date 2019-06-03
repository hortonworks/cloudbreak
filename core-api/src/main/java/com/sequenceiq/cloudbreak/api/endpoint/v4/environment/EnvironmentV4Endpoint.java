package com.sequenceiq.cloudbreak.api.endpoint.v4.environment;

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
import javax.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.api.endpoint.v4.environment.requests.DatalakePrerequisiteV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.environment.requests.EnvironmentAttachV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.environment.requests.EnvironmentDetachV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.environment.requests.RegisterDatalakeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.environment.responses.DatalakePrerequisiteV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.environment.responses.DetailedEnvironmentV4Response;
import com.sequenceiq.cloudbreak.doc.ContentType;
import com.sequenceiq.cloudbreak.doc.ControllerDescription;
import com.sequenceiq.cloudbreak.doc.Notes;
import com.sequenceiq.cloudbreak.doc.OperationDescriptions.EnvironmentOpDescription;
import com.sequenceiq.cloudbreak.doc.OperationDescriptions.UtilityOpDescription;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentEditRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentRequest;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.SimpleEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.SimpleEnvironmentResponses;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/v4/{workspaceId}/environments")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v4/{workspaceId}/environments", description = ControllerDescription.ENVIRONMENT_V4_DESCRIPTION, protocols = "http,https")
public interface EnvironmentV4Endpoint {

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = EnvironmentOpDescription.CREATE, produces = ContentType.JSON, notes = Notes.ENVIRONMENT_NOTES,
            nickname = "createEnvironment")
    DetailedEnvironmentResponse post(@PathParam("workspaceId") Long workspaceId, @Valid EnvironmentRequest request);

    @GET
    @Path("/{crn}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = EnvironmentOpDescription.GET, produces = ContentType.JSON, notes = Notes.ENVIRONMENT_NOTES,
            nickname = "getEnvironment")
    DetailedEnvironmentResponse get(@PathParam("workspaceId") Long workspaceId, @PathParam("crn") String environmentCrn);

    @DELETE
    @Path("/{crn}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = EnvironmentOpDescription.DELETE, produces = ContentType.JSON, notes = Notes.ENVIRONMENT_NOTES,
            nickname = "deleteEnvironment")
    SimpleEnvironmentResponse delete(@PathParam("workspaceId") Long workspaceId, @PathParam("crn") String environmentCrn);

    @PUT
    @Path("/{crn}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = EnvironmentOpDescription.EDIT, produces = ContentType.JSON, notes = Notes.ENVIRONMENT_NOTES,
            nickname = "editEnvironment")
    DetailedEnvironmentResponse edit(@PathParam("workspaceId") Long workspaceId, @PathParam("crn") String environmentCrn,
            @NotNull EnvironmentEditRequest request);

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = EnvironmentOpDescription.LIST, produces = ContentType.JSON, notes = Notes.ENVIRONMENT_NOTES,
            nickname = "listEnvironment")
    SimpleEnvironmentResponses list(@PathParam("workspaceId") Long workspaceId);

    @PUT
    @Path("/{crn}/attach")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = EnvironmentOpDescription.ATTACH_RESOURCES, produces = ContentType.JSON, notes = Notes.ENVIRONMENT_NOTES,
            nickname = "attachResourcesToEnvironment")
    DetailedEnvironmentV4Response attach(@PathParam("workspaceId") Long workspaceId, @PathParam("crn") String environmentCrn,
            @Valid EnvironmentAttachV4Request request);

    @PUT
    @Path("/{crn}/detach")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = EnvironmentOpDescription.DETACH_RESOURCES, produces = ContentType.JSON, notes = Notes.ENVIRONMENT_NOTES,
            nickname = "detachResourcesFromEnvironment")
    DetailedEnvironmentV4Response detach(@PathParam("workspaceId") Long workspaceId, @PathParam("crn") String environmentCrn,
            @Valid EnvironmentDetachV4Request request);

    @PUT
    @Path("/{crn}/register_datalake")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = EnvironmentOpDescription.REGISTER_EXTERNAL_DATALAKE, produces = ContentType.JSON, notes = Notes.ENVIRONMENT_NOTES,
            nickname = "registerExternalDatalake")
    DetailedEnvironmentV4Response registerExternalDatalake(@PathParam("workspaceId") Long workspaceId, @PathParam("crn") String environmentCrn,
            @Valid RegisterDatalakeV4Request request);

    @POST
    @Path("/{crn}/register_datalake_prerequisites")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = UtilityOpDescription.DATALAKE_PREREQUISITES, produces = ContentType.JSON,
            nickname = "registerDatalakePrerequisites")
    DatalakePrerequisiteV4Response registerDatalakePrerequisite(
            @PathParam("workspaceId") Long workspaceId,
            @PathParam("crn") String environmentName,
            @Valid DatalakePrerequisiteV4Request datalakePrerequisiteV4Request);
}
