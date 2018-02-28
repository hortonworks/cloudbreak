package com.sequenceiq.cloudbreak.service.proxy;

import java.util.Set;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.base.Preconditions;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUserRole;
import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.controller.NotFoundException;
import com.sequenceiq.cloudbreak.domain.ProxyConfig;
import com.sequenceiq.cloudbreak.repository.ClusterRepository;
import com.sequenceiq.cloudbreak.repository.ProxyConfigRepository;
import com.sequenceiq.cloudbreak.service.AuthorizationService;

@Service
@Transactional
public class ProxyConfigService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProxyConfigService.class);

    @Inject
    private ProxyConfigRepository proxyConfigRepository;

    @Inject
    private ClusterRepository clusterRepository;

    @Inject
    private AuthorizationService authorizationService;

    public Set<ProxyConfig> retrievePrivateProxyConfigs(IdentityUser user) {
        return proxyConfigRepository.findForUser(user.getUserId());
    }

    public ProxyConfig getPrivateProxyConfig(String name, IdentityUser user) {
        ProxyConfig proxyConfig = proxyConfigRepository.findByNameInUser(name, user.getUserId());
        if (proxyConfig == null) {
            throw new NotFoundException(String.format("Proxy configuration '%s' not found.", name));
        }
        return proxyConfig;
    }

    public ProxyConfig getPublicProxyConfig(String name, IdentityUser user) {
        ProxyConfig proxyConfig = proxyConfigRepository.findOneByName(name, user.getAccount());
        if (proxyConfig == null) {
            throw new NotFoundException(String.format("Proxy configuration '%s' not found.", name));
        }
        return proxyConfig;
    }

    public Set<ProxyConfig> retrieveAccountProxyConfigs(IdentityUser user) {
        return user.getRoles().contains(IdentityUserRole.ADMIN) ? proxyConfigRepository.findAllBasedOnAccount(user.getAccount())
                : proxyConfigRepository.findPublicInAccountForUser(user.getUserId(), user.getAccount());
    }

    public ProxyConfig get(Long id) {
        ProxyConfig proxyConfig = proxyConfigRepository.findById(id);
        if (proxyConfig == null) {
            throw new NotFoundException(String.format("Proxy configuration '%s' not found.", id));
        }
        authorizationService.hasReadPermission(proxyConfig);
        return proxyConfig;
    }

    public void delete(Long id, IdentityUser user) {
        ProxyConfig proxyConfig = proxyConfigRepository.findByIdInAccount(id, user.getAccount());
        if (proxyConfig == null) {
            throw new NotFoundException(String.format("Proxy configuration '%s' not found.", id));
        }
        delete(proxyConfig);
    }

    public void delete(String name, IdentityUser user) {
        ProxyConfig proxyConfig = proxyConfigRepository.findByNameBasedOnAccount(name, user.getAccount(), user.getUserId());
        if (proxyConfig == null) {
            throw new NotFoundException(String.format("Proxy configuration '%s' not found.", name));
        }
        delete(proxyConfig);
    }

    public ProxyConfig create(IdentityUser user, ProxyConfig proxyConfig) {
        LOGGER.debug("Creating Proxy configuration: [User: '{}', Account: '{}']", user.getUsername(), user.getAccount());
        proxyConfig.setOwner(user.getUserId());
        proxyConfig.setAccount(user.getAccount());
        return proxyConfigRepository.save(proxyConfig);
    }

    public ProxyConfig create(ProxyConfig proxyConfig) {
        Preconditions.checkNotNull(proxyConfig.getOwner(), "Owner cannot be null");
        Preconditions.checkNotNull(proxyConfig.getAccount(), "Account cannot be null");
        LOGGER.debug("Creating Proxy configuration: [User: '{}', Account: '{}']", proxyConfig.getOwner(), proxyConfig.getAccount());
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
        if (clusterRepository.countByProxyConfig(proxyConfig) != 0) {
            throw new BadRequestException(String.format(
                    "There are clusters associated with proxy config '%s'. Please remove these before deleting the proxy configuration.", proxyConfig.getId()));
        }
        proxyConfigRepository.delete(proxyConfig);
    }
}
