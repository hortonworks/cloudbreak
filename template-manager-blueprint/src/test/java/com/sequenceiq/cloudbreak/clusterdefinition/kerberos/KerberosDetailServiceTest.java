package com.sequenceiq.cloudbreak.clusterdefinition.kerberos;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.sequenceiq.cloudbreak.domain.KerberosConfig;
import com.sequenceiq.cloudbreak.type.KerberosType;

public class KerberosDetailServiceTest {

    private static final String TEST_DEFAULT_HOST = "test.host.com:88";

    private KerberosDetailService underTest = new KerberosDetailService();

    private KerberosConfig config;

    @Before
    public void setUp() {
        config = new KerberosConfig();
    }

    @Test
    public void testResolveHostForKerberosWhenUrlIsNullThenDefaultHostShouldReturn() {
        config.setUrl(null);
        String result = underTest.resolveHostForKerberos(config, TEST_DEFAULT_HOST);
        assertEquals(TEST_DEFAULT_HOST, result);
    }

    @Test
    public void testResolveHostForKerberosWhenUrlIsEmptyThenDefaultHostShouldReturn() {
        config.setUrl("");
        String result = underTest.resolveHostForKerberos(config, TEST_DEFAULT_HOST);
        assertEquals(TEST_DEFAULT_HOST, result);
    }

    @Test
    public void testResolveHostForKerberosWhenUrlIsNotEmptyThenThatValueShouldReturn() {
        String expected = "some value";
        config.setUrl(expected);
        String result = underTest.resolveHostForKerberos(config, TEST_DEFAULT_HOST);
        assertEquals(expected, result);
    }

    @Test
    public void testResolveHostForKerberosWhenUrlContainsOnlySpacesThenDefaultHostShouldReturn() {
        config.setUrl("   ");
        String result = underTest.resolveHostForKerberos(config, TEST_DEFAULT_HOST);
        assertEquals(TEST_DEFAULT_HOST, result);
    }

    @Test
    public void testResolveHostForKdcAdminWhenUrlIsNullThenDefaultHostShouldReturn() {
        config.setAdminUrl(null);
        String result = underTest.resolveHostForKdcAdmin(config, TEST_DEFAULT_HOST);
        assertEquals(TEST_DEFAULT_HOST, result);
    }

    @Test
    public void testResolveHostForKdcAdminWhenUrlIsEmptyThenDefaultHostShouldReturn() {
        config.setAdminUrl("");
        String result = underTest.resolveHostForKdcAdmin(config, TEST_DEFAULT_HOST);
        assertEquals(TEST_DEFAULT_HOST, result);
    }

    @Test
    public void testResolveHostForKdcAdminWhenUrlIsNotEmptyThenThatValueShouldReturn() {
        String expected = "some value";
        config.setAdminUrl(expected);
        String result = underTest.resolveHostForKdcAdmin(config, TEST_DEFAULT_HOST);
        assertEquals(expected, result);
    }

    @Test
    public void testResolveHostForKdcAdminWhenUrlContainsOnlySpacesThenDefaultHostShouldReturn() {
        config.setAdminUrl("   ");
        String result = underTest.resolveHostForKdcAdmin(config, TEST_DEFAULT_HOST);
        assertEquals(TEST_DEFAULT_HOST, result);
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
        config.setDescriptor("{\"kerberos-env\":{\"properties\":{\"install_packages\":false}}}");
        Assert.assertFalse(underTest.isAmbariManagedKerberosPackages(config));
    }

    public void testAmbariManagedKrb5ConfMissing() throws IOException {
        Assert.assertFalse(underTest.isAmbariManagedKrb5Conf(config));
    }

    @Test
    public void testAmbariManagedKrb5ConfTrue() throws IOException {
        config.setKrb5Conf("{\"krb5-conf\":{\"properties\":{\"manage_krb5_conf\":true}}}");
        Assert.assertTrue(underTest.isAmbariManagedKrb5Conf(config));
    }

    @Test
    public void testAmbariManagedKrb5ConfFalse() throws IOException {
        config.setKrb5Conf("{\"krb5-conf\":{\"properties\":{\"manage_krb5_conf\":false}}}");
        Assert.assertFalse(underTest.isAmbariManagedKrb5Conf(config));
    }

    @Test
    public void testResolveTypeForKerberos() {
        config.setType(KerberosType.FREEIPA);
        assertEquals("ipa", underTest.resolveTypeForKerberos(config));
        config.setType(KerberosType.ACTIVE_DIRECTORY);
        assertEquals("active-directory", underTest.resolveTypeForKerberos(config));
        config.setType(KerberosType.MIT);
        assertEquals("mit-kdc", underTest.resolveTypeForKerberos(config));
        config.setType(KerberosType.AMBARI_DESCRIPTOR);
        assertEquals("mit-kdc", underTest.resolveTypeForKerberos(config));
    }
}
