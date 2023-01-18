package com.sequenceiq.freeipa.api.v1.kerberos;

import static com.sequenceiq.freeipa.api.v1.kerberos.doc.KerberosConfigNotes.KERBEROS_CONFIG_NOTES;
import static com.sequenceiq.freeipa.api.v1.kerberos.doc.KerberosConfigOperationDescription.CREATE_FOR_ENVIRONMENT;
import static com.sequenceiq.freeipa.api.v1.kerberos.doc.KerberosConfigOperationDescription.DELETE_BY_ENVIRONMENT;
import static com.sequenceiq.freeipa.api.v1.kerberos.doc.KerberosConfigOperationDescription.DESCRIBE_FOR_ENVIRONMENT;
import static com.sequenceiq.freeipa.api.v1.kerberos.doc.KerberosConfigOperationDescription.GET_REQUEST;
import static com.sequenceiq.freeipa.api.v1.kerberos.doc.KerberosConfigOperationDescription.KERBEROS_CONFIG_V4_DESCRIPTION;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;
import com.sequenceiq.freeipa.api.v1.kerberos.doc.KerberosConfigOperationDescription;
import com.sequenceiq.freeipa.api.v1.kerberos.model.create.CreateKerberosConfigRequest;
import com.sequenceiq.freeipa.api.v1.kerberos.model.describe.DescribeKerberosConfigResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RetryAndMetrics
@Path("/v1/kerberos")
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "/v1/kerberos", description = KERBEROS_CONFIG_V4_DESCRIPTION)
public interface KerberosConfigV1Endpoint {
    @GET
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = DESCRIBE_FOR_ENVIRONMENT, description = KERBEROS_CONFIG_NOTES,
            operationId = "getKerberosConfigForEnvironment",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    DescribeKerberosConfigResponse describe(@QueryParam("environmentCrn") @NotEmpty String environmentCrn);

    @GET
    @Path("cluster")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = KerberosConfigOperationDescription.GET_BY_ENV_FOR_CLUSTER, description = KERBEROS_CONFIG_NOTES,
            operationId = "getKerberosConfigForClusterV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    DescribeKerberosConfigResponse getForCluster(@QueryParam("environmentCrn") @NotEmpty String environmentCrn,
            @QueryParam("clusterName") @NotEmpty String clusterName);

    @POST
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = CREATE_FOR_ENVIRONMENT, description = KERBEROS_CONFIG_NOTES,
            operationId = "createKerberosConfigForEnvironment",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    DescribeKerberosConfigResponse create(@Valid CreateKerberosConfigRequest request);

    @DELETE
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = DELETE_BY_ENVIRONMENT, description = KERBEROS_CONFIG_NOTES,
            operationId = "deleteKerberosConfigForEnvironment",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    void delete(@QueryParam("environmentCrn") @NotEmpty String environmentCrn);

    @GET
    @Path("request")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = GET_REQUEST, description = KERBEROS_CONFIG_NOTES,
            operationId = "getCreateKerberosRequestForEnvironment",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    CreateKerberosConfigRequest getRequest(@QueryParam("environmentCrn") @NotEmpty String environmentCrn);
}
