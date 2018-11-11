package com.sequenceiq.cloudbreak.service.cluster.ambari;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;

public class AmbariSecurityConfigProviderTest {

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    private final AmbariSecurityConfigProvider underTest = new AmbariSecurityConfigProvider();

    @Test
    public void testGetAmbariUserNameWhenCloudbreakAmbariUserNameIsNullThenShouldReturnUserName() {
        Cluster cluster = TestUtil.cluster();
        cluster.setCloudbreakAmbariUser(null);
        cluster.setUserName("admin");

        String ambariUserName = underTest.getAmbariUserName(cluster);

        Assert.assertEquals("admin", ambariUserName);
    }

    @Test
    public void testGetAmbariUserNameWhenCloudbreakAmbariUserNameIsExistThenShouldReturnAmbariUserName() {
        Cluster cluster = TestUtil.cluster();
        cluster.setCloudbreakAmbariUser("ambariUserName");
        cluster.setUserName("admin");

        String ambariUserName = underTest.getAmbariUserName(cluster);

        Assert.assertEquals("ambariUserName", ambariUserName);
    }

    @Test
    public void testGetAmbariPasswordNameWhenCloudbreakAmbariPasswordIsNullThenShouldReturnPassword() {
        Cluster cluster = TestUtil.cluster();
        cluster.setCloudbreakAmbariPassword(null);
        cluster.setPassword("admin");

        String ambariPassword = underTest.getAmbariPassword(cluster);

        Assert.assertEquals("admin", ambariPassword);
    }

    @Test
    public void testGetAmbariPasswordWhenCloudbreakAmbariPasswordIsExistThenShouldReturnAmbariPassword() {
        Cluster cluster = TestUtil.cluster();
        cluster.setCloudbreakAmbariPassword("ambariPassword");
        cluster.setPassword("admin");

        String ambariPassword = underTest.getAmbariPassword(cluster);

        Assert.assertEquals("ambariPassword", ambariPassword);
    }

    @Test
    public void testGetAmbariSecurityMasterKeyWhenAmbariSecurityMasterKeyIsNullThenShouldReturnBigData() {
        Cluster cluster = TestUtil.cluster();
        cluster.setAmbariSecurityMasterKey(null);

        String ambariSecurityMasterKey = underTest.getAmbariSecurityMasterKey(cluster);

        Assert.assertEquals("bigdata", ambariSecurityMasterKey);
    }

    @Test
    public void testGetAmbariSecurityMasterKeyWhenAmbariSecurityMasterKeyIsExistThenShouldReturnAmbariSecurityMasterKey() {
        Cluster cluster = TestUtil.cluster();
        cluster.setAmbariSecurityMasterKey("masterkey");

        String ambariSecurityMasterKey = underTest.getAmbariSecurityMasterKey(cluster);

        Assert.assertEquals("masterkey", ambariSecurityMasterKey);
    }
}