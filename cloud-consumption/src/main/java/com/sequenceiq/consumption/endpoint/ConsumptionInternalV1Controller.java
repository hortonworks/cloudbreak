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
            @ValidCrn(resource = CrnResourceDescriptor.USER) @InitiatorUserCrn @NotEmpty String initiatorUserCrn) {
        ConsumptionCreationDto consumptionCreationDto = consumptionApiConverter.initCreationDtoForStorage(request);
        LOGGER.info("Registering storage consumption collection for resource with CRN [{}] and location [{}]",
                consumptionCreationDto.getMonitoredResourceCrn(), consumptionCreationDto.getStorageLocation());
        Optional<Consumption> consumptionOpt = consumptionService.create(consumptionCreationDto);
        consumptionOpt.ifPresent(consumption -> jobService.schedule(consumption.getId()));
    }

    @Override
    @InternalOnly
    public void unscheduleStorageConsumptionCollection(@AccountId String accountId,
            @NotNull @ValidCrn(resource = {CrnResourceDescriptor.ENVIRONMENT, CrnResourceDescriptor.DATALAKE}) String monitoredResourceCrn,
            @NotEmpty String storageLocation, @ValidCrn(resource = CrnResourceDescriptor.USER) @InitiatorUserCrn @NotEmpty String initiatorUserCrn) {
        LOGGER.info("Unregistering storage consumption collection for resource with CRN [{}] and location [{}]", monitoredResourceCrn, storageLocation);
    Optional<Consumption> consumptionOpt =
                consumptionService.findStorageConsumptionByMonitoredResourceCrnAndLocation(monitoredResourceCrn, storageLocation);
        consumptionOpt.ifPresent(consumption -> {
            jobService.unschedule(consumption.getId().toString());
            consumptionService.delete(consumption);
        });
    }

    @Override
    @InternalOnly
    public ConsumptionExistenceResponse doesStorageConsumptionCollectionExist(@AccountId String accountId,
            @NotNull @ValidCrn(resource = {CrnResourceDescriptor.ENVIRONMENT, CrnResourceDescriptor.DATALAKE}) String monitoredResourceCrn,
            @NotEmpty String storageLocation, @ValidCrn(resource = CrnResourceDescriptor.USER) @InitiatorUserCrn @NotEmpty String initiatorUserCrn) {
        ConsumptionExistenceResponse response = new ConsumptionExistenceResponse();
        response.setExists(consumptionService.isConsumptionPresentForLocationAndMonitoredCrn(monitoredResourceCrn, storageLocation));
        return response;
    }
}
