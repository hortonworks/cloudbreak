package com.sequenceiq.cloudbreak.converter.v4.kerberos;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;

import org.apache.commons.codec.binary.Base64;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.cloudbreak.api.endpoint.v4.kerberos.requests.AmbariKerberosDescriptor;
import com.sequenceiq.cloudbreak.domain.KerberosConfig;
import com.sequenceiq.cloudbreak.type.KerberosType;

public class AmbariKerberosDescriptorToKerberosConfigConverterTest {

    private static final String DESCRIPTOR = "{}";

    private static final String DESCRIPTOR_BASE64 = Base64.encodeBase64String(DESCRIPTOR.getBytes());

    private static final String KRB_5_CONF = "{}";

    private static final String KRB_5_CONF_BASE64 = Base64.encodeBase64String(KRB_5_CONF.getBytes());

    private static final String PRINCIPAL = "somePrincipal";

    private static final String DOMAIN = "someDomain";

    private static final String NAME_SERVERS = "1.1.1.1";

    private static final String PASSWORD = "somePassword";

    private static final Boolean VERIFY_KDC_TRUST = Boolean.FALSE;

    private static final Boolean TPC_ALLOWED = Boolean.TRUE;

    private AmbariKerberosDescriptorToKerberosConfigConverter underTest;

    @Mock
    private AmbariKerberosDescriptor source;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        underTest = new AmbariKerberosDescriptorToKerberosConfigConverter();
        when(source.getDescriptor()).thenReturn(DESCRIPTOR_BASE64);
        when(source.getKrb5Conf()).thenReturn(KRB_5_CONF_BASE64);
        when(source.getPrincipal()).thenReturn(PRINCIPAL);
        when(source.getType()).thenReturn(KerberosType.AMBARI_DESCRIPTOR);
        when(source.getDomain()).thenReturn(DOMAIN);
        when(source.getNameServers()).thenReturn(NAME_SERVERS);
        when(source.getPassword()).thenReturn(PASSWORD);
        when(source.getVerifyKdcTrust()).thenReturn(VERIFY_KDC_TRUST);
        when(source.getTcpAllowed()).thenReturn(TPC_ALLOWED);
    }

    @Test
    public void testConvert() {
        KerberosConfig result = underTest.convert(source);

        assertNotNull(result);
        assertEquals(DESCRIPTOR, result.getDescriptor());
        assertEquals(KRB_5_CONF, result.getKrb5Conf());
        assertEquals(PRINCIPAL, result.getPrincipal());
        assertEquals(KerberosType.AMBARI_DESCRIPTOR, result.getType());
        assertEquals(DOMAIN, result.getDomain());
        assertEquals(NAME_SERVERS, result.getNameServers());
        assertEquals(PASSWORD, result.getPassword());
        assertEquals(VERIFY_KDC_TRUST, result.getVerifyKdcTrust());
        assertEquals(TPC_ALLOWED, result.isTcpAllowed());
        assertNull(result.getName());
        assertNull(result.getDescription());
    }

}