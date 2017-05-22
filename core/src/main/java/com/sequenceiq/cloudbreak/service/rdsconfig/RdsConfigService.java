package com.sequenceiq.cloudbreak.service.rdsconfig;

import java.util.Set;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUserRole;
import com.sequenceiq.cloudbreak.common.type.RdsType;
import com.sequenceiq.cloudbreak.common.type.ResourceStatus;
import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.controller.NotFoundException;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.repository.ClusterRepository;
import com.sequenceiq.cloudbreak.repository.RdsConfigRepository;
import com.sequenceiq.cloudbreak.service.AuthorizationService;
import com.sequenceiq.cloudbreak.util.NameUtil;

@Service
@Transactional
public class RdsConfigService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RdsConfigService.class);

    @Inject
    private RdsConfigRepository rdsConfigRepository;

    @Inject
    private ClusterRepository clusterRepository;

    @Inject
    private AuthorizationService authorizationService;

    public Set<RDSConfig> retrievePrivateRdsConfigs(IdentityUser user) {
        return rdsConfigRepository.findForUser(user.getUserId());
    }

    public RDSConfig getPrivateRdsConfig(String name, IdentityUser user) {
        RDSConfig rdsConfig = rdsConfigRepository.findByNameInUser(name, user.getUserId());
        if (rdsConfig == null) {
            throw new NotFoundException(String.format("RDS configuration '%s' not found.", name));
        }
        return rdsConfig;
    }

    public RDSConfig getPublicRdsConfig(String name, IdentityUser user) {
        RDSConfig rdsConfig = rdsConfigRepository.findOneByName(name, user.getAccount());
        if (rdsConfig == null) {
            throw new NotFoundException(String.format("RDS configuration '%s' not found.", name));
        }
        return rdsConfig;
    }

    public Set<RDSConfig> retrieveAccountRdsConfigs(IdentityUser user) {
        if (user.getRoles().contains(IdentityUserRole.ADMIN)) {
            return rdsConfigRepository.findAllInAccount(user.getAccount());
        } else {
            return rdsConfigRepository.findPublicInAccountForUser(user.getUserId(), user.getAccount());
        }
    }

    @PostAuthorize("hasPermission(returnObject,'read')")
    public RDSConfig get(Long id) {
        RDSConfig rdsConfig = rdsConfigRepository.findById(id);
        if (rdsConfig == null) {
            throw new NotFoundException(String.format("RDS configuration '%s' not found.", id));
        }
        return rdsConfig;
    }

    public void delete(Long id, IdentityUser user) {
        RDSConfig rdsConfig = rdsConfigRepository.findByIdInAccount(id, user.getAccount());
        if (rdsConfig == null) {
            throw new NotFoundException(String.format("RDS configuration '%s' not found.", id));
        }
        delete(rdsConfig);
    }

    public void delete(String name, IdentityUser user) {
        RDSConfig rdsConfig = rdsConfigRepository.findByNameInAccount(name, user.getAccount(), user.getUserId());
        if (rdsConfig == null) {
            throw new NotFoundException(String.format("RDS configuration '%s' not found.", name));
        }
        delete(rdsConfig);
    }

    public RDSConfig create(IdentityUser user, RDSConfig rdsConfig) {
        LOGGER.debug("Creating RDS configuration: [User: '{}', Account: '{}']", user.getUsername(), user.getAccount());
        rdsConfig.setOwner(user.getUserId());
        rdsConfig.setAccount(user.getAccount());
        return rdsConfigRepository.save(rdsConfig);
    }

    public RDSConfig createIfNotExists(IdentityUser user, RDSConfig rdsConfig) {
        try {
            return getPrivateRdsConfig(rdsConfig.getName(), user);
        } catch (NotFoundException e) {
            return create(user, rdsConfig);
        }
    }

    public Set<RDSConfig> findByClusterId(String user, String account, Long clusterId) {
        return rdsConfigRepository.findByClusterId(user, account, clusterId);
    }

    public RDSConfig findByClusterIdAndType(String user, String account, Long clusterId, RdsType rdsType) {
        return rdsConfigRepository.findByClusterIdAndType(user, account, clusterId, rdsType);
    }

    private void delete(RDSConfig rdsConfig) {
        authorizationService.hasWritePermission(rdsConfig);
        if (!clusterRepository.findAllClustersByRDSConfig(rdsConfig.getId()).isEmpty()) {
            throw new BadRequestException(String.format(
                    "There are clusters associated with RDS config '%s'. Please remove these before deleting the RDS configuration.", rdsConfig.getId()));
        }
        if (ResourceStatus.USER_MANAGED.equals(rdsConfig.getStatus())) {
            rdsConfigRepository.delete(rdsConfig);
        } else {
            rdsConfig.setName(NameUtil.postfixWithTimestamp(rdsConfig.getName()));
            rdsConfig.setStatus(ResourceStatus.DEFAULT_DELETED);
            rdsConfigRepository.save(rdsConfig);
        }
    }
}
