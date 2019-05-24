package com.sequenceiq.environment.proxy.service;

import static com.sequenceiq.cloudbreak.common.exception.NotFoundException.notFound;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.environment.proxy.domain.ProxyConfig;
import com.sequenceiq.environment.proxy.repository.ProxyConfigRepository;

@Service
public class ProxyConfigService  {

    private final ProxyConfigRepository proxyConfigRepository;

    public ProxyConfigService(ProxyConfigRepository proxyConfigRepository) {
        this.proxyConfigRepository = proxyConfigRepository;
    }

    public ProxyConfig get(Long id) {
        return proxyConfigRepository.findById(id).orElseThrow(notFound("Proxy configuration", id));
    }

    public ProxyConfig deleteInAccount(String name, String accountId) {
        ProxyConfig proxyConfig = proxyConfigRepository.findByNameOrResourceCrnInAccount(name, accountId)
                .orElseThrow(notFound("Proxy config with name:", name));
        MDCBuilder.buildMdcContext(proxyConfig);
        proxyConfigRepository.delete(proxyConfig);
        return proxyConfig;
    }

    public Set<ProxyConfig> deleteMultipleInAccount(Set<String> names, String accountId) {
        Set<ProxyConfig> toBeDeleted = getByNamesForAccountId(names, accountId);
        return toBeDeleted.stream()
                .map(credential -> deleteInAccount(credential.getName(), accountId))
                .collect(Collectors.toSet());
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
        return proxyConfigRepository.findByNameOrResourceCrnInAccount(name, accountId)
                .orElseThrow(notFound("No proxy config found with name", name));
    }

    public Set<ProxyConfig> listInAccount(String accountId) {
        return proxyConfigRepository.findAllInAccount(accountId);
    }

    public ProxyConfig create(ProxyConfig proxyConfig, String accountId) {
        proxyConfig.setResourceCrn(createCRN(accountId));
        proxyConfig.setAccountId(accountId);
        try {
            return proxyConfigRepository.save(proxyConfig);
        } catch (DataIntegrityViolationException e) {
            throw new AccessDeniedException("Access denied");
        }
    }

    private String createCRN(String accountId) {
        return Crn.builder()
                .setService(Crn.Service.ENVIRONMENTS)
                .setAccountId(accountId)
                .setResourceType(Crn.ResourceType.CREDENTIAL)
                .setResource(UUID.randomUUID().toString())
                .build()
                .toString();
    }
}
