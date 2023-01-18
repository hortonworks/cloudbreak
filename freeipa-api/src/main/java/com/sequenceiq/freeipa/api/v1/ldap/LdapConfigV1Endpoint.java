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

import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;
import com.sequenceiq.cloudbreak.validation.ValidCrn;
import com.sequenceiq.freeipa.api.v1.ldap.doc.LdapConfigOpDescription;
import com.sequenceiq.freeipa.api.v1.ldap.model.create.CreateLdapConfigRequest;
import com.sequenceiq.freeipa.api.v1.ldap.model.describe.DescribeLdapConfigResponse;
import com.sequenceiq.freeipa.api.v1.ldap.model.test.TestLdapConfigRequest;
import com.sequenceiq.freeipa.api.v1.ldap.model.test.TestLdapConfigResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Path("/v1/ldaps")
@RetryAndMetrics
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "/v1/ldaps", description = LDAP_CONFIG_DESCRIPTION)
public interface LdapConfigV1Endpoint {
    @GET
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = LdapConfigOpDescription.GET_BY_ENV, description = LDAP_CONFIG_NOTES,
            operationId = "getLdapConfigV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    DescribeLdapConfigResponse describe(@QueryParam("environmentCrn") @NotEmpty String environmentCrn);

    @GET
    @Path("cluster")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = LdapConfigOpDescription.GET_BY_ENV_FOR_CLUSTER, description = LDAP_CONFIG_NOTES,
            operationId = "getLdapConfigForClusterV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    DescribeLdapConfigResponse getForCluster(@QueryParam("environmentCrn") @NotEmpty String environmentCrn,
            @QueryParam("clusterName") @NotEmpty String clusterName);

    @POST
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = LdapConfigOpDescription.CREATE, description = LDAP_CONFIG_NOTES, operationId = "createLdapConfigV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    DescribeLdapConfigResponse create(@Valid @NotNull CreateLdapConfigRequest request);

    @DELETE
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = LdapConfigOpDescription.DELETE_BY_ENV, description = LDAP_CONFIG_NOTES,
            operationId = "deleteLdapConfigV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    void delete(@QueryParam("environmentCrn") @NotEmpty String environmentCrn);

    @POST
    @Path("test")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = LdapConfigOpDescription.POST_CONNECTION_TEST, operationId = "testLdapConfigV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    TestLdapConfigResponse test(@Valid TestLdapConfigRequest ldapValidationRequest);

    @GET
    @Path("request")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = LdapConfigOpDescription.GET_REQUEST, description = LDAP_CONFIG_NOTES,
            operationId = "getLdapRequestByNameV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    CreateLdapConfigRequest getRequest(@QueryParam("environmentCrn") @NotEmpty String environmentCrn);

    @GET
    @Path("usersync")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = LdapConfigOpDescription.GET_BY_ENV_FOR_USERSYNC, description = LDAP_CONFIG_NOTES,
            operationId = "getLdapConfigForUserSyncV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    DescribeLdapConfigResponse getForUserSync(
            @ValidCrn(resource = CrnResourceDescriptor.ENVIRONMENT) @QueryParam("environmentCrn") @NotEmpty String environmentCrn);
}
