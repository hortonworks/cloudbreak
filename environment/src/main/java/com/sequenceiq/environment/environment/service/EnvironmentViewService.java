package com.sequenceiq.environment.environment.service;

import static com.sequenceiq.cloudbreak.common.exception.NotFoundException.notFound;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.validation.constraints.NotNull;

import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.sequenceiq.environment.api.v1.environment.model.response.SimpleEnvironmentResponse;
import com.sequenceiq.environment.environment.domain.EnvironmentView;
import com.sequenceiq.environment.environment.repository.EnvironmentViewRepository;

@Service
public class EnvironmentViewService {

    private final EnvironmentViewRepository environmentViewRepository;

    private final ConversionService conversionService;

    public EnvironmentViewService(EnvironmentViewRepository environmentViewRepository, ConversionService conversionService) {
        this.environmentViewRepository = environmentViewRepository;
        this.conversionService = conversionService;
    }

    public Set<EnvironmentView> findByNamesInAccount(Set<String> names, @NotNull String accountid) {
        return CollectionUtils.isEmpty(names) ? new HashSet<>() : environmentViewRepository
                .findAllByNameInAndAccountIdAndArchivedIsFalse(names, accountid);
    }

    public Set<EnvironmentView> findByResourceCrnsInAccount(Set<String> resourceCrns, @NotNull String accountid) {
        return CollectionUtils.isEmpty(resourceCrns) ? new HashSet<>() : environmentViewRepository
                .findAllByResourceCrnInAndAccountIdAndArchivedIsFalse(resourceCrns, accountid);
    }

    public Set<SimpleEnvironmentResponse> listByAccountId(String accountId) {
        return findAllByAccountId(accountId).stream()
                .map(env -> conversionService.convert(env, SimpleEnvironmentResponse.class))
                .collect(Collectors.toSet());
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
}
