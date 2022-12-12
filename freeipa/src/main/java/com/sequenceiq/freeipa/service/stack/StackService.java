package com.sequenceiq.freeipa.service.stack;

import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status.CREATE_IN_PROGRESS;
import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status.DELETED_ON_PROVIDER_SIDE;
import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status.DELETE_COMPLETED;
import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status.DELETE_IN_PROGRESS;
import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status.REQUESTED;
import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status.STACK_AVAILABLE;

import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;
import com.sequenceiq.authorization.service.EnvironmentPropertyProvider;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.common.dal.ResourceBasicView;
import com.sequenceiq.cloudbreak.common.event.PayloadContext;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.monitoring.MonitoringEnablementService;
import com.sequenceiq.cloudbreak.quartz.model.JobResource;
import com.sequenceiq.common.api.telemetry.model.Telemetry;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.flow.core.PayloadContextProvider;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.dto.StackIdWithStatus;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.repository.StackRepository;

@Service
public class StackService implements EnvironmentPropertyProvider, PayloadContextProvider, MonitoringEnablementService<Stack> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackService.class);

    @Inject
    private StackRepository stackRepository;

    @Inject
    private ChildEnvironmentService childEnvironmentService;

    public List<Stack> findAllRunning() {
        return stackRepository.findAllRunning();
    }

    public JobResource getJobResource(Long resourceId) {
        return stackRepository.getJobResource(resourceId).orElseThrow(() -> new NotFoundException(String.format("FreeIPA stack [%s] not found", resourceId)));
    }

    public List<JobResource> findAllForAutoSync() {
        return stackRepository.findAllRunningAndStatusNotIn(List.of(
                REQUESTED,
                CREATE_IN_PROGRESS,
                STACK_AVAILABLE,
                DELETE_IN_PROGRESS,
                DELETE_COMPLETED,
                DELETED_ON_PROVIDER_SIDE));
    }

    public Stack getByIdWithListsInTransaction(Long id) {
        return stackRepository.findOneWithLists(id).orElseThrow(() -> new NotFoundException(String.format("FreeIPA stack [%s] not found", id)));
    }

    public Stack getStackById(Long id) {
        return stackRepository.findById(id).orElseThrow(() -> new NotFoundException(String.format("FreeIPA stack [%s] not found", id)));
    }

    @Override
    public PayloadContext getPayloadContext(Long resourceId) {
        return stackRepository.getStackAsPayloadContextById(resourceId).orElse(null);
    }

    public Stack getByEnvironmentCrnAndAccountIdWithListsAndMdcContext(String environmentCrn, String accountId) {
        Stack stack = getByEnvironmentCrnAndAccountIdWithLists(environmentCrn, accountId);
        MDCBuilder.buildMdcContext(stack);
        return stack;
    }

    public Optional<String> findAccountById(Long id) {
        LOGGER.debug("Trying to fetch account ID based on the following id: {}", id);
        Optional<String> crn = stackRepository.findAccountIdByStackId(id);
        crn.ifPresentOrElse(s -> LOGGER.debug("Account ID has found [for stack id: {}]: {}", id, s),
                () -> LOGGER.debug("No account ID has been found for stack id: {}", id));
        return crn;
    }

    public Stack save(Stack stack) {
        return stackRepository.save(stack);
    }

    public Stack getByEnvironmentCrnAndAccountId(String environmentCrn, String accountId) {
        return findByEnvironmentCrnAndAccountId(environmentCrn, accountId)
                .or(() -> childEnvironmentService.findParentByEnvironmentCrnAndAccountId(environmentCrn, accountId))
                .orElseThrow(() -> new NotFoundException(String.format("FreeIPA stack by environment [%s] not found", environmentCrn)));
    }

    public Stack getByCrnAndAccountIdEvenIfTerminated(String environmentCrn, String accountId, String crn) {
        return findByCrnAndAccountIdWithListsEvenIfTerminated(environmentCrn, accountId, crn)
                .orElseThrow(() -> new NotFoundException(String.format("FreeIPA stack by environment [%s] with CRN [%s] not found", environmentCrn, crn)));
    }

    public Optional<Stack> findByEnvironmentCrnAndAccountId(String environmentCrn, String accountId) {
        return stackRepository.findByEnvironmentCrnAndAccountId(environmentCrn, accountId)
                .or(() -> childEnvironmentService.findParentByEnvironmentCrnAndAccountId(environmentCrn, accountId));
    }

    public Optional<Stack> findByCrnAndAccountIdWithListsEvenIfTerminated(String environmentCrn, String accountId, String crn) {
        return stackRepository.findByAccountIdEnvironmentCrnAndCrnWithListsEvenIfTerminated(environmentCrn, accountId, crn)
                .or(() -> childEnvironmentService.findParentStackByChildEnvironmentCrnAndCrnWithListsEvenIfTerminated(environmentCrn, accountId, crn));
    }

    public List<Stack> findMultipleByEnvironmentCrnAndAccountIdEvenIfTerminated(String environmentCrn, String accountId) {
        List<Stack> stacks = stackRepository.findMultipleByEnvironmentCrnAndAccountIdEvenIfTerminated(environmentCrn, accountId);
        if (stacks.isEmpty()) {
            stacks = childEnvironmentService.findMultipleParentStackByChildEnvironmentCrnEvenIfTerminated(environmentCrn, accountId);
        }
        return stacks;
    }

    public List<Stack> findMultipleByEnvironmentCrnAndAccountIdEvenIfTerminatedWithList(String environmentCrn, String accountId) {
        List<Stack> stacks = stackRepository.findMultipleByEnvironmentCrnAndAccountIdEvenIfTerminatedWithList(environmentCrn, accountId);
        if (stacks.isEmpty()) {
            stacks = childEnvironmentService.findMultipleParentStackByChildEnvironmentCrnEvenIfTerminatedWithList(environmentCrn, accountId);
        }
        return stacks;
    }

    public List<Stack> getMultipleByEnvironmentCrnOrChildEnvironmantCrnAndAccountId(Collection<String> environmentCrns, String accountId) {
        if (environmentCrns.isEmpty()) {
            return Lists.newArrayList(getAllByAccountId(accountId));
        } else {
            return stackRepository.findMultipleByEnvironmentCrnOrChildEnvironmentCrnAndAccountId(environmentCrns, accountId);
        }
    }

    public List<Stack> findAllByEnvironmentCrnAndAccountId(String environmentCrn, String accountId) {
        return stackRepository.findAllByEnvironmentCrnAndAccountId(environmentCrn, accountId);
    }

    public List<Long> findAllIdByEnvironmentCrnAndAccountId(String environmentCrn, String accountId) {
        return stackRepository.findAllIdByEnvironmentCrnAndAccountId(environmentCrn, accountId);
    }

    public Long getIdByEnvironmentCrnAndAccountId(String environmentCrn, String accountId) {
        List<Long> ids = stackRepository.findAllIdByEnvironmentCrnAndAccountId(environmentCrn, accountId);
        if (ids.isEmpty()) {
            throw new NotFoundException(String.format("FreeIPA stack by environment [%s] not found", environmentCrn));
        } else if (ids.size() > 1) {
            throw new BadRequestException(String.format("Multiple FreeIPA stack by environment [%s] found", environmentCrn));
        } else {
            return ids.get(0);
        }
    }

    public ResourceBasicView getResourceBasicViewByEnvironmentCrnAndAccountId(String environmentCrn, String accountId) {
        List<ResourceBasicView> views = stackRepository.findAllResourceBasicViewByEnvironmentCrnAndAccountId(environmentCrn, accountId);
        if (views.isEmpty()) {
            throw new NotFoundException(String.format("FreeIPA stack by environment [%s] not found", environmentCrn));
        } else if (views.size() > 1) {
            throw new BadRequestException(String.format("Multiple FreeIPA stack by environment [%s] found", environmentCrn));
        } else {
            return views.get(0);
        }
    }

    public Stack getByEnvironmentCrnAndAccountIdWithLists(String environmentCrn, String accountId) {
        return stackRepository.findByEnvironmentCrnAndAccountIdWithList(environmentCrn, accountId)
                .or(() -> stackRepository.findByChildEnvironmentCrnAndAccountIdWithList(environmentCrn, accountId))
                .orElseThrow(() -> new NotFoundException(String.format("FreeIPA stack by environment [%s] not found", environmentCrn)));
    }

    public Stack getByOwnEnvironmentCrnAndAccountIdWithLists(String environmentCrn, String accountId) {
        return stackRepository.findByEnvironmentCrnAndAccountIdWithList(environmentCrn, accountId)
                .orElseThrow(() -> new NotFoundException(String.format("FreeIPA stack by environment [%s] not found", environmentCrn)));
    }

    public Set<Stack> getAllByAccountId(String accountId) {
        return stackRepository.findByAccountId(accountId);
    }

    public List<StackIdWithStatus> getStatuses(Set<Long> stackIds) {
        return stackRepository.findStackStatusesWithoutAuth(stackIds);
    }

    public List<Stack> findAllWithStatuses(Collection<Status> statuses) {
        return stackRepository.findAllWithStatuses(statuses);
    }

    public List<Stack> findAllWithDetailedStackStatuses(Collection<DetailedStackStatus> detailedStackStatuses) {
        return stackRepository.findAllWithDetailedStackStatuses(detailedStackStatuses);
    }

    public List<Stack> findAllByAccountIdWithStatuses(String accountId, Collection<Status> statuses) {
        return stackRepository.findByAccountIdWithStatuses(accountId, statuses);
    }

    public List<Stack> findMultipleByEnvironmentCrnAndAccountIdWithStatuses(Collection<String> environmentCrns, String accountId, Collection<Status> statuses) {
        if (environmentCrns.isEmpty()) {
            return findAllByAccountIdWithStatuses(accountId, statuses);
        } else {
            return stackRepository.findMultipleByEnvironmentCrnAndAccountIdWithStatuses(environmentCrns, accountId, statuses);
        }
    }

    @Override
    public Map<String, Optional<String>> getNamesByCrnsForMessage(Collection<String> crns) {
        Map<String, Optional<String>> result = new HashMap<>();
        Boolean envType = Optional.ofNullable(crns)
                .map(Collection::stream)
                .flatMap(Stream::findFirst)
                .map(Crn::fromString)
                .map(Crn::getResourceType)
                .map(type -> type == Crn.ResourceType.ENVIRONMENT)
                .orElse(Boolean.FALSE);
        if (envType) {
            stackRepository.findNamesByEnvironmentCrnAndAccountId(crns, ThreadBasedUserCrnProvider.getAccountId())
                    .forEach(nameAndCrn -> result.put(nameAndCrn.getCrn(), Optional.ofNullable(nameAndCrn.getName())));
        } else {
            stackRepository.findNamesByResourceCrnAndAccountId(crns, ThreadBasedUserCrnProvider.getAccountId())
                    .forEach(nameAndCrn -> result.put(nameAndCrn.getCrn(), Optional.ofNullable(nameAndCrn.getName())));
        }
        return result;
    }

    @Override
    public EnumSet<Crn.ResourceType> getSupportedCrnResourceTypes() {
        return EnumSet.of(Crn.ResourceType.FREEIPA, Crn.ResourceType.ENVIRONMENT);
    }

    public Stack getFreeIpaStackWithMdcContext(String envCrn, String accountId) {
        LOGGER.debug("Looking for stack using env:{} and accountId:{}", envCrn, accountId);
        Stack stack = getByEnvironmentCrnAndAccountId(envCrn, accountId);
        MDCBuilder.buildMdcContext(stack);
        LOGGER.debug("Stack is fetched for env:{} and accountId:{} ", envCrn, accountId);
        return stack;
    }

    public int setCcmV2AgentCrnByStackId(Long stackId, String ccmV2AgentCrn) {
        return stackRepository.setCcmV2AgentCrnByStackId(stackId, ccmV2AgentCrn);
    }

    public int setTunnelByStackId(Long stackId, Tunnel tunnel) {
        return stackRepository.setTunnelByStackId(stackId, tunnel);
    }

    @Override
    public Optional<Boolean> computeMonitoringEnabled(Stack entity) {
        Telemetry telemetry = entity.getTelemetry();
        if (telemetry != null) {
            return Optional.of(telemetry.isComputeMonitoringEnabled());
        } else {
            return Optional.empty();
        }
    }
}
