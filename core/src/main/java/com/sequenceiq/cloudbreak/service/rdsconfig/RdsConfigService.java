package com.sequenceiq.cloudbreak.service.rdsconfig;

import java.util.Set;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.type.CbUserRole;
import com.sequenceiq.cloudbreak.common.type.ResourceStatus;
import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.controller.NotFoundException;
import com.sequenceiq.cloudbreak.domain.CbUser;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.repository.ClusterRepository;
import com.sequenceiq.cloudbreak.repository.RdsConfigRepository;

@Service
@Transactional
public class RdsConfigService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RdsConfigService.class);

    @Inject
    private RdsConfigRepository rdsConfigRepository;

    @Inject
    private ClusterRepository clusterRepository;

    public Set<RDSConfig> retrievePrivateRdsConfigs(CbUser user) {
        return rdsConfigRepository.findForUser(user.getUserId());
    }

    public RDSConfig getPrivateRdsConfig(String name, CbUser user) {
        RDSConfig rdsConfig = rdsConfigRepository.findByNameInUser(name, user.getUserId());
        if (rdsConfig == null) {
            throw new NotFoundException(String.format("RDS configuration '%s' not found.", name));
        }
        return rdsConfig;
    }

    public RDSConfig getPublicRdsConfig(String name, CbUser user) {
        RDSConfig rdsConfig = rdsConfigRepository.findOneByName(name, user.getAccount());
        if (rdsConfig == null) {
            throw new NotFoundException(String.format("RDS configuration '%s' not found.", name));
        }
        return rdsConfig;
    }

    public Set<RDSConfig> retrieveAccountRdsConfigs(CbUser user) {
        if (user.getRoles().contains(CbUserRole.ADMIN)) {
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

    public void delete(Long id, CbUser user) {
        RDSConfig rdsConfig = rdsConfigRepository.findByIdInAccount(id, user.getAccount());
        if (rdsConfig == null) {
            throw new NotFoundException(String.format("RDS configuration '%s' not found.", id));
        }
        delete(rdsConfig, user);
    }

    public void delete(String name, CbUser user) {
        RDSConfig rdsConfig = rdsConfigRepository.findByNameInAccount(name, user.getAccount(), user.getUserId());
        if (rdsConfig == null) {
            throw new NotFoundException(String.format("RDS configuration '%s' not found.", name));
        }
        delete(rdsConfig, user);
    }

    public RDSConfig create(CbUser user, RDSConfig rdsConfig) {
        LOGGER.debug("Creating RDS configuration: [User: '{}', Account: '{}']", user.getUsername(), user.getAccount());
        rdsConfig.setOwner(user.getUserId());
        rdsConfig.setAccount(user.getAccount());
        return rdsConfigRepository.save(rdsConfig);
    }

    private void delete(RDSConfig rdsConfig, CbUser user) {
        if (clusterRepository.findAllClustersByRDSConfig(rdsConfig.getId()).isEmpty()) {
            if (!user.getUserId().equals(rdsConfig.getOwner()) && !user.getRoles().contains(CbUserRole.ADMIN)) {
                throw new BadRequestException("RDS configurations can only be deleted by account admins or owners.");
            }
            if (ResourceStatus.USER_MANAGED.equals(rdsConfig.getStatus())) {
                rdsConfigRepository.delete(rdsConfig);
            } else {
                rdsConfig.setStatus(ResourceStatus.DEFAULT_DELETED);
                rdsConfigRepository.save(rdsConfig);
            }
        } else {
            throw new BadRequestException(String.format(
                    "There are clusters associated with RDS config '%s'. Please remove these before deleting the RDS configuration.", rdsConfig.getId()));
        }
    }
}
