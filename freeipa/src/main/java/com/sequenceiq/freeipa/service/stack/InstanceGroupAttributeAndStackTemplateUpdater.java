package com.sequenceiq.freeipa.service.stack;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.event.platform.GetPlatformTemplateRequest;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.stack.StackContext;
import com.sequenceiq.freeipa.service.stack.instance.DefaultInstanceGroupProvider;

@Component
public class InstanceGroupAttributeAndStackTemplateUpdater {

    private static final Logger LOGGER = LoggerFactory.getLogger(InstanceGroupAttributeAndStackTemplateUpdater.class);

    @Inject
    private DefaultInstanceGroupProvider defaultDefaultInstanceGroupProvider;

    @Inject
    private StackTemplateService templateService;

    @Inject
    private StackService stackService;

    public void updateInstanceGroupAttributesAndTemplateIfDefaultDifferent(StackContext context, Stack staleStack) {
        AtomicBoolean igUpdated = new AtomicBoolean(false);
        staleStack.getInstanceGroups().forEach(group -> {
            Json attributes = defaultDefaultInstanceGroupProvider.createAttributes(CloudPlatform.fromName(staleStack.getCloudPlatform()),
                    staleStack.getName(), group.getGroupName());
            if (!Objects.equals(attributes, group.getAttributes())) {
                LOGGER.info("Updating default instance group attributes for [{}]. Original: {}, updated: {}",
                        group.getGroupName(), group.getAttributes(), attributes);
                group.setAttributes(attributes);
                igUpdated.set(true);
            }
        });
        if (igUpdated.get()) {
            GetPlatformTemplateRequest getPlatformTemplateRequest =
                    templateService.triggerGetTemplate(context.getCloudContext(), context.getCloudCredential());
            Stack freshStack = stackService.getByIdWithListsInTransaction(staleStack.getId());
            String template = templateService.waitGetTemplate(getPlatformTemplateRequest);
            freshStack.setTemplate(template);
            freshStack.setInstanceGroups(staleStack.getInstanceGroups());
            stackService.save(freshStack);
        }
    }
}
