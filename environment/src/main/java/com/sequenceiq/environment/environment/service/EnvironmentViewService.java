package com.sequenceiq.environment.environment.service;

import static com.sequenceiq.cloudbreak.common.exception.NotFoundException.notFound;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.validation.constraints.NotNull;

import org.slf4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.sequenceiq.environment.environment.EnvironmentDeletionType;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.domain.EnvironmentView;
import com.sequenceiq.environment.environment.repository.EnvironmentViewRepository;

@Service
public class EnvironmentViewService {

    private static final Logger LOGGER = getLogger(EnvironmentViewService.class);

    private final EnvironmentViewRepository environmentViewRepository;

    public EnvironmentViewService(EnvironmentViewRepository environmentViewRepository) {
        this.environmentViewRepository = environmentViewRepository;
    }

    public Set<EnvironmentView> findByNamesInAccount(Set<String> names, @NotNull String accountid) {
        return CollectionUtils.isEmpty(names) ? new HashSet<>() : environmentViewRepository
                .findAllByNameInAndAccountIdAndArchivedIsFalse(names, accountid);
    }

    public Set<EnvironmentView> findByResourceCrnsInAccount(Set<String> resourceCrns, @NotNull String accountid) {
        return CollectionUtils.isEmpty(resourceCrns) ? new HashSet<>() : environmentViewRepository
                .findAllByResourceCrnInAndAccountIdAndArchivedIsFalse(resourceCrns, accountid);
    }

    public Set<EnvironmentView> findAllByAccountId(String accountId) {
        return environmentViewRepository
                .findAllByAccountId(accountId);
    }

    public Set<EnvironmentView> findAllByCredentialId(Long credentialId) {
        return environmentViewRepository
                .findAllByCredentialIdAndArchivedIsFalse(credentialId);
    }

    public Long getIdByName(String environmentName, String accountId) {
        return Optional.ofNullable(environmentViewRepository
                        .getIdByNameAndAccountIdAndArchivedIsFalse(environmentName, accountId))
                .orElseThrow(notFound("Environment with name", environmentName));
    }

    public Long getIdByCrn(String crn, String accountId) {
        return Optional.ofNullable(environmentViewRepository
                        .getIdByResourceCrnAndAccountIdAndArchivedIsFalse(crn, accountId))
                .orElseThrow(notFound("Environment with CRN", crn));
    }

    public EnvironmentView getByCrnAndAccountId(String crn, String accountId) {
        return environmentViewRepository
                .findByResourceCrnAndAccountIdAndArchivedIsFalse(crn, accountId)
                .orElseThrow(notFound("Environment with CRN", crn));
    }

    public EnvironmentView getById(Long id) {
        return environmentViewRepository.findByIdAndArchivedIsFalse(id)
                .orElseThrow(notFound("Environment with id", id));
    }

    public Optional<EnvironmentView> getByIdOpt(Long id) {
        return environmentViewRepository.findByIdAndArchivedIsFalse(id);
    }

    public EnvironmentView getByNameAndAccountId(String name, String accountId) {
        return environmentViewRepository
                .findByNameAndAccountIdAndArchivedIsFalse(name, accountId)
                .orElseThrow(notFound("Environment with name", name));
    }

    public void editDeletionType(EnvironmentView environment, boolean forced) {
        EnvironmentDeletionType deletionType = forced ? EnvironmentDeletionType.FORCE : EnvironmentDeletionType.SIMPLE;
        LOGGER.debug("Editing deletion type to {} for environment.", deletionType);
        int count = environmentViewRepository.updateDeletionTypeById(environment.getId(), deletionType);
        if (count == 1) {
            environment.setDeletionType(deletionType);
            LOGGER.debug("Environment deletion type updated successfully.");
        }
    }

    public void editStatusAndStatusReason(EnvironmentView environment, EnvironmentStatus status, String statusReason) {
        LOGGER.debug("Update environment status and status reason to {}, {}", status, statusReason);
        int count = environmentViewRepository.updateStatusAndStatusReasonById(environment.getId(), status, statusReason);
        if (count == 1) {
            environment.setStatusReason(statusReason);
            environment.setStatus(status);
            LOGGER.debug("Environment status and status reason updated successfully.");
        }
    }

    public List<String> findNameWithAccountIdAndParentEnvIdAndArchivedIsFalse(String accountId, Long parentEnvironmentId) {
        return environmentViewRepository.findNameWithAccountIdAndParentEnvIdAndArchivedIsFalse(accountId, parentEnvironmentId);
    }
}
