package com.sequenceiq.cloudbreak.api.endpoint.v4;

import static com.sequenceiq.cloudbreak.doc.ApiDescription.CUSTOM_CONFIGURATIONS_NOTES;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.api.endpoint.v4.requests.CloneCustomConfigurationsV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.requests.CustomConfigurationsV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.responses.CustomConfigurationsV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.responses.CustomConfigurationsV4Responses;
import com.sequenceiq.cloudbreak.api.endpoint.v4.responses.RoleTypeV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.responses.ServiceTypeV4Response;
import com.sequenceiq.cloudbreak.doc.ApiDescription.CustomConfigurationsOpDescription;
import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RetryAndMetrics
@Path("/v4/custom_configurations")
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "/v4/custom_configurations")
public interface CustomConfigurationsV4Endpoint {
    @GET
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = CustomConfigurationsOpDescription.GET_ALL, operationId = "list",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true),
            description = CUSTOM_CONFIGURATIONS_NOTES)
    CustomConfigurationsV4Responses list();

    @GET
    @Path("/crn/{crn}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = CustomConfigurationsOpDescription.GET_BY_CRN, operationId = "getByCrn",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true),
            description = CUSTOM_CONFIGURATIONS_NOTES)
    CustomConfigurationsV4Response getByCrn(@PathParam("crn") @NotNull String crn);

    @GET
    @Path("/name/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = CustomConfigurationsOpDescription.GET_BY_NAME, operationId = "getByName",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true),
            description = CUSTOM_CONFIGURATIONS_NOTES)
    CustomConfigurationsV4Response getByName(@PathParam("name") @NotNull String name);

    @POST
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = CustomConfigurationsOpDescription.CREATE, operationId = "post",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true),
            description = CUSTOM_CONFIGURATIONS_NOTES)
    CustomConfigurationsV4Response post(@Valid CustomConfigurationsV4Request request);

    @POST
    @Path("/name/{name}/clone")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = CustomConfigurationsOpDescription.CLONE_BY_NAME, operationId = "cloneByName",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true),
            description = CUSTOM_CONFIGURATIONS_NOTES)
    CustomConfigurationsV4Response cloneByName(@PathParam("name") @NotNull String name, @Valid CloneCustomConfigurationsV4Request cloneCustomConfigsRequest);

    @POST
    @Path("/crn/{crn}/clone")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = CustomConfigurationsOpDescription.CLONE_BY_CRN, operationId = "cloneByCrn",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true),
            description = CUSTOM_CONFIGURATIONS_NOTES)
    CustomConfigurationsV4Response cloneByCrn(@PathParam("crn") @NotNull String crn, @Valid CloneCustomConfigurationsV4Request cloneCustomConfigsRequest);

    @DELETE
    @Path("/crn/{crn}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = CustomConfigurationsOpDescription.DELETE_BY_CRN, operationId = "deleteByCrn",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true),
            description = CUSTOM_CONFIGURATIONS_NOTES)
    CustomConfigurationsV4Response deleteByCrn(@PathParam("crn") @NotNull String crn);

    @DELETE
    @Path("/name/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = CustomConfigurationsOpDescription.DELETE_BY_NAME, operationId = "deleteByName",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true),
            description = CUSTOM_CONFIGURATIONS_NOTES)
    CustomConfigurationsV4Response deleteByName(@PathParam("name") @NotNull String name);

    @GET
    @Path("/service_types")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = CustomConfigurationsOpDescription.GET_SERVICE_TYPES, operationId = "getServiceTypes",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    ServiceTypeV4Response getServiceTypes();

    @GET
    @Path("/role_types")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = CustomConfigurationsOpDescription.GET_ROLE_TYPES, operationId = "getRoleTypes",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    RoleTypeV4Response getRoleTypes();
}
