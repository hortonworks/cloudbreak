package com.sequenceiq.sdx.api.endpoint;

import javax.validation.constraints.NotNull;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.springframework.validation.annotation.Validated;

import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.responses.DbConnectionParamsV4Response;
import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Validated
@Path("/dbconfig")
@RetryAndMetrics
@Tag(name = "/dbconfig")
public interface DatabaseConfigEndpoint {

    @GET
    @Path("connectionparams")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "get datalake database config", operationId = "getDatalakeDbConfig",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    DbConnectionParamsV4Response getDbConfig(@QueryParam("datalakeCrn") @NotNull String datalakeCrn,
            @QueryParam("databaseType") @NotNull DatabaseType databaseType);
}
