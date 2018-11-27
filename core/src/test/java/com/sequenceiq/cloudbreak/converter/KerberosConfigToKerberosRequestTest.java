package com.sequenceiq.cloudbreak.converter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.MockitoAnnotations.initMocks;

import org.apache.commons.codec.binary.Base64;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.api.model.kerberos.KerberosRequest;
import com.sequenceiq.cloudbreak.domain.KerberosConfig;
import com.sequenceiq.cloudbreak.domain.view.EnvironmentView;
import com.sequenceiq.cloudbreak.type.KerberosType;

public class KerberosConfigToKerberosRequestTest {

    @InjectMocks
    private KerberosConfigToKerberosRequestConverter underTest;

    @Before
    public void setup() {
        initMocks(this);
    }

    @Test
    public void testAd() {
        KerberosRequest kerberosRequest = underTest.convert(createConfig(KerberosType.ACTIVE_DIRECTORY));

        assertNotNull(kerberosRequest.getActiveDirectory());
        assertEquals("fake-realm", kerberosRequest.getActiveDirectory().getRealm());
    }

    @Test
    public void testMit() {
        KerberosRequest kerberosRequest = underTest.convert(createConfig(KerberosType.MIT));

        assertNotNull(kerberosRequest.getMit());
        assertEquals("fake-password", kerberosRequest.getMit().getPassword());
        assertEquals("fake-principal", kerberosRequest.getMit().getPrincipal());
    }

    @Test
    public void testFreeIpa() {
        KerberosRequest kerberosRequest = underTest.convert(createConfig(KerberosType.FREEIPA));

        assertNotNull(kerberosRequest.getFreeIpa());
        assertEquals("fake-password", kerberosRequest.getFreeIpa().getPassword());
        assertEquals("fake-realm", kerberosRequest.getFreeIpa().getRealm());
    }

    @Test
    public void testAmbariKerberosDescriptor() {
        KerberosRequest kerberosRequest = underTest.convert(createConfig(KerberosType.AMBARI_DESCRIPTOR));

        assertNotNull(kerberosRequest.getAmbariKerberosDescriptor());
        assertEquals("{}", new String(Base64.decodeBase64(kerberosRequest.getAmbariKerberosDescriptor().getKrb5Conf())));
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
