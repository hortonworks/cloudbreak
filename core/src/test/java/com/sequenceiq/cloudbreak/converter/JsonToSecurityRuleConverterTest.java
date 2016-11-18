package com.sequenceiq.cloudbreak.converter;

import java.util.Collections;

import org.junit.Before;
import org.junit.Test;

import com.sequenceiq.cloudbreak.api.model.SecurityRuleRequest;
import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.domain.SecurityRule;

public class JsonToSecurityRuleConverterTest extends AbstractJsonConverterTest<SecurityRuleRequest> {

    private JsonToSecurityRuleConverter underTest;

    @Before
    public void setUp() {
        underTest = new JsonToSecurityRuleConverter();
    }

    @Test
    public void testConvert() {
        SecurityRule result = underTest.convert(createRequest("80,22,9443,1-65535"));
        assertAllFieldsNotNull(result, Collections.singletonList("securityGroup"));
    }

    @Test
    public void testConvert2() {
        SecurityRule result = underTest.convert(createRequest("22,9443"));
        assertAllFieldsNotNull(result, Collections.singletonList("securityGroup"));
    }

    @Test
    public void testConvert3() {
        SecurityRule result = underTest.convert(createRequest("1-65535"));
        assertAllFieldsNotNull(result, Collections.singletonList("securityGroup"));
    }

    @Test
    public void testConvert4() {
        SecurityRule result = underTest.convert(createRequest("20-23,9400-9500"));
        assertAllFieldsNotNull(result, Collections.singletonList("securityGroup"));
    }

    @Test
    public void testConvert5() {
        SecurityRule result = underTest.convert(createRequest("22,9400-9500"));
        assertAllFieldsNotNull(result, Collections.singletonList("securityGroup"));
    }

    @Test(expected = BadRequestException.class)
    public void testInvalidPorts() {
        underTest.convert(createRequest("0,22,443,70000"));
    }

    @Test(expected = BadRequestException.class)
    public void testInvalidPorts2() {
        underTest.convert(createRequest("0-65535"));
    }

    @Test(expected = BadRequestException.class)
    public void testInvalidPorts3() {
        underTest.convert(createRequest("0"));
    }

    @Override
    public Class<SecurityRuleRequest> getRequestClass() {
        return SecurityRuleRequest.class;
    }

    private SecurityRuleRequest createRequest(String ports) {
        SecurityRuleRequest request = new SecurityRuleRequest();
        request.setPorts(ports);
        request.setModifiable(true);
        request.setProtocol("tcp");
        request.setSubnet("0.0.0.0/0");
        return request;
    }
}
