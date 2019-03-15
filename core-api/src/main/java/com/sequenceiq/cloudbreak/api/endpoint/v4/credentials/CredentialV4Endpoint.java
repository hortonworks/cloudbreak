package com.sequenceiq.cloudbreak.api.endpoint.v4.credentials;

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

import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.requests.CredentialV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.responses.CredentialPrerequisitesV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.responses.CredentialV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.responses.CredentialV4Responses;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.responses.InteractiveCredentialV4Response;
import com.sequenceiq.cloudbreak.doc.ContentType;
import com.sequenceiq.cloudbreak.doc.ControllerDescription;
import com.sequenceiq.cloudbreak.doc.Notes;
import com.sequenceiq.cloudbreak.doc.OperationDescriptions.CredentialOpDescription;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/v4/{workspaceId}/credentials")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v4/{workspaceId}/credentials", description = ControllerDescription.CREDENTIAL_V4_DESCRIPTION, protocols = "http,https")
public interface CredentialV4Endpoint {

    @GET
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = CredentialOpDescription.LIST_BY_WORKSPACE, produces = ContentType.JSON, notes = Notes.CREDENTIAL_NOTES,
            nickname = "listCredentialsByWorkspace", httpMethod = "GET")
    CredentialV4Responses list(@PathParam("workspaceId") Long workspaceId);

    @GET
    @Path("{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = CredentialOpDescription.GET_BY_NAME_IN_WORKSPACE, produces = ContentType.JSON, notes = Notes.CREDENTIAL_NOTES,
            nickname = "getCredentialInWorkspace", httpMethod = "GET")
    CredentialV4Response get(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name);

    @POST
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = CredentialOpDescription.CREATE_IN_WORKSPACE, produces = ContentType.JSON, notes = Notes.CREDENTIAL_NOTES,
            nickname = "createCredentialInWorkspace", httpMethod = "POST")
    CredentialV4Response post(@PathParam("workspaceId") Long workspaceId, @Valid CredentialV4Request request);

    @DELETE
    @Path("{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = CredentialOpDescription.DELETE_BY_NAME_IN_WORKSPACE, produces = ContentType.JSON, notes = Notes.CREDENTIAL_NOTES,
            nickname = "deleteCredentialInWorkspace", httpMethod = "DELETE")
    CredentialV4Response delete(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name);

    @DELETE
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = CredentialOpDescription.DELETE_MULTIPLE_BY_NAME_IN_WORKSPACE, produces = ContentType.JSON, notes = Notes.CREDENTIAL_NOTES,
            nickname = "deleteCredentialsInWorkspace", httpMethod = "DELETE")
    CredentialV4Responses deleteMultiple(@PathParam("workspaceId") Long workspaceId, Set<String> names);

    @PUT
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = CredentialOpDescription.PUT_IN_WORKSPACE, produces = ContentType.JSON, notes = Notes.CREDENTIAL_NOTES,
            nickname = "putCredentialInWorkspace", httpMethod = "PUT")
    CredentialV4Response put(@PathParam("workspaceId") Long workspaceId, @Valid CredentialV4Request credentialRequest);

    @POST
    @Path("interactive_login")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = CredentialOpDescription.INTERACTIVE_LOGIN, produces = ContentType.JSON, notes = Notes.CREDENTIAL_NOTES,
            nickname = "interactiveLoginCredentialInWorkspace", httpMethod = "POST")
    InteractiveCredentialV4Response interactiveLogin(@PathParam("workspaceId") Long workspaceId, @Valid CredentialV4Request credentialRequest);

    @GET
    @Path("prerequisites/{cloudPlatform}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = CredentialOpDescription.GET_PREREQUISTIES_BY_CLOUD_PROVIDER, produces = ContentType.JSON, notes = Notes.CREDENTIAL_NOTES,
            nickname = "getPrerequisitesForCloudPlatform", httpMethod = "GET")
    CredentialPrerequisitesV4Response getPrerequisitesForCloudPlatform(@PathParam("workspaceId") Long workspaceId, @PathParam("cloudPlatform") String platform,
            @QueryParam("deploymentAddress") String deploymentAddress);

    @GET
    @Path("code_grant_flow/init")
    @ApiOperation(value = CredentialOpDescription.INIT_CODE_GRANT_FLOW, notes = Notes.CREDENTIAL_NOTES,
            nickname = "codeGrantFlowBasedCredentialInWorkspace", httpMethod = "GET")
    Response initCodeGrantFlow(@PathParam("workspaceId") Long workspaceId, @Valid CredentialV4Request credentialRequest);

    @GET
    @Path("code_grant_flow/init/{name}")
    @ApiOperation(value = CredentialOpDescription.INIT_CODE_GRANT_FLOW_ON_EXISTING, notes = Notes.CREDENTIAL_NOTES,
            nickname = "codeGrantFlowOnExistingCredentialInWorkspace", httpMethod = "GET")
    Response initCodeGrantFlowOnExisting(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name);

    @GET
    @Path("code_grant_flow/authorization/{cloudPlatform}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = CredentialOpDescription.AUTHORIZE_CODE_GRANT_FLOW, produces = ContentType.JSON,
            nickname = "authorizeCodeGrantFlowBasedCredentialInWorkspace", notes = "Authorize code grant flow based credential creation.",
            httpMethod = "GET")
    CredentialV4Response authorizeCodeGrantFlow(@PathParam("workspaceId") Long workspaceId, @PathParam("cloudPlatform") String platform,
            @QueryParam("code") String code, @QueryParam("state") String state);
}
