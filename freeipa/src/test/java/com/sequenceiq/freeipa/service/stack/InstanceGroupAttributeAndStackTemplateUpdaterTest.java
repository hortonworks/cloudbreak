package com.sequenceiq.freeipa.service.stack;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetPlatformTemplateRequest;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.freeipa.entity.InstanceGroup;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.stack.StackContext;
import com.sequenceiq.freeipa.service.stack.instance.DefaultInstanceGroupProvider;

@ExtendWith(MockitoExtension.class)
class InstanceGroupAttributeAndStackTemplateUpdaterTest {
    @Mock
    private DefaultInstanceGroupProvider defaultInstanceGroupProvider;

    @Mock
    private StackTemplateService templateService;

    @Mock
    private StackService stackService;

    @Mock
    private StackContext stackContext;

    @InjectMocks
    private InstanceGroupAttributeAndStackTemplateUpdater updater;

    @Test
    void testUpdateInstanceGroupAttributesAndTemplateIfDefaultDifferent() {
        Stack staleStack = mock(Stack.class);
        Stack freshStack = mock(Stack.class);
        InstanceGroup instanceGroup = mock(InstanceGroup.class);
        Json oldAttributes = new Json("{\"key\":\"oldValue\"}");
        Json newAttributes = new Json("{\"key\":\"newValue\"}");
        GetPlatformTemplateRequest templateRequest = mock(GetPlatformTemplateRequest.class);
        String newTemplate = "newTemplate";

        when(stackContext.getCloudContext()).thenReturn(mock(CloudContext.class));
        when(stackContext.getCloudCredential()).thenReturn(mock(CloudCredential.class));
        when(staleStack.getInstanceGroups()).thenReturn(Set.of(instanceGroup));
        when(staleStack.getCloudPlatform()).thenReturn("AWS");
        when(staleStack.getId()).thenReturn(3L);
        when(instanceGroup.getGroupName()).thenReturn("group1");
        when(instanceGroup.getAttributes()).thenReturn(oldAttributes);
        when(defaultInstanceGroupProvider.createAttributes(CloudPlatform.fromName("AWS"), staleStack.getName(), "group1"))
                .thenReturn(newAttributes);
        when(templateService.triggerGetTemplate(any(CloudContext.class), any(CloudCredential.class))).thenReturn(templateRequest);
        when(templateService.waitGetTemplate(templateRequest)).thenReturn(newTemplate);
        when(stackService.getByIdWithListsInTransaction(3L)).thenReturn(freshStack);

        updater.updateInstanceGroupAttributesAndTemplateIfDefaultDifferent(stackContext, staleStack);

        verify(instanceGroup).setAttributes(newAttributes);
        verify(templateService).triggerGetTemplate(stackContext.getCloudContext(), stackContext.getCloudCredential());
        verify(templateService).waitGetTemplate(templateRequest);
        verify(stackService).getByIdWithListsInTransaction(3L);
        verify(freshStack).setTemplate(newTemplate);
        verify(freshStack).setInstanceGroups(staleStack.getInstanceGroups());
        verify(stackService).save(freshStack);
    }

    @Test
    void testUpdateInstanceGroupAttributesAndTemplateIfNoChange() {
        Stack staleStack = mock(Stack.class);
        InstanceGroup instanceGroup = mock(InstanceGroup.class);
        Json attributes = new Json("{\"key\":\"value\"}");

        when(staleStack.getInstanceGroups()).thenReturn(Set.of(instanceGroup));
        when(staleStack.getCloudPlatform()).thenReturn("AWS");
        when(instanceGroup.getGroupName()).thenReturn("group1");
        when(instanceGroup.getAttributes()).thenReturn(attributes);
        when(defaultInstanceGroupProvider.createAttributes(CloudPlatform.fromName("AWS"), staleStack.getName(), "group1"))
                .thenReturn(attributes);

        updater.updateInstanceGroupAttributesAndTemplateIfDefaultDifferent(stackContext, staleStack);

        verify(templateService, never()).triggerGetTemplate(any(CloudContext.class), any(CloudCredential.class));
        verify(stackService, never()).save(any());
    }
}