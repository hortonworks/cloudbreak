package com.sequenceiq.cloudbreak.converter;

import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;

import com.sequenceiq.cloudbreak.api.model.StackRequest;
import com.sequenceiq.cloudbreak.cloud.model.StackParamValidation;
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
        // GIVEN
        given(conversionService.convert(any(Object.class), any(TypeDescriptor.class), any(TypeDescriptor.class)))
                .willReturn(new HashSet<InstanceGroup>());
        given(conversionService.convert(any(Object.class), any(Class.class)))
                .willReturn(new FailurePolicy())
                .willReturn(new Orchestrator());
        given(stackParameterService.getStackParams(any(StackRequest.class))).willReturn(new ArrayList<StackParamValidation>());
        // WHEN
        Stack stack = underTest.convert(getRequest("stack/stack.json"));
        // THEN
        assertAllFieldsNotNull(stack, Arrays.asList("description", "statusReason", "cluster", "credential", "gatewayPort",
                "template", "network", "securityConfig", "securityGroup", "version", "created", "platformVariant", "cloudPlatform"));
    }

    @Override
    public Class<StackRequest> getRequestClass() {
        return StackRequest.class;
    }
}
