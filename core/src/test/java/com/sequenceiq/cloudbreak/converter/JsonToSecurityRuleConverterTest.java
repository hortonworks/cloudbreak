package com.sequenceiq.cloudbreak.converter;

import java.util.Collections;

import org.junit.Before;
import org.junit.Test;

import com.sequenceiq.cloudbreak.api.model.SecurityRuleJson;
import com.sequenceiq.cloudbreak.domain.SecurityRule;

public class JsonToSecurityRuleConverterTest extends AbstractJsonConverterTest<SecurityRuleJson> {

    private JsonToSecurityRuleConverter underTest;

    @Before
    public void setUp() {
        underTest = new JsonToSecurityRuleConverter();
    }

    @Test
    public void testConvert() {
        // GIVEN
        // WHEN
        SecurityRule result = underTest.convert(getRequest("security-group/security-rule.json"));
        // THEN
        assertAllFieldsNotNull(result, Collections.singletonList("securityGroup"));
    }

    @Override
    public Class<SecurityRuleJson> getRequestClass() {
        return SecurityRuleJson.class;
    }
}
