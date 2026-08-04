package com.sequenceiq.freeipa.service.stack;

import java.util.Set;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.freeipa.entity.InstanceGroup;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.entity.Template;
import com.sequenceiq.freeipa.service.stack.instance.InstanceGroupService;
import com.sequenceiq.freeipa.service.stack.instance.TemplateService;

@Service
public class FreeIpaInstanceTypeUpdateService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaInstanceTypeUpdateService.class);

    @Inject
    private StackService stackService;

    @Inject
    private InstanceGroupService instanceGroupService;

    @Inject
    private TemplateService templateService;

    public void updateInstanceType(String environmentCrn, String instanceType) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        Stack stack = stackService.getByEnvironmentCrnAndAccountId(environmentCrn, accountId);
        Set<InstanceGroup> instanceGroups = instanceGroupService.findByStackId(stack.getId());
        for (InstanceGroup instanceGroup : instanceGroups) {
            Template template = instanceGroup.getTemplate();
            LOGGER.info("Updating instance type from '{}' to '{}' for group '{}' in FreeIPA stack '{}'",
                    template.getInstanceType(), instanceType, instanceGroup.getGroupName(), environmentCrn);
            template.setInstanceType(instanceType);
            templateService.save(template);
        }
    }
}
