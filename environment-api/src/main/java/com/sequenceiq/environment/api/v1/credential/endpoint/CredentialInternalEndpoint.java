package com.sequenceiq.environment.api.v1.credential.endpoint;


import static com.sequenceiq.environment.api.doc.credential.CredentialDescriptor.CREDENTIAL_DESCRIPTION;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;
import com.sequenceiq.environment.api.doc.credential.CredentialDescriptor;
import com.sequenceiq.environment.api.v1.credential.model.response.CredentialResponse;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@RetryAndMetrics
@Path("/v1/internal/credentials")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v1/internal/credentials", description = CREDENTIAL_DESCRIPTION, protocols = "http,https", consumes = MediaType.APPLICATION_JSON)
public interface CredentialInternalEndpoint {

    @GET
    @Path("crn/{crn}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "get internal Credential (includes the deleted credentials as well)", produces = MediaType.APPLICATION_JSON,
            notes = CredentialDescriptor.CREDENTIAL_NOTES, nickname = "getCredentialInternalByResourceCrnV1", httpMethod = "GET")
    CredentialResponse getByResourceCrn(@PathParam("crn") String credentialCrn);

}
