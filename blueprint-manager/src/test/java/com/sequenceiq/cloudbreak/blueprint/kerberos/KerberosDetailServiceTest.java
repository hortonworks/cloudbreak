package com.sequenceiq.cloudbreak.blueprint.kerberos;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.domain.KerberosConfig;

@RunWith(MockitoJUnitRunner.class)
public class KerberosDetailServiceTest {

    private KerberosDetailService underTest = new KerberosDetailService();

    @Test
    public void testAmbariManagedKerberosMissing() throws IOException {
        KerberosConfig config = new KerberosConfig();
        Assert.assertTrue(underTest.isAmbariManagedKerberosPackages(config));
    }

    @Test
    public void testAmbariManagedKerberosTrue() throws IOException {
        KerberosConfig config = new KerberosConfig();
        config.setDescriptor("{\"kerberos-env\":{\"properties\":{\"install_packages\":true}}}");
        Assert.assertTrue(underTest.isAmbariManagedKerberosPackages(config));
    }

    @Test
    public void testAmbariManagedKerberosFalse() throws IOException {
        KerberosConfig config = new KerberosConfig();
        config.setDescriptor("{\"kerberos-env\":{\"properties\":{\"install_packages\":false}}}");
        Assert.assertFalse(underTest.isAmbariManagedKerberosPackages(config));
    }
}
