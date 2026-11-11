package com.sequenceiq.distrox.api.v1.distrox.endpoint;

import static com.sequenceiq.distrox.api.v1.distrox.doc.DistroXOpDescription.CHANGE_DATABASE_SERVER_INSTANCE_TYPE_BY_CRN;
import static com.sequenceiq.distrox.api.v1.distrox.doc.DistroXOpDescription.CHANGE_DATABASE_SERVER_INSTANCE_TYPE_BY_NAME;
import static com.sequenceiq.distrox.api.v1.distrox.doc.DistroXOpDescription.GET_DATABASE_SERVER_BY_CLUSTER_CRN;
import static com.sequenceiq.distrox.api.v1.distrox.doc.DistroXOpDescription.GET_DATABASE_SERVER_VALID_INSTANCE_TYPE_OPTIONS_BY_CRN;
import static com.sequenceiq.distrox.api.v1.distrox.doc.DistroXOpDescription.GET_DATABASE_SERVER_VALID_INSTANCE_TYPE_OPTIONS_BY_NAME;
import static com.sequenceiq.distrox.api.v1.distrox.doc.DistroXOpDescription.INCREASE_DATABASE_SERVER_STORAGE_SIZE_BY_CRN;
import static com.sequenceiq.distrox.api.v1.distrox.doc.DistroXOpDescription.INCREASE_DATABASE_SERVER_STORAGE_SIZE_BY_NAME;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import org.springframework.validation.annotation.Validated;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.database.DatabaseServerChangeInstanceTypeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.database.DatabaseServerIncreaseStorageSizeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.database.DatabaseInstanceTypesV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.database.DatabaseServerChangeInstanceTypeV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.database.DatabaseServerIncreaseStorageSizeV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.database.DatabaseServerValidInstanceTypeOptionsV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.database.StackDatabaseServerResponse;
import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;
import com.sequenceiq.cloudbreak.validation.ValidCrn;
import com.sequenceiq.common.model.DatabaseCapabilityType;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Validated
@Path("/v1/distrox")
@RetryAndMetrics
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "/v1/distrox")
public interface DistroXDatabaseServerV1Endpoint {

    @GET
    @Path("/crn/{clusterCrn}/dbserver")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = GET_DATABASE_SERVER_BY_CLUSTER_CRN, operationId = "getDatabaseServerByClusterCrn",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    StackDatabaseServerResponse getDatabaseServerByCrn(@PathParam("clusterCrn") @ValidCrn(resource = CrnResourceDescriptor.DATAHUB) String clusterCrn);

    @GET
    @Path("/database/instance_types")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "list available database instance types for datahub", operationId = "listDistroXDatabaseInstanceTypes",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    DatabaseInstanceTypesV4Response getDatabaseInstanceTypes(
            @QueryParam("environmentCrn") @NotEmpty @ValidCrn(resource = CrnResourceDescriptor.ENVIRONMENT) String environmentCrn,
            @QueryParam("databaseType") DatabaseCapabilityType databaseType,
            @QueryParam("architecture") String architecture);

    @PUT
    @Path("/name/{name}/dbserver/change_instance_type")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = CHANGE_DATABASE_SERVER_INSTANCE_TYPE_BY_NAME, operationId = "changeDistroXDatabaseServerInstanceTypeByName",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    DatabaseServerChangeInstanceTypeV4Response changeDatabaseServerInstanceTypeByName(@PathParam("name") String name,
            @Valid @NotNull DatabaseServerChangeInstanceTypeV4Request request);

    @PUT
    @Path("/crn/{crn}/dbserver/change_instance_type")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = CHANGE_DATABASE_SERVER_INSTANCE_TYPE_BY_CRN, operationId = "changeDistroXDatabaseServerInstanceTypeByCrn",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    DatabaseServerChangeInstanceTypeV4Response changeDatabaseServerInstanceTypeByCrn(
            @ValidCrn(resource = CrnResourceDescriptor.DATAHUB) @PathParam("crn") String crn,
            @Valid @NotNull DatabaseServerChangeInstanceTypeV4Request request);

    @PUT
    @Path("/name/{name}/dbserver/increase_storage_size")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = INCREASE_DATABASE_SERVER_STORAGE_SIZE_BY_NAME, operationId = "increaseDistroXDatabaseServerStorageSizeByName",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    DatabaseServerIncreaseStorageSizeV4Response increaseDatabaseServerStorageSizeByName(@PathParam("name") String name,
            @Valid @NotNull DatabaseServerIncreaseStorageSizeV4Request request);

    @PUT
    @Path("/crn/{crn}/dbserver/increase_storage_size")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = INCREASE_DATABASE_SERVER_STORAGE_SIZE_BY_CRN, operationId = "increaseDistroXDatabaseServerStorageSizeByCrn",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    DatabaseServerIncreaseStorageSizeV4Response increaseDatabaseServerStorageSizeByCrn(
            @ValidCrn(resource = CrnResourceDescriptor.DATAHUB) @PathParam("crn") String crn,
            @Valid @NotNull DatabaseServerIncreaseStorageSizeV4Request request);

    @GET
    @Path("/name/{name}/dbserver/instance_types")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = GET_DATABASE_SERVER_VALID_INSTANCE_TYPE_OPTIONS_BY_NAME, operationId = "getDistroXDatabaseServerValidInstanceTypeOptionsByName",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    DatabaseServerValidInstanceTypeOptionsV4Response getDatabaseServerValidInstanceTypeOptionsByName(@PathParam("name") String name);

    @GET
    @Path("/crn/{crn}/dbserver/instance_types")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = GET_DATABASE_SERVER_VALID_INSTANCE_TYPE_OPTIONS_BY_CRN, operationId = "getDistroXDatabaseServerValidInstanceTypeOptionsByCrn",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    DatabaseServerValidInstanceTypeOptionsV4Response getDatabaseServerValidInstanceTypeOptionsByCrn(
            @ValidCrn(resource = CrnResourceDescriptor.DATAHUB) @PathParam("crn") String crn);
}
