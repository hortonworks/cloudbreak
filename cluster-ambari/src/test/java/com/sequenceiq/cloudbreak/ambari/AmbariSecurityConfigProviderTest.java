package com.sequenceiq.cloudbreak.ambari;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;

public class AmbariSecurityConfigProviderTest {

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    private final AmbariSecurityConfigProvider underTest = new AmbariSecurityConfigProvider();

    @Test
    public void testGetAmbariUserNameWhenCloudbreakAmbariUserNameIsExistThenShouldReturnAmbariUserName() {
        Cluster cluster = TestUtil.cluster();
        cluster.setCloudbreakAmbariUser("ambariUserName");
        cluster.setUserName("admin");

        String ambariUserName = underTest.getCloudbreakClusterUserName(cluster);

        Assert.assertEquals("ambariUserName", ambariUserName);
    }

    @Test
    public void testGetAmbariPasswordWhenCloudbreakAmbariPasswordIsExistThenShouldReturnAmbariPassword() {
        Cluster cluster = TestUtil.cluster();
        cluster.setCloudbreakAmbariPassword("ambariPassword");
        cluster.setPassword("admin");

        String ambariPassword = underTest.getCloudbreakClusterPassword(cluster);

        Assert.assertEquals("ambariPassword", ambariPassword);
    }

    @Test
    public void testGetAmbariSecurityMasterKeyWhenAmbariSecurityMasterKeyIsNullThenShouldReturnBigData() {
        Cluster cluster = TestUtil.cluster();
        cluster.setAmbariSecurityMasterKey(null);

        String ambariSecurityMasterKey = underTest.getMasterKey(cluster);

        Assert.assertEquals("bigdata", ambariSecurityMasterKey);
    }

    @Test
    public void testGetAmbariSecurityMasterKeyWhenAmbariSecurityMasterKeyIsExistThenShouldReturnAmbariSecurityMasterKey() {
        Cluster cluster = TestUtil.cluster();
        cluster.setAmbariSecurityMasterKey("masterkey");

        String ambariSecurityMasterKey = underTest.getMasterKey(cluster);

        Assert.assertEquals("masterkey", ambariSecurityMasterKey);
    }
}