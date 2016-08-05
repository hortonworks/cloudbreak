package com.sequenceiq.cloudbreak.converter;

import org.junit.Before;
import org.junit.Test;

import com.sequenceiq.cloudbreak.api.model.LdapConfigRequest;
import com.sequenceiq.cloudbreak.domain.LdapConfig;

public class JsonToLdapConfigConverterTest extends AbstractJsonConverterTest<LdapConfigRequest> {

    private JsonToLdapConfigConverter underTest;

    @Before
    public void setUp() {
        underTest = new JsonToLdapConfigConverter();
    }

    @Test
    public void testConvert() {
        // GIVEN
        // WHEN
        LdapConfig result = underTest.convert(getRequest("stack/ldap_config.json"));
        // THEN
        assertAllFieldsNotNull(result);
    }

    @Override
    public Class<LdapConfigRequest> getRequestClass() {
        return LdapConfigRequest.class;
    }
}
