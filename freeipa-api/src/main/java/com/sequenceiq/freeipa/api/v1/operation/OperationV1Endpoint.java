package com.sequenceiq.freeipa.api.v1.operation;

import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.auth.security.internal.AccountId;
import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;
import com.sequenceiq.flow.api.model.operation.OperationView;
import com.sequenceiq.freeipa.api.v1.operation.doc.OperationDescriptions;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationStatus;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@RetryAndMetrics
@Path("/v1/operation")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v1/operation", description = "Manage operations in FreeIPA", protocols = "http,https", consumes = MediaType.APPLICATION_JSON)
public interface OperationV1Endpoint {

    @GET
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.OPERATION_STATUS, notes = OperationDescriptions.NOTES, produces = MediaType.APPLICATION_JSON,
            nickname = "getOperationStatusV1")
    OperationStatus getOperationStatus(@NotNull @QueryParam("operationId") String operationId,
            @AccountId @QueryParam("accountId") String accountId);

    @GET
    @Path("/resource/crn/{environmentCrn}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.GET_OPERATION, produces = "application/json", notes = OperationDescriptions.NOTES,
            nickname = "getOperationProgressByEnvironmentCrn")
    OperationView getOperationProgressByEnvironmentCrn(@PathParam("environmentCrn") String environmentCrn,
            @DefaultValue("false") @QueryParam("detailed") boolean detailed);
}
