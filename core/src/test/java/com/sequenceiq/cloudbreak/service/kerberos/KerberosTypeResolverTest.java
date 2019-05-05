package com.sequenceiq.cloudbreak.service.kerberos;

import static org.junit.Assert.assertTrue;
import static org.mockito.MockitoAnnotations.initMocks;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.InjectMocks;

import com.sequenceiq.cloudbreak.api.endpoint.v4.kerberos.requests.ActiveDirectoryKerberosDescriptor;
import com.sequenceiq.cloudbreak.api.endpoint.v4.kerberos.requests.AmbariKerberosDescriptor;
import com.sequenceiq.cloudbreak.api.endpoint.v4.kerberos.requests.FreeIPAKerberosDescriptor;
import com.sequenceiq.cloudbreak.api.endpoint.v4.kerberos.requests.KerberosTypeBase;
import com.sequenceiq.cloudbreak.api.endpoint.v4.kerberos.requests.KerberosV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.kerberos.requests.MITKerberosDescriptor;
import com.sequenceiq.cloudbreak.exception.BadRequestException;

public class KerberosTypeResolverTest {

    private static final String NAME = "somename";

    private static final String IMPROPER_KERBEROS_MESSAGE = "Improper KerberosV4Request!";

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @InjectMocks
    private KerberosTypeResolver underTest;

    @Before
    public void setup() {
        initMocks(this);
    }

    @Test
    public void testNullValidation() {
        thrown.expect(BadRequestException.class);
        thrown.expectMessage(IMPROPER_KERBEROS_MESSAGE);

        underTest.propagateKerberosConfiguration(null);
    }

    @Test
    public void testInvalid() {
        KerberosV4Request request = new KerberosV4Request();
        request.setActiveDirectory(new ActiveDirectoryKerberosDescriptor());
        request.setAmbariDescriptor(new AmbariKerberosDescriptor());

        thrown.expect(BadRequestException.class);
        thrown.expectMessage(IMPROPER_KERBEROS_MESSAGE);

        underTest.propagateKerberosConfiguration(request);
    }

    @Test
    public void testAd() {
        KerberosV4Request request = new KerberosV4Request();
        request.setActiveDirectory(new ActiveDirectoryKerberosDescriptor());
        request.setName(NAME);

        KerberosTypeBase kerberosTypeBase = underTest.propagateKerberosConfiguration(request);
        assertTrue(kerberosTypeBase instanceof ActiveDirectoryKerberosDescriptor);
    }

    @Test
    public void testMit() {
        KerberosV4Request request = new KerberosV4Request();
        request.setMit(new MITKerberosDescriptor());
        request.setName(NAME);

        KerberosTypeBase kerberosTypeBase = underTest.propagateKerberosConfiguration(request);
        assertTrue(kerberosTypeBase instanceof MITKerberosDescriptor);
    }

    @Test
    public void testFreeIpa() {
        KerberosV4Request request = new KerberosV4Request();
        request.setFreeIpa(new FreeIPAKerberosDescriptor());
        request.setName(NAME);

        KerberosTypeBase kerberosTypeBase = underTest.propagateKerberosConfiguration(request);
        assertTrue(kerberosTypeBase instanceof FreeIPAKerberosDescriptor);
    }

    @Test
    public void testCustom() {
        KerberosV4Request request = new KerberosV4Request();
        request.setAmbariDescriptor(new AmbariKerberosDescriptor());
        request.setName(NAME);

        KerberosTypeBase kerberosTypeBase = underTest.propagateKerberosConfiguration(request);
        assertTrue(kerberosTypeBase instanceof AmbariKerberosDescriptor);
    }

    @Test
    public void testAdWithoutName() {
        KerberosV4Request request = new KerberosV4Request();
        request.setActiveDirectory(new ActiveDirectoryKerberosDescriptor());

        thrown.expect(BadRequestException.class);
        thrown.expectMessage(IMPROPER_KERBEROS_MESSAGE);

        KerberosTypeBase kerberosTypeBase = underTest.propagateKerberosConfiguration(request);
        assertTrue(kerberosTypeBase instanceof ActiveDirectoryKerberosDescriptor);
    }

    @Test
    public void testMitWithoutName() {
        KerberosV4Request request = new KerberosV4Request();
        request.setMit(new MITKerberosDescriptor());

        thrown.expect(BadRequestException.class);
        thrown.expectMessage(IMPROPER_KERBEROS_MESSAGE);

        KerberosTypeBase kerberosTypeBase = underTest.propagateKerberosConfiguration(request);
        assertTrue(kerberosTypeBase instanceof MITKerberosDescriptor);
    }

    @Test
    public void testFreeIpaWithoutName() {
        KerberosV4Request request = new KerberosV4Request();
        request.setFreeIpa(new FreeIPAKerberosDescriptor());

        thrown.expect(BadRequestException.class);
        thrown.expectMessage(IMPROPER_KERBEROS_MESSAGE);

        KerberosTypeBase kerberosTypeBase = underTest.propagateKerberosConfiguration(request);
        assertTrue(kerberosTypeBase instanceof FreeIPAKerberosDescriptor);
    }

    @Test
    public void testCustomWithoutName() {
        KerberosV4Request request = new KerberosV4Request();
        request.setAmbariDescriptor(new AmbariKerberosDescriptor());

        thrown.expect(BadRequestException.class);
        thrown.expectMessage(IMPROPER_KERBEROS_MESSAGE);

        KerberosTypeBase kerberosTypeBase = underTest.propagateKerberosConfiguration(request);
        assertTrue(kerberosTypeBase instanceof AmbariKerberosDescriptor);
    }

}
