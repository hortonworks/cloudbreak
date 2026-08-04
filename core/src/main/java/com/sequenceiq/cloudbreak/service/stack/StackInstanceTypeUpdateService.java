package com.sequenceiq.cloudbreak.service.stack;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.template.TemplateService;

@Service
public class StackInstanceTypeUpdateService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackInstanceTypeUpdateService.class);

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private InstanceGroupService instanceGroupService;

    @Inject
    private TemplateService templateService;

    public void updateInstanceType(String crn, String groupName, String instanceType) {
        StackDto stack = stackDtoService.getByCrn(crn);
        InstanceGroup instanceGroup = instanceGroupService
                .getInstanceGroupWithTemplateAndInstancesByGroupNameInStack(stack.getId(), groupName)
                .orElseThrow(() -> new NotFoundException(String.format("Instance group '%s' not found for stack with CRN '%s'", groupName, crn)));
        Template template = templateService.get(instanceGroup.getTemplate().getId());
        LOGGER.info("Updating instance type from '{}' to '{}' for group '{}' in stack '{}'",
                template.getInstanceType(), instanceType, groupName, crn);
        template.setInstanceType(instanceType);
        templateService.savePure(template);
    }
}
