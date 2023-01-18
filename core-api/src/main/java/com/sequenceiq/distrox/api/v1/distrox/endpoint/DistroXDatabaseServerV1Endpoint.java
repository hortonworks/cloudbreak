package com.sequenceiq.distrox.api.v1.distrox.endpoint;

import static com.sequenceiq.distrox.api.v1.distrox.doc.DistroXOpDescription.GET_DATABASE_SERVER_BY_CLUSTER_CRN;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.springframework.validation.annotation.Validated;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.database.StackDatabaseServerResponse;
import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;
import com.sequenceiq.cloudbreak.validation.ValidCrn;

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
}
