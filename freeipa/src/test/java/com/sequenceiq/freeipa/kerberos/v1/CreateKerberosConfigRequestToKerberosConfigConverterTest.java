package com.sequenceiq.freeipa.kerberos.v1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.freeipa.api.v1.kerberos.model.KerberosType;
import com.sequenceiq.freeipa.api.v1.kerberos.model.create.ActiveDirectoryKerberosDescriptor;
import com.sequenceiq.freeipa.api.v1.kerberos.model.create.CreateKerberosConfigRequest;
import com.sequenceiq.freeipa.api.v1.kerberos.model.create.FreeIpaKerberosDescriptor;
import com.sequenceiq.freeipa.api.v1.kerberos.model.create.KerberosDescriptorBase;
import com.sequenceiq.freeipa.api.v1.kerberos.model.create.MITKerberosDescriptor;
import com.sequenceiq.freeipa.kerberos.KerberosConfig;

@ExtendWith(MockitoExtension.class)
public class CreateKerberosConfigRequestToKerberosConfigConverterTest {
    private static final String NAME = "somename";

    private static final String IMPROPER_KERBEROS_MESSAGE = "Improper CreateKerberosConfigRequest!";

    private static final String ADMIN_URL = "someurl";

    private static final String CONTAINER_DN = "dn";

    private static final String LDAP_URL = "ldap://someurl.com";

    private static final String REALM = "someRealm";

    private static final String URL = "someotherurl.com";

    private static final String PRINCIPAL = "principal";

    private static final String DOMAIN = "someDomain";

    private static final String NAME_SERVERS = "1.1.1.1";

    private static final String PASSWORD = "somePassword";

    private static final Boolean VERIFY_KDC_TRUST = false;

    private static final Boolean TCP_ALLOWED = false;

    @InjectMocks
    private CreateKerberosConfigRequestToKerberosConfigConverter underTest;

    @Test
    public void testNullValidation() {
        BadRequestException ex = assertThrows(BadRequestException.class, () -> {
            underTest.convert(null);
        });
        assertEquals(IMPROPER_KERBEROS_MESSAGE, ex.getMessage());
    }

    @Test
    public void testInvalid() {
        CreateKerberosConfigRequest request = new CreateKerberosConfigRequest();
        request.setActiveDirectory(new ActiveDirectoryKerberosDescriptor());
        request.setMit(new MITKerberosDescriptor());

        BadRequestException ex = assertThrows(BadRequestException.class, () -> {
            underTest.convert(request);
        });
        assertEquals(IMPROPER_KERBEROS_MESSAGE, ex.getMessage());
    }

    @Test
    public void testAd() {
        CreateKerberosConfigRequest request = new CreateKerberosConfigRequest();
        ActiveDirectoryKerberosDescriptor adDescriptor = createActiveDirectoryKerberosDescriptor(true);
        request.setActiveDirectory(adDescriptor);
        request.setName(NAME);

        KerberosConfig kerberosConfig = underTest.convert(request);
        checkActiveDirectoryParams(kerberosConfig, true);
    }

    @Test
    public void testAdWithoutDomain() {
        CreateKerberosConfigRequest request = new CreateKerberosConfigRequest();
        ActiveDirectoryKerberosDescriptor adDescriptor = createActiveDirectoryKerberosDescriptor(false);
        request.setActiveDirectory(adDescriptor);
        request.setName(NAME);

        KerberosConfig kerberosConfig = underTest.convert(request);
        checkActiveDirectoryParams(kerberosConfig, false);
    }

    @Test
    public void testMit() {
        CreateKerberosConfigRequest request = new CreateKerberosConfigRequest();
        request.setMit(createMitKerberosDescriptor());
        request.setName(NAME);

        KerberosConfig kerberosConfig = underTest.convert(request);
        checkMitParams(kerberosConfig);
    }

    @Test
    public void testFreeIpa() {
        CreateKerberosConfigRequest request = new CreateKerberosConfigRequest();
        request.setFreeIpa(createFreeIpaDescriptor(true));
        request.setName(NAME);

        KerberosConfig kerberosConfig = underTest.convert(request);
        checkFreeIpaParams(kerberosConfig, true);
    }

    @Test
    public void testFreeIpaWithoutDomain() {
        CreateKerberosConfigRequest request = new CreateKerberosConfigRequest();
        request.setFreeIpa(createFreeIpaDescriptor(false));
        request.setName(NAME);

        KerberosConfig kerberosConfig = underTest.convert(request);
        checkFreeIpaParams(kerberosConfig, false);
    }

    @Test
    public void testAdWithoutName() {
        CreateKerberosConfigRequest request = new CreateKerberosConfigRequest();
        request.setActiveDirectory(new ActiveDirectoryKerberosDescriptor());

        BadRequestException ex = assertThrows(BadRequestException.class, () -> {
            underTest.convert(request);
        });
        assertEquals(IMPROPER_KERBEROS_MESSAGE, ex.getMessage());
    }

    @Test
    public void testMitWithoutName() {
        CreateKerberosConfigRequest request = new CreateKerberosConfigRequest();
        request.setMit(new MITKerberosDescriptor());

        BadRequestException ex = assertThrows(BadRequestException.class, () -> {
            underTest.convert(request);
        });
        assertEquals(IMPROPER_KERBEROS_MESSAGE, ex.getMessage());
    }

    @Test
    public void testFreeIpaWithoutName() {
        CreateKerberosConfigRequest request = new CreateKerberosConfigRequest();
        request.setFreeIpa(new FreeIpaKerberosDescriptor());

        BadRequestException ex = assertThrows(BadRequestException.class, () -> {
            underTest.convert(request);
        });
        assertEquals(IMPROPER_KERBEROS_MESSAGE, ex.getMessage());
    }

    private ActiveDirectoryKerberosDescriptor createActiveDirectoryKerberosDescriptor(boolean withDomain) {
        ActiveDirectoryKerberosDescriptor adDescriptor = new ActiveDirectoryKerberosDescriptor();
        setupCommonFields(adDescriptor);
        adDescriptor.setAdminUrl(ADMIN_URL);
        adDescriptor.setContainerDn(CONTAINER_DN);
        adDescriptor.setLdapUrl(LDAP_URL);
        adDescriptor.setRealm(REALM);
        adDescriptor.setUrl(URL);
        if (withDomain) {
            adDescriptor.setDomain(DOMAIN);
        }
        return adDescriptor;
    }

    private MITKerberosDescriptor createMitKerberosDescriptor() {
        MITKerberosDescriptor mitDescriptor = new MITKerberosDescriptor();
        setupCommonFields(mitDescriptor);
        mitDescriptor.setAdminUrl(ADMIN_URL);
        mitDescriptor.setRealm(REALM);
        mitDescriptor.setUrl(URL);
        mitDescriptor.setDomain(DOMAIN);
        return mitDescriptor;
    }

    public FreeIpaKerberosDescriptor createFreeIpaDescriptor(boolean withDomain) {
        FreeIpaKerberosDescriptor freeIpaDescriptor = new FreeIpaKerberosDescriptor();
        setupCommonFields(freeIpaDescriptor);
        freeIpaDescriptor.setAdminUrl(ADMIN_URL);
        freeIpaDescriptor.setRealm(REALM);
        freeIpaDescriptor.setUrl(URL);
        if (withDomain) {
            freeIpaDescriptor.setDomain(DOMAIN);
        }
        return freeIpaDescriptor;
    }

    private void setupCommonFields(KerberosDescriptorBase kerberosDescriptor) {
        kerberosDescriptor.setPrincipal(PRINCIPAL);
        kerberosDescriptor.setNameServers(NAME_SERVERS);
        kerberosDescriptor.setPassword(PASSWORD);
        kerberosDescriptor.setVerifyKdcTrust(VERIFY_KDC_TRUST);
        kerberosDescriptor.setTcpAllowed(TCP_ALLOWED);
    }

    private void checkActiveDirectoryParams(KerberosConfig kerberosConfig, boolean withDomain) {
        checkCommonParams(kerberosConfig);
        assertEquals(ADMIN_URL, kerberosConfig.getAdminUrl());
        assertEquals(CONTAINER_DN, kerberosConfig.getContainerDn());
        assertEquals(LDAP_URL, kerberosConfig.getLdapUrl());
        assertEquals(REALM, kerberosConfig.getRealm());
        assertEquals(URL, kerberosConfig.getUrl());
        assertEquals(KerberosType.ACTIVE_DIRECTORY, kerberosConfig.getType());
        if (withDomain) {
            assertEquals(DOMAIN, kerberosConfig.getDomain());
        } else {
            assertEquals(REALM.toLowerCase(), kerberosConfig.getDomain());
        }
    }

    private void checkMitParams(KerberosConfig kerberosConfig) {
        checkCommonParams(kerberosConfig);
        assertEquals(URL, kerberosConfig.getUrl());
        assertEquals(REALM, kerberosConfig.getRealm());
        assertEquals(ADMIN_URL, kerberosConfig.getAdminUrl());
        assertEquals(KerberosType.MIT, kerberosConfig.getType());
        assertEquals(DOMAIN, kerberosConfig.getDomain());
    }

    private void checkFreeIpaParams(KerberosConfig kerberosConfig, boolean withDomain) {
        checkCommonParams(kerberosConfig);
        assertEquals(URL, kerberosConfig.getUrl());
        assertEquals(ADMIN_URL, kerberosConfig.getAdminUrl());
        assertEquals(REALM, kerberosConfig.getRealm());
        assertEquals(KerberosType.FREEIPA, kerberosConfig.getType());
        if (withDomain) {
            assertEquals(DOMAIN, kerberosConfig.getDomain());
        } else {
            assertEquals(REALM.toLowerCase(), kerberosConfig.getDomain());
        }
        assertNull(kerberosConfig.getDescription());
        assertNull(kerberosConfig.getDescriptor());
    }

    private void checkCommonParams(KerberosConfig kerberosConfig) {
        assertEquals(NAME, kerberosConfig.getName());
        assertEquals(PRINCIPAL, kerberosConfig.getPrincipal());
        assertEquals(NAME_SERVERS, kerberosConfig.getNameServers());
        assertEquals(PASSWORD, kerberosConfig.getPassword());
        assertEquals(VERIFY_KDC_TRUST, kerberosConfig.getVerifyKdcTrust());
        assertEquals(TCP_ALLOWED, kerberosConfig.isTcpAllowed());
    }
}
