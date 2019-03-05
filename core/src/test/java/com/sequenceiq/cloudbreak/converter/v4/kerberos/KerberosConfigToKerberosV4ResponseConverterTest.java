package com.sequenceiq.cloudbreak.converter.v4.kerberos;

import static com.sequenceiq.cloudbreak.type.KerberosType.ACTIVE_DIRECTORY;
import static com.sequenceiq.cloudbreak.type.KerberosType.AMBARI_DESCRIPTOR;
import static com.sequenceiq.cloudbreak.type.KerberosType.FREEIPA;
import static com.sequenceiq.cloudbreak.type.KerberosType.MIT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.LinkedHashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.convert.ConversionService;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.responses.SecretV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.kerberos.responses.KerberosV4Response;
import com.sequenceiq.cloudbreak.domain.KerberosConfig;
import com.sequenceiq.cloudbreak.domain.view.EnvironmentView;
import com.sequenceiq.cloudbreak.type.KerberosType;

@RunWith(Parameterized.class)
public class KerberosConfigToKerberosV4ResponseConverterTest {

    private static final String URL = "http://someurl.com";

    private static final String ADMIN_URL = "http://someadminurl.com";

    private static final String REALM = "someRealm";

    private static final String LDAP_URL = "http://someldapurl.com";

    private static final String CONTAINER_DN = "cb";

    private static final Boolean TCP_ALLOWED = true;

    private static final Boolean VERIFY_KDC_TRUST = false;

    private static final String ADMIN_SECRET = "someAdminSecret";

    private static final String PASSWORD_SECRET = "somePasswordSecret";

    private static final String PRINCIPAL_SECRET = "somePrincipalSecret";

    private static final String DESCRIPTOR_SECRET = "{\"someSecretDescriptorKey\": \"someSecretDescriptorValue\"}";

    private static final String KRB_5_CONF_SECRET = "{\"someSecretKrb5ConfKey\": \"someSecretKrb5ConfValue\"}";

    private static final String DOMAIN = "someDomain";

    private static final String NAMESERVERS = "1.1.1.1";

    private static final String NAME = "someKerberosConfigName";

    private static final String DESCRIPTION = "someDescription";

    private static final Long ID = 1L;

    private Set<EnvironmentView> environmentViews = new LinkedHashSet<>();

    @Mock
    private ConversionService conversionService;

    @InjectMocks
    private KerberosConfigToKerberosV4ResponseConverter underTest;

    @Mock
    private KerberosConfig source;

    @Mock
    private SecretV4Response adminSecretResponse;

    @Mock
    private SecretV4Response passwordSecretResponse;

    @Mock
    private SecretV4Response principalSecretResponse;

    @Mock
    private SecretV4Response descriptorSecretResponse;

    @Mock
    private SecretV4Response krb5ConfSecretResponse;

    private KerberosType kerberosType;

    public KerberosConfigToKerberosV4ResponseConverterTest(KerberosType kerberosType) {
        this.kerberosType = kerberosType;
    }

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(source.getType()).thenReturn(kerberosType);
        when(source.getUrl()).thenReturn(URL);
        when(source.getAdminUrl()).thenReturn(ADMIN_URL);
        when(source.getRealm()).thenReturn(REALM);
        when(source.getLdapUrl()).thenReturn(LDAP_URL);
        when(source.getContainerDn()).thenReturn(CONTAINER_DN);
        when(source.isTcpAllowed()).thenReturn(TCP_ALLOWED);
        when(source.getAdminSecret()).thenReturn(ADMIN_SECRET);
        when(conversionService.convert(ADMIN_SECRET, SecretV4Response.class)).thenReturn(adminSecretResponse);
        when(source.getPasswordSecret()).thenReturn(PASSWORD_SECRET);
        when(conversionService.convert(PASSWORD_SECRET, SecretV4Response.class)).thenReturn(passwordSecretResponse);
        when(source.getPrincipalSecret()).thenReturn(PRINCIPAL_SECRET);
        when(conversionService.convert(PRINCIPAL_SECRET, SecretV4Response.class)).thenReturn(principalSecretResponse);
        when(source.getDescriptorSecret()).thenReturn(DESCRIPTOR_SECRET);
        when(conversionService.convert(DESCRIPTOR_SECRET, SecretV4Response.class)).thenReturn(descriptorSecretResponse);
        when(source.getKrb5ConfSecret()).thenReturn(KRB_5_CONF_SECRET);
        when(conversionService.convert(KRB_5_CONF_SECRET, SecretV4Response.class)).thenReturn(krb5ConfSecretResponse);
        when(source.getDomain()).thenReturn(DOMAIN);
        when(source.getNameServers()).thenReturn(NAMESERVERS);
        when(source.getName()).thenReturn(NAME);
        when(source.getDescription()).thenReturn(DESCRIPTION);
        when(source.getId()).thenReturn(ID);
        when(source.getEnvironments()).thenReturn(environmentViews);
        when(source.getVerifyKdcTrust()).thenReturn(VERIFY_KDC_TRUST);
    }

    @Parameters(name = "[{index}] Kerberos type: {0}")
    public static Object[] data() {
        return new Object[]{ACTIVE_DIRECTORY, MIT, FREEIPA, AMBARI_DESCRIPTOR};
    }

    @Test
    public void testConvertAgainstDifferentKindOfKerberosTypes() {
        KerberosV4Response result = underTest.convert(source);

        assertNotNull(result);
        assertEquals(URL, result.getUrl());
        assertEquals(ADMIN_URL, result.getAdminUrl());
        assertEquals(REALM, result.getRealm());
        assertEquals(LDAP_URL, result.getLdapUrl());
        assertEquals(CONTAINER_DN, result.getContainerDn());
        assertEquals(TCP_ALLOWED, result.getTcpAllowed());
        assertEquals(adminSecretResponse, result.getAdmin());
        assertEquals(passwordSecretResponse, result.getPassword());
        assertEquals(principalSecretResponse, result.getPrincipal());
        assertEquals(descriptorSecretResponse, result.getDescriptor());
        assertEquals(krb5ConfSecretResponse, result.getKrb5Conf());
        assertEquals(DOMAIN, result.getDomain());
        assertEquals(NAMESERVERS, result.getNameServers());
        assertEquals(NAME, result.getName());
        assertEquals(DESCRIPTION, result.getDescription());
        assertEquals(ID, result.getId());
        assertTrue(result.getEnvironments().isEmpty());
        assertEquals(VERIFY_KDC_TRUST, result.getVerifyKdcTrust());
        verify(conversionService, times(5)).convert(anyString(), eq(SecretV4Response.class));
        verify(conversionService, times(1)).convert(ADMIN_SECRET, SecretV4Response.class);
        verify(conversionService, times(1)).convert(PASSWORD_SECRET, SecretV4Response.class);
        verify(conversionService, times(1)).convert(PRINCIPAL_SECRET, SecretV4Response.class);
        verify(conversionService, times(1)).convert(DESCRIPTOR_SECRET, SecretV4Response.class);
        verify(conversionService, times(1)).convert(KRB_5_CONF_SECRET, SecretV4Response.class);
    }

}