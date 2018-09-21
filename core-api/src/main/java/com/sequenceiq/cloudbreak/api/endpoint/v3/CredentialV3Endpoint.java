package com.sequenceiq.cloudbreak.api.endpoint.v3;

import java.util.Map;
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
import javax.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.api.model.v3.credential.CredentialPrerequisites;
import com.sequenceiq.cloudbreak.api.model.CredentialRequest;
import com.sequenceiq.cloudbreak.api.model.CredentialResponse;
import com.sequenceiq.cloudbreak.doc.ContentType;
import com.sequenceiq.cloudbreak.doc.ControllerDescription;
import com.sequenceiq.cloudbreak.doc.Notes;
import com.sequenceiq.cloudbreak.doc.OperationDescriptions.CredentialOpDescription;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/v3/{workspaceId}/credentials")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v3/{workspaceId}/credentials", description = ControllerDescription.CREDENTIAL_V3_DESCRIPTION, protocols = "http,https")
public interface CredentialV3Endpoint {

    @GET
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = CredentialOpDescription.LIST_BY_WORKSPACE, produces = ContentType.JSON, notes = Notes.CREDENTIAL_NOTES,
            nickname = "listCredentialsByWorkspace")
    Set<CredentialResponse> listByWorkspace(@PathParam("workspaceId") Long workspaceId);

    @GET
    @Path("{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = CredentialOpDescription.GET_BY_NAME_IN_WORKSPACE, produces = ContentType.JSON, notes = Notes.CREDENTIAL_NOTES,
            nickname = "getCredentialInWorkspace")
    CredentialResponse getByNameInWorkspace(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name);

    @POST
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = CredentialOpDescription.CREATE_IN_WORKSPACE, produces = ContentType.JSON, notes = Notes.CREDENTIAL_NOTES,
            nickname = "createCredentialInWorkspace")
    CredentialResponse createInWorkspace(@PathParam("workspaceId") Long workspaceId, @Valid CredentialRequest request);

    @DELETE
    @Path("{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = CredentialOpDescription.DELETE_BY_NAME_IN_WORKSPACE, produces = ContentType.JSON, notes = Notes.CREDENTIAL_NOTES,
            nickname = "deleteCredentialInWorkspace")
    CredentialResponse deleteInWorkspace(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name);

    @PUT
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = CredentialOpDescription.PUT_IN_WORKSPACE, produces = ContentType.JSON, notes = Notes.CREDENTIAL_NOTES,
            nickname = "putCredentialInWorkspace")
    CredentialResponse putInWorkspace(@PathParam("workspaceId") Long workspaceId, @Valid CredentialRequest credentialRequest);

    @POST
    @Path("interactivelogin")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = CredentialOpDescription.INTERACTIVE_LOGIN, produces = ContentType.JSON, notes = Notes.CREDENTIAL_NOTES,
            nickname = "interactiveLoginCredentialInWorkspace")
    Map<String, String> interactiveLogin(@PathParam("workspaceId") Long workspaceId, @Valid CredentialRequest credentialRequest);

    @GET
    @Path("prerequisites/{cloudPlatform}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = CredentialOpDescription.GET_PREREQUISTIES_BY_CLOUD_PROVIDER, produces = ContentType.JSON, notes = Notes.CREDENTIAL_NOTES,
            nickname = "getPrerequisitesForCloudPlatform")
    CredentialPrerequisites getPrerequisitesForCloudPlatform(@PathParam("workspaceId") Long workspaceId, @PathParam("cloudPlatform") String platform);
}