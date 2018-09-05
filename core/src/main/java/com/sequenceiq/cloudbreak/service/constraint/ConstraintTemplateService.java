package com.sequenceiq.cloudbreak.service.constraint;


import java.util.Collection;
import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.model.ResourceStatus;
import com.sequenceiq.cloudbreak.authorization.OrganizationResource;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.domain.ConstraintTemplate;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.repository.ConstraintTemplateRepository;
import com.sequenceiq.cloudbreak.repository.organization.OrganizationResourceRepository;
import com.sequenceiq.cloudbreak.service.AbstractOrganizationAwareResourceService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.util.NameUtil;

@Service
public class ConstraintTemplateService extends AbstractOrganizationAwareResourceService<ConstraintTemplate> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConstraintTemplateService.class);

    private static final String CONSTRAINT_NOT_FOUND_MSG = "Constraint template '%s' not found.";

    private static final String CONSTRAINT_NOT_FOUND_BY_ID_MSG = "Constraint template not found by id: '%d'.";

    @Inject
    private ConstraintTemplateRepository constraintTemplateRepository;

    @Inject
    private ClusterService clusterService;

    @Override
    public ConstraintTemplate deleteByNameFromOrganization(String name, Long organizationId) {
        ConstraintTemplate constraintTemplate = constraintTemplateRepository.findByNameAndOrganizationId(name, organizationId);
        return delete(constraintTemplate);
    }

    @Override
    public ConstraintTemplate delete(ConstraintTemplate constraintTemplate) {
        LOGGER.info("Deleting constraint-template. {} - {}", new Object[]{constraintTemplate.getId(), constraintTemplate.getName()});
        List<Cluster> clusters = clusterService.findAllClustersForConstraintTemplate(constraintTemplate.getId());
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
        return constraintTemplate;
    }

    @Override
    public OrganizationResourceRepository<ConstraintTemplate, Long> repository() {
        return constraintTemplateRepository;
    }

    @Override
    public OrganizationResource resource() {
        return OrganizationResource.CONSTRAINT_TEMPLATE;
    }

    @Override
    protected void prepareDeletion(ConstraintTemplate resource) {
    }

    @Override
    protected void prepareCreation(ConstraintTemplate resource) {
    }

    private boolean isRunningClusterReferToTemplate(Collection<Cluster> clusters) {
        return clusters.stream().anyMatch(c -> !c.isDeleteCompleted());
    }
}
