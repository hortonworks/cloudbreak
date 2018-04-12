package com.sequenceiq.cloudbreak.converter;

import java.util.Collections;

import org.junit.Before;
import org.junit.Test;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.api.model.ldap.LdapConfigResponse;
import com.sequenceiq.cloudbreak.domain.LdapConfig;

public class LdapConfigToLdapConfigResponseConverterTest extends AbstractEntityConverterTest<LdapConfig> {

    private LdapConfigToLdapConfigResponseConverter underTest;

    @Before
    public void setUp() {
        underTest = new LdapConfigToLdapConfigResponseConverter();
    }

    @Test
    public void testConvert() {
        // GIVEN
        // WHEN
        LdapConfigResponse result = underTest.convert(getSource());
        // THEN
        assertAllFieldsNotNull(result, Collections.singletonList("id"));
    }

    @Override
    public LdapConfig createSource() {
        return TestUtil.ldapConfig();
    }
}
