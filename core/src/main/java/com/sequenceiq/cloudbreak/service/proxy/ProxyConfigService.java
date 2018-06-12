package com.sequenceiq.cloudbreak.service.proxy;

import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUserRole;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.controller.exception.NotFoundException;
import com.sequenceiq.cloudbreak.domain.ProxyConfig;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.repository.ClusterRepository;
import com.sequenceiq.cloudbreak.repository.ProxyConfigRepository;
import com.sequenceiq.cloudbreak.service.AuthorizationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ProxyConfigService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProxyConfigService.class);

    @Inject
    private ProxyConfigRepository proxyConfigRepository;

    @Inject
    private ClusterRepository clusterRepository;

    @Inject
    private AuthorizationService authorizationService;

    public Set<ProxyConfig> retrievePrivateProxyConfigs(IdentityUser user) {
        Set<ProxyConfig> proxyConfigs = proxyConfigRepository.findAllByOwner(user.getUserId());
        authorizationService.hasReadPermission(proxyConfigs);
        return proxyConfigs;
    }

    public ProxyConfig getPrivateProxyConfig(String name, IdentityUser user) {
        ProxyConfig proxyConfig = proxyConfigRepository.findByNameAndOwner(name, user.getUserId());
        if (proxyConfig == null) {
            throw new NotFoundException(String.format("Proxy configuration '%s' not found.", name));
        }
        authorizationService.hasReadPermission(proxyConfig);
        return proxyConfig;
    }

    public ProxyConfig getPublicProxyConfig(String name, IdentityUser user) {
        ProxyConfig proxyConfig = proxyConfigRepository.findByNameAndAccount(name, user.getAccount());
        if (proxyConfig == null) {
            throw new NotFoundException(String.format("Proxy configuration '%s' not found.", name));
        }
        authorizationService.hasReadPermission(proxyConfig);
        return proxyConfig;
    }

    public Set<ProxyConfig> retrieveAccountProxyConfigs(IdentityUser user) {
        Set<ProxyConfig> proxyConfigs = user.getRoles().contains(IdentityUserRole.ADMIN) ? proxyConfigRepository.findAllByAccount(user.getAccount())
                : proxyConfigRepository.findPublicInAccountForUser(user.getUserId(), user.getAccount());
        authorizationService.hasReadPermission(proxyConfigs);
        return proxyConfigs;
    }

    public ProxyConfig get(Long id) {
        ProxyConfig proxyConfig = proxyConfigRepository.findOne(id);
        if (proxyConfig == null) {
            throw new NotFoundException(String.format("Proxy configuration '%s' not found.", id));
        }
        authorizationService.hasReadPermission(proxyConfig);
        return proxyConfig;
    }

    public void delete(Long id, IdentityUser user) {
        ProxyConfig proxyConfig = proxyConfigRepository.findByIdAndAccount(id, user.getAccount());
        if (proxyConfig == null) {
            throw new NotFoundException(String.format("Proxy configuration '%s' not found.", id));
        }
        authorizationService.hasWritePermission(proxyConfig);
        delete(proxyConfig);
    }

    public void delete(String name, IdentityUser user) {
        ProxyConfig proxyConfig = proxyConfigRepository.findByNameBasedOnAccount(name, user.getAccount(), user.getUserId());
        if (proxyConfig == null) {
            throw new NotFoundException(String.format("Proxy configuration '%s' not found.", name));
        }
        authorizationService.hasWritePermission(proxyConfig);
        delete(proxyConfig);
    }

    public ProxyConfig create(IdentityUser user, ProxyConfig proxyConfig) {
        LOGGER.debug("Creating Proxy configuration: [User: '{}', Account: '{}']", user.getUsername(), user.getAccount());
        proxyConfig.setOwner(user.getUserId());
        proxyConfig.setAccount(user.getAccount());
        return proxyConfigRepository.save(proxyConfig);
    }

    public ProxyConfig createIfNotExists(IdentityUser user, ProxyConfig proxyConfig) {
        try {
            return getPrivateProxyConfig(proxyConfig.getName(), user);
        } catch (NotFoundException ignored) {
            return create(user, proxyConfig);
        }
    }

    private void delete(ProxyConfig proxyConfig) {
        authorizationService.hasWritePermission(proxyConfig);
        List<Cluster> clustersWithThisProxy = new ArrayList<>(clusterRepository.findByProxyConfig(proxyConfig));
        if (!clustersWithThisProxy.isEmpty()) {
            if (clustersWithThisProxy.size() > 1) {
                String clusters = clustersWithThisProxy
                        .stream()
                        .map(Cluster::getName)
                        .collect(Collectors.joining(", "));
                throw new BadRequestException(String.format(
                        "There are clusters associated with proxy config '%s'. Please remove these before deleting the proxy configuration. "
                                + "The following clusters are using this proxy configuration: [%s]", proxyConfig.getName(), clusters));
            } else {
                throw new BadRequestException(String.format("There is a cluster ['%s'] which uses proxy configuration '%s'. Please remove this "
                        + "cluster before deleting the proxy configuration", clustersWithThisProxy.get(0).getName(), proxyConfig.getName()));
            }
        }
        proxyConfigRepository.delete(proxyConfig);
    }
}
