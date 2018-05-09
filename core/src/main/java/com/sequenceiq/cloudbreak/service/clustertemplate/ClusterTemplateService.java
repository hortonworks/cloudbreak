package com.sequenceiq.cloudbreak.service.clustertemplate;

import static com.sequenceiq.cloudbreak.util.SqlUtil.getProperSqlErrorMessage;

import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUserRole;
import com.sequenceiq.cloudbreak.common.type.APIResourceType;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.controller.exception.NotFoundException;
import com.sequenceiq.cloudbreak.domain.ClusterTemplate;
import com.sequenceiq.cloudbreak.repository.ClusterRepository;
import com.sequenceiq.cloudbreak.repository.ClusterTemplateRepository;
import com.sequenceiq.cloudbreak.service.AuthorizationService;

@Service
public class ClusterTemplateService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterTemplateService.class);

    @Inject
    private ClusterTemplateRepository clusterTemplateRepository;

    @Inject
    private ClusterRepository clusterRepository;

    @Inject
    private AuthorizationService authorizationService;

    public Set<ClusterTemplate> retrievePrivateClusterTemplates(IdentityUser user) {
        return clusterTemplateRepository.findForUser(user.getUserId());
    }

    public Set<ClusterTemplate> retrieveAccountClusterTemplates(IdentityUser user) {
        return user.getRoles().contains(IdentityUserRole.ADMIN) ? clusterTemplateRepository.findAllInAccount(user.getAccount())
                : clusterTemplateRepository.findPublicInAccountForUser(user.getUserId(), user.getAccount());
    }

    public ClusterTemplate get(Long id) {
        ClusterTemplate clusterTemplate = clusterTemplateRepository.findOne(id);
        if (clusterTemplate == null) {
            throw new NotFoundException(String.format("ClusterTemplate '%s' not found.", id));
        }
        authorizationService.hasReadPermission(clusterTemplate);
        return clusterTemplate;
    }

    public ClusterTemplate getByName(String name, IdentityUser user) {
        ClusterTemplate clusterTemplate = clusterTemplateRepository.findByNameInAccount(name, user.getAccount(), user.getUserId());
        if (clusterTemplate == null) {
            throw new NotFoundException(String.format("Blueprint '%s' not found.", name));
        }
        authorizationService.hasReadPermission(clusterTemplate);
        return clusterTemplate;
    }

    public ClusterTemplate create(IdentityUser user, ClusterTemplate clusterTemplate) {
        LOGGER.debug("Creating clusterTemplate: [User: '{}', Account: '{}']", user.getUsername(), user.getAccount());
        ClusterTemplate savedClusterTemplate;
        clusterTemplate.setOwner(user.getUserId());
        clusterTemplate.setAccount(user.getAccount());
        try {
            savedClusterTemplate = clusterTemplateRepository.save(clusterTemplate);
        } catch (DataIntegrityViolationException ex) {
            String msg = String.format("Error with resource [%s], error: [%s]", APIResourceType.CLUSTER_TEMPLATE, getProperSqlErrorMessage(ex));
            throw new BadRequestException(msg);
        }
        return savedClusterTemplate;
    }

    public void delete(Long id, IdentityUser user) {
        ClusterTemplate clusterTemplate = clusterTemplateRepository.findByIdInAccount(id, user.getAccount());
        if (clusterTemplate == null) {
            throw new NotFoundException(String.format("ClusterTemplate '%s' not found.", id));
        }
        delete(clusterTemplate);
    }

    public ClusterTemplate getPublicClusterTemplate(String name, IdentityUser user) {
        ClusterTemplate clusterTemplate = clusterTemplateRepository.findOneByName(name, user.getAccount());
        if (clusterTemplate == null) {
            throw new NotFoundException(String.format("ClusterTemplate '%s' not found.", name));
        }
        return clusterTemplate;
    }

    public ClusterTemplate getPrivateClusterTemplate(String name, IdentityUser user) {
        ClusterTemplate clusterTemplate = clusterTemplateRepository.findByNameInUser(name, user.getUserId());
        if (clusterTemplate == null) {
            throw new NotFoundException(String.format("ClusterTemplate '%s' not found.", name));
        }
        return clusterTemplate;
    }

    public void delete(String name, IdentityUser user) {
        ClusterTemplate clusterTemplate = clusterTemplateRepository.findByNameInAccount(name, user.getAccount(), user.getUserId());
        if (clusterTemplate == null) {
            throw new NotFoundException(String.format("ClusterTemplate '%s' not found.", name));
        }
        delete(clusterTemplate);
    }

    public Iterable<ClusterTemplate> save(Iterable<ClusterTemplate> entities) {
        return clusterTemplateRepository.save(entities);
    }

    private void delete(ClusterTemplate clusterTemplate) {
        authorizationService.hasWritePermission(clusterTemplate);
        clusterTemplateRepository.delete(clusterTemplate);
    }
}
