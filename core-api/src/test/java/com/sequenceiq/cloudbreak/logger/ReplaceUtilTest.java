package com.sequenceiq.cloudbreak.logger;


import static com.sequenceiq.cloudbreak.logger.ReplaceUtil.anonymize;

import org.junit.Assert;
import org.junit.Test;

public class ReplaceUtilTest {

    @Test
    public void testNullInput() {
        Assert.assertNull(anonymize(null));
    }

    @Test
    public void testHidePasswordSingleQuote() {
        String testData = " DC=hortonworks,DC=com '--ldap-manager-password=2#KQ01DLbUdljJ!AVs' --ldap-sync-usern' sd dsds '";
        Assert.assertEquals(" DC=hortonworks,DC=com '--ldap-manager-password=****' --ldap-sync-usern' sd dsds '",
                anonymize(testData));
    }

    @Test
    public void testCapitalHidePasswordSingleQuote() {
        String testData = " DC=hortonworks,DC=com '--ldap-manager-PASSWORD=2#KQ01DLbUdljJ!AVs' --ldap-sync-usern' sd dsds '";
        Assert.assertEquals(" DC=hortonworks,DC=com '--ldap-manager-PASSWORD=****' --ldap-sync-usern' sd dsds '",
                anonymize(testData));
    }

    @Test
    public void testHidePasswordDoubleQuote() {
        String testData = " DC=hortonworks,DC=com \"--ldap-manager-password=2#KQ01DLbUdljJ!AVs\" --ldap-sync-usern\" sd dsds \"";
        Assert.assertEquals(" DC=hortonworks,DC=com \"--ldap-manager-password=****\" --ldap-sync-usern\" sd dsds \"",
                anonymize(testData));
    }

    @Test
    public void testCapitalHidePasswordDoubleQuote() {
        String testData = " DC=hortonworks,DC=com \"--ldap-manager-PASSWORD=2#KQ01DLbUdljJ!AVs\" --ldap-sync-usern\" sd dsds \"";
        Assert.assertEquals(" DC=hortonworks,DC=com \"--ldap-manager-PASSWORD=****\" --ldap-sync-usern\" sd dsds \"",
                anonymize(testData));
    }

    @Test
    public void testHidePasswordNoQuotes() {
        String testData = " DC=hortonworks,DC=com --ldap-manager-password=2#KQ01DLbUdljJ!AVs --ldap-sync-usern' sd dsds '";
        Assert.assertEquals(" DC=hortonworks,DC=com --ldap-manager-password=**** --ldap-sync-usern' sd dsds '",
                anonymize(testData));
    }

    @Test
    public void testCapitalHidePasswordNoQuotes() {
        String testData = " DC=hortonworks,DC=com --ldap-manager-PASSWORD=2#KQ01DLbUdljJ!AVs --ldap-sync-usern' sd dsds '";
        Assert.assertEquals(" DC=hortonworks,DC=com --ldap-manager-PASSWORD=**** --ldap-sync-usern' sd dsds '",
                anonymize(testData));
    }

    @Test
    public void testHideInJson() {
        String testData = "username\":\"admin\",\"external_ranger_admin_password\":\"rangerpassword123\"}}},"
                + "{\"admin-properties\":{\"properties\":{\"db_root_user\":\"rangerroot\",\"db_root_password\":\"rangerpass\",\"db_host\":\"localhost\"}}}";
        Assert.assertEquals("username\":\"admin\",\"external_ranger_admin_password\":\"****\"}}},"
                        + "{\"admin-properties\":{\"properties\":{\"db_root_user\":\"rangerroot\",\"db_root_password\":\"****\",\"db_host\":\"localhost\"}}}",
                anonymize(testData));
    }

}
