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

import com.sequenceiq.cloudbreak.api.model.CredentialRequest;
import com.sequenceiq.cloudbreak.api.model.CredentialResponse;
import com.sequenceiq.cloudbreak.doc.ContentType;
import com.sequenceiq.cloudbreak.doc.ControllerDescription;
import com.sequenceiq.cloudbreak.doc.Notes;
import com.sequenceiq.cloudbreak.doc.OperationDescriptions.CredentialOpDescription;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/v3/{organizationId}/credentials")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v3/{organizationId}/credentials", description = ControllerDescription.CREDENTIAL_V3_DESCRIPTION, protocols = "http,https")
public interface CredentialV3Endpoint {

    @GET
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = CredentialOpDescription.LIST_BY_ORGANIZATION, produces = ContentType.JSON, notes = Notes.CREDENTIAL_NOTES,
            nickname = "listCredentialsByOrganization")
    Set<CredentialResponse> listByOrganization(@PathParam("organizationId") Long organizationId);

    @GET
    @Path("{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = CredentialOpDescription.GET_BY_NAME_IN_ORG, produces = ContentType.JSON, notes = Notes.CREDENTIAL_NOTES,
            nickname = "getCredentialInOrganization")
    CredentialResponse getByNameInOrganization(@PathParam("organizationId") Long organizationId, @PathParam("name") String name);

    @POST
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = CredentialOpDescription.CREATE_IN_ORG, produces = ContentType.JSON, notes = Notes.CREDENTIAL_NOTES,
            nickname = "createCredentialInOrganization")
    CredentialResponse createInOrganization(@PathParam("organizationId") Long organizationId, @Valid CredentialRequest request);

    @DELETE
    @Path("{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = CredentialOpDescription.DELETE_BY_NAME_IN_ORG, produces = ContentType.JSON, notes = Notes.CREDENTIAL_NOTES,
            nickname = "deleteCredentialInOrganization")
    CredentialResponse deleteInOrganization(@PathParam("organizationId") Long organizationId, @PathParam("name") String name);

    @PUT
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = CredentialOpDescription.PUT_IN_ORG, produces = ContentType.JSON, notes = Notes.CREDENTIAL_NOTES,
            nickname = "putCredentialInOrganization")
    CredentialResponse putInOrganization(@PathParam("organizationId") Long organizationId, @Valid CredentialRequest credentialRequest);

    @POST
    @Path("interactivelogin")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = CredentialOpDescription.INTERACTIVE_LOGIN, produces = ContentType.JSON, notes = Notes.CREDENTIAL_NOTES,
            nickname = "interactiveLoginCredentialInOrganization")
    Map<String, String> interactiveLogin(@PathParam("organizationId") Long organizationId, @Valid CredentialRequest credentialRequest);

}
