package com.sequenceiq.cloudbreak.converter.v4.stacks;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.response.DependentHostGroupsV4Response;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessorFactory;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.stack.DependentRolesHealthCheckService;

@ExtendWith(MockitoExtension.class)
class StackToDependentHostGroupV4ResponseConverterTest {

    @Mock
    private CmTemplateProcessorFactory cmTemplateProcessorFactory;

    @Mock
    private DependentRolesHealthCheckService dependentRolesHealthCheckService;

    @InjectMocks
    private StackToDependentHostGroupV4ResponseConverter underTest;

    @Test
    void testConvertWhenTheStackBlueprintIsNull() {
        StackDto stackDto = mock(StackDto.class);
        when(stackDto.getBlueprint()).thenReturn(null);

        DependentHostGroupsV4Response dependentHostGroupsV4Response = assertDoesNotThrow(() -> underTest.convert(stackDto, Set.of()));

        verifyNoInteractions(cmTemplateProcessorFactory);
        verifyNoInteractions(dependentRolesHealthCheckService);
        assertTrue(dependentHostGroupsV4Response.getDependentHostGroups().isEmpty());
    }

    @Test
    void testConvertWhenTheStackBlueprintIsNotNull() {
        StackDto stackDto = mock(StackDto.class, Answers.RETURNS_DEEP_STUBS);
        String blueprintText = "example blueprint";
        when(stackDto.getBlueprintJsonText()).thenReturn(blueprintText);

        DependentHostGroupsV4Response dependentHostGroupsV4Response = assertDoesNotThrow(() -> underTest.convert(stackDto, Set.of()));

        verifyNoInteractions(dependentRolesHealthCheckService);
        verify(cmTemplateProcessorFactory, times(1)).get(blueprintText);
        assertTrue(dependentHostGroupsV4Response.getDependentHostGroups().isEmpty());
    }
}