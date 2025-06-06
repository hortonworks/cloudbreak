package com.sequenceiq.environment.api.v1.proxy.endpoint;

import java.util.Set;

import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;
import com.sequenceiq.environment.api.doc.proxy.ProxyConfigDescription;
import com.sequenceiq.environment.api.v1.proxy.model.request.ProxyRequest;
import com.sequenceiq.environment.api.v1.proxy.model.response.ProxyResponse;
import com.sequenceiq.environment.api.v1.proxy.model.response.ProxyResponses;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RetryAndMetrics
@Path("/v1/proxies")
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "/v1/proxies")
public interface ProxyEndpoint {

    @GET
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = ProxyConfigDescription.LIST, description = ProxyConfigDescription.PROXY_CONFIG_NOTES,
            operationId = "listProxyConfigsV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    ProxyResponses list();

    @GET
    @Path("name/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = ProxyConfigDescription.GET_BY_NAME,
            description = ProxyConfigDescription.PROXY_CONFIG_NOTES, operationId = "getProxyConfigByNameV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    ProxyResponse getByName(@PathParam("name") String name);

    @GET
    @Path("account/{accountId}/name/{name}/crn")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = ProxyConfigDescription.GET_CRN_BY_NAME,
            description = ProxyConfigDescription.PROXY_CONFIG_NOTES, operationId = "getProxyConfigCrnByNameV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    String getCrnByAccountIdAndName(@PathParam("accountId") String accountId, @PathParam("name") String name);

    @GET
    @Path("environment/crn/{environmentCrn}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = ProxyConfigDescription.GET_BY_ENVIRONMENT_CRN,
            description = ProxyConfigDescription.PROXY_CONFIG_NOTES, operationId = "getProxyConfigByEnvironmentCrnV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    ProxyResponse getByEnvironmentCrn(@PathParam("environmentCrn") String environmentCrn);

    @GET
    @Path("crn/{crn}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = ProxyConfigDescription.GET_BY_CRN,
            description = ProxyConfigDescription.PROXY_CONFIG_NOTES, operationId = "getProxyConfigByCrnV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    ProxyResponse getByResourceCrn(@PathParam("crn") String crn);

    @POST
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = ProxyConfigDescription.CREATE,
            description = ProxyConfigDescription.PROXY_CONFIG_NOTES, operationId = "createProxyConfigV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    ProxyResponse post(@Valid ProxyRequest request);

    @DELETE
    @Path("name/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = ProxyConfigDescription.DELETE_BY_NAME,
            description = ProxyConfigDescription.PROXY_CONFIG_NOTES, operationId = "deleteProxyConfigByNameV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    ProxyResponse deleteByName(@PathParam("name") String name);

    @DELETE
    @Path("crn/{crn}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = ProxyConfigDescription.DELETE_BY_CRN,
            description = ProxyConfigDescription.PROXY_CONFIG_NOTES, operationId = "deleteProxyConfigByCrnV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    ProxyResponse deleteByCrn(@PathParam("crn") String crn);

    @DELETE
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = ProxyConfigDescription.DELETE_MULTIPLE_BY_NAME,
            description = ProxyConfigDescription.PROXY_CONFIG_NOTES, operationId = "deleteProxyConfigsV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    ProxyResponses deleteMultiple(Set<String> names);

    @GET
    @Path("{name}/request")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = ProxyConfigDescription.GET_REQUEST_BY_NAME,
            description = ProxyConfigDescription.PROXY_CONFIG_NOTES,
            operationId = "getProxyRequestFromNameV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    ProxyRequest getRequest(@PathParam("name") String name);
}
