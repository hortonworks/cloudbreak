package com.sequenceiq.cloudbreak.converter;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.when;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.securitygroup.SecurityGroupV4Request;
import com.sequenceiq.cloudbreak.common.converter.MissingResourceNameGenerator;
import com.sequenceiq.cloudbreak.common.type.APIResourceType;
import com.sequenceiq.cloudbreak.converter.v4.stacks.instancegroup.securitygroup.SecurityGroupV4RequestToSecurityGroupConverter;
import com.sequenceiq.cloudbreak.converter.v4.stacks.instancegroup.securitygroup.securityrule.SecurityRuleV4RequestToSecurityRuleConverter;
import com.sequenceiq.cloudbreak.domain.SecurityGroup;
import com.sequenceiq.cloudbreak.domain.SecurityRule;

@RunWith(MockitoJUnitRunner.class)
public class SecurityGroupRequestToSecurityGroupConverterTest extends AbstractJsonConverterTest<SecurityGroupV4Request> {

    @InjectMocks
    private SecurityGroupV4RequestToSecurityGroupConverter underTest;

    @Mock
    private MissingResourceNameGenerator missingResourceNameGenerator;

    @Mock
    private SecurityRuleV4RequestToSecurityRuleConverter securityRuleV4RequestToSecurityRuleConverter;

    @Before
    public void setUp() {
        when(missingResourceNameGenerator.generateName(APIResourceType.SECURITY_GROUP)).thenReturn("name");
    }

    @Test
    public void testConvert() {
        // GIVEN
        given(securityRuleV4RequestToSecurityRuleConverter.convert(any())).willReturn(new SecurityRule());
        // WHEN
        SecurityGroup result = underTest.convert(getRequest("security-group/security-group.json"));
        // THEN
        assertAllFieldsNotNull(result, Arrays.asList("cloudPlatform", "description"));
    }

    @Test
    public void testConvertWithNoSecurityRules() {
        // WHEN
        SecurityGroup result = underTest.convert(getRequest("security-group/security-group-id.json"));
        // THEN
        assertAllFieldsNotNull(result, Arrays.asList("cloudPlatform", "description"));
    }

    @Override
    public Class<SecurityGroupV4Request> getRequestClass() {
        return SecurityGroupV4Request.class;
    }
}
