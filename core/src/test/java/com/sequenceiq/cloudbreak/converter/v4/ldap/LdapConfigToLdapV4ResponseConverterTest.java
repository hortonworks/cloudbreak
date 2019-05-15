package com.sequenceiq.cloudbreak.converter.v4.ldap;

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
import com.sequenceiq.cloudbreak.api.endpoint.v4.ldaps.responses.LdapV4Response;
import com.sequenceiq.cloudbreak.converter.AbstractEntityConverterTest;
import com.sequenceiq.cloudbreak.converter.v4.ldaps.LdapConfigToLdapV4ResponseConverter;
import com.sequenceiq.cloudbreak.domain.LdapConfig;
import com.sequenceiq.secret.model.SecretResponse;

public class LdapConfigToLdapV4ResponseConverterTest extends AbstractEntityConverterTest<LdapConfig> {

    @InjectMocks
    private LdapConfigToLdapV4ResponseConverter underTest;

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
        LdapV4Response result = underTest.convert(getSource());
        // THEN
        assertAllFieldsNotNull(result, Collections.singletonList("id"));
    }

    @Override
    public LdapConfig createSource() {
        return TestUtil.ldapConfig();
    }
}
