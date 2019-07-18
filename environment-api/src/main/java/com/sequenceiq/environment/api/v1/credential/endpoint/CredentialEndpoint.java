package com.sequenceiq.environment.api.v1.credential.endpoint;


import static com.sequenceiq.environment.api.doc.credential.CredentialDescriptor.CREDENTIAL_DESCRIPTION;

import java.util.Set;

import javax.validation.Valid;
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
import javax.ws.rs.core.Response;

import com.sequenceiq.cloudbreak.cloud.response.CredentialPrerequisitesResponse;
import com.sequenceiq.environment.api.doc.credential.CredentialDescriptor;
import com.sequenceiq.environment.api.doc.credential.CredentialOpDescription;
import com.sequenceiq.environment.api.v1.credential.model.request.CredentialRequest;
import com.sequenceiq.environment.api.v1.credential.model.response.CredentialResponse;
import com.sequenceiq.environment.api.v1.credential.model.response.CredentialResponses;
import com.sequenceiq.environment.api.v1.credential.model.response.InteractiveCredentialResponse;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/v1/credentials")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v1/credentials", description = CREDENTIAL_DESCRIPTION, protocols = "http,https")
public interface CredentialEndpoint {

    @GET
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = CredentialOpDescription.LIST, produces = MediaType.APPLICATION_JSON, notes = CredentialDescriptor.CREDENTIAL_NOTES,
            nickname = "listCredentialsV1", httpMethod = "GET")
    CredentialResponses list();

    @GET
    @Path("name/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = CredentialOpDescription.GET_BY_NAME, produces = MediaType.APPLICATION_JSON,
            notes = CredentialDescriptor.CREDENTIAL_NOTES, nickname = "getCredentialByNameV1", httpMethod = "GET")
    CredentialResponse getByName(@PathParam("name") String credentialName);

    @GET
    @Path("environment/crn/{environmentCrn}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = CredentialOpDescription.GET_BY_ENVIRONMENT_CRN, produces = MediaType.APPLICATION_JSON,
            notes = CredentialDescriptor.CREDENTIAL_NOTES, nickname = "getCredentialByEnvironmentCrnV1", httpMethod = "GET")
    CredentialResponse getByEnvironmentCrn(@PathParam("environmentCrn") String environmentCrn);

    @GET
    @Path("environment/name/{environmentName}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = CredentialOpDescription.GET_BY_ENVIRONMENT_NAME, produces = MediaType.APPLICATION_JSON,
            notes = CredentialDescriptor.CREDENTIAL_NOTES, nickname = "getCredentialByEnvironmentNameV1", httpMethod = "GET")
    CredentialResponse getByEnvironmentName(@PathParam("environmentName") String environmentName);

    @GET
    @Path("crn/{crn}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = CredentialOpDescription.GET_BY_CRN, produces = MediaType.APPLICATION_JSON,
            notes = CredentialDescriptor.CREDENTIAL_NOTES, nickname = "getCredentialByResourceCrnV1", httpMethod = "GET")
    CredentialResponse getByResourceCrn(@PathParam("crn") String credentialCrn);

    @POST
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = CredentialOpDescription.CREATE, produces = MediaType.APPLICATION_JSON, notes = CredentialDescriptor.CREDENTIAL_NOTES,
            nickname = "createCredentialV1", httpMethod = "POST")
    CredentialResponse post(@Valid CredentialRequest request);

    @DELETE
    @Path("name/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = CredentialOpDescription.DELETE_BY_NAME, produces = MediaType.APPLICATION_JSON,
            notes = CredentialDescriptor.CREDENTIAL_NOTES, nickname = "deleteCredentialByNameV1", httpMethod = "DELETE")
    CredentialResponse deleteByName(@PathParam("name") String name);

    @DELETE
    @Path("crn/{crn}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = CredentialOpDescription.DELETE_BY_CRN, produces = MediaType.APPLICATION_JSON,
            notes = CredentialDescriptor.CREDENTIAL_NOTES, nickname = "deleteCredentialByResourceCrnV1", httpMethod = "DELETE")
    CredentialResponse deleteByResourceCrn(@PathParam("crn") String crn);

    @DELETE
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = CredentialOpDescription.DELETE_MULTIPLE_BY_NAME, produces = MediaType.APPLICATION_JSON,
            notes = CredentialDescriptor.CREDENTIAL_NOTES, nickname = "deleteCredentialsV1", httpMethod = "DELETE")
    CredentialResponses deleteMultiple(Set<String> names);

    @PUT
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = CredentialOpDescription.PUT, produces = MediaType.APPLICATION_JSON, notes = CredentialDescriptor.CREDENTIAL_NOTES,
            nickname = "putCredentialV1", httpMethod = "PUT")
    CredentialResponse put(@Valid CredentialRequest credentialRequest);

    @POST
    @Path("interactive_login")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = CredentialOpDescription.INTERACTIVE_LOGIN, produces = MediaType.APPLICATION_JSON, notes = CredentialDescriptor.CREDENTIAL_NOTES,
            nickname = "interactiveLoginCredentialV1", httpMethod = "POST")
    InteractiveCredentialResponse interactiveLogin(@Valid CredentialRequest credentialRequest);

    @GET
    @Path("prerequisites/{cloudPlatform}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = CredentialOpDescription.GET_PREREQUISTIES_BY_CLOUD_PROVIDER, produces = MediaType.APPLICATION_JSON,
            notes = CredentialDescriptor.CREDENTIAL_NOTES, nickname = "getPrerequisitesForCloudPlatform", httpMethod = "GET")
    CredentialPrerequisitesResponse getPrerequisitesForCloudPlatform(@PathParam("cloudPlatform") String platform,
            @QueryParam("deploymentAddress") String deploymentAddress);

    @POST
    @Path("code_grant_flow/init")
    @ApiOperation(value = CredentialOpDescription.INIT_CODE_GRANT_FLOW, produces = MediaType.APPLICATION_JSON, notes = CredentialDescriptor.CREDENTIAL_NOTES,
            nickname = "codeGrantFlowBasedCredentialV1", httpMethod = "POST")
    Response initCodeGrantFlow(@Valid CredentialRequest credentialRequest);

    @GET
    @Path("code_grant_flow/init/{name}")
    @ApiOperation(value = CredentialOpDescription.INIT_CODE_GRANT_FLOW_ON_EXISTING, notes = CredentialDescriptor.CREDENTIAL_NOTES,
            nickname = "codeGrantFlowOnExistingCredentialV1", httpMethod = "GET")
    Response initCodeGrantFlowOnExisting(@PathParam("name") String name);

    @GET
    @Path("code_grant_flow/authorization/{cloudPlatform}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = CredentialOpDescription.AUTHORIZE_CODE_GRANT_FLOW, produces = MediaType.APPLICATION_JSON,
            nickname = "authorizeCodeGrantFlowBasedCredentialV1", notes = "Authorize code grant flow based credential creation.",
            httpMethod = "GET")
    CredentialResponse authorizeCodeGrantFlow(@PathParam("cloudPlatform") String platform,
            @QueryParam("code") String code, @QueryParam("state") String state);
}
