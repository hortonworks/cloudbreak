package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
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
@Path("/v4/stacks")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "/v4/stacks")
public interface StackKraftMigrationV4Endpoint {

    @PUT
    @Path("internal/crn/{crn}/migrate_from_zookeeper_to_kraft")
    @Operation(summary = "Initiate the migration from ZooKeeper to KRaft broker in Kafka (internal).",
            operationId = "migrateFromZookeeperToKraftByCrnInternal",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    FlowIdentifier migrateFromZookeeperToKraftByCrnInternal(
            @ValidCrn(resource = {CrnResourceDescriptor.DATAHUB, CrnResourceDescriptor.VM_DATALAKE}) @PathParam("crn") String crn,
            @ValidCrn(resource = {CrnResourceDescriptor.USER, CrnResourceDescriptor.MACHINE_USER}) @QueryParam("initiatorUserCrn") String initiatorUserCrn);

    @PUT
    @Path("internal/crn/{crn}/finalize_zookeeper_to_kraft_migration")
    @Operation(summary = "Finalize the migration from ZooKeeper to KRaft broker in Kafka (internal).",
            operationId = "finalizeMigrationFromZookeeperToKraftByCrnInternal",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    FlowIdentifier finalizeMigrationFromZookeeperToKraftByCrnInternal(
            @ValidCrn(resource = {CrnResourceDescriptor.DATAHUB, CrnResourceDescriptor.VM_DATALAKE}) @PathParam("crn") String crn,
            @ValidCrn(resource = {CrnResourceDescriptor.USER, CrnResourceDescriptor.MACHINE_USER}) @QueryParam("initiatorUserCrn") String initiatorUserCrn);

    @PUT
    @Path("internal/crn/{crn}/rollback_zookeeper_to_kraft_migration")
    @Operation(summary = "Rollback the migration from ZooKeeper to KRaft broker in Kafka (internal).",
            operationId = "rollbackMigrationFromZookeeperToKraftByCrnInternal",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    FlowIdentifier rollbackMigrationFromZookeeperToKraftByCrnInternal(
            @ValidCrn(resource = {CrnResourceDescriptor.DATAHUB, CrnResourceDescriptor.VM_DATALAKE}) @PathParam("crn") String crn,
            @ValidCrn(resource = {CrnResourceDescriptor.USER, CrnResourceDescriptor.MACHINE_USER}) @QueryParam("initiatorUserCrn") String initiatorUserCrn);

    @GET
    @Path("internal/crn/{crn}/zookeeper_to_kraft_migration_status")
    @Operation(summary = "Returns the status of the migration from ZooKeeper to KRaft broker in Kafka (internal).",
            operationId = "zookeeperToKraftMigrationStatusByCrnInternal",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    KraftMigrationStatusResponse zookeeperToKraftMigrationStatusByCrnInternal(
            @ValidCrn(resource = {CrnResourceDescriptor.DATAHUB, CrnResourceDescriptor.VM_DATALAKE}) @PathParam("crn") String crn,
            @ValidCrn(resource = {CrnResourceDescriptor.USER, CrnResourceDescriptor.MACHINE_USER}) @QueryParam("initiatorUserCrn") String initiatorUserCrn);
}
