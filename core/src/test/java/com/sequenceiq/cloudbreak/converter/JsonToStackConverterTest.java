package com.sequenceiq.cloudbreak.converter;

import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;

import java.util.Arrays;
import java.util.HashSet;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;

import com.sequenceiq.cloudbreak.controller.json.StackRequest;
import com.sequenceiq.cloudbreak.domain.FailurePolicy;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.Stack;

public class JsonToStackConverterTest extends AbstractJsonConverterTest<StackRequest> {

    @InjectMocks
    private JsonToStackConverter underTest;

    @Mock
    private ConversionService conversionService;

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
                .willReturn(new FailurePolicy());
        // WHEN
        Stack stack = underTest.convert(getRequest("stack/stack.json"));
        // THEN
        assertAllFieldsNotNull(stack, Arrays.asList("description", "statusReason", "cluster", "credential",
                "template", "network", "securityConfig", "securityGroup", "version", "created", "platformVariant"));
    }

    @Override
    public Class<StackRequest> getRequestClass() {
        return StackRequest.class;
    }
}
