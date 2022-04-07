package com.sequenceiq.consumption.service;

import static com.sequenceiq.cloudbreak.common.exception.NotFoundException.notFound;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.common.dal.repository.AccountAwareResourceRepository;
import com.sequenceiq.cloudbreak.common.event.PayloadContext;
import com.sequenceiq.cloudbreak.common.service.account.AbstractAccountAwareResourceService;
import com.sequenceiq.consumption.api.v1.consumption.model.request.StorageConsumptionScheduleRequest;
import com.sequenceiq.consumption.api.v1.consumption.model.request.StorageConsumptionUnscheduleRequest;
import com.sequenceiq.consumption.domain.Consumption;
import com.sequenceiq.consumption.configuration.repository.ConsumptionRepository;
import com.sequenceiq.flow.core.PayloadContextProvider;
import com.sequenceiq.flow.core.ResourceIdProvider;

@Service
public class ConsumptionService extends AbstractAccountAwareResourceService<Consumption> implements ResourceIdProvider,
        PayloadContextProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConsumptionService.class);

    @Value("${consumption.admin.group.default.prefix:}")
    private String adminGroupNamePrefix;

    private final ConsumptionRepository consumptionRepository;

    public ConsumptionService(ConsumptionRepository consumptionRepository) {
        this.consumptionRepository = consumptionRepository;
    }

    @Override
    public Long getResourceIdByResourceCrn(String resourceCrn) {
        return consumptionRepository.findIdByResourceCrnAndAccountId(resourceCrn, ThreadBasedUserCrnProvider.getAccountId())
                .orElseThrow(notFound("Consumption with crn:", resourceCrn));
    }

    @Override
    public Long getResourceIdByResourceName(String resourceName) {
        return consumptionRepository.findIdByNameAndAccountId(resourceName, ThreadBasedUserCrnProvider.getAccountId())
                .orElseThrow(notFound("Consumption with name:", resourceName));
    }

    @Override
    public PayloadContext getPayloadContext(Long resourceId) {
        return null;
    }

    @Override
    protected AccountAwareResourceRepository<Consumption, Long> repository() {
        return consumptionRepository;
    }

    @Override
    protected void prepareDeletion(Consumption resource) {
    }

    @Override
    protected void prepareCreation(Consumption resource) {
    }

    public Optional<Consumption> getById(Long environmentId) {
        return Optional.empty();
    }

    public void scheduleStorageConsumptionCollection(StorageConsumptionScheduleRequest request) {
    }

    public void unscheduleStorageConsumptionCollection(StorageConsumptionUnscheduleRequest request) {
    }
}
