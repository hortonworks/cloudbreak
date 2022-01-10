package com.sequenceiq.freeipa.service.stack;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.sequenceiq.authorization.resource.AuthorizationResourceType;
import com.sequenceiq.authorization.service.ResourcePropertyProvider;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.dto.StackIdWithStatus;
import com.sequenceiq.freeipa.entity.ImageEntity;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.repository.StackRepository;

@Service
public class StackService implements ResourcePropertyProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackService.class);

    @VisibleForTesting
    Supplier<LocalDateTime> nowSupplier = LocalDateTime::now;

    @Inject
    private StackRepository stackRepository;

    @Inject
    private ChildEnvironmentService childEnvironmentService;

    public List<Stack> findAllRunning() {
        return stackRepository.findAllRunning();
    }

    public List<Stack> findAllForAutoSync() {
        return stackRepository.findAllRunningAndStatusIn(List.of(
                Status.AVAILABLE,
                Status.UPDATE_FAILED,
                Status.START_FAILED,
                Status.STOP_FAILED,
                Status.UNREACHABLE,
                Status.UNHEALTHY,
                Status.UNKNOWN,
                Status.STOPPED,
                Status.START_IN_PROGRESS,
                Status.STOP_IN_PROGRESS,
                Status.STOP_REQUESTED,
                Status.START_REQUESTED));
    }

    public Stack getByIdWithListsInTransaction(Long id) {
        return stackRepository.findOneWithLists(id).orElseThrow(() -> new NotFoundException(String.format("FreeIPA stack [%s] not found", id)));
    }

    public Stack getStackById(Long id) {
        return stackRepository.findById(id).orElseThrow(() -> new NotFoundException(String.format("FreeIPA stack [%s] not found", id)));
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
    public Optional<AuthorizationResourceType> getSupportedAuthorizationResourceType() {
        return Optional.of(AuthorizationResourceType.ENVIRONMENT);
    }

    @Override
    public Map<String, Optional<String>> getNamesByCrns(Collection<String> crns) {
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

    public List<ImageEntity> getImagesOfAliveStacks(Integer thresholdInDays) {
        final LocalDateTime thresholdDate = nowSupplier.get()
                .minusDays(Optional.ofNullable(thresholdInDays).orElse(0));
        final long thresholdTimestamp = Timestamp.valueOf(thresholdDate).getTime();
        return stackRepository.findImagesOfAliveStacks(thresholdTimestamp);
    }

    public Stack getFreeIpaStackWithMdcContext(String envCrn, String accountId) {
        LOGGER.debug("Looking for stack using env:{} and accountId:{}", envCrn, accountId);
        Stack stack = getByEnvironmentCrnAndAccountId(envCrn, accountId);
        MDCBuilder.buildMdcContext(stack);
        LOGGER.debug("Stack is fetched for env:{} and accountId:{} ", envCrn, accountId);
        return stack;
    }
}
