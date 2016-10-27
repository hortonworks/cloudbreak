package com.sequenceiq.cloudbreak.converter;

import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;

import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.api.model.SecurityGroupRequest;
import com.sequenceiq.cloudbreak.domain.SecurityGroup;
import com.sequenceiq.cloudbreak.domain.SecurityRule;

public class JsonToSecurityGroupConverterTest extends AbstractJsonConverterTest<SecurityGroupRequest> {

    @InjectMocks
    private JsonToSecurityGroupConverter underTest;

    @Mock
    private ConversionService conversionService;

    @Before
    public void setUp() {
        underTest = new JsonToSecurityGroupConverter();
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testConvert() {
        // GIVEN
        given(conversionService.convert(any(Object.class), any(TypeDescriptor.class), any(TypeDescriptor.class)))
                .willReturn(Sets.newConcurrentHashSet(Collections.singletonList(new SecurityRule())));
        // WHEN
        SecurityGroup result = underTest.convert(getRequest("security-group/security-group.json"));
        // THEN
        assertAllFieldsNotNull(result);
    }

    @Override
    public Class<SecurityGroupRequest> getRequestClass() {
        return SecurityGroupRequest.class;
    }
}
