package com.sequenceiq.freeipa.api.v1.ldap;

import static com.sequenceiq.freeipa.api.v1.ldap.doc.LdapConfigModelDescription.LDAP_CONFIG_DESCRIPTION;
import static com.sequenceiq.freeipa.api.v1.ldap.doc.LdapConfigModelDescription.LDAP_CONFIG_NOTES;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.jerseyclient.retry.RetryingRestClient;
import com.sequenceiq.freeipa.api.v1.ldap.doc.LdapConfigOpDescription;
import com.sequenceiq.freeipa.api.v1.ldap.model.create.CreateLdapConfigRequest;
import com.sequenceiq.freeipa.api.v1.ldap.model.describe.DescribeLdapConfigResponse;
import com.sequenceiq.freeipa.api.v1.ldap.model.test.TestLdapConfigRequest;
import com.sequenceiq.freeipa.api.v1.ldap.model.test.TestLdapConfigResponse;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/v1/ldaps")
@RetryingRestClient
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v1/ldaps", description = LDAP_CONFIG_DESCRIPTION, protocols = "http,https")
public interface LdapConfigV1Endpoint {
    @GET
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = LdapConfigOpDescription.GET_BY_ENV, produces = MediaType.APPLICATION_JSON, notes = LDAP_CONFIG_NOTES,
            nickname = "getLdapConfigV1")
    DescribeLdapConfigResponse describe(@QueryParam("environmentCrn") @NotEmpty String environmentCrn);

    @GET
    @Path("cluster")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = LdapConfigOpDescription.GET_BY_ENV_FOR_CLUSTER, produces = MediaType.APPLICATION_JSON, notes = LDAP_CONFIG_NOTES,
            nickname = "getLdapConfigForClusterV1")
    DescribeLdapConfigResponse getForCluster(@QueryParam("environmentCrn") @NotEmpty String environmentCrn,
            @QueryParam("clusterName") @NotEmpty String clusterName) throws Exception;

    @POST
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = LdapConfigOpDescription.CREATE, produces = MediaType.APPLICATION_JSON, notes = LDAP_CONFIG_NOTES, nickname = "createLdapConfigV1")
    DescribeLdapConfigResponse create(@Valid @NotNull CreateLdapConfigRequest request);

    @DELETE
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = LdapConfigOpDescription.DELETE_BY_ENV, produces = MediaType.APPLICATION_JSON, notes = LDAP_CONFIG_NOTES,
            nickname = "deleteLdapConfigV1")
    void delete(@QueryParam("environmentCrn") @NotEmpty String environmentCrn);

    @POST
    @Path("test")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = LdapConfigOpDescription.POST_CONNECTION_TEST, produces = MediaType.APPLICATION_JSON, nickname = "testLdapConfigV1")
    TestLdapConfigResponse test(@Valid TestLdapConfigRequest ldapValidationRequest);

    @GET
    @Path("request")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = LdapConfigOpDescription.GET_REQUEST, produces = MediaType.APPLICATION_JSON, notes = LDAP_CONFIG_NOTES,
            nickname = "getLdapRequestByNameV1")
    CreateLdapConfigRequest getRequest(@QueryParam("environmentCrn") @NotEmpty String environmentCrn);
}
