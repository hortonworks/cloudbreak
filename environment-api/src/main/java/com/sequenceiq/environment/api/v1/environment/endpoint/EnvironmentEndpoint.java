package com.sequenceiq.environment.api.v1.environment.endpoint;

import static com.sequenceiq.environment.api.doc.environment.EnvironmentDescription.ENVIRONMENT_NOTES;

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
import javax.ws.rs.core.MediaType;

import com.sequenceiq.environment.api.WelcomeResponse;
import com.sequenceiq.environment.api.doc.environment.EnvironmentOpDescription;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentChangeCredentialRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentEditRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentRequest;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.SimpleEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.SimpleEnvironmentResponses;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/v1/env")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v1/env", protocols = "http,https")
public interface EnvironmentEndpoint {

    @GET
    @Path("/welcome")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "welcome", produces = MediaType.APPLICATION_JSON, nickname = "getWelcomeMessage")
    WelcomeResponse welcome();

    @POST
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = EnvironmentOpDescription.CREATE, produces = MediaType.APPLICATION_JSON, notes = ENVIRONMENT_NOTES, nickname = "createEnvironmentV1")
    DetailedEnvironmentResponse post(@Valid EnvironmentRequest request);

    @GET
    @Path("/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = EnvironmentOpDescription.GET, produces = MediaType.APPLICATION_JSON, notes = ENVIRONMENT_NOTES, nickname = "getEnvironmentV1")
    DetailedEnvironmentResponse get(@PathParam("name") String environmentName);

    @DELETE
    @Path("/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = EnvironmentOpDescription.DELETE, produces = MediaType.APPLICATION_JSON, notes = ENVIRONMENT_NOTES, nickname = "deleteEnvironmentV1")
    SimpleEnvironmentResponse delete(@PathParam("name") String environmentName);

    @DELETE
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = EnvironmentOpDescription.DELETE_MULTIPLE_BY_NAME, produces = MediaType.APPLICATION_JSON,
            notes = ENVIRONMENT_NOTES, nickname = "deleteEnvironments", httpMethod = "DELETE")
    SimpleEnvironmentResponses deleteMultiple(Set<String> names);

    @PUT
    @Path("/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = EnvironmentOpDescription.EDIT, produces = MediaType.APPLICATION_JSON, notes = ENVIRONMENT_NOTES, nickname = "editEnvironmentV1")
    DetailedEnvironmentResponse edit(@PathParam("name") String environmentName, @NotNull EnvironmentEditRequest request);

    @GET
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = EnvironmentOpDescription.LIST, produces = MediaType.APPLICATION_JSON, notes = ENVIRONMENT_NOTES, nickname = "listEnvironmentV1")
    SimpleEnvironmentResponses list();

    @PUT
    @Path("/{name}/change_credential")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = EnvironmentOpDescription.CHANGE_CREDENTIAL, produces = MediaType.APPLICATION_JSON, notes = ENVIRONMENT_NOTES,
            nickname = "changeCredentialInEnvironmentV1")
    DetailedEnvironmentResponse changeCredential(@PathParam("name") String environmentName, @Valid EnvironmentChangeCredentialRequest request);

}
