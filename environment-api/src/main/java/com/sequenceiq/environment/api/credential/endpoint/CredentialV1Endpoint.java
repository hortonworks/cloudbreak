package com.sequenceiq.environment.api.credential.endpoint;


import static com.sequenceiq.environment.api.credential.doc.CredentialDescriptor.CREDENTIAL_DESCRIPTION;

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
import com.sequenceiq.environment.api.credential.doc.CredentialDescriptor;
import com.sequenceiq.environment.api.credential.doc.CredentialOpDescription;
import com.sequenceiq.environment.api.credential.model.request.CredentialV1Request;
import com.sequenceiq.environment.api.credential.model.response.CredentialV1Response;
import com.sequenceiq.environment.api.credential.model.response.CredentialV1Responses;
import com.sequenceiq.environment.api.credential.model.response.InteractiveCredentialV1Response;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/v1/credentials")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v1/credentials", description = CREDENTIAL_DESCRIPTION, protocols = "http,https")
public interface CredentialV1Endpoint {

    @GET
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = CredentialOpDescription.LIST, produces = MediaType.APPLICATION_JSON, notes = CredentialDescriptor.CREDENTIAL_NOTES,
            nickname = "listCredentialsV1", httpMethod = "GET")
    CredentialV1Responses list();

    @GET
    @Path("{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = CredentialOpDescription.GET_BY_NAME, produces = MediaType.APPLICATION_JSON,
            notes = CredentialDescriptor.CREDENTIAL_NOTES, nickname = "getCredentialV1", httpMethod = "GET")
    CredentialV1Response get(@PathParam("name") String name);

    @POST
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = CredentialOpDescription.CREATE, produces = MediaType.APPLICATION_JSON, notes = CredentialDescriptor.CREDENTIAL_NOTES,
            nickname = "createCredentialV1", httpMethod = "POST")
    CredentialV1Response post(@Valid CredentialV1Request request);

    @DELETE
    @Path("{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = CredentialOpDescription.DELETE_BY_NAME, produces = MediaType.APPLICATION_JSON,
            notes = CredentialDescriptor.CREDENTIAL_NOTES, nickname = "deleteCredentialV1", httpMethod = "DELETE")
    CredentialV1Response delete(@PathParam("name") String name);

    @DELETE
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = CredentialOpDescription.DELETE_MULTIPLE_BY_NAME, produces = MediaType.APPLICATION_JSON,
            notes = CredentialDescriptor.CREDENTIAL_NOTES, nickname = "deleteCredentialsV1", httpMethod = "DELETE")
    CredentialV1Responses deleteMultiple(Set<String> names);

    @PUT
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = CredentialOpDescription.PUT, produces = MediaType.APPLICATION_JSON, notes = CredentialDescriptor.CREDENTIAL_NOTES,
            nickname = "putCredentialV1", httpMethod = "PUT")
    CredentialV1Response put(@Valid CredentialV1Request credentialRequest);

    @POST
    @Path("interactive_login")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = CredentialOpDescription.INTERACTIVE_LOGIN, produces = MediaType.APPLICATION_JSON, notes = CredentialDescriptor.CREDENTIAL_NOTES,
            nickname = "interactiveLoginCredentialV1", httpMethod = "POST")
    InteractiveCredentialV1Response interactiveLogin(@Valid CredentialV1Request credentialRequest);

    @GET
    @Path("prerequisites/{cloudPlatform}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = CredentialOpDescription.GET_PREREQUISTIES_BY_CLOUD_PROVIDER, produces = MediaType.APPLICATION_JSON,
            notes = CredentialDescriptor.CREDENTIAL_NOTES, nickname = "getPrerequisitesForCloudPlatform", httpMethod = "GET")
    CredentialPrerequisitesResponse getPrerequisitesForCloudPlatform(@PathParam("cloudPlatform") String platform,
            @QueryParam("deploymentAddress") String deploymentAddress);

    @GET
    @Path("code_grant_flow/init")
    @ApiOperation(value = CredentialOpDescription.INIT_CODE_GRANT_FLOW, notes = CredentialDescriptor.CREDENTIAL_NOTES,
            nickname = "codeGrantFlowBasedCredentialV1", httpMethod = "GET")
    Response initCodeGrantFlow(@Valid CredentialV1Request credentialRequest);

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
    CredentialV1Response authorizeCodeGrantFlow(@PathParam("cloudPlatform") String platform,
            @QueryParam("code") String code, @QueryParam("state") String state);
}
