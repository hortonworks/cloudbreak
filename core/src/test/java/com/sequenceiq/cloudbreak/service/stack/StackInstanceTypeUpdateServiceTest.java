package com.sequenceiq.cloudbreak.service.stack;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.template.TemplateService;

@ExtendWith(MockitoExtension.class)
class StackInstanceTypeUpdateServiceTest {

    private static final String CRN = "crn:cdp:datahub:us-west-1:cloudera:cluster:abc-123";

    private static final String GROUP_NAME = "worker";

    private static final String NEW_INSTANCE_TYPE = "m5.2xlarge";

    private static final Long STACK_ID = 1L;

    private static final Long TEMPLATE_ID = 10L;

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private InstanceGroupService instanceGroupService;

    @Mock
    private TemplateService templateService;

    @InjectMocks
    private StackInstanceTypeUpdateService underTest;

    @Test
    void testUpdateInstanceType() {
        StackDto stackDto = createStackDto();
        InstanceGroup instanceGroup = createInstanceGroup();
        Template template = instanceGroup.getTemplate();

        when(stackDtoService.getByCrn(CRN)).thenReturn(stackDto);
        when(instanceGroupService.getInstanceGroupWithTemplateAndInstancesByGroupNameInStack(STACK_ID, GROUP_NAME))
                .thenReturn(Optional.of(instanceGroup));
        when(templateService.get(TEMPLATE_ID)).thenReturn(template);

        underTest.updateInstanceType(CRN, GROUP_NAME, NEW_INSTANCE_TYPE);

        verify(templateService).savePure(template);
        assert template.getInstanceType().equals(NEW_INSTANCE_TYPE);
    }

    @Test
    void testUpdateInstanceTypeThrowsWhenGroupNotFound() {
        StackDto stackDto = createStackDto();

        when(stackDtoService.getByCrn(CRN)).thenReturn(stackDto);
        when(instanceGroupService.getInstanceGroupWithTemplateAndInstancesByGroupNameInStack(STACK_ID, GROUP_NAME))
                .thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> underTest.updateInstanceType(CRN, GROUP_NAME, NEW_INSTANCE_TYPE));
        verifyNoInteractions(templateService);
    }

    private StackDto createStackDto() {
        StackDto stackDto = org.mockito.Mockito.mock(StackDto.class);
        when(stackDto.getId()).thenReturn(STACK_ID);
        return stackDto;
    }

    private InstanceGroup createInstanceGroup() {
        Template template = new Template();
        template.setId(TEMPLATE_ID);
        template.setInstanceType("m5.xlarge");
        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setTemplate(template);
        instanceGroup.setGroupName(GROUP_NAME);
        return instanceGroup;
    }
}
