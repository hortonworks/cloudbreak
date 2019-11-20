package com.sequenceiq.freeipa.api.v1.operation;

import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.jerseyclient.retry.RetryingRestClient;
import com.sequenceiq.freeipa.api.v1.operation.doc.OperationDescriptions;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationStatus;
import com.sequenceiq.service.api.doc.ContentType;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@RetryingRestClient
@Path("/v1/operation")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v1/operation", description = "Manage operations in FreeIPA", protocols = "http,https")
public interface OperationV1Endpoint {

    @GET
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.OPERATION_STATUS, notes = OperationDescriptions.NOTES, produces = ContentType.JSON,
            nickname = "getOperationStatusV1")
    OperationStatus getOperationStatus(@NotNull @QueryParam("operationId") String operationId);
}
