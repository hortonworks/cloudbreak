package com.sequenceiq.cloudbreak.converter;

import java.util.HashSet;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.convert.ConversionService;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.securitygroup.SecurityGroupV4Response;
import com.sequenceiq.cloudbreak.api.util.ConverterUtil;
import com.sequenceiq.cloudbreak.converter.v4.stacks.instancegroup.securitygroup.SecurityGroupToSecurityGroupResponseConverter;
import com.sequenceiq.cloudbreak.domain.SecurityGroup;

@RunWith(MockitoJUnitRunner.class)
public class SecurityGroupToSecurityGroupResponseConverterTest extends AbstractEntityConverterTest<SecurityGroup> {

    @InjectMocks
    private SecurityGroupToSecurityGroupResponseConverter underTest;

    @Mock
    private ConverterUtil converterUtil;

    @Mock
    private ConversionService conversionService;

    @Test
    public void testConvert() {
        // GIVEN
        // WHEN
        SecurityGroupV4Response result = underTest.convert(getSource());
        // THEN
        assertAllFieldsNotNull(result);
    }

    @Override
    public SecurityGroup createSource() {
        return TestUtil.securityGroup(new HashSet<>());
    }

}
