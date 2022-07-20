package com.sequenceiq.consumption.api.v1.consumption.endpoint;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.auth.security.internal.AccountId;
import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;
import com.sequenceiq.cloudbreak.validation.ValidCrn;
import com.sequenceiq.consumption.api.v1.consumption.model.request.StorageConsumptionRequest;
import com.sequenceiq.consumption.api.doc.ConsumptionOpDescription;
import com.sequenceiq.consumption.api.v1.consumption.model.response.ConsumptionExistenceResponse;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/v1/internal/consumption")
@RetryAndMetrics
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v1/internal/consumption", protocols = "http,https", consumes = MediaType.APPLICATION_JSON)
public interface ConsumptionInternalEndpoint {

    @POST
    @Path("schedule/storage")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ConsumptionOpDescription.SCHEDULE_STORAGE, produces = MediaType.APPLICATION_JSON, nickname = "scheduleStorageCollection")
    void scheduleStorageConsumptionCollection(@AccountId @QueryParam("accountId") String accountId,
            @Valid @NotNull StorageConsumptionRequest request,
            @ValidCrn(resource = CrnResourceDescriptor.USER) @QueryParam("initiatorUserCrn") @NotEmpty String initiatorUserCrn);

    @DELETE
    @Path("unschedule/storage")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ConsumptionOpDescription.UNSCHEDULE_STORAGE, produces = MediaType.APPLICATION_JSON, nickname = "unscheduleStorageCollection")
    void unscheduleStorageConsumptionCollection(@AccountId @QueryParam("accountId") String accountId,
            @NotNull @ValidCrn(resource = {CrnResourceDescriptor.ENVIRONMENT, CrnResourceDescriptor.DATALAKE})
            @QueryParam("monitoredResourceCrn") String monitoredResourceCrn, @NotEmpty @QueryParam("storageLocation") String storageLocation,
            @ValidCrn(resource = CrnResourceDescriptor.USER) @QueryParam("initiatorUserCrn") @NotEmpty String initiatorUserCrn);

    @GET
    @Path("exists/storage")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ConsumptionOpDescription.STORAGE_EXISTS, produces = MediaType.APPLICATION_JSON, nickname = "storageCollectionExists")
    ConsumptionExistenceResponse doesStorageConsumptionCollectionExist(@AccountId @QueryParam("accountId") String accountId,
            @NotNull @ValidCrn(resource = {CrnResourceDescriptor.ENVIRONMENT, CrnResourceDescriptor.DATALAKE})
            @QueryParam("monitoredResourceCrn") String monitoredResourceCrn, @NotEmpty @QueryParam("storageLocation") String storageLocation,
            @ValidCrn(resource = CrnResourceDescriptor.USER) @QueryParam("initiatorUserCrn") @NotEmpty String initiatorUserCrn);
}
