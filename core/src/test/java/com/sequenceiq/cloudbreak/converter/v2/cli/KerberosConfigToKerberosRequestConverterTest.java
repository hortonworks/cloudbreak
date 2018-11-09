package com.sequenceiq.cloudbreak.converter.v2.cli;

import org.junit.Test;

import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.api.model.KerberosRequest;
import com.sequenceiq.cloudbreak.converter.AbstractConverterTest;
import com.sequenceiq.cloudbreak.domain.KerberosConfig;
import com.sequenceiq.cloudbreak.type.KerberosType;

public class KerberosConfigToKerberosRequestConverterTest extends AbstractConverterTest {

    private final KerberosConfigToKerberosRequestConverter underTest = new KerberosConfigToKerberosRequestConverter();

    @Test
    public void testCbManaged() {
        KerberosConfig config = TestUtil.kerberosConfig();
        KerberosRequest request = underTest.convert(config);
        assertAllFieldsNotNull(request, Lists.newArrayList("url", "adminUrl", "realm", "ldapUrl", "containerDn", "descriptor", "krb5Conf",
                "principal", "admin"));
    }

    @Test
    public void testExistingAd() {
        KerberosConfig config = new KerberosConfig();
        config.setType(KerberosType.EXISTING_AD);
        config.setPassword("");
        config.setPrincipal("");
        config.setUrl("");
        config.setAdminUrl("");
        config.setRealm("");
        config.setLdapUrl("");
        config.setContainerDn("");
        config.setTcpAllowed(true);
        KerberosRequest request = underTest.convert(config);
        assertAllFieldsNotNull(request, Lists.newArrayList("admin", "descriptor", "krb5Conf", "masterKey"));
    }

    @Test
    public void testExistingMit() {
        KerberosConfig config = new KerberosConfig();
        config.setType(KerberosType.EXISTING_MIT);
        config.setPassword("");
        config.setPrincipal("");
        config.setUrl("");
        config.setAdminUrl("");
        config.setRealm("");
        config.setTcpAllowed(true);
        KerberosRequest request = underTest.convert(config);
        assertAllFieldsNotNull(request, Lists.newArrayList("admin", "ldapUrl", "containerDn", "descriptor", "krb5Conf", "masterKey"));
    }

    @Test
    public void testCustom() {
        KerberosConfig config = new KerberosConfig();
        config.setType(KerberosType.CUSTOM);
        config.setPassword("");
        config.setPrincipal("");
        config.setDescriptor("");
        config.setKrb5Conf("");
        config.setTcpAllowed(true);
        KerberosRequest request = underTest.convert(config);
        assertAllFieldsNotNull(request, Lists.newArrayList("admin", "url", "adminUrl", "realm", "ldapUrl", "containerDn", "masterKey", "principal"));
    }
}
