package com.sequenceiq.cloudbreak.api.endpoint;

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

import com.sequenceiq.cloudbreak.doc.ContentType;
import com.sequenceiq.cloudbreak.doc.ControllerDescription;
import com.sequenceiq.cloudbreak.doc.Notes;
import com.sequenceiq.cloudbreak.doc.OperationDescriptions;
import com.sequenceiq.cloudbreak.api.model.CredentialRequest;
import com.sequenceiq.cloudbreak.api.model.CredentialResponse;
import com.sequenceiq.cloudbreak.api.model.IdJson;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/credentials", description = ControllerDescription.CREDENTIAL_DESCRIPTION, position = 1)
public interface CredentialEndpoint {

    @POST
    @Path("user/credentials")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.CredentialOpDescription.POST_PRIVATE, produces = ContentType.JSON, notes = Notes.CREDENTIAL_NOTES)
    IdJson postPrivate(@Valid CredentialRequest credentialRequest);

    @POST
    @Path("account/credentials")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.CredentialOpDescription.POST_PUBLIC, produces = ContentType.JSON, notes = Notes.CREDENTIAL_NOTES)
    IdJson postPublic(@Valid CredentialRequest credentialRequest);

    @GET
    @Path("user/credentials")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.CredentialOpDescription.GET_PRIVATE, produces = ContentType.JSON, notes = Notes.CREDENTIAL_NOTES)
    Set<CredentialResponse> getPrivates();

    @GET
    @Path("account/credentials")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.CredentialOpDescription.GET_PUBLIC, produces = ContentType.JSON, notes = Notes.CREDENTIAL_NOTES)
    Set<CredentialResponse> getPublics();

    @GET
    @Path("user/credentials/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.CredentialOpDescription.GET_PRIVATE_BY_NAME, produces = ContentType.JSON, notes = Notes.CREDENTIAL_NOTES)
    CredentialResponse getPrivate(@PathParam(value = "name") String name);

    @GET
    @Path("account/credentials/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.CredentialOpDescription.GET_PUBLIC_BY_NAME, produces = ContentType.JSON, notes = Notes.CREDENTIAL_NOTES)
    CredentialResponse getPublic(@PathParam(value = "name") String name);

    @GET
    @Path("credentials/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.CredentialOpDescription.GET_BY_ID, produces = ContentType.JSON, notes = Notes.CREDENTIAL_NOTES)
    CredentialResponse get(@PathParam(value = "id") Long credentialId);

    @DELETE
    @Path("credentials/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.CredentialOpDescription.DELETE_BY_ID, produces = ContentType.JSON, notes = Notes.CREDENTIAL_NOTES)
    void delete(@PathParam(value = "id") Long credentialId);

    @DELETE
    @Path("account/credentials/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.CredentialOpDescription.DELETE_PUBLIC_BY_NAME, produces = ContentType.JSON, notes = Notes.CREDENTIAL_NOTES)
    void deletePublic(@PathParam(value = "name") String name);

    @DELETE
    @Path("user/credentials/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.CredentialOpDescription.DELETE_PRIVATE_BY_NAME, produces = ContentType.JSON, notes = Notes.CREDENTIAL_NOTES)
    void deletePrivate(@PathParam(value = "name") String name);
}
