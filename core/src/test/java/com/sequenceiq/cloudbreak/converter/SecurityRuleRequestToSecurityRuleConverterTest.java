package com.sequenceiq.cloudbreak.converter;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.sequenceiq.cloudbreak.api.endpoint.v4.util.requests.SecurityRuleV4Request;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.converter.v4.stacks.instancegroup.securitygroup.securityrule.SecurityRuleV4RequestToSecurityRuleConverter;
import com.sequenceiq.cloudbreak.domain.SecurityRule;

public class SecurityRuleRequestToSecurityRuleConverterTest extends AbstractJsonConverterTest<SecurityRuleV4Request> {

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    private SecurityRuleV4RequestToSecurityRuleConverter underTest;

    @Before
    public void setUp() {
        underTest = new SecurityRuleV4RequestToSecurityRuleConverter();
    }

    @Test
    public void testConvert() {
        SecurityRule result = underTest.convert(createRequest("80", "22", "9443", "1-65535"));
        assertAllFieldsNotNull(result, Collections.singletonList("securityGroup"));
    }

    @Test
    public void testConvert2() {
        SecurityRule result = underTest.convert(createRequest("22", "9443"));
        assertAllFieldsNotNull(result, Collections.singletonList("securityGroup"));
    }

    @Test
    public void testConvert3() {
        SecurityRule result = underTest.convert(createRequest("1-65535"));
        assertAllFieldsNotNull(result, Collections.singletonList("securityGroup"));
    }

    @Test
    public void testConvert4() {
        SecurityRule result = underTest.convert(createRequest("20-23", "9400-9500"));
        assertAllFieldsNotNull(result, Collections.singletonList("securityGroup"));
    }

    @Test
    public void testConvert5() {
        SecurityRule result = underTest.convert(createRequest("22", "9400-9500"));
        assertAllFieldsNotNull(result, Collections.singletonList("securityGroup"));
    }

    @Test
    public void testInvalidPorts() {
        thrown.expect(BadRequestException.class);
        thrown.expectMessage("Ports must be in range of 1-65535");
        underTest.convert(createRequest("0", "22", "443", "70000"));
    }

    @Test
    public void testInvalidPorts2() {
        thrown.expect(BadRequestException.class);
        thrown.expectMessage("Ports must be in range of 1-65535");
        underTest.convert(createRequest("0-65535"));
    }

    @Test
    public void testInvalidPorts3() {
        thrown.expect(BadRequestException.class);
        thrown.expectMessage("Ports must be in range of 1-65535");
        underTest.convert(createRequest("0"));
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
