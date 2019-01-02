package com.sequenceiq.cloudbreak.validation;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.sequenceiq.cloudbreak.api.endpoint.v4.kerberos.requests.ActiveDirectoryKerberosDescriptor;
import com.sequenceiq.cloudbreak.api.endpoint.v4.kerberos.requests.AmbariKerberosDescriptor;
import com.sequenceiq.cloudbreak.api.endpoint.v4.kerberos.requests.FreeIPAKerberosDescriptor;
import com.sequenceiq.cloudbreak.api.endpoint.v4.kerberos.requests.KerberosV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.kerberos.requests.MITKerberosDescriptor;

@RunWith(Parameterized.class)
public class KerberosV4RequestValidatorTest {

    private static final String TEST_PASSWORD = "someTestPassword";

    private static final FreeIPAKerberosDescriptor FREEIPA = new FreeIPAKerberosDescriptor();

    private static final MITKerberosDescriptor MIT = new MITKerberosDescriptor();

    private static final ActiveDirectoryKerberosDescriptor ACTIVE_DIRECTORY = new ActiveDirectoryKerberosDescriptor();

    private static final AmbariKerberosDescriptor AMBARI_KERBEROS_DESCRIPTOR = new AmbariKerberosDescriptor();

    private KerberosV4Request request;

    private boolean expected;

    public KerberosV4RequestValidatorTest(KerberosV4Request request, boolean expected) {
        this.request = request;
        this.expected = expected;
    }

    @Parameterized.Parameters(name = "[{index}] Test KerberosV4Request: {0}")
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {createRequest(ACTIVE_DIRECTORY, FREEIPA, MIT, AMBARI_KERBEROS_DESCRIPTOR), false},
                {createRequest(ACTIVE_DIRECTORY, FREEIPA, MIT, null), false},
                {createRequest(ACTIVE_DIRECTORY, null, null, AMBARI_KERBEROS_DESCRIPTOR), false},
                {createRequest(ACTIVE_DIRECTORY, null, MIT, null), false},
                {createRequest(null, FREEIPA, MIT, AMBARI_KERBEROS_DESCRIPTOR), false},
                {createRequest(null, null, MIT, AMBARI_KERBEROS_DESCRIPTOR), false},
                {createRequest(ACTIVE_DIRECTORY, FREEIPA, null, null), false},
                {createRequest(null, FREEIPA, MIT, null), false},
                {createRequest(null, FREEIPA, null, AMBARI_KERBEROS_DESCRIPTOR), false},
                {createRequest(null, null, null, null), false},
                {createRequest(ACTIVE_DIRECTORY, null, null, null), true},
                {createRequest(null, null, null, AMBARI_KERBEROS_DESCRIPTOR), true},
                {createRequest(null, null, MIT, null), true},
                {createRequest(null, FREEIPA, null, null), true}
        });
    }

    @Test
    public void testAgainstDifferentInputs() {
        Assert.assertEquals(expected, KerberosRequestValidator.isValid(request));
    }

    @Test
    public void testAgainstDifferentInputsButIfEmptyNameProvidedThenEachOfThemShouldFail() {
        request.setName("");
        Assert.assertFalse(KerberosRequestValidator.isValid(request));
    }

    @Test
    public void testAgainstDifferentInputsButIfNoNameProvidedThenEachOfThemShouldFail() {
        request.setName(null);
        Assert.assertFalse(KerberosRequestValidator.isValid(request));
    }

    private static KerberosV4Request createRequest(ActiveDirectoryKerberosDescriptor existingAd, FreeIPAKerberosDescriptor existingFreeIpaKerberosDescriptor,
            MITKerberosDescriptor existingMitKerberosDescriptor,
            AmbariKerberosDescriptor ambariKerberosDescriptor) {
        KerberosV4Request request = new KerberosV4Request();
        request.setMit(existingMitKerberosDescriptor);
        request.setFreeIpa(existingFreeIpaKerberosDescriptor);
        request.setActiveDirectory(existingAd);
        request.setAmbariDescriptor(ambariKerberosDescriptor);
        request.setName(TEST_PASSWORD);
        return request;
    }

}