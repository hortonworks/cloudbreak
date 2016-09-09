package com.sequenceiq.cloudbreak.service.clustertemplate;

import java.util.Set;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.type.APIResourceType;
import com.sequenceiq.cloudbreak.common.type.CbUserRole;
import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.controller.NotFoundException;
import com.sequenceiq.cloudbreak.domain.CbUser;
import com.sequenceiq.cloudbreak.domain.ClusterTemplate;
import com.sequenceiq.cloudbreak.repository.ClusterRepository;
import com.sequenceiq.cloudbreak.repository.ClusterTemplateRepository;
import com.sequenceiq.cloudbreak.service.DuplicateKeyValueException;

@Service
@Transactional
public class ClusterTemplateService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterTemplateService.class);

    @Inject
    private ClusterTemplateRepository clusterTemplateRepository;

    @Inject
    private ClusterRepository clusterRepository;

    public Set<ClusterTemplate> retrievePrivateClusterTemplates(CbUser user) {
        return clusterTemplateRepository.findForUser(user.getUserId());
    }

    public Set<ClusterTemplate> retrieveAccountClusterTemplates(CbUser user) {
        if (user.getRoles().contains(CbUserRole.ADMIN)) {
            return clusterTemplateRepository.findAllInAccount(user.getAccount());
        } else {
            return clusterTemplateRepository.findPublicInAccountForUser(user.getUserId(), user.getAccount());
        }
    }

    @PostAuthorize("hasPermission(returnObject,'read')")
    public ClusterTemplate get(Long id) {
        ClusterTemplate clusterTemplate = clusterTemplateRepository.findOne(id);
        if (clusterTemplate == null) {
            throw new NotFoundException(String.format("ClusterTemplate '%s' not found.", id));
        }
        return clusterTemplate;
    }

    @PostAuthorize("hasPermission(returnObject,'read')")
    public ClusterTemplate getByName(String name, CbUser user) {
        ClusterTemplate clusterTemplate = clusterTemplateRepository.findByNameInAccount(name, user.getAccount(), user.getUsername());
        if (clusterTemplate == null) {
            throw new NotFoundException(String.format("Blueprint '%s' not found.", name));
        }
        return clusterTemplate;
    }

    @Transactional(Transactional.TxType.NEVER)
    public ClusterTemplate create(CbUser user, ClusterTemplate clusterTemplate) {
        LOGGER.debug("Creating clusterTemplate: [User: '{}', Account: '{}']", user.getUsername(), user.getAccount());
        ClusterTemplate savedClusterTemplate;
        clusterTemplate.setOwner(user.getUserId());
        clusterTemplate.setAccount(user.getAccount());
        try {
            savedClusterTemplate = clusterTemplateRepository.save(clusterTemplate);
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateKeyValueException(APIResourceType.CLUSTER_TEMPLATE, clusterTemplate.getName(), ex);
        }
        return savedClusterTemplate;
    }

    public void delete(Long id, CbUser user) {
        ClusterTemplate clusterTemplate = clusterTemplateRepository.findByIdInAccount(id, user.getAccount());
        if (clusterTemplate == null) {
            throw new NotFoundException(String.format("ClusterTemplate '%s' not found.", id));
        }
        delete(clusterTemplate, user);
    }

    public ClusterTemplate getPublicClusterTemplate(String name, CbUser user) {
        ClusterTemplate clusterTemplate = clusterTemplateRepository.findOneByName(name, user.getAccount());
        if (clusterTemplate == null) {
            throw new NotFoundException(String.format("ClusterTemplate '%s' not found.", name));
        }
        return clusterTemplate;
    }

    public ClusterTemplate getPrivateClusterTemplate(String name, CbUser user) {
        ClusterTemplate clusterTemplate = clusterTemplateRepository.findByNameInUser(name, user.getUserId());
        if (clusterTemplate == null) {
            throw new NotFoundException(String.format("ClusterTemplate '%s' not found.", name));
        }
        return clusterTemplate;
    }

    public void delete(String name, CbUser user) {
        ClusterTemplate clusterTemplate = clusterTemplateRepository.findByNameInAccount(name, user.getAccount(), user.getUserId());
        if (clusterTemplate == null) {
            throw new NotFoundException(String.format("ClusterTemplate '%s' not found.", name));
        }
        delete(clusterTemplate, user);
    }

    @Transactional(Transactional.TxType.NEVER)
    public Iterable<ClusterTemplate> save(Iterable<ClusterTemplate> entities) {
        return clusterTemplateRepository.save(entities);
    }

    private void delete(ClusterTemplate clusterTemplate, CbUser user) {
        if (!user.getUserId().equals(clusterTemplate.getOwner()) && !user.getRoles().contains(CbUserRole.ADMIN)) {
            throw new BadRequestException("ClusterTemplate can only be deleted by account admins or owners.");
        }
        clusterTemplateRepository.delete(clusterTemplate);
    }
}
