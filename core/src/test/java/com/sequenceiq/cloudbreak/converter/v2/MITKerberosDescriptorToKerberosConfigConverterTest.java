package com.sequenceiq.cloudbreak.converter.v2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Before;
import org.junit.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.kerberos.requests.MITKerberosDescriptor;
import com.sequenceiq.cloudbreak.domain.KerberosConfig;
import com.sequenceiq.cloudbreak.type.KerberosType;

public class MITKerberosDescriptorToKerberosConfigConverterTest {

    private static final String URL = "http://someurl.com";

    private static final String ADMIN_URL = "http://someadminurl.com";

    private static final String REALM = "someRealm";

    private static final Boolean TCP_ALLOWED = true;

    private static final Boolean VERIFY_KDC_TRUST = false;

    private static final String PASSWORD = "somePassword";

    private static final String PRINCIPAL = "somePrincipal";

    private static final String DOMAIN = "someDomain";

    private static final String NAMESERVERS = "1.1.1.1";

    private MITKerberosDescriptorToKerberosConfigConverter underTest;

    private MITKerberosDescriptor source;

    @Before
    public void setUp() {
        underTest = new MITKerberosDescriptorToKerberosConfigConverter();
        source = new MITKerberosDescriptor();
        source.setAdminUrl(ADMIN_URL);
        source.setRealm(REALM);
        source.setUrl(URL);
        source.setPrincipal(PRINCIPAL);
        source.setDomain(DOMAIN);
        source.setNameServers(NAMESERVERS);
        source.setPassword(PASSWORD);
        source.setVerifyKdcTrust(VERIFY_KDC_TRUST);
        source.setTcpAllowed(TCP_ALLOWED);
    }

    @Test
    public void testConvert() {
        KerberosConfig result = underTest.convert(source);

        assertNotNull(result);
        assertEquals(URL, result.getUrl());
        assertEquals(REALM, result.getRealm());
        assertEquals(ADMIN_URL, result.getAdminUrl());
        assertEquals(PRINCIPAL, result.getPrincipal());
        assertEquals(KerberosType.MIT, result.getType());
        assertEquals(DOMAIN, result.getDomain());
        assertEquals(NAMESERVERS, result.getNameServers());
        assertEquals(PASSWORD, result.getPassword());
        assertEquals(VERIFY_KDC_TRUST, result.getVerifyKdcTrust());
        assertEquals(TCP_ALLOWED, result.isTcpAllowed());
        assertNull(result.getName());
        assertNull(result.getDescription());
    }

}