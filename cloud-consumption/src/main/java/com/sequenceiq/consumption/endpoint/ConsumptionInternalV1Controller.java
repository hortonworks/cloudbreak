package com.sequenceiq.consumption.endpoint;

import javax.transaction.Transactional;
import javax.validation.Valid;
import javax.validation.ValidationException;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.InternalOnly;
import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.auth.security.internal.AccountId;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.structuredevent.rest.annotation.AccountEntityType;
import com.sequenceiq.cloudbreak.validation.ValidCrn;
import com.sequenceiq.common.model.FileSystemType;
import com.sequenceiq.consumption.api.v1.consumption.endpoint.ConsumptionInternalEndpoint;
import com.sequenceiq.consumption.api.v1.consumption.model.request.StorageConsumptionRequest;
import com.sequenceiq.consumption.api.v1.consumption.model.response.ConsumptionExistenceResponse;
import com.sequenceiq.consumption.domain.Consumption;
import com.sequenceiq.consumption.dto.ConsumptionCreationDto;
import com.sequenceiq.consumption.endpoint.converter.ConsumptionApiConverter;
import com.sequenceiq.consumption.job.storage.StorageConsumptionJobService;
import com.sequenceiq.consumption.service.ConsumptionService;
import com.sequenceiq.consumption.util.CloudStorageLocationUtil;

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
    public void scheduleStorageConsumptionCollection(@AccountId String accountId, @Valid @NotNull StorageConsumptionRequest request) {
        ConsumptionCreationDto consumptionCreationDto = consumptionApiConverter.initCreationDtoForStorage(request);
        try {
            CloudStorageLocationUtil.validateCloudStorageType(FileSystemType.S3, consumptionCreationDto.getStorageLocation());
            LOGGER.info("Registering storage consumption collection for resource with CRN [{}] and location [{}]",
                    consumptionCreationDto.getMonitoredResourceCrn(), consumptionCreationDto.getStorageLocation());
            Consumption consumption = consumptionService.create(consumptionCreationDto);
            jobService.schedule(consumption.getId());
        } catch (ValidationException e) {
            throw new BadRequestException(String.format("Storage location validation failed, error: %s", e.getMessage()));
        }
    }

    @Override
    @InternalOnly
    public void unscheduleStorageConsumptionCollection(@AccountId String accountId,
            @NotNull @ValidCrn(resource = {CrnResourceDescriptor.ENVIRONMENT, CrnResourceDescriptor.DATALAKE}) String monitoredResourceCrn,
            @NotEmpty String storageLocation) {
        LOGGER.info("Deregistering storage consumption collection for resource with CRN [{}] and location [{}]", monitoredResourceCrn, storageLocation);
        Consumption consumption = consumptionService.findStorageConsumptionByMonitoredResourceCrnAndLocation(monitoredResourceCrn, storageLocation);
        jobService.unschedule(consumption.getId().toString());
        consumptionService.delete(consumption);
    }

    @Override
    @InternalOnly
    public ConsumptionExistenceResponse doesStorageConsumptionCollectionExist(@AccountId String accountId,
            @NotNull @ValidCrn(resource = {CrnResourceDescriptor.ENVIRONMENT, CrnResourceDescriptor.DATALAKE}) String monitoredResourceCrn,
            @NotEmpty String storageLocation) {
        ConsumptionExistenceResponse response = new ConsumptionExistenceResponse();
        response.setExists(consumptionService.isConsumptionPresentForLocationAndMonitoredCrn(monitoredResourceCrn, storageLocation));
        return response;
    }
}
