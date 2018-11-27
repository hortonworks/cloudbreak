package com.sequenceiq.cloudbreak.validation;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.sequenceiq.cloudbreak.api.model.kerberos.ActiveDirectoryKerberosDescriptor;
import com.sequenceiq.cloudbreak.api.model.kerberos.AmbariKerberosDescriptor;
import com.sequenceiq.cloudbreak.api.model.kerberos.FreeIPAKerberosDescriptor;
import com.sequenceiq.cloudbreak.api.model.kerberos.MITKerberosDescriptor;
import com.sequenceiq.cloudbreak.api.model.kerberos.KerberosRequest;

@RunWith(Parameterized.class)
public class KerberosRequestValidatorTest {

    private static final String TEST_PASSWORD = "someTestPassword";

    private static final FreeIPAKerberosDescriptor FREEIPA = new FreeIPAKerberosDescriptor();

    private static final MITKerberosDescriptor MIT = new MITKerberosDescriptor();

    private static final ActiveDirectoryKerberosDescriptor ACTIVE_DIRECTORY = new ActiveDirectoryKerberosDescriptor();

    private static final AmbariKerberosDescriptor AMBARI_KERBEROS_DESCRIPTOR = new AmbariKerberosDescriptor();

    private KerberosRequest request;

    private boolean expected;

    public KerberosRequestValidatorTest(KerberosRequest request, boolean expected) {
        this.request = request;
        this.expected = expected;
    }

    @Parameterized.Parameters(name = "[{index}] Test KerberosRequest: {0}")
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

    private static KerberosRequest createRequest(ActiveDirectoryKerberosDescriptor existingAd, FreeIPAKerberosDescriptor existingFreeIpaKerberosDescriptor,
            MITKerberosDescriptor existingMitKerberosDescriptor,
            AmbariKerberosDescriptor ambariKerberosDescriptor) {
        KerberosRequest request = new KerberosRequest();
        request.setMit(existingMitKerberosDescriptor);
        request.setFreeIpa(existingFreeIpaKerberosDescriptor);
        request.setActiveDirectory(existingAd);
        request.setAmbariKerberosDescriptor(ambariKerberosDescriptor);
        request.setName(TEST_PASSWORD);
        return request;
    }

}