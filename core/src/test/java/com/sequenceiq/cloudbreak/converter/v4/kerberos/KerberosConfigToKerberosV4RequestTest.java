package com.sequenceiq.cloudbreak.converter.v4.kerberos;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.MockitoAnnotations.initMocks;

import org.apache.commons.codec.binary.Base64;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.api.endpoint.v4.kerberos.requests.KerberosV4Request;
import com.sequenceiq.cloudbreak.domain.KerberosConfig;
import com.sequenceiq.cloudbreak.domain.view.EnvironmentView;
import com.sequenceiq.cloudbreak.type.KerberosType;

public class KerberosConfigToKerberosV4RequestTest {

    @InjectMocks
    private KerberosConfigToKerberosV4RequestConverter underTest;

    @Before
    public void setup() {
        initMocks(this);
    }

    @Test
    public void testAd() {
        KerberosV4Request kerberosV4Request = underTest.convert(createConfig(KerberosType.ACTIVE_DIRECTORY));

        assertNotNull(kerberosV4Request.getActiveDirectory());
        assertEquals("fake-realm", kerberosV4Request.getActiveDirectory().getRealm());
    }

    @Test
    public void testMit() {
        KerberosV4Request kerberosV4Request = underTest.convert(createConfig(KerberosType.MIT));

        assertNotNull(kerberosV4Request.getMit());
        assertEquals("fake-password", kerberosV4Request.getMit().getPassword());
        assertEquals("fake-principal", kerberosV4Request.getMit().getPrincipal());
    }

    @Test
    public void testFreeIpa() {
        KerberosV4Request kerberosV4Request = underTest.convert(createConfig(KerberosType.FREEIPA));

        assertNotNull(kerberosV4Request.getFreeIpa());
        assertEquals("fake-password", kerberosV4Request.getFreeIpa().getPassword());
        assertEquals("fake-realm", kerberosV4Request.getFreeIpa().getRealm());
    }

    @Test
    public void testAmbariKerberosDescriptor() {
        KerberosV4Request kerberosV4Request = underTest.convert(createConfig(KerberosType.AMBARI_DESCRIPTOR));

        assertNotNull(kerberosV4Request.getAmbariDescriptor());
        assertEquals("{}", new String(Base64.decodeBase64(kerberosV4Request.getAmbariDescriptor().getKrb5Conf())));
    }

    private KerberosConfig createConfig(KerberosType type) {
        KerberosConfig config = new KerberosConfig();
        config.setId(1L);
        config.setName("name");
        config.setDescription("desc");
        EnvironmentView environmentView = new EnvironmentView();
        environmentView.setName("env");
        config.setEnvironments(Sets.newHashSet(environmentView));
        config.setKrb5Conf("krb5conf");
        config.setAdminUrl("adminurl");
        config.setAdmin("admin");
        config.setType(type);
        config.setUrl("url");
        config.setTcpAllowed(false);
        config.setPassword("pass");
        config.setPrincipal("princ");
        config.setDomain("domain");
        config.setVerifyKdcTrust(true);
        config.setLdapUrl("ldapurl");
        config.setContainerDn("containerdn");
        config.setRealm("realm");
        return config;
    }

}
