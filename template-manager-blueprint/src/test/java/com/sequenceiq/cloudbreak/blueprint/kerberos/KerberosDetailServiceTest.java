package com.sequenceiq.cloudbreak.blueprint.kerberos;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.sequenceiq.cloudbreak.dto.KerberosConfig;
import com.sequenceiq.cloudbreak.template.kerberos.KerberosDetailService;
import com.sequenceiq.cloudbreak.type.KerberosType;

public class KerberosDetailServiceTest {

    private static final String TEST_DEFAULT_HOST = "test.host.com:88";

    private KerberosDetailService underTest = new KerberosDetailService();

    private KerberosConfig.KerberosConfigBuilder configBuilder;

    @Before
    public void setUp() {
        configBuilder = KerberosConfig.KerberosConfigBuilder.aKerberosConfig();
    }

    @Test
    public void testResolveHostForKerberosWhenUrlIsNullThenDefaultHostShouldReturn() {
        KerberosConfig config = configBuilder.withUrl(null).build();
        String result = underTest.resolveHostForKerberos(config, TEST_DEFAULT_HOST);
        assertEquals(TEST_DEFAULT_HOST, result);
    }

    @Test
    public void testResolveHostForKerberosWhenUrlIsEmptyThenDefaultHostShouldReturn() {
        KerberosConfig config = configBuilder.withUrl("").build();
        String result = underTest.resolveHostForKerberos(config, TEST_DEFAULT_HOST);
        assertEquals(TEST_DEFAULT_HOST, result);
    }

    @Test
    public void testResolveHostForKerberosWhenUrlIsNotEmptyThenThatValueShouldReturn() {
        String expected = "some value";
        KerberosConfig config = configBuilder.withUrl(expected).build();
        String result = underTest.resolveHostForKerberos(config, TEST_DEFAULT_HOST);
        assertEquals(expected, result);
    }

    @Test
    public void testResolveHostForKerberosWhenUrlContainsOnlySpacesThenDefaultHostShouldReturn() {
        KerberosConfig config = configBuilder.withUrl("   ").build();
        String result = underTest.resolveHostForKerberos(config, TEST_DEFAULT_HOST);
        assertEquals(TEST_DEFAULT_HOST, result);
    }

    @Test
    public void testResolveHostForKdcAdminWhenUrlIsNullThenDefaultHostShouldReturn() {
        KerberosConfig config = configBuilder.withAdminUrl(null).build();
        String result = underTest.resolveHostForKdcAdmin(config, TEST_DEFAULT_HOST);
        assertEquals(TEST_DEFAULT_HOST, result);
    }

    @Test
    public void testResolveHostForKdcAdminWhenUrlIsEmptyThenDefaultHostShouldReturn() {
        KerberosConfig config = configBuilder.withAdminUrl("").build();
        String result = underTest.resolveHostForKdcAdmin(config, TEST_DEFAULT_HOST);
        assertEquals(TEST_DEFAULT_HOST, result);
    }

    @Test
    public void testResolveHostForKdcAdminWhenUrlIsNotEmptyThenThatValueShouldReturn() {
        String expected = "some value";
        KerberosConfig config = configBuilder.withAdminUrl(expected).build();
        String result = underTest.resolveHostForKdcAdmin(config, TEST_DEFAULT_HOST);
        assertEquals(expected, result);
    }

    @Test
    public void testResolveHostForKdcAdminWhenUrlContainsOnlySpacesThenDefaultHostShouldReturn() {
        KerberosConfig config = configBuilder.withAdminUrl("   ").build();
        String result = underTest.resolveHostForKdcAdmin(config, TEST_DEFAULT_HOST);
        assertEquals(TEST_DEFAULT_HOST, result);
    }

    @Test
    public void testAmbariManagedKerberosMissing() throws IOException {
        Assert.assertTrue(underTest.isAmbariManagedKerberosPackages(configBuilder.build()));
    }

    @Test
    public void testAmbariManagedKerberosTrue() throws IOException {
        configBuilder.withDescriptor("{\"kerberos-env\":{\"properties\":{\"install_packages\":true}}}");
        Assert.assertTrue(underTest.isAmbariManagedKerberosPackages(configBuilder.build()));
    }

    @Test
    public void testAmbariManagedKerberosFalse() throws IOException {
        configBuilder.withDescriptor("{\"kerberos-env\":{\"properties\":{\"install_packages\":false}}}");
        Assert.assertFalse(underTest.isAmbariManagedKerberosPackages(configBuilder.build()));
    }

    public void testAmbariManagedKrb5ConfMissing() throws IOException {
        Assert.assertFalse(underTest.isAmbariManagedKrb5Conf(configBuilder.build()));
    }

    @Test
    public void testAmbariManagedKrb5ConfTrue() throws IOException {
        configBuilder.withKrb5Conf("{\"krb5-conf\":{\"properties\":{\"manage_krb5_conf\":true}}}");
        Assert.assertTrue(underTest.isAmbariManagedKrb5Conf(configBuilder.build()));
    }

    @Test
    public void testAmbariManagedKrb5ConfFalse() throws IOException {
        configBuilder.withKrb5Conf("{\"krb5-conf\":{\"properties\":{\"manage_krb5_conf\":false}}}");
        Assert.assertFalse(underTest.isAmbariManagedKrb5Conf(configBuilder.build()));
    }

    @Test
    public void testResolveTypeForKerberos() {
        configBuilder.withType(KerberosType.FREEIPA);
        assertEquals("ipa", underTest.resolveTypeForKerberos(configBuilder.build()));
        configBuilder.withType(KerberosType.ACTIVE_DIRECTORY);
        assertEquals("active-directory", underTest.resolveTypeForKerberos(configBuilder.build()));
        configBuilder.withType(KerberosType.MIT);
        assertEquals("mit-kdc", underTest.resolveTypeForKerberos(configBuilder.build()));
        configBuilder.withType(KerberosType.AMBARI_DESCRIPTOR);
        assertEquals("mit-kdc", underTest.resolveTypeForKerberos(configBuilder.build()));
    }
}
