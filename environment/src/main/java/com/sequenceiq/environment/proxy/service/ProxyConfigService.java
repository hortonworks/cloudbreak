package com.sequenceiq.environment.proxy.service;

import static com.sequenceiq.cloudbreak.common.exception.NotFoundException.notFound;

import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.ws.rs.BadRequestException;

import org.springframework.stereotype.Service;

import com.google.common.collect.Sets;
import com.sequenceiq.authorization.resource.AuthorizationResourceType;
import com.sequenceiq.authorization.service.CompositeAuthResourcePropertyProvider;
import com.sequenceiq.authorization.service.OwnerAssignmentService;
import com.sequenceiq.authorization.service.list.ResourceWithId;
import com.sequenceiq.authorization.service.model.projection.ResourceCrnAndNameView;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareCrnGenerator;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.environment.environment.domain.EnvironmentView;
import com.sequenceiq.environment.environment.service.EnvironmentViewService;
import com.sequenceiq.environment.proxy.domain.ProxyConfig;
import com.sequenceiq.environment.proxy.repository.ProxyConfigRepository;

@Service
public class ProxyConfigService implements CompositeAuthResourcePropertyProvider {

    private final ProxyConfigRepository proxyConfigRepository;

    private final RegionAwareCrnGenerator regionAwareCrnGenerator;

    private final OwnerAssignmentService ownerAssignmentService;

    private final TransactionService transactionService;

    private final ProxyConfigModificationService proxyConfigModificationService;

    private final EnvironmentViewService environmentService;

    public ProxyConfigService(ProxyConfigRepository proxyConfigRepository,
            RegionAwareCrnGenerator regionAwareCrnGenerator,
            OwnerAssignmentService ownerAssignmentService,
            TransactionService transactionService,
            ProxyConfigModificationService proxyConfigModificationService,
            EnvironmentViewService environmentService) {
        this.proxyConfigRepository = proxyConfigRepository;
        this.regionAwareCrnGenerator = regionAwareCrnGenerator;
        this.ownerAssignmentService = ownerAssignmentService;
        this.transactionService = transactionService;
        this.proxyConfigModificationService = proxyConfigModificationService;
        this.environmentService = environmentService;
    }

    public ProxyConfig get(Long id) {
        return proxyConfigRepository.findById(id).orElseThrow(notFound("Proxy configuration", id));
    }

    public ProxyConfig deleteByNameInAccount(String name, String accountId) {
        ProxyConfig proxyConfig = proxyConfigRepository.findByNameInAccount(name, accountId)
                .orElseThrow(notFound("Proxy config with name:", name));
        checkForProxyConfigIfUsedByAnyEnvironment(proxyConfig);
        return proxyConfig;
    }

    public ProxyConfig deleteByCrnInAccount(String crn, String accountId) {
        ProxyConfig proxyConfig = proxyConfigRepository.findByResourceCrnInAccount(crn, accountId)
                .orElseThrow(notFound("Proxy config with crn:", crn));
        checkForProxyConfigIfUsedByAnyEnvironment(proxyConfig);
        return proxyConfig;
    }

    public Set<ProxyConfig> deleteMultipleInAccount(Set<String> names, String accountId) {
        Set<ProxyConfig> toBeDeleted = getByNamesForAccountId(names, accountId);
        toBeDeleted.forEach(this::checkForProxyConfigIfUsedByAnyEnvironment);
        return toBeDeleted;
    }

    private void checkForProxyConfigIfUsedByAnyEnvironment(ProxyConfig proxyConfig) {
        MDCBuilder.buildMdcContext(proxyConfig);
        Set<EnvironmentView> environments = environmentService.findAllByProxyConfigIdAndArchivedIsFalse(proxyConfig.getId());
        if (!environments.isEmpty()) {
            String environmentList = environments.stream().map(EnvironmentView::getName).collect(Collectors.joining(", "));
            String message = "Proxy Configuration '%s' cannot be deleted because the following environments are using it: [%s].";
            throw new BadRequestException(String.format(message, proxyConfig.getName(), environmentList));
        }
        proxyConfigRepository.delete(proxyConfig);
        ownerAssignmentService.notifyResourceDeleted(proxyConfig.getResourceCrn());
    }

    private Set<ProxyConfig> getByNamesForAccountId(Set<String> names, String accountId) {
        Set<ProxyConfig> results = proxyConfigRepository.findByNameOrResourceCrnInAccount(names, accountId);
        Set<String> notFound = Sets.difference(names,
                results.stream().map(ProxyConfig::getName).collect(Collectors.toSet()));

        if (!notFound.isEmpty()) {
            throw new NotFoundException(String.format("No proxy config found with name(s) '%s'",
                    notFound.stream().map(name -> '\'' + name + '\'').collect(Collectors.joining(", "))));
        }

        return results;
    }

    public ProxyConfig getByNameForAccountId(String name, String accountId) {
        return proxyConfigRepository.findByNameInAccount(name, accountId)
                .orElseThrow(notFound("No proxy config found with name", name));
    }

    public ProxyConfig getByCrn(String crn) {
        String accountId = Crn.safeFromString(crn).getAccountId();
        return getByCrnForAccountId(crn, accountId);
    }

    public ProxyConfig getByCrnForAccountId(String crn, String accountId) {
        return proxyConfigRepository.findByResourceCrnInAccount(crn, accountId)
                .orElseThrow(notFound("No proxy config found with crn", crn));
    }

    public Optional<ProxyConfig> getOptionalByEnvironmentCrn(String environmentCrn) {
        String accountId = Crn.safeFromString(environmentCrn).getAccountId();
        return proxyConfigRepository.findByEnvironmentCrnAndAccountId(environmentCrn, accountId);
    }

    public ProxyConfig getByEnvironmentCrnAndAccountId(String environmentCrn, String accountId) {
        return proxyConfigRepository.findByEnvironmentCrnAndAccountId(environmentCrn, accountId)
                .orElseThrow(notFound("ProxyConfig for environmentCrn:", environmentCrn));
    }

    public Set<ProxyConfig> listInAccount(String accountId) {
        return proxyConfigRepository.findAllInAccount(accountId);
    }

    public ProxyConfig create(ProxyConfig proxyConfig, String accountId, String creator) {
        if (proxyConfigRepository.findResourceCrnByNameAndTenantId(proxyConfig.getName(), accountId).isPresent()) {
            throw new BadRequestException(String.format("Proxy config with name %s already exists in account %s",
                    proxyConfig.getName(), accountId));
        }
        proxyConfig.setResourceCrn(createCRN(accountId));
        proxyConfig.setCreator(creator);
        proxyConfig.setAccountId(accountId);
        ownerAssignmentService.assignResourceOwnerRoleIfEntitled(creator, proxyConfig.getResourceCrn());
        try {
            return transactionService.required(() -> proxyConfigRepository.save(proxyConfig));
        } catch (TransactionService.TransactionExecutionException e) {
            ownerAssignmentService.notifyResourceDeleted(proxyConfig.getResourceCrn());
            throw e.getCause();
        }
    }

    public List<ResourceWithId> getProxyResources() {
        return proxyConfigRepository.findAuthorizationResourcesByAccountId(ThreadBasedUserCrnProvider.getAccountId());
    }

    private String createCRN(String accountId) {
        return regionAwareCrnGenerator.generateCrnStringWithUuid(CrnResourceDescriptor.PROXY, accountId);
    }

    @Override
    public List<String> getResourceCrnListByResourceNameList(List<String> resourceNames) {
        return proxyConfigRepository.findAllResourceCrnsByNamesAndTenantId(resourceNames, ThreadBasedUserCrnProvider.getAccountId());
    }

    @Override
    public String getResourceCrnByResourceName(String resourceName) {
        return proxyConfigRepository.findResourceCrnByNameAndTenantId(resourceName, ThreadBasedUserCrnProvider.getAccountId())
                .orElseThrow(notFound("Proxy config", resourceName));
    }

    @Override
    public AuthorizationResourceType getSupportedAuthorizationResourceType() {
        return AuthorizationResourceType.PROXY;
    }

    @Override
    public Map<String, Optional<String>> getNamesByCrnsForMessage(Collection<String> crns) {
        return proxyConfigRepository.findAllResourceNamesByCrnsAndTenantId(crns, ThreadBasedUserCrnProvider.getAccountId())
                .stream()
                .collect(Collectors.toMap(ResourceCrnAndNameView::getCrn, nameAndCrnView -> Optional.of(nameAndCrnView.getName())));
    }

    @Override
    public EnumSet<Crn.ResourceType> getSupportedCrnResourceTypes() {
        return EnumSet.of(Crn.ResourceType.PROXY_CONFIG);
    }
}
