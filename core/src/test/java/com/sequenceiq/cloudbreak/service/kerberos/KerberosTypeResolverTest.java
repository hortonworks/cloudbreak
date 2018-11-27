package com.sequenceiq.cloudbreak.service.kerberos;

import static org.junit.Assert.assertTrue;
import static org.mockito.MockitoAnnotations.initMocks;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.InjectMocks;

import com.sequenceiq.cloudbreak.api.model.kerberos.ActiveDirectoryKerberosDescriptor;
import com.sequenceiq.cloudbreak.api.model.kerberos.AmbariKerberosDescriptor;
import com.sequenceiq.cloudbreak.api.model.kerberos.FreeIPAKerberosDescriptor;
import com.sequenceiq.cloudbreak.api.model.kerberos.KerberosTypeBase;
import com.sequenceiq.cloudbreak.api.model.kerberos.MITKerberosDescriptor;
import com.sequenceiq.cloudbreak.api.model.kerberos.KerberosRequest;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;

public class KerberosTypeResolverTest {

    private static final String NAME = "somename";

    private static final String IMPROPER_KERBEROS_MESSAGE = "Improper KerberosRequest!";

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
        KerberosRequest request = new KerberosRequest();
        request.setActiveDirectory(new ActiveDirectoryKerberosDescriptor());
        request.setAmbariKerberosDescriptor(new AmbariKerberosDescriptor());

        thrown.expect(BadRequestException.class);
        thrown.expectMessage(IMPROPER_KERBEROS_MESSAGE);

        underTest.propagateKerberosConfiguration(request);
    }

    @Test
    public void testAd() {
        KerberosRequest request = new KerberosRequest();
        request.setActiveDirectory(new ActiveDirectoryKerberosDescriptor());
        request.setName(NAME);

        KerberosTypeBase kerberosTypeBase = underTest.propagateKerberosConfiguration(request);
        assertTrue(kerberosTypeBase instanceof ActiveDirectoryKerberosDescriptor);
    }

    @Test
    public void testMit() {
        KerberosRequest request = new KerberosRequest();
        request.setMit(new MITKerberosDescriptor());
        request.setName(NAME);

        KerberosTypeBase kerberosTypeBase = underTest.propagateKerberosConfiguration(request);
        assertTrue(kerberosTypeBase instanceof MITKerberosDescriptor);
    }

    @Test
    public void testFreeIpa() {
        KerberosRequest request = new KerberosRequest();
        request.setFreeIpa(new FreeIPAKerberosDescriptor());
        request.setName(NAME);

        KerberosTypeBase kerberosTypeBase = underTest.propagateKerberosConfiguration(request);
        assertTrue(kerberosTypeBase instanceof FreeIPAKerberosDescriptor);
    }

    @Test
    public void testCustom() {
        KerberosRequest request = new KerberosRequest();
        request.setAmbariKerberosDescriptor(new AmbariKerberosDescriptor());
        request.setName(NAME);

        KerberosTypeBase kerberosTypeBase = underTest.propagateKerberosConfiguration(request);
        assertTrue(kerberosTypeBase instanceof AmbariKerberosDescriptor);
    }

    @Test
    public void testAdWithoutName() {
        KerberosRequest request = new KerberosRequest();
        request.setActiveDirectory(new ActiveDirectoryKerberosDescriptor());

        thrown.expect(BadRequestException.class);
        thrown.expectMessage(IMPROPER_KERBEROS_MESSAGE);

        KerberosTypeBase kerberosTypeBase = underTest.propagateKerberosConfiguration(request);
        assertTrue(kerberosTypeBase instanceof ActiveDirectoryKerberosDescriptor);
    }

    @Test
    public void testMitWithoutName() {
        KerberosRequest request = new KerberosRequest();
        request.setMit(new MITKerberosDescriptor());

        thrown.expect(BadRequestException.class);
        thrown.expectMessage(IMPROPER_KERBEROS_MESSAGE);

        KerberosTypeBase kerberosTypeBase = underTest.propagateKerberosConfiguration(request);
        assertTrue(kerberosTypeBase instanceof MITKerberosDescriptor);
    }

    @Test
    public void testFreeIpaWithoutName() {
        KerberosRequest request = new KerberosRequest();
        request.setFreeIpa(new FreeIPAKerberosDescriptor());

        thrown.expect(BadRequestException.class);
        thrown.expectMessage(IMPROPER_KERBEROS_MESSAGE);

        KerberosTypeBase kerberosTypeBase = underTest.propagateKerberosConfiguration(request);
        assertTrue(kerberosTypeBase instanceof FreeIPAKerberosDescriptor);
    }

    @Test
    public void testCustomWithoutName() {
        KerberosRequest request = new KerberosRequest();
        request.setAmbariKerberosDescriptor(new AmbariKerberosDescriptor());

        thrown.expect(BadRequestException.class);
        thrown.expectMessage(IMPROPER_KERBEROS_MESSAGE);

        KerberosTypeBase kerberosTypeBase = underTest.propagateKerberosConfiguration(request);
        assertTrue(kerberosTypeBase instanceof AmbariKerberosDescriptor);
    }

}
