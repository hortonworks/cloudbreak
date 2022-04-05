package com.sequenceiq.consumption.configuration.service;

import static com.sequenceiq.cloudbreak.common.exception.NotFoundException.notFound;

import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.authorization.service.CompositeAuthResourcePropertyProvider;
import com.sequenceiq.authorization.service.ConsumptionPropertyProvider;
import com.sequenceiq.authorization.service.OwnerAssignmentService;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.auth.altus.service.RoleCrnGenerator;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.common.dal.repository.AccountAwareResourceRepository;
import com.sequenceiq.cloudbreak.common.event.PayloadContext;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.common.service.account.AbstractAccountAwareResourceService;
import com.sequenceiq.consumption.configuration.domain.Consumption;
import com.sequenceiq.consumption.configuration.repository.ConsumptionRepository;
import com.sequenceiq.flow.core.PayloadContextProvider;
import com.sequenceiq.flow.core.ResourceIdProvider;

@Service
public class ConsumptionService extends AbstractAccountAwareResourceService<Consumption> implements ResourceIdProvider, ConsumptionPropertyProvider,
        PayloadContextProvider, CompositeAuthResourcePropertyProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConsumptionService.class);

    @Value("${consumption.admin.group.default.prefix:}")
    private String adminGroupNamePrefix;

    private final OwnerAssignmentService ownerAssignmentService;

    private final GrpcUmsClient grpcUmsClient;

    private final TransactionService transactionService;

    private final RoleCrnGenerator roleCrnGenerator;

    private final ConsumptionRepository consumptionRepository;

    public ConsumptionService(
            OwnerAssignmentService ownerAssignmentService,
            GrpcUmsClient grpcUmsClient,
            TransactionService transactionService,
            RoleCrnGenerator roleCrnGenerator,
            ConsumptionRepository consumptionRepository) {
        this.ownerAssignmentService = ownerAssignmentService;
        this.grpcUmsClient = grpcUmsClient;
        this.transactionService = transactionService;
        this.roleCrnGenerator = roleCrnGenerator;
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
                .orElseThrow(notFound("Environment with name:", resourceName));
    }

    @Override
    public PayloadContext getPayloadContext(Long resourceId) {
        return null;
    }

    @Override
    public String getResourceCrnByResourceName(String resourceName) {
        return "";
    }

    @Override
    public List<String> getResourceCrnListByResourceNameList(List<String> resourceNames) {
        return List.of();
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

    @Override
    public Map<String, Optional<String>> getNamesByCrnsForMessage(Collection<String> crns) {
        return new HashMap<String, Optional<String>>();
    }

    @Override
    public EnumSet<Crn.ResourceType> getSupportedCrnResourceTypes() {
        return EnumSet.of(Crn.ResourceType.ENVIRONMENT);
    }

    public Optional<Consumption> getById(Long environmentId) {
        return Optional.empty();
    }
}
