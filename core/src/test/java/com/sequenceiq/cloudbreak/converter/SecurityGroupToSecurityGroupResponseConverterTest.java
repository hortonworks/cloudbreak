package com.sequenceiq.cloudbreak.converter;

import java.util.HashSet;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.securitygroup.SecurityGroupV4Response;
import com.sequenceiq.cloudbreak.converter.v4.stacks.instancegroup.securitygroup.SecurityGroupToSecurityGroupResponseConverter;
import com.sequenceiq.cloudbreak.domain.SecurityGroup;

class SecurityGroupToSecurityGroupResponseConverterTest extends AbstractEntityConverterTest<SecurityGroup> {

    private SecurityGroupToSecurityGroupResponseConverter underTest = new SecurityGroupToSecurityGroupResponseConverter();

    @Test
    void testConvert() {
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
