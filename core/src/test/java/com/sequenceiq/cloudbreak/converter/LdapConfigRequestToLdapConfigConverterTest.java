package com.sequenceiq.cloudbreak.converter;

import java.util.Collections;

import org.junit.Before;
import org.junit.Test;

import com.sequenceiq.cloudbreak.api.model.ldap.LdapConfigRequest;
import com.sequenceiq.cloudbreak.domain.LdapConfig;

public class LdapConfigRequestToLdapConfigConverterTest extends AbstractJsonConverterTest<LdapConfigRequest> {

    private LdapConfigRequestToLdapConfigConverter underTest;

    @Before
    public void setUp() {
        underTest = new LdapConfigRequestToLdapConfigConverter();
    }

    @Test
    public void testConvert() {
        // GIVEN
        // WHEN
        LdapConfig result = underTest.convert(getRequest("stack/ldap_config.json"));
        // THEN
        assertAllFieldsNotNull(result, Collections.singletonList("environments"));
    }

    @Override
    public Class<LdapConfigRequest> getRequestClass() {
        return LdapConfigRequest.class;
    }
}
