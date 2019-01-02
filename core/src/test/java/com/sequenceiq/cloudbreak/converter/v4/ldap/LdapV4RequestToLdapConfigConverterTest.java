package com.sequenceiq.cloudbreak.converter.v4.ldap;

import java.util.Collections;

import org.junit.Before;
import org.junit.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.ldaps.requests.LdapV4Request;
import com.sequenceiq.cloudbreak.converter.AbstractJsonConverterTest;
import com.sequenceiq.cloudbreak.converter.v4.ldaps.LdapV4RequestToLdapConfigConverter;
import com.sequenceiq.cloudbreak.domain.LdapConfig;

public class LdapV4RequestToLdapConfigConverterTest extends AbstractJsonConverterTest<LdapV4Request> {

    private LdapV4RequestToLdapConfigConverter underTest;

    @Before
    public void setUp() {
        underTest = new LdapV4RequestToLdapConfigConverter();
    }

    @Test
    public void testConvert() {
        // GIVEN
        // WHEN
        LdapConfig result = underTest.convert(getRequest("ldap_config.json"));
        // THEN
        assertAllFieldsNotNull(result, Collections.singletonList("environments"));
    }

    @Override
    public Class<LdapV4Request> getRequestClass() {
        return LdapV4Request.class;
    }
}
