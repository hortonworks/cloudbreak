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

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Validated
@Path("/dbconfig")
@RetryAndMetrics
@Api(value = "/dbconfig", protocols = "http,https")
public interface DatabaseConfigEndpoint {

    @GET
    @Path("connectionparams")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "get datalake database config", produces = "application/json", nickname = "getDatalakeDbConfig")
    DbConnectionParamsV4Response getDbConfig(
            @QueryParam("datalakeCrn") @NotNull String datalakeCrn,
            @QueryParam("databaseType") @NotNull DatabaseType databaseType);
}
