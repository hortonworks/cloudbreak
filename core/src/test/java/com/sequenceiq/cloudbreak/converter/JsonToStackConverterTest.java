package com.sequenceiq.cloudbreak.converter;

import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.api.model.InstanceGroupType;
import com.sequenceiq.cloudbreak.api.model.OrchestratorRequest;
import com.sequenceiq.cloudbreak.api.model.StackRequest;
import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.domain.FailurePolicy;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.Orchestrator;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.stack.StackParameterService;

public class JsonToStackConverterTest extends AbstractJsonConverterTest<StackRequest> {

    @InjectMocks
    private JsonToStackConverter underTest;

    @Mock
    private ConversionService conversionService;

    @Mock
    private StackParameterService stackParameterService;

    @Before
    public void setUp() {
        underTest = new JsonToStackConverter();
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testConvert() {
        InstanceGroup instanceGroup = mock(InstanceGroup.class);
        when(instanceGroup.getInstanceGroupType()).thenReturn(InstanceGroupType.GATEWAY);

        // GIVEN
        ReflectionTestUtils.setField(underTest, "defaultRegions", "AWS:eu-west-2");
        given(conversionService.convert(any(Object.class), any(TypeDescriptor.class), any(TypeDescriptor.class)))
                .willReturn(new HashSet<>(Collections.singletonList(instanceGroup)));
        given(conversionService.convert(any(Object.class), any(Class.class)))
                .willReturn(new FailurePolicy())
                .willReturn(new Orchestrator());
        given(stackParameterService.getStackParams(any(StackRequest.class))).willReturn(new ArrayList<>());
        // WHEN
        Stack stack = underTest.convert(getRequest("stack/stack.json"));
        // THEN
        assertAllFieldsNotNull(stack, Arrays.asList("description", "statusReason", "cluster", "credential", "gatewayPort",
                "template", "network", "securityConfig", "securityGroup", "version", "created", "platformVariant", "cloudPlatform", "saltPassword"));
        Assert.assertEquals("eu-west-1", stack.getRegion());
    }

    @Test(expected = BadRequestException.class)
    public void testForNoRegionAndNoDefaultRegion() {
        InstanceGroup instanceGroup = mock(InstanceGroup.class);
        when(instanceGroup.getInstanceGroupType()).thenReturn(InstanceGroupType.GATEWAY);

        // GIVEN
        given(conversionService.convert(any(Object.class), any(TypeDescriptor.class), any(TypeDescriptor.class)))
                .willReturn(new HashSet<>(Collections.singletonList(instanceGroup)));
        given(conversionService.convert(any(Object.class), any(Class.class)))
                .willReturn(new FailurePolicy())
                .willReturn(new Orchestrator());
        given(stackParameterService.getStackParams(any(StackRequest.class))).willReturn(new ArrayList<>());

        // WHEN
        StackRequest stackRequest = getRequest("stack/stack.json");
        OrchestratorRequest orchestratorRequest = new OrchestratorRequest();
        orchestratorRequest.setType("SALT");
        stackRequest.setOrchestrator(orchestratorRequest);
        stackRequest.setRegion(null);
        underTest.convert(stackRequest);
    }

    @Test
    public void testForNoRegionAndDefaultRegion() {
        InstanceGroup instanceGroup = mock(InstanceGroup.class);
        when(instanceGroup.getInstanceGroupType()).thenReturn(InstanceGroupType.GATEWAY);

        // GIVEN
        ReflectionTestUtils.setField(underTest, "defaultRegions", "AWS:eu-west-1");
        given(conversionService.convert(any(Object.class), any(TypeDescriptor.class), any(TypeDescriptor.class)))
                .willReturn(new HashSet<>(Collections.singletonList(instanceGroup)));
        given(conversionService.convert(any(Object.class), any(Class.class)))
                .willReturn(new FailurePolicy())
                .willReturn(new Orchestrator());
        given(stackParameterService.getStackParams(any(StackRequest.class))).willReturn(new ArrayList<>());

        // WHEN
        StackRequest stackRequest = getRequest("stack/stack.json");
        OrchestratorRequest orchestratorRequest = new OrchestratorRequest();
        orchestratorRequest.setType("SALT");
        stackRequest.setOrchestrator(orchestratorRequest);
        stackRequest.setRegion(null);
        Stack stack = underTest.convert(stackRequest);

        // THEN
        Assert.assertEquals("eu-west-1", stack.getRegion());
    }

    @Override
    public Class<StackRequest> getRequestClass() {
        return StackRequest.class;
    }
}
