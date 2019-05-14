package com.sequenceiq.environment.api.environment.endpoint;

import static com.sequenceiq.environment.api.environment.doc.EnvironmentDescription.ENVIRONMENT_NOTES;

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

import com.sequenceiq.environment.api.environment.doc.EnvironmentOpDescription;
import com.sequenceiq.environment.api.environment.model.request.EnvironmentAttachV1Request;
import com.sequenceiq.environment.api.environment.model.request.EnvironmentChangeCredentialV1Request;
import com.sequenceiq.environment.api.environment.model.request.EnvironmentDetachV1Request;
import com.sequenceiq.environment.api.environment.model.request.EnvironmentEditV1Request;
import com.sequenceiq.environment.api.environment.model.request.EnvironmentV1Request;
import com.sequenceiq.environment.api.environment.model.response.DetailedEnvironmentV1Response;
import com.sequenceiq.environment.api.environment.model.response.SimpleEnvironmentV1Response;
import com.sequenceiq.environment.api.environment.model.response.SimpleEnvironmentV1Responses;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/v1/env")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v1/env", protocols = "http,https")
public interface EnvironmentV1Endpoint {

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = EnvironmentOpDescription.CREATE, produces = MediaType.APPLICATION_JSON, notes = ENVIRONMENT_NOTES, nickname = "createEnvironmentV1")
    DetailedEnvironmentV1Response post(@Valid EnvironmentV1Request request);

    @GET
    @Path("/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = EnvironmentOpDescription.GET, produces = MediaType.APPLICATION_JSON, notes = ENVIRONMENT_NOTES, nickname = "getEnvironmentV1")
    DetailedEnvironmentV1Response get(@PathParam("name") String environmentName);

    @DELETE
    @Path("/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = EnvironmentOpDescription.DELETE, produces = MediaType.APPLICATION_JSON, notes = ENVIRONMENT_NOTES, nickname = "deleteEnvironmentV1")
    SimpleEnvironmentV1Response delete(@PathParam("name") String environmentName);

    @PUT
    @Path("/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = EnvironmentOpDescription.EDIT, produces = MediaType.APPLICATION_JSON, notes = ENVIRONMENT_NOTES, nickname = "editEnvironmentV1")
    DetailedEnvironmentV1Response edit(@PathParam("name") String environmentName, @NotNull EnvironmentEditV1Request request);

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = EnvironmentOpDescription.LIST, produces = MediaType.APPLICATION_JSON, notes = ENVIRONMENT_NOTES, nickname = "listEnvironmentV1")
    SimpleEnvironmentV1Responses list();

    @PUT
    @Path("/{name}/attach")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = EnvironmentOpDescription.ATTACH_RESOURCES, produces = MediaType.APPLICATION_JSON, notes = ENVIRONMENT_NOTES,
            nickname = "attachResourcesToEnvironmentV1")
    DetailedEnvironmentV1Response attach(@PathParam("name") String environmentName, @Valid EnvironmentAttachV1Request request);

    @PUT
    @Path("/{name}/detach")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = EnvironmentOpDescription.DETACH_RESOURCES, produces = MediaType.APPLICATION_JSON, notes = ENVIRONMENT_NOTES,
            nickname = "detachResourcesFromEnvironmentV1")
    DetailedEnvironmentV1Response detach(@PathParam("name") String environmentName, @Valid EnvironmentDetachV1Request request);

    @PUT
    @Path("/{name}/change_credential")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = EnvironmentOpDescription.CHANGE_CREDENTIAL, produces = MediaType.APPLICATION_JSON, notes = ENVIRONMENT_NOTES,
            nickname = "changeCredentialInEnvironmentV1")
    DetailedEnvironmentV1Response changeCredential(@PathParam("name") String environmentName, @Valid EnvironmentChangeCredentialV1Request request);
}
