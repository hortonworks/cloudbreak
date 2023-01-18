package com.sequenceiq.consumption.api.v1.consumption.endpoint;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.auth.security.internal.AccountId;
import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;
import com.sequenceiq.cloudbreak.validation.ValidCrn;
import com.sequenceiq.consumption.api.doc.ConsumptionOpDescription;
import com.sequenceiq.consumption.api.v1.consumption.model.request.CloudResourceConsumptionRequest;
import com.sequenceiq.consumption.api.v1.consumption.model.request.StorageConsumptionRequest;
import com.sequenceiq.consumption.api.v1.consumption.model.response.ConsumptionExistenceResponse;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;

@Path("/v1/internal/consumption")
@RetryAndMetrics
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "/v1/internal/consumption")
public interface ConsumptionInternalEndpoint {

    @POST
    @Path("schedule/storage")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary =  ConsumptionOpDescription.SCHEDULE_STORAGE, operationId ="scheduleStorageCollection")
    void scheduleStorageConsumptionCollection(@AccountId @QueryParam("accountId") String accountId,
            @Valid @NotNull StorageConsumptionRequest request,
            @ValidCrn(resource = { CrnResourceDescriptor.USER, CrnResourceDescriptor.MACHINE_USER })
            @QueryParam("initiatorUserCrn") @NotEmpty String initiatorUserCrn);

    @DELETE
    @Path("unschedule/storage")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary =  ConsumptionOpDescription.UNSCHEDULE_STORAGE, operationId ="unscheduleStorageCollection")
    void unscheduleStorageConsumptionCollection(@AccountId @QueryParam("accountId") String accountId,
            @NotNull @ValidCrn(resource = {CrnResourceDescriptor.ENVIRONMENT, CrnResourceDescriptor.DATALAKE})
            @QueryParam("monitoredResourceCrn") String monitoredResourceCrn, @NotEmpty @QueryParam("storageLocation") String storageLocation,
            @ValidCrn(resource = { CrnResourceDescriptor.USER, CrnResourceDescriptor.MACHINE_USER })
            @QueryParam("initiatorUserCrn") @NotEmpty String initiatorUserCrn);

    @GET
    @Path("exists/storage")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary =  ConsumptionOpDescription.STORAGE_EXISTS, operationId ="storageCollectionExists")
    ConsumptionExistenceResponse doesStorageConsumptionCollectionExist(@AccountId @QueryParam("accountId") String accountId,
            @NotNull @ValidCrn(resource = {CrnResourceDescriptor.ENVIRONMENT, CrnResourceDescriptor.DATALAKE})
            @QueryParam("monitoredResourceCrn") String monitoredResourceCrn, @NotEmpty @QueryParam("storageLocation") String storageLocation,
            @ValidCrn(resource = { CrnResourceDescriptor.USER, CrnResourceDescriptor.MACHINE_USER })
            @QueryParam("initiatorUserCrn") @NotEmpty String initiatorUserCrn);

    @POST
    @Path("schedule")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary =  ConsumptionOpDescription.SCHEDULE_CLOUD_RESOURCE,
            operationId = "scheduleConsumtpionCollection")
    void scheduleCloudResourceConsumptionCollection(@AccountId @QueryParam("accountId") String accountId,
            @Valid @NotNull CloudResourceConsumptionRequest request,
            @ValidCrn(resource = { CrnResourceDescriptor.USER, CrnResourceDescriptor.MACHINE_USER })
            @QueryParam("initiatorUserCrn") @NotEmpty String initiatorUserCrn);

    @DELETE
    @Path("{monitoredResourceCrn}/unschedule")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary =  ConsumptionOpDescription.UNSCHEDULE_CLOUD_RESOURCE,
            operationId = "unscheduleConsumptionCollection")
    void unscheduleCloudResourceConsumptionCollection(@AccountId @QueryParam("accountId") String accountId,
            @NotNull @ValidCrn(resource = {CrnResourceDescriptor.DATAHUB, CrnResourceDescriptor.DATALAKE, CrnResourceDescriptor.ENVIRONMENT})
            @PathParam("monitoredResourceCrn") String monitoredResourceCrn,
            @NotEmpty @QueryParam("cloudResourceId") String cloudResourceId,
            @ValidCrn(resource = { CrnResourceDescriptor.USER, CrnResourceDescriptor.MACHINE_USER })
            @QueryParam("initiatorUserCrn") @NotEmpty String initiatorUserCrn);

    @GET
    @Path("{monitoredResourceCrn}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary =  ConsumptionOpDescription.CLOUD_RESOURCE_EXISTS,
            operationId = "ConsumptionCollectionExists")
    ConsumptionExistenceResponse doesCloudResourceConsumptionCollectionExist(@AccountId @QueryParam("accountId") String accountId,
            @NotNull @ValidCrn(resource = {CrnResourceDescriptor.DATAHUB, CrnResourceDescriptor.DATALAKE, CrnResourceDescriptor.ENVIRONMENT})
            @PathParam("monitoredResourceCrn") String monitoredResourceCrn,
            @NotEmpty @QueryParam("cloudResourceId") String cloudResourceId,
            @ValidCrn(resource = { CrnResourceDescriptor.USER, CrnResourceDescriptor.MACHINE_USER })
            @QueryParam("initiatorUserCrn") @NotEmpty String initiatorUserCrn);

}
