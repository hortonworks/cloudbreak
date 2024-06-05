package com.sequenceiq.sdx.api.endpoint;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.springframework.validation.annotation.Validated;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.database.StackDatabaseServerResponse;
import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;
import com.sequenceiq.cloudbreak.validation.ValidCrn;

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
    @Operation(summary = "get database server for SDX cluster by cluster crn", operationId = "getDatabaseServerByClusterCrn",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    StackDatabaseServerResponse getDatabaseServerByCrn(@PathParam("clusterCrn") @ValidCrn(resource = CrnResourceDescriptor.VM_DATALAKE) String clusterCrn);
}
