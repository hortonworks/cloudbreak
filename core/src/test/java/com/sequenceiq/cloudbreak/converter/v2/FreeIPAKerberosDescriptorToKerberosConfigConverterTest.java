package com.sequenceiq.cloudbreak.converter.v2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Before;
import org.junit.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.kerberos.requests.FreeIPAKerberosDescriptor;
import com.sequenceiq.cloudbreak.domain.KerberosConfig;
import com.sequenceiq.cloudbreak.type.KerberosType;

public class FreeIPAKerberosDescriptorToKerberosConfigConverterTest {

    private static final String URL = "http://someurl.com";

    private static final String ADMIN_URL = "http://someadminurl.com";

    private static final String REALM = "someRealm";

    private static final String PASSWORD = "somePassword";

    private static final Boolean VERIFY_KDC_TRUST = true;

    private static final Boolean TCP_ALLOWED = true;

    private static final String DOMAIN = "someDomain";

    private static final String NAMESERVERS = "1.1.1.1";

    private static final String PRINCIPAL = "principal";

    private FreeIPAKerberosDescriptorToKerberosConfigConverter underTest;

    private FreeIPAKerberosDescriptor source;

    @Before
    public void setUp() {
        underTest = new FreeIPAKerberosDescriptorToKerberosConfigConverter();
        source = new FreeIPAKerberosDescriptor();
        source.setAdminUrl(ADMIN_URL);
        source.setRealm(REALM);
        source.setUrl(URL);
        source.setDomain(DOMAIN);
        source.setNameServers(NAMESERVERS);
        source.setPassword(PASSWORD);
        source.setVerifyKdcTrust(VERIFY_KDC_TRUST);
        source.setTcpAllowed(TCP_ALLOWED);
        source.setPrincipal(PRINCIPAL);
    }

    @Test
    public void testConvert() {
        KerberosConfig result = underTest.convert(source);

        commonAsserts(result);
        assertEquals(DOMAIN, result.getDomain());
    }

    @Test
    public void testConvertWithoutDomain() {
        source.setDomain(null);

        KerberosConfig result = underTest.convert(source);

        commonAsserts(result);
        assertEquals(REALM.toLowerCase(), result.getDomain());
    }

    private void commonAsserts(KerberosConfig result) {
        assertNotNull(result);
        assertEquals(URL, result.getUrl());
        assertEquals(ADMIN_URL, result.getAdminUrl());
        assertEquals(REALM, result.getRealm());
        assertEquals(PASSWORD, result.getPassword());
        assertEquals(VERIFY_KDC_TRUST, result.getVerifyKdcTrust());
        assertEquals(TCP_ALLOWED, result.isTcpAllowed());
        assertEquals(NAMESERVERS, result.getNameServers());
        assertEquals(KerberosType.FREEIPA, result.getType());
        assertEquals(PRINCIPAL, result.getPrincipal());
        assertNull(result.getName());
        assertNull(result.getDescription());
        assertNull(result.getDescriptor());
    }

}