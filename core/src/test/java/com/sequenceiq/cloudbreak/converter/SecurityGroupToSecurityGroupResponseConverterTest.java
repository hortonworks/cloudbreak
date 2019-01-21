package com.sequenceiq.cloudbreak.converter;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

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
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.securitygroup.SecurityGroupV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.requests.SecurityRuleV4Request;
import com.sequenceiq.cloudbreak.converter.v4.stacks.instancegroup.securitygroup.SecurityGroupToSecurityGroupResponseConverter;
import com.sequenceiq.cloudbreak.domain.SecurityGroup;

public class SecurityGroupToSecurityGroupResponseConverterTest extends AbstractEntityConverterTest<SecurityGroup> {

    @InjectMocks
    private SecurityGroupToSecurityGroupResponseConverter underTest;

    @Mock
    private ConversionService conversionService;

    @Before
    public void setUp() {
        underTest = new SecurityGroupToSecurityGroupResponseConverter();
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testConvert() {
        // GIVEN
        given(conversionService.convert(any(Object.class), any(TypeDescriptor.class), any(TypeDescriptor.class)))
                .willReturn(new ArrayList<SecurityRuleV4Request>());
        // WHEN
        SecurityGroupV4Response result = underTest.convert(getSource());
        // THEN
        assertAllFieldsNotNull(result, Arrays.asList("owner", "account", "workspace"));
    }

    @Override
    public SecurityGroup createSource() {
        return TestUtil.securityGroup(new HashSet<>());
    }
}
