package com.sequenceiq.environment.api.v1.credential.endpoint;


import static com.sequenceiq.environment.api.doc.credential.CredentialDescriptor.CREDENTIAL_DESCRIPTION;

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

import com.sequenceiq.cloudbreak.auth.security.internal.AccountId;
import com.sequenceiq.cloudbreak.cloud.response.CredentialPrerequisitesResponse;
import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;
import com.sequenceiq.environment.api.doc.credential.CredentialDescriptor;
import com.sequenceiq.environment.api.doc.credential.CredentialOpDescription;
import com.sequenceiq.environment.api.v1.credential.model.request.CredentialRequest;
import com.sequenceiq.environment.api.v1.credential.model.request.EditCredentialRequest;
import com.sequenceiq.environment.api.v1.credential.model.response.CredentialResponse;
import com.sequenceiq.environment.api.v1.credential.model.response.CredentialResponses;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@RetryAndMetrics
@Path("/v1/credentials/audit")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v1/credentials/audit", description = CREDENTIAL_DESCRIPTION, protocols = "http,https", consumes = MediaType.APPLICATION_JSON)
public interface AuditCredentialEndpoint {

    @GET
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = CredentialOpDescription.LIST, produces = MediaType.APPLICATION_JSON, notes = CredentialDescriptor.CREDENTIAL_NOTES,
            nickname = "listAuditCredentialsV1", httpMethod = "GET")
    CredentialResponses list();

    @GET
    @Path("crn/{crn}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = CredentialOpDescription.GET_BY_CRN, produces = MediaType.APPLICATION_JSON,
            notes = CredentialDescriptor.CREDENTIAL_NOTES, nickname = "getAuditCredentialByResourceCrnV1", httpMethod = "GET")
    CredentialResponse getByResourceCrn(@PathParam("crn") String credentialCrn);

    @GET
    @Path("name/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = CredentialOpDescription.GET_BY_NAME, produces = MediaType.APPLICATION_JSON,
            notes = CredentialDescriptor.CREDENTIAL_NOTES, nickname = "getAuditCredentialByResourceNameV1", httpMethod = "GET")
    CredentialResponse getByResourceName(@PathParam("name") String credentialName, @AccountId @QueryParam("accountId") String accountId);

    @POST
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = CredentialOpDescription.CREATE, produces = MediaType.APPLICATION_JSON, notes = CredentialDescriptor.CREDENTIAL_NOTES,
            nickname = "createAuditCredentialV1", httpMethod = "POST")
    CredentialResponse post(@Valid CredentialRequest request);

    @PUT
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = CredentialOpDescription.PUT, produces = MediaType.APPLICATION_JSON, notes = CredentialDescriptor.CREDENTIAL_NOTES,
            nickname = "putAuditCredentialV1", httpMethod = "PUT")
    CredentialResponse put(@Valid EditCredentialRequest credentialRequest);

    @GET
    @Path("prerequisites/{cloudPlatform}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = CredentialOpDescription.GET_PREREQUISTIES_BY_CLOUD_PROVIDER, produces = MediaType.APPLICATION_JSON,
            notes = CredentialDescriptor.CREDENTIAL_NOTES, nickname = "getAuditPrerequisitesForCloudPlatform", httpMethod = "GET")
    CredentialPrerequisitesResponse getPrerequisitesForCloudPlatform(@PathParam("cloudPlatform") String platform,
        @QueryParam("govCloud") boolean govCloud,
        @QueryParam("deploymentAddress") String deploymentAddress);

    @DELETE
    @Path("name/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = CredentialOpDescription.DELETE_BY_NAME, produces = MediaType.APPLICATION_JSON,
            notes = CredentialDescriptor.CREDENTIAL_NOTES, nickname = "deleteAuditCredentialByNameV1", httpMethod = "DELETE")
    CredentialResponse deleteByName(@PathParam("name") String name);

    @DELETE
    @Path("crn/{crn}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = CredentialOpDescription.DELETE_BY_CRN, produces = MediaType.APPLICATION_JSON,
            notes = CredentialDescriptor.CREDENTIAL_NOTES, nickname = "deleteAuditCredentialByResourceCrnV1", httpMethod = "DELETE")
    CredentialResponse deleteByResourceCrn(@PathParam("crn") String crn);
}
