package com.sequenceiq.cloudbreak.converter;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.ConstraintTemplateRequest;
import com.sequenceiq.cloudbreak.api.model.ResourceStatus;
import com.sequenceiq.cloudbreak.domain.ConstraintTemplate;
import com.sequenceiq.cloudbreak.domain.organization.Organization;
import com.sequenceiq.cloudbreak.service.RestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.organization.OrganizationService;

@Component
public class ConstraintTemplateRequestToConstraintTemplateConverter
        extends AbstractConversionServiceAwareConverter<ConstraintTemplateRequest, ConstraintTemplate> {

    @Inject
    private OrganizationService organizationService;

    @Inject
    private RestRequestThreadLocalService restRequestThreadLocalService;

    @Override
    public ConstraintTemplate convert(ConstraintTemplateRequest source) {
        ConstraintTemplate constraintTemplate = new ConstraintTemplate();
        constraintTemplate.setCpu(source.getCpu());
        constraintTemplate.setMemory(source.getMemory());
        constraintTemplate.setDisk(source.getDisk());
        constraintTemplate.setOrchestratorType(source.getOrchestratorType());
        constraintTemplate.setName(source.getName());
        constraintTemplate.setDescription(source.getDescription());
        constraintTemplate.setStatus(ResourceStatus.USER_MANAGED);
        Long orgId = restRequestThreadLocalService.getRequestedOrgId();
        if (orgId == null) {
            orgId = organizationService.getDefaultOrganizationForCurrentUser().getId();
        }
        Organization organization = organizationService.get(orgId);
        constraintTemplate.setOrganization(organization);
        return constraintTemplate;
    }

}
