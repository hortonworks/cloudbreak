package com.sequenceiq.cloudbreak.blueprint.kerberos;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.domain.KerberosConfig;
import com.sequenceiq.cloudbreak.service.VaultService;

@RunWith(MockitoJUnitRunner.class)
public class KerberosDetailServiceTest {

    private static final String TEST_DEFAULT_HOST = "test.host.com:88";

    @InjectMocks
    private KerberosDetailService underTest;

    @Mock
    private VaultService vaultService;

    private KerberosConfig config;

    @Before
    public void setUp() {
        config = new KerberosConfig();
    }

    @Test
    public void testResolveHostForKerberosWhenUrlIsNullThenDefaultHostShouldReturn() {
        config.setUrl(null);
        String result = underTest.resolveHostForKerberos(config, TEST_DEFAULT_HOST);
        Assert.assertEquals(TEST_DEFAULT_HOST, result);
    }

    @Test
    public void testResolveHostForKerberosWhenUrlIsEmptyThenDefaultHostShouldReturn() {
        config.setUrl("");
        String result = underTest.resolveHostForKerberos(config, TEST_DEFAULT_HOST);
        Assert.assertEquals(TEST_DEFAULT_HOST, result);
    }

    @Test
    public void testResolveHostForKerberosWhenUrlIsNotEmptyThenThatValueShouldReturn() {
        String expected = "some value";
        config.setUrl(expected);
        String result = underTest.resolveHostForKerberos(config, TEST_DEFAULT_HOST);
        Assert.assertEquals(expected, result);
    }

    @Test
    public void testResolveHostForKerberosWhenUrlContainsOnlySpacesThenDefaultHostShouldReturn() {
        config.setUrl("   ");
        String result = underTest.resolveHostForKerberos(config, TEST_DEFAULT_HOST);
        Assert.assertEquals(TEST_DEFAULT_HOST, result);
    }

    @Test
    public void testResolveHostForKdcAdminWhenUrlIsNullThenDefaultHostShouldReturn() {
        config.setAdminUrl(null);
        String result = underTest.resolveHostForKdcAdmin(config, TEST_DEFAULT_HOST);
        Assert.assertEquals(TEST_DEFAULT_HOST, result);
    }

    @Test
    public void testResolveHostForKdcAdminWhenUrlIsEmptyThenDefaultHostShouldReturn() {
        config.setAdminUrl("");
        String result = underTest.resolveHostForKdcAdmin(config, TEST_DEFAULT_HOST);
        Assert.assertEquals(TEST_DEFAULT_HOST, result);
    }

    @Test
    public void testResolveHostForKdcAdminWhenUrlIsNotEmptyThenThatValueShouldReturn() {
        String expected = "some value";
        config.setAdminUrl(expected);
        String result = underTest.resolveHostForKdcAdmin(config, TEST_DEFAULT_HOST);
        Assert.assertEquals(expected, result);
    }

    @Test
    public void testResolveHostForKdcAdminWhenUrlContainsOnlySpacesThenDefaultHostShouldReturn() {
        config.setAdminUrl("   ");
        String result = underTest.resolveHostForKdcAdmin(config, TEST_DEFAULT_HOST);
        Assert.assertEquals(TEST_DEFAULT_HOST, result);
    }

    @Test
    public void testAmbariManagedKerberosMissing() throws IOException {
        Assert.assertTrue(underTest.isAmbariManagedKerberosPackages(config));
    }

    @Test
    public void testAmbariManagedKerberosTrue() throws IOException {
        config.setDescriptor("{\"kerberos-env\":{\"properties\":{\"install_packages\":true}}}");
        Assert.assertTrue(underTest.isAmbariManagedKerberosPackages(config));
    }

    @Test
    public void testAmbariManagedKerberosFalse() throws IOException {
        config.setDescriptor("secret/descriptor");
        when(vaultService.resolveSingleValue(any())).thenReturn("{\"kerberos-env\":{\"properties\":{\"install_packages\":false}}}");
        Assert.assertFalse(underTest.isAmbariManagedKerberosPackages(config));
    }
}
