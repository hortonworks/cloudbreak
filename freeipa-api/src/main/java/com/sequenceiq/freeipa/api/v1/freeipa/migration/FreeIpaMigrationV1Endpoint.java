package com.sequenceiq.freeipa.api.v1.freeipa.migration;

import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;
import com.sequenceiq.freeipa.api.v1.freeipa.migration.model.FreeIpaMultiAzMigrationV1Request;
import com.sequenceiq.freeipa.api.v1.freeipa.migration.model.FreeIpaMultiAzMigrationV1Response;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.doc.FreeIpaNotes;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.doc.FreeIpaOperationDescriptions;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RetryAndMetrics
@Path("/v1/freeipa/migration")
@Tag(name = "/v1/freeipa/migration")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public interface FreeIpaMigrationV1Endpoint {

    @POST
    @Path("multiaz")
    @Operation(summary = FreeIpaOperationDescriptions.MULTI_AZ_MIGRATION, description = FreeIpaNotes.FREEIPA_NOTES,
            operationId = "migrateFreeIpaToMultiAzV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    FreeIpaMultiAzMigrationV1Response migrateToMultiAz(@Valid FreeIpaMultiAzMigrationV1Request request);

}
