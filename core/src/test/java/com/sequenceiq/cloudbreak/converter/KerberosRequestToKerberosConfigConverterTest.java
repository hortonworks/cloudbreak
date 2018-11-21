package com.sequenceiq.cloudbreak.converter;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.api.model.KerberosRequest;
import com.sequenceiq.cloudbreak.domain.KerberosConfig;
import com.sequenceiq.cloudbreak.type.KerberosType;

public class KerberosRequestToKerberosConfigConverterTest extends AbstractConverterTest {

    private KerberosRequest request;

    private KerberosRequestToKerberosConfigConverter underTest;

    @Before
    public void init() {
        underTest = new KerberosRequestToKerberosConfigConverter();
        request = new KerberosRequest();
        request.setType(KerberosType.EXISTING_FREEIPA);
        request.setMasterKey("masterkey");
        request.setAdmin("admin");
        request.setPassword("pass");
        request.setUrl("url");
        request.setAdminUrl("adminurl");
        request.setRealm("REALM");
        request.setTcpAllowed(Boolean.TRUE);
        request.setPrincipal("princupal");
        request.setLdapUrl("ldap");
        request.setContainerDn("container");
        request.setDescriptor("descriptor");
        request.setKrb5Conf("conf");
        request.setDomain("domain");
        request.setNameServers("nameservers");
    }

    @Test
    public void testEveryFieldMapped() {
        KerberosConfig kerberosConfig = underTest.convert(request);
        assertAllFieldsNotNull(kerberosConfig, Lists.newArrayList("id", "workspace"));
        assertEquals(request.getAdminUrl(), kerberosConfig.getAdminUrl());
        assertEquals(request.getDomain(), kerberosConfig.getDomain());
    }

    @Test
    public void testAdminUrlMappedToUrlIfMissing() {
        request.setAdminUrl(null);
        KerberosConfig kerberosConfig = underTest.convert(request);
        assertEquals(request.getUrl(), kerberosConfig.getAdminUrl());
    }

    @Test
    public void testDomainMappedToRealmLowercaseIfNull() {
        request.setDomain(null);
        KerberosConfig kerberosConfig = underTest.convert(request);
        assertEquals(request.getRealm().toLowerCase(), kerberosConfig.getDomain());
    }

    @Test
    public void testDomainMappedToRealmLowercaseIfEmpty() {
        request.setDomain("");
        KerberosConfig kerberosConfig = underTest.convert(request);
        assertEquals(request.getRealm().toLowerCase(), kerberosConfig.getDomain());
    }
}