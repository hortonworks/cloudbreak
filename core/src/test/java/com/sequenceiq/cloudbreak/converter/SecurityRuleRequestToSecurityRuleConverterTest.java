package com.sequenceiq.cloudbreak.converter;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.util.requests.SecurityRuleV4Request;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.converter.v4.stacks.instancegroup.securitygroup.securityrule.SecurityRuleV4RequestToSecurityRuleConverter;
import com.sequenceiq.cloudbreak.domain.SecurityRule;

class SecurityRuleRequestToSecurityRuleConverterTest extends AbstractJsonConverterTest<SecurityRuleV4Request> {

    private SecurityRuleV4RequestToSecurityRuleConverter underTest = new SecurityRuleV4RequestToSecurityRuleConverter();

    @Test
    void testConvert() {
        SecurityRule result = underTest.convert(createRequest("80", "22", "9443", "1-65535"));
        assertAllFieldsNotNull(result, Collections.singletonList("securityGroup"));
    }

    @Test
    void testConvert2() {
        SecurityRule result = underTest.convert(createRequest("22", "9443"));
        assertAllFieldsNotNull(result, Collections.singletonList("securityGroup"));
    }

    @Test
    void testConvert3() {
        SecurityRule result = underTest.convert(createRequest("1-65535"));
        assertAllFieldsNotNull(result, Collections.singletonList("securityGroup"));
    }

    @Test
    void testConvert4() {
        SecurityRule result = underTest.convert(createRequest("20-23", "9400-9500"));
        assertAllFieldsNotNull(result, Collections.singletonList("securityGroup"));
    }

    @Test
    void testConvert5() {
        SecurityRule result = underTest.convert(createRequest("22", "9400-9500"));
        assertAllFieldsNotNull(result, Collections.singletonList("securityGroup"));
    }

    @Test
    void testInvalidPorts() {
        assertThrows(BadRequestException.class, () -> underTest.convert(createRequest("0", "22", "443", "70000")), "Ports must be in range of 1-65535");
    }

    @Test
    void testInvalidPorts2() {
        assertThrows(BadRequestException.class, () -> underTest.convert(createRequest("0-65535")), "Ports must be in range of 1-65535");
    }

    @Test
    void testInvalidPorts3() {
        assertThrows(BadRequestException.class, () -> underTest.convert(createRequest("0")), "Ports must be in range of 1-65535");
    }

    @Override
    public Class<SecurityRuleV4Request> getRequestClass() {
        return SecurityRuleV4Request.class;
    }

    private SecurityRuleV4Request createRequest(String... ports) {
        SecurityRuleV4Request request = new SecurityRuleV4Request();
        request.setPorts(Arrays.asList(ports));
        request.setModifiable(true);
        request.setProtocol("tcp");
        request.setSubnet("0.0.0.0/0");
        return request;
    }
}
