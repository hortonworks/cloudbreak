package com.sequenceiq.cloudbreak.converter;

import java.util.Collections;

import org.junit.Before;
import org.junit.Test;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.api.model.LdapConfigResponse;
import com.sequenceiq.cloudbreak.domain.LdapConfig;

public class LdapConfigToJsonConverterTest extends AbstractEntityConverterTest<LdapConfig> {

    private LdapConfigToJsonConverter underTest;

    @Before
    public void setUp() {
        underTest = new LdapConfigToJsonConverter();
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
