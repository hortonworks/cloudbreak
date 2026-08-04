package com.sequenceiq.freeipa.service.stack;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.freeipa.entity.InstanceGroup;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.entity.Template;
import com.sequenceiq.freeipa.service.stack.instance.InstanceGroupService;
import com.sequenceiq.freeipa.service.stack.instance.TemplateService;

@ExtendWith(MockitoExtension.class)
class FreeIpaInstanceTypeUpdateServiceTest {

    private static final String ENVIRONMENT_CRN = "crn:cdp:environments:us-west-1:cloudera:environment:abc-123";

    private static final String ACCOUNT_ID = "cloudera";

    private static final String NEW_INSTANCE_TYPE = "m5.2xlarge";

    private static final Long STACK_ID = 1L;

    @Mock
    private StackService stackService;

    @Mock
    private InstanceGroupService instanceGroupService;

    @Mock
    private TemplateService templateService;

    @InjectMocks
    private FreeIpaInstanceTypeUpdateService underTest;

    @Test
    void testUpdateInstanceTypeUpdatesAllGroups() {
        Stack stack = new Stack();
        stack.setId(STACK_ID);
        InstanceGroup group1 = createInstanceGroup("master", "m5.xlarge");
        InstanceGroup group2 = createInstanceGroup("worker", "m5.large");

        when(stackService.getByEnvironmentCrnAndAccountId(ENVIRONMENT_CRN, ACCOUNT_ID)).thenReturn(stack);
        when(instanceGroupService.findByStackId(STACK_ID)).thenReturn(Set.of(group1, group2));

        try (MockedStatic<ThreadBasedUserCrnProvider> mockedStatic = Mockito.mockStatic(ThreadBasedUserCrnProvider.class)) {
            mockedStatic.when(ThreadBasedUserCrnProvider::getAccountId).thenReturn(ACCOUNT_ID);
            underTest.updateInstanceType(ENVIRONMENT_CRN, NEW_INSTANCE_TYPE);
        }

        verify(templateService).save(group1.getTemplate());
        verify(templateService).save(group2.getTemplate());
        assert group1.getTemplate().getInstanceType().equals(NEW_INSTANCE_TYPE);
        assert group2.getTemplate().getInstanceType().equals(NEW_INSTANCE_TYPE);
    }

    @Test
    void testUpdateInstanceTypeSingleGroup() {
        Stack stack = new Stack();
        stack.setId(STACK_ID);
        InstanceGroup group = createInstanceGroup("master", "m5.xlarge");

        when(stackService.getByEnvironmentCrnAndAccountId(ENVIRONMENT_CRN, ACCOUNT_ID)).thenReturn(stack);
        when(instanceGroupService.findByStackId(STACK_ID)).thenReturn(Set.of(group));

        try (MockedStatic<ThreadBasedUserCrnProvider> mockedStatic = Mockito.mockStatic(ThreadBasedUserCrnProvider.class)) {
            mockedStatic.when(ThreadBasedUserCrnProvider::getAccountId).thenReturn(ACCOUNT_ID);
            underTest.updateInstanceType(ENVIRONMENT_CRN, NEW_INSTANCE_TYPE);
        }

        verify(templateService).save(group.getTemplate());
        assert group.getTemplate().getInstanceType().equals(NEW_INSTANCE_TYPE);
    }

    private InstanceGroup createInstanceGroup(String groupName, String instanceType) {
        Template template = new Template();
        template.setInstanceType(instanceType);
        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setGroupName(groupName);
        instanceGroup.setTemplate(template);
        return instanceGroup;
    }
}
