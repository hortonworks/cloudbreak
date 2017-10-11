package com.sequenceiq.cloudbreak.api.endpoint.v1;

import java.util.Map;
import java.util.Set;

import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
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

@Path("/v1/credentials")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v1/credentials", description = ControllerDescription.CREDENTIAL_DESCRIPTION, protocols = "http,https")
public interface CredentialEndpoint {

    @POST
    @Path("user")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = CredentialOpDescription.POST_PRIVATE, produces = ContentType.JSON, notes = Notes.CREDENTIAL_NOTES,
            nickname = "postPrivateCredential")
    CredentialResponse postPrivate(@Valid CredentialRequest credentialRequest);

    @POST
    @Path("account")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = CredentialOpDescription.POST_PUBLIC, produces = ContentType.JSON, notes = Notes.CREDENTIAL_NOTES,
            nickname = "postPublicCredential")
    CredentialResponse postPublic(@Valid CredentialRequest credentialRequest);

    @GET
    @Path("user")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = CredentialOpDescription.GET_PRIVATE, produces = ContentType.JSON, notes = Notes.CREDENTIAL_NOTES,
            nickname = "getPrivatesCredential")
    Set<CredentialResponse> getPrivates();

    @GET
    @Path("account")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = CredentialOpDescription.GET_PUBLIC, produces = ContentType.JSON, notes = Notes.CREDENTIAL_NOTES,
            nickname = "getPublicsCredential")
    Set<CredentialResponse> getPublics();

    @GET
    @Path("user/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = CredentialOpDescription.GET_PRIVATE_BY_NAME, produces = ContentType.JSON, notes = Notes.CREDENTIAL_NOTES,
            nickname = "getPrivateCredential")
    CredentialResponse getPrivate(@PathParam("name") String name);

    @GET
    @Path("account/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = CredentialOpDescription.GET_PUBLIC_BY_NAME, produces = ContentType.JSON, notes = Notes.CREDENTIAL_NOTES,
            nickname = "getPublicCredential")
    CredentialResponse getPublic(@PathParam("name") String name);

    @GET
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = CredentialOpDescription.GET_BY_ID, produces = ContentType.JSON, notes = Notes.CREDENTIAL_NOTES,
            nickname = "getCredential")
    CredentialResponse get(@PathParam("id") Long credentialId);

    @DELETE
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = CredentialOpDescription.DELETE_BY_ID, produces = ContentType.JSON, notes = Notes.CREDENTIAL_NOTES,
            nickname = "deleteCredential")
    void delete(@PathParam("id") Long credentialId);

    @DELETE
    @Path("account/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = CredentialOpDescription.DELETE_PUBLIC_BY_NAME, produces = ContentType.JSON, notes = Notes.CREDENTIAL_NOTES,
            nickname = "deletePublicCredential")
    void deletePublic(@PathParam("name") String name);

    @DELETE
    @Path("user/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = CredentialOpDescription.DELETE_PRIVATE_BY_NAME, produces = ContentType.JSON, notes = Notes.CREDENTIAL_NOTES,
        nickname = "deletePrivateCredential")
    void deletePrivate(@PathParam("name") String name);

    @POST
    @Path("userinteractivelogin")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = CredentialOpDescription.INTERACTIVE_LOGIN, produces = ContentType.JSON, notes = Notes.CREDENTIAL_NOTES,
            nickname = "privateInteractiveLoginCredential")
    Map<String, String> privateInteractiveLogin(@Valid CredentialRequest credentialRequest);

    @POST
    @Path("accountinteractivelogin")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = CredentialOpDescription.INTERACTIVE_LOGIN, produces = ContentType.JSON, notes = Notes.CREDENTIAL_NOTES,
            nickname = "publicInteractiveLoginCredential")
    Map<String, String> publicInteractiveLogin(@Valid CredentialRequest credentialRequest);
}
