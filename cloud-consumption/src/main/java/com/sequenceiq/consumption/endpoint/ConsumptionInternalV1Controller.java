package com.sequenceiq.consumption.endpoint;

import java.util.Optional;

import javax.transaction.Transactional;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.InternalOnly;
import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.auth.security.internal.AccountId;
import com.sequenceiq.cloudbreak.auth.security.internal.InitiatorUserCrn;
import com.sequenceiq.cloudbreak.structuredevent.rest.annotation.AccountEntityType;
import com.sequenceiq.cloudbreak.validation.ValidCrn;
import com.sequenceiq.consumption.api.v1.consumption.endpoint.ConsumptionInternalEndpoint;
import com.sequenceiq.consumption.api.v1.consumption.model.common.ConsumptionType;
import com.sequenceiq.consumption.api.v1.consumption.model.request.CloudResourceConsumptionRequest;
import com.sequenceiq.consumption.api.v1.consumption.model.request.StorageConsumptionRequest;
import com.sequenceiq.consumption.api.v1.consumption.model.response.ConsumptionExistenceResponse;
import com.sequenceiq.consumption.domain.Consumption;
import com.sequenceiq.consumption.dto.ConsumptionCreationDto;
import com.sequenceiq.consumption.endpoint.converter.ConsumptionApiConverter;
import com.sequenceiq.consumption.job.storage.StorageConsumptionJobService;
import com.sequenceiq.consumption.service.ConsumptionService;

@Controller
@Transactional(Transactional.TxType.NEVER)
@AccountEntityType(Consumption.class)
public class ConsumptionInternalV1Controller implements ConsumptionInternalEndpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConsumptionInternalV1Controller.class);

    private final ConsumptionService consumptionService;

    private final ConsumptionApiConverter consumptionApiConverter;

    private final StorageConsumptionJobService jobService;

    public ConsumptionInternalV1Controller(ConsumptionService consumptionService, ConsumptionApiConverter consumptionApiConverter,
            StorageConsumptionJobService jobService) {
        this.consumptionService = consumptionService;
        this.consumptionApiConverter = consumptionApiConverter;
        this.jobService = jobService;
    }

    @Override
    @InternalOnly
    public void scheduleStorageConsumptionCollection(@AccountId String accountId, @Valid @NotNull StorageConsumptionRequest request,
            @ValidCrn(resource = { CrnResourceDescriptor.USER, CrnResourceDescriptor.MACHINE_USER }) @InitiatorUserCrn @NotEmpty String initiatorUserCrn) {
        LOGGER.info("Registering storage consumption collection for resource with CRN [{}] and location [{}]",
                request.getMonitoredResourceCrn(), request.getStorageLocation());
        scheduleStorageConsumption(request);
    }

    @Override
    @InternalOnly
    public void unscheduleStorageConsumptionCollection(@AccountId String accountId,
            @NotNull @ValidCrn(resource = {CrnResourceDescriptor.ENVIRONMENT, CrnResourceDescriptor.DATALAKE}) String monitoredResourceCrn,
            @NotEmpty String storageLocation, @ValidCrn(resource = { CrnResourceDescriptor.USER, CrnResourceDescriptor.MACHINE_USER })
            @InitiatorUserCrn @NotEmpty String initiatorUserCrn) {
        LOGGER.info("Unregistering storage consumption collection for resource with CRN [{}] and location [{}]",
                monitoredResourceCrn, storageLocation);
        unscheduleConsumption(monitoredResourceCrn, storageLocation);
    }

    @Override
    @InternalOnly
    public ConsumptionExistenceResponse doesStorageConsumptionCollectionExist(@AccountId String accountId,
            @NotNull @ValidCrn(resource = {CrnResourceDescriptor.ENVIRONMENT, CrnResourceDescriptor.DATALAKE}) String monitoredResourceCrn,
            @NotEmpty String storageLocation,
            @ValidCrn(resource = { CrnResourceDescriptor.USER, CrnResourceDescriptor.MACHINE_USER }) @InitiatorUserCrn @NotEmpty String initiatorUserCrn) {
        return getConsumptionExistenceResponse(monitoredResourceCrn, storageLocation);
    }

    @Override
    @InternalOnly
    public void scheduleConsumptionCollection(@AccountId String accountId,
            @Valid @NotNull CloudResourceConsumptionRequest request,
            @ValidCrn(resource = {CrnResourceDescriptor.USER, CrnResourceDescriptor.MACHINE_USER}) @NotEmpty String initiatorUserCrn) {
        LOGGER.info("Registering consumption collection for resource with CRN [{}] and location [{}]",
                request.getMonitoredResourceCrn(), request.getCloudResourceId());
        scheduleCloudResourceConsumption(request, request.getConsumptionType());
    }

    @Override
    @InternalOnly
    public void unscheduleConsumptionCollection(@AccountId String accountId,
            @NotNull @ValidCrn(resource = {CrnResourceDescriptor.DATAHUB, CrnResourceDescriptor.DATALAKE, CrnResourceDescriptor.ENVIRONMENT})
                    String monitoredResourceCrn, @NotEmpty String cloudResourceId,
            @ValidCrn(resource = {CrnResourceDescriptor.USER, CrnResourceDescriptor.MACHINE_USER}) @NotEmpty String initiatorUserCrn) {
        LOGGER.info("Unregistering consumption collection for resource with CRN [{}] and location [{}]",
                monitoredResourceCrn, cloudResourceId);
        unscheduleConsumption(monitoredResourceCrn, cloudResourceId);
    }

    @Override
    @InternalOnly
    public ConsumptionExistenceResponse doesConsumptionCollectionExist(@AccountId String accountId,
            @NotNull @ValidCrn(resource = {CrnResourceDescriptor.DATAHUB, CrnResourceDescriptor.DATALAKE, CrnResourceDescriptor.ENVIRONMENT})
                    String monitoredResourceCrn, @NotEmpty String cloudResourceId,
            @ValidCrn(resource = {CrnResourceDescriptor.USER, CrnResourceDescriptor.MACHINE_USER}) @NotEmpty String initiatorUserCrn) {
        return getConsumptionExistenceResponse(monitoredResourceCrn, cloudResourceId);
    }

    private ConsumptionExistenceResponse getConsumptionExistenceResponse(String monitoredResourceCrn, String objectId) {
        ConsumptionExistenceResponse response = new ConsumptionExistenceResponse();
        response.setExists(consumptionService.isConsumptionPresentForLocationAndMonitoredCrn(monitoredResourceCrn, objectId));
        return response;
    }

    private void unscheduleConsumption(String monitoredResourceCrn, String objectId) {
        Optional<Consumption> consumptionOpt =
                consumptionService.findStorageConsumptionByMonitoredResourceCrnAndLocation(monitoredResourceCrn, objectId);
        consumptionOpt.ifPresent(consumption -> {
            jobService.unschedule(consumption);
            consumptionService.delete(consumption);
        });
    }

    private void scheduleStorageConsumption(StorageConsumptionRequest request) {
        ConsumptionCreationDto consumptionCreationDto = consumptionApiConverter.initCreationDtoForStorage(request);
        Optional<Consumption> consumptionOpt = consumptionService.create(consumptionCreationDto);
        consumptionOpt.ifPresent(jobService::schedule);
    }

    private void scheduleCloudResourceConsumption(CloudResourceConsumptionRequest request, ConsumptionType storage) {
        ConsumptionCreationDto consumptionCreationDto = consumptionApiConverter.initCreationDtoForCloudResource(request, storage);
        Optional<Consumption> consumptionOpt = consumptionService.create(consumptionCreationDto);
        consumptionOpt.ifPresent(jobService::schedule);
    }
}
