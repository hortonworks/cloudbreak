package com.sequenceiq.consumption.endpoint;

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
import com.sequenceiq.cloudbreak.structuredevent.rest.annotation.AccountEntityType;
import com.sequenceiq.cloudbreak.validation.ValidCrn;
import com.sequenceiq.consumption.api.v1.consumption.endpoint.ConsumptionInternalEndpoint;
import com.sequenceiq.consumption.api.v1.consumption.model.request.StorageConsumptionRequest;
import com.sequenceiq.consumption.domain.Consumption;
import com.sequenceiq.consumption.dto.ConsumptionCreationDto;
import com.sequenceiq.consumption.endpoint.converter.ConsumptionApiConverter;
import com.sequenceiq.consumption.service.ConsumptionService;

@Controller
@Transactional(Transactional.TxType.NEVER)
@AccountEntityType(Consumption.class)
public class ConsumptionInternalV1Controller implements ConsumptionInternalEndpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConsumptionInternalV1Controller.class);

    private final ConsumptionService consumptionService;

    private final ConsumptionApiConverter consumptionApiConverter;

    public ConsumptionInternalV1Controller(ConsumptionService consumptionService, ConsumptionApiConverter consumptionApiConverter) {
        this.consumptionService = consumptionService;
        this.consumptionApiConverter = consumptionApiConverter;
    }

    @Override
    @InternalOnly
    public void scheduleStorageConsumptionCollection(@AccountId String accountId, @Valid @NotNull StorageConsumptionRequest request) {
        ConsumptionCreationDto consumptionCreationDto = consumptionApiConverter.initCreationDtoForStorage(request);
        consumptionService.create(consumptionCreationDto);
    }

    @Override
    @InternalOnly
    public void unscheduleStorageConsumptionCollection(@AccountId String accountId,
            @NotNull @ValidCrn(resource = {CrnResourceDescriptor.ENVIRONMENT, CrnResourceDescriptor.DATALAKE}) String monitoredResourceCrn,
            @NotEmpty String storageLocation) {
        Consumption consumption = consumptionService.findStorageConsumptionByMonitoredResourceCrnAndLocation(monitoredResourceCrn, storageLocation);
        consumptionService.delete(consumption);
    }
}
