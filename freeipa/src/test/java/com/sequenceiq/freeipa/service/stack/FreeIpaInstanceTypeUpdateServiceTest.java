package com.sequenceiq.freeipa.service.stack;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.flow.core.FlowLogService;
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

    @Mock
    private FlowLogService flowLogService;

    @InjectMocks
    private FreeIpaInstanceTypeUpdateService underTest;

    @Test
    void testUpdateInstanceTypeUpdatesAllGroups() {
        Stack stack = createStack();
        InstanceGroup group1 = createInstanceGroup("master", "m5.xlarge");
        InstanceGroup group2 = createInstanceGroup("worker", "m5.large");

        when(stackService.getByEnvironmentCrnAndAccountId(ENVIRONMENT_CRN, ACCOUNT_ID)).thenReturn(stack);
        when(flowLogService.isOtherFlowRunning(STACK_ID)).thenReturn(false);
        when(instanceGroupService.findByStackId(STACK_ID)).thenReturn(Set.of(group1, group2));

        try (MockedStatic<ThreadBasedUserCrnProvider> mockedStatic = Mockito.mockStatic(ThreadBasedUserCrnProvider.class)) {
            mockedStatic.when(ThreadBasedUserCrnProvider::getAccountId).thenReturn(ACCOUNT_ID);
            underTest.updateInstanceType(ENVIRONMENT_CRN, NEW_INSTANCE_TYPE);
        }

        verify(templateService).save(group1.getTemplate());
        verify(templateService).save(group2.getTemplate());
        assertEquals(NEW_INSTANCE_TYPE, group1.getTemplate().getInstanceType());
        assertEquals(NEW_INSTANCE_TYPE, group2.getTemplate().getInstanceType());
    }

    @Test
    void testUpdateInstanceTypeThrowsWhenFlowRunning() {
        Stack stack = createStack();

        when(stackService.getByEnvironmentCrnAndAccountId(ENVIRONMENT_CRN, ACCOUNT_ID)).thenReturn(stack);
        when(flowLogService.isOtherFlowRunning(STACK_ID)).thenReturn(true);

        try (MockedStatic<ThreadBasedUserCrnProvider> mockedStatic = Mockito.mockStatic(ThreadBasedUserCrnProvider.class)) {
            mockedStatic.when(ThreadBasedUserCrnProvider::getAccountId).thenReturn(ACCOUNT_ID);
            assertThrows(BadRequestException.class, () -> underTest.updateInstanceType(ENVIRONMENT_CRN, NEW_INSTANCE_TYPE));
        }

        verifyNoInteractions(templateService);
    }

    private Stack createStack() {
        Stack stack = new Stack();
        stack.setId(STACK_ID);
        stack.setName("test-freeipa");
        return stack;
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
