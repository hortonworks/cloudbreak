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

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.controller.json.SecurityGroupJson;
import com.sequenceiq.cloudbreak.controller.json.SecurityRuleJson;
import com.sequenceiq.cloudbreak.domain.SecurityGroup;
import com.sequenceiq.cloudbreak.domain.SecurityRule;

public class SecurityGroupToJsonConverterTest extends AbstractEntityConverterTest<SecurityGroup> {

    @InjectMocks
    private SecurityGroupToJsonConverter underTest;

    @Mock
    private ConversionService conversionService;

    @Before
    public void setUp() {
        underTest = new SecurityGroupToJsonConverter();
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testConvert() {
        // GIVEN
        given(conversionService.convert(any(Object.class), any(TypeDescriptor.class), any(TypeDescriptor.class)))
                .willReturn(new ArrayList<SecurityRuleJson>());
        // WHEN
        SecurityGroupJson result = underTest.convert(getSource());
        // THEN
        assertAllFieldsNotNull(result, Arrays.asList("owner", "account"));
    }

    @Override
    public SecurityGroup createSource() {
        return TestUtil.securityGroup(new HashSet<SecurityRule>());
    }
}
