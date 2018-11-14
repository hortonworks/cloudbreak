package com.sequenceiq.cloudbreak.converter;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.convert.ConversionService;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.api.model.SecretResponse;
import com.sequenceiq.cloudbreak.api.model.ldap.LdapConfigResponse;
import com.sequenceiq.cloudbreak.domain.LdapConfig;

public class LdapConfigToLdapConfigResponseConverterTest extends AbstractEntityConverterTest<LdapConfig> {

    @InjectMocks
    private LdapConfigToLdapConfigResponseConverter underTest;

    @Mock
    private ConversionService conversionService;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(conversionService.convert(anyString(), any())).thenAnswer(invocation -> new SecretResponse(null, invocation.getArgument(0)));
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
