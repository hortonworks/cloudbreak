package com.sequenceiq.cloudbreak.api.endpoint.v4.database;

import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import org.springframework.validation.annotation.Validated;

import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.responses.DbConnectionParamsV4Response;
import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Validated
@Path("/v4/dbconfig")
@RetryAndMetrics
@Tag(name = "/v4/dbconfig")
public interface DatabaseConfigV4Endpoint {

    @GET
    @Path("connectionparams")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "get database config", operationId = "getDbConfig",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    DbConnectionParamsV4Response getDbConfig(@QueryParam("stackCrn") @NotNull String stackCrn, @QueryParam("databaseType") @NotNull DatabaseType databaseType);
}
