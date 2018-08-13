package com.sequenceiq.cloudbreak.service.clustertemplate;

import static com.sequenceiq.cloudbreak.controller.exception.NotFoundException.notFound;
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
import com.sequenceiq.cloudbreak.domain.ClusterTemplate;
import com.sequenceiq.cloudbreak.domain.organization.Organization;
import com.sequenceiq.cloudbreak.repository.ClusterTemplateRepository;
import com.sequenceiq.cloudbreak.service.AuthorizationService;
import com.sequenceiq.cloudbreak.service.organization.OrganizationService;

@Service
public class ClusterTemplateService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterTemplateService.class);

    @Inject
    private ClusterTemplateRepository clusterTemplateRepository;

    @Inject
    private AuthorizationService authorizationService;

    @Inject
    private OrganizationService organizationService;

    public Set<ClusterTemplate> retrievePrivateClusterTemplates(IdentityUser user) {
        return clusterTemplateRepository.findForUser(user.getUserId());
    }

    public Set<ClusterTemplate> retrieveAccountClusterTemplates(IdentityUser user) {
        return user.getRoles().contains(IdentityUserRole.ADMIN) ? clusterTemplateRepository.findAllInAccount(user.getAccount())
                : clusterTemplateRepository.findPublicInAccountForUser(user.getUserId(), user.getAccount());
    }

    public ClusterTemplate get(Long id) {
        return clusterTemplateRepository.findById(id).orElseThrow(notFound("ClusterTemplate", id));
    }

    public ClusterTemplate getByName(String name, IdentityUser user) {
        return clusterTemplateRepository.findByNameInAccount(name, user.getAccount(), user.getUserId());
    }

    public ClusterTemplate create(IdentityUser user, ClusterTemplate clusterTemplate, Organization organization) {
        LOGGER.debug("Creating clusterTemplate: [User: '{}', Account: '{}']", user.getUsername(), user.getAccount());
        ClusterTemplate savedClusterTemplate;
        clusterTemplate.setOwner(user.getUserId());
        clusterTemplate.setAccount(user.getAccount());
        if (organization != null) {
            clusterTemplate.setOrganization(organization);
        } else {
            clusterTemplate.setOrganization(organizationService.getDefaultOrganizationForCurrentUser());
        }
        try {
            savedClusterTemplate = clusterTemplateRepository.save(clusterTemplate);
        } catch (DataIntegrityViolationException ex) {
            String msg = String.format("Error with resource [%s], %s", APIResourceType.CLUSTER_TEMPLATE, getProperSqlErrorMessage(ex));
            throw new BadRequestException(msg);
        }
        return savedClusterTemplate;
    }

    public void delete(Long id, IdentityUser user) {
        ClusterTemplate clusterTemplate = clusterTemplateRepository.findByIdInAccount(id, user.getAccount());
        delete(clusterTemplate);
    }

    public ClusterTemplate getPublicClusterTemplate(String name, IdentityUser user) {
        return clusterTemplateRepository.findOneByName(name, user.getAccount());
    }

    public ClusterTemplate getPrivateClusterTemplate(String name, IdentityUser user) {
        return clusterTemplateRepository.findByNameInUser(name, user.getUserId());
    }

    public void delete(String name, IdentityUser user) {
        ClusterTemplate clusterTemplate = clusterTemplateRepository.findByNameInAccount(name, user.getAccount(), user.getUserId());
        delete(clusterTemplate);
    }

    public Iterable<ClusterTemplate> save(Iterable<ClusterTemplate> entities) {
        return clusterTemplateRepository.saveAll(entities);
    }

    private void delete(ClusterTemplate clusterTemplate) {
        LOGGER.info("Deleting cluster-template with name: {}", clusterTemplate.getName());
        clusterTemplateRepository.delete(clusterTemplate);
    }
}
