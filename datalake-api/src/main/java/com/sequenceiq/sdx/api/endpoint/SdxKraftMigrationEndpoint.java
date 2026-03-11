package com.sequenceiq.sdx.api.endpoint;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;
import com.sequenceiq.cloudbreak.validation.ValidCrn;
import com.sequenceiq.distrox.api.v1.distrox.model.KraftMigrationStatusResponse;
import com.sequenceiq.flow.api.model.FlowIdentifier;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RetryAndMetrics
@Path("/sdx")
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "/sdx")
public interface SdxKraftMigrationEndpoint {

    @PUT
    @Path("crn/{crn}/migrate_from_zookeeper_to_kraft")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Initiate the migration from Zookeeper to KRaft broker in Kafka for a DataLake cluster.",
            operationId = "migrateDataLakeFromZookeeperToKraftByCrn",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    FlowIdentifier migrateFromZookeeperToKraftByCrn(@ValidCrn(resource = CrnResourceDescriptor.VM_DATALAKE) @PathParam("crn") String crn);

    @PUT
    @Path("crn/{crn}/finalize_zookeeper_to_kraft_migration")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Finalize the migration from Zookeeper to KRaft broker in Kafka for a DataLake cluster.",
            operationId = "finalizeDataLakeMigrationFromZookeeperToKraftByCrn",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    FlowIdentifier finalizeMigrationFromZookeeperToKraftByCrn(@ValidCrn(resource = CrnResourceDescriptor.VM_DATALAKE) @PathParam("crn") String crn);

    @PUT
    @Path("crn/{crn}/rollback_zookeeper_to_kraft_migration")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Rollback the migration from Zookeeper to KRaft broker in Kafka for a DataLake cluster.",
            operationId = "rollbackDataLakeMigrationFromZookeeperToKraftByCrn",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    FlowIdentifier rollbackMigrationFromZookeeperToKraftByCrn(@ValidCrn(resource = CrnResourceDescriptor.VM_DATALAKE) @PathParam("crn") String crn);

    @GET
    @Path("crn/{crn}/zookeeper_to_kraft_migration_status")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Returns the status of the migration from Zookeeper to KRaft broker in Kafka for a DataLake cluster.",
            operationId = "dataLakeZookeeperToKraftMigrationStatusByCrn",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    KraftMigrationStatusResponse zookeeperToKraftMigrationStatusByCrn(@ValidCrn(resource = CrnResourceDescriptor.VM_DATALAKE) @PathParam("crn") String crn);
}
