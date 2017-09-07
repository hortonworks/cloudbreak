package com.sequenceiq.cloudbreak.service.constraint;


import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUserRole;
import com.sequenceiq.cloudbreak.common.type.APIResourceType;
import com.sequenceiq.cloudbreak.common.type.ResourceStatus;
import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.controller.NotFoundException;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.ConstraintTemplate;
import com.sequenceiq.cloudbreak.repository.ClusterRepository;
import com.sequenceiq.cloudbreak.repository.ConstraintTemplateRepository;
import com.sequenceiq.cloudbreak.service.AuthorizationService;
import com.sequenceiq.cloudbreak.service.DuplicateKeyValueException;
import com.sequenceiq.cloudbreak.util.NameUtil;

@Service
@Transactional
public class ConstraintTemplateService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConstraintTemplateService.class);

    private static final String CONSTRAINT_NOT_FOUND_MSG = "Constraint template '%s' not found.";

    @Inject
    private ConstraintTemplateRepository constraintTemplateRepository;

    @Inject
    private ClusterRepository clusterRepository;

    @Inject
    private AuthorizationService authorizationService;

    public Set<ConstraintTemplate> retrievePrivateConstraintTemplates(IdentityUser user) {
        return constraintTemplateRepository.findForUser(user.getUserId());
    }

    public Set<ConstraintTemplate> retrieveAccountConstraintTemplates(IdentityUser user) {
        if (user.getRoles().contains(IdentityUserRole.ADMIN)) {
            return constraintTemplateRepository.findAllInAccount(user.getAccount());
        } else {
            return constraintTemplateRepository.findPublicInAccountForUser(user.getUserId(), user.getAccount());
        }
    }

    @PostAuthorize("hasPermission(returnObject,'read')")
    public ConstraintTemplate get(Long id) {
        ConstraintTemplate constraintTemplate = constraintTemplateRepository.findOne(id);
        if (constraintTemplate == null) {
            throw new NotFoundException(String.format(CONSTRAINT_NOT_FOUND_MSG, id));
        } else {
            return constraintTemplate;
        }
    }

    public ConstraintTemplate create(IdentityUser user, ConstraintTemplate constraintTemplate) {
        LOGGER.debug("Creating constraint template: [User: '{}', Account: '{}']", user.getUsername(), user.getAccount());
        constraintTemplate.setOwner(user.getUserId());
        constraintTemplate.setAccount(user.getAccount());
        try {
            return constraintTemplateRepository.save(constraintTemplate);
        } catch (RuntimeException e) {
            throw new DuplicateKeyValueException(APIResourceType.CONSTRAINT_TEMPLATE, constraintTemplate.getName(), e);
        }
    }

    public void delete(String name, IdentityUser user) {
        ConstraintTemplate constraintTemplate = constraintTemplateRepository.findByNameInAccount(name, user.getAccount(), user.getUserId());
        if (constraintTemplate == null) {
            throw new NotFoundException(String.format(CONSTRAINT_NOT_FOUND_MSG, name));
        }
        delete(constraintTemplate);
    }

    public void delete(Long id, IdentityUser user) {
        ConstraintTemplate constraintTemplate = constraintTemplateRepository.findByIdInAccount(id, user.getAccount());
        if (constraintTemplate == null) {
            throw new NotFoundException(String.format(CONSTRAINT_NOT_FOUND_MSG, id));
        }
        delete(constraintTemplate);
    }

    @PostAuthorize("hasPermission(returnObject,'read')")
    public ConstraintTemplate getPublicTemplate(String name, IdentityUser user) {
        ConstraintTemplate constraintTemplate = constraintTemplateRepository.findOneByName(name, user.getAccount());
        if (constraintTemplate == null) {
            throw new NotFoundException(String.format(CONSTRAINT_NOT_FOUND_MSG, name));
        } else {
            return constraintTemplate;
        }
    }

    public ConstraintTemplate getPrivateTemplate(String name, IdentityUser user) {
        ConstraintTemplate constraintTemplate = constraintTemplateRepository.findByNameInUser(name, user.getUserId());
        if (constraintTemplate == null) {
            throw new NotFoundException(String.format(CONSTRAINT_NOT_FOUND_MSG, name));
        } else {
            return constraintTemplate;
        }
    }

    private void delete(ConstraintTemplate constraintTemplate) {
        authorizationService.hasWritePermission(constraintTemplate);
        LOGGER.debug("Deleting constraint template. {} - {}", new Object[]{constraintTemplate.getId(), constraintTemplate.getName()});
        List<Cluster> clusters = clusterRepository.findAllClustersForConstraintTemplate(constraintTemplate.getId());
        if (clusters.isEmpty()) {
            if (ResourceStatus.USER_MANAGED.equals(constraintTemplate.getStatus())) {
                constraintTemplateRepository.delete(constraintTemplate);
            } else {
                constraintTemplate.setName(NameUtil.postfixWithTimestamp(constraintTemplate.getName()));
                constraintTemplate.setStatus(ResourceStatus.DEFAULT_DELETED);
                constraintTemplateRepository.save(constraintTemplate);
            }
        } else if (isRunningClusterReferToTemplate(clusters)) {
            throw new BadRequestException(String.format(
                    "There are stacks associated with template '%s'. Please remove these before deleting the template.", constraintTemplate.getName()));
        } else {
            constraintTemplate.setName(NameUtil.postfixWithTimestamp(constraintTemplate.getName()));
            constraintTemplate.setDeleted(true);
            if (ResourceStatus.DEFAULT.equals(constraintTemplate.getStatus())) {
                constraintTemplate.setStatus(ResourceStatus.DEFAULT_DELETED);
            }
            constraintTemplateRepository.save(constraintTemplate);
        }
    }

    private boolean isRunningClusterReferToTemplate(List<Cluster> clusters) {
        return clusters.stream().anyMatch(c -> !c.isDeleteCompleted());
    }
}
