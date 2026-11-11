package com.sequenceiq.sdx.api.endpoint;

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
@Path("/sdx")
@RetryAndMetrics
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "/sdx")
public interface DatabaseServerEndpoint {

    @GET
    @Path("/crn/{clusterCrn}/dbserver")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "get database server for Data Lake cluster by cluster crn", operationId = "getDatabaseServerByClusterCrn",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    StackDatabaseServerResponse getDatabaseServerByCrn(@PathParam("clusterCrn") @ValidCrn(resource = CrnResourceDescriptor.VM_DATALAKE) String clusterCrn);

    @GET
    @Path("/database/instance_types")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "list available database instance types for Data Lake", operationId = "listSdxDatabaseInstanceTypes",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    DatabaseInstanceTypesV4Response getDatabaseInstanceTypes(
            @QueryParam("environmentCrn") @NotEmpty @ValidCrn(resource = CrnResourceDescriptor.ENVIRONMENT) String environmentCrn,
            @QueryParam("databaseType") DatabaseCapabilityType databaseType,
            @QueryParam("architecture") String architecture);

    @PUT
    @Path("/name/{name}/dbserver/change_instance_type")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "change the instance type of the database server of the Data Lake by name",
            operationId = "changeSdxDatabaseServerInstanceTypeByName",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    DatabaseServerChangeInstanceTypeV4Response changeDatabaseServerInstanceTypeByName(@PathParam("name") String name,
            @Valid @NotNull DatabaseServerChangeInstanceTypeV4Request request);

    @PUT
    @Path("/crn/{crn}/dbserver/change_instance_type")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "change the instance type of the database server of the Data Lake by crn",
            operationId = "changeSdxDatabaseServerInstanceTypeByCrn",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    DatabaseServerChangeInstanceTypeV4Response changeDatabaseServerInstanceTypeByCrn(
            @ValidCrn(resource = CrnResourceDescriptor.VM_DATALAKE) @PathParam("crn") String crn,
            @Valid @NotNull DatabaseServerChangeInstanceTypeV4Request request);

    @PUT
    @Path("/name/{name}/dbserver/increase_storage_size")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "increase the storage size of the database server of the Data Lake by name",
            operationId = "increaseSdxDatabaseServerStorageSizeByName",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    DatabaseServerIncreaseStorageSizeV4Response increaseDatabaseServerStorageSizeByName(@PathParam("name") String name,
            @Valid @NotNull DatabaseServerIncreaseStorageSizeV4Request request);

    @PUT
    @Path("/crn/{crn}/dbserver/increase_storage_size")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "increase the storage size of the database server of the Data Lake by crn",
            operationId = "increaseSdxDatabaseServerStorageSizeByCrn",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    DatabaseServerIncreaseStorageSizeV4Response increaseDatabaseServerStorageSizeByCrn(
            @ValidCrn(resource = CrnResourceDescriptor.VM_DATALAKE) @PathParam("crn") String crn,
            @Valid @NotNull DatabaseServerIncreaseStorageSizeV4Request request);

    @GET
    @Path("/name/{name}/dbserver/instance_types")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "get the valid target instance types of the database server of the Data Lake by name",
            operationId = "getSdxDatabaseServerValidInstanceTypeOptionsByName",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    DatabaseServerValidInstanceTypeOptionsV4Response getDatabaseServerValidInstanceTypeOptionsByName(@PathParam("name") String name);

    @GET
    @Path("/crn/{crn}/dbserver/instance_types")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "get the valid target instance types of the database server of the Data Lake by crn",
            operationId = "getSdxDatabaseServerValidInstanceTypeOptionsByCrn",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    DatabaseServerValidInstanceTypeOptionsV4Response getDatabaseServerValidInstanceTypeOptionsByCrn(
            @ValidCrn(resource = CrnResourceDescriptor.VM_DATALAKE) @PathParam("crn") String crn);
}
