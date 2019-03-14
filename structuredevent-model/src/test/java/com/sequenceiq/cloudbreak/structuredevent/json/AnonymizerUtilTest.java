package com.sequenceiq.cloudbreak.structuredevent.json;


import static com.sequenceiq.cloudbreak.structuredevent.json.AnonymizerUtil.anonymize;

import org.junit.Assert;
import org.junit.Test;

public class AnonymizerUtilTest {

    @Test
    public void testNullInput() {
        Assert.assertNull(anonymize(null));
    }

    @Test
    public void testHidePasswordSingleQuote() {
        String testData = " DC=hortonworks,DC=com '--ldap-manager-password=2#KQ01DLbUdljJ!AVs' --ldap-sync-usern' sd dsds '";
        Assert.assertEquals(" DC=hortonworks,DC=com '--ldap-manager-password=" + AnonymizerUtil.REPLACEMENT + "' --ldap-sync-usern' sd dsds '",
                anonymize(testData));
    }

    @Test
    public void testCapitalHidePasswordSingleQuote() {
        String testData = " DC=hortonworks,DC=com '--ldap-manager-PASSWORD=2#KQ01DLbUdljJ!AVs' --ldap-sync-usern' sd dsds '";
        Assert.assertEquals(" DC=hortonworks,DC=com '--ldap-manager-PASSWORD=" + AnonymizerUtil.REPLACEMENT + "' --ldap-sync-usern' sd dsds '",
                anonymize(testData));
    }

    @Test
    public void testHidePasswordDoubleQuote() {
        String testData = " DC=hortonworks,DC=com \"--ldap-manager-password=2#KQ01DLbUdljJ!AVs\" --ldap-sync-usern\" sd dsds \"";
        Assert.assertEquals(" DC=hortonworks,DC=com \"--ldap-manager-password=" + AnonymizerUtil.REPLACEMENT + "\" --ldap-sync-usern\" sd dsds \"",
                anonymize(testData));
    }

    @Test
    public void testCapitalHidePasswordDoubleQuote() {
        String testData = " DC=hortonworks,DC=com \"--ldap-manager-PASSWORD=2#KQ01DLbUdljJ!AVs\" --ldap-sync-usern\" sd dsds \"";
        Assert.assertEquals(" DC=hortonworks,DC=com \"--ldap-manager-PASSWORD=" + AnonymizerUtil.REPLACEMENT + "\" --ldap-sync-usern\" sd dsds \"",
                anonymize(testData));
    }

    @Test
    public void testHidePasswordNoQuotes() {
        String testData = " DC=hortonworks,DC=com --ldap-manager-password=2#KQ01DLbUdljJ!AVs --ldap-sync-usern' sd dsds '";
        Assert.assertEquals(" DC=hortonworks,DC=com --ldap-manager-password=" + AnonymizerUtil.REPLACEMENT + " --ldap-sync-usern' sd dsds '",
                anonymize(testData));
    }

    @Test
    public void testCapitalHidePasswordNoQuotes() {
        String testData = " DC=hortonworks,DC=com --ldap-manager-PASSWORD=2#KQ01DLbUdljJ!AVs --ldap-sync-usern' sd dsds '";
        Assert.assertEquals(" DC=hortonworks,DC=com --ldap-manager-PASSWORD=" + AnonymizerUtil.REPLACEMENT + " --ldap-sync-usern' sd dsds '",
                anonymize(testData));
    }

    @Test
    public void testHideInJson() {
        String testData = "username\":\"admin\",\"external_ranger_admin_password\":\"rangerpassword123\"}}},"
                + "{\"admin-properties\":{\"properties\":{\"db_root_user\":\"rangerroot\",\"db_root_password\":\"rangerpass\",\"db_host\":\"localhost\"}}}";
        Assert.assertEquals("username\":\"admin\",\"external_ranger_admin_password\":\"" + AnonymizerUtil.REPLACEMENT + "\"}}},"
                        + "{\"admin-properties\":{\"properties\":{\"db_root_user\":\"rangerroot\",\"db_root_password\":\""
                        + AnonymizerUtil.REPLACEMENT + "\",\"db_host\":\"localhost\"}}}",
                anonymize(testData));
    }

    @Test
    public void testAwsKeybasedJson() {
        String testData = "{\"cloudPlatform\":\"AWS\",\"description\":\"\",\"name\":\"anyad\",\"parameters\":{\"selector\":\"key-based\","
                + "\"secretKey\":\"dontshowup\",\"accessKey\":\"dontshowup\"}}";
        Assert.assertEquals("{\"cloudPlatform\":\"AWS\",\"description\":\"\",\"name\":\"anyad\",\"parameters\":{\"selector\":\"key-based\","
                        + "\"secretKey\":\"" + AnonymizerUtil.REPLACEMENT + "\",\"accessKey\":\"" + AnonymizerUtil.REPLACEMENT + "\"}}",
                anonymize(testData));
    }

    @Test
    public void testWasbJson() {
        String testData = "{\"hive-site\":{\"hive.metastore.warehouse.dir\":\"wasb://asdf/mmolnar-v/apps/hive/warehouse\"},"
                + "\"core-site\":{\"fs.AbstractFileSystem.wasbs.impl\":\"org.apache.hadoop.fs.azure.Wasbs\",\"fs.AbstractFileSystem.wasb.impl\""
                + ":\"org.apache.hadoop.fs.azure.Wasbs\",\"fs.azure.account.key.mmolnar.blob.core.windows.net\":\"dontshowup\","
                + "\"fs.azure.selfthrottling.read.factor\":\"1.0\",\"fs.azure.selfthrottling.write.factor\":\"1.0\"}}";
        Assert.assertEquals("{\"hive-site\":{\"hive.metastore.warehouse.dir\":\"wasb://asdf/mmolnar-v/apps/hive/warehouse\"},"
                        + "\"core-site\":{\"fs.AbstractFileSystem.wasbs.impl\":\"org.apache.hadoop.fs.azure.Wasbs\",\"fs.AbstractFileSystem.wasb.impl\""
                        + ":\"org.apache.hadoop.fs.azure.Wasbs\",\"fs.azure.account.key.mmolnar.blob.core.windows.net\":\"" + AnonymizerUtil.REPLACEMENT + "\","
                        + "\"fs.azure.selfthrottling.read.factor\":\"1.0\",\"fs.azure.selfthrottling.write.factor\":\"1.0\"}}",
                anonymize(testData));
    }

    @Test
    public void testLdapPassword() {
        String testData = "ambari-server sync-ldap --all --ldap-sync-admin-name cloudbreak --ldap-sync-admin-password thisismypassword";
        Assert.assertEquals("ambari-server sync-ldap --all --ldap-sync-admin-name cloudbreak --ldap-sync-admin-password " + AnonymizerUtil.REPLACEMENT,
                anonymize(testData));
    }

    @Test
    public void testCmTemplatePassword() {
        String testData = "{\n"
                + "        \"name\": \"hive-hive_metastore_database_password\",\n"
                + "        \"value\": \"Horton01\"\n"
                + "      },";
        String expectedData = "{\n"
                + "        \"name\": \"hive-hive_metastore_database_password\",\n"
                + "        \"value\": \"" + AnonymizerUtil.REPLACEMENT + "\"\n"
                + "      },";
        Assert.assertEquals(expectedData, anonymize(testData));
    }

    @Test
    public void testCmTemplatePasswordOneLine() {
        String testData = "{\"name\": \"hive-hive_metastore_database_password\",\"value\": \"Horton01\"},";
        String expectedData = "{\"name\": \"hive-hive_metastore_database_password\",\"value\": \"" + AnonymizerUtil.REPLACEMENT + "\"},";
        Assert.assertEquals(expectedData, anonymize(testData));

    }
}
