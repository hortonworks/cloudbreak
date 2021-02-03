package com.sequenceiq.cloudbreak.common.anonymizer;

import static com.sequenceiq.cloudbreak.common.anonymizer.AnonymizerUtil.anonymize;

import org.junit.Assert;
import org.junit.Test;

public class AnonymizerUtilTest {

    private static final String REPLACEMENT = "****";

    @Test
    public void testNullInput() {
        Assert.assertNull(anonymize(null));
    }

    @Test
    public void testHidePasswordSingleQuote() {
        String testData = " DC=hortonworks,DC=com '--ldap-manager-password=2#KQ01DLbUdljJ!AVs' --ldap-sync-usern' sd dsds '";
        Assert.assertEquals(" DC=hortonworks,DC=com '--ldap-manager-password=" + REPLACEMENT + "' --ldap-sync-usern' sd dsds '",
                anonymize(testData));
    }

    @Test
    public void testCapitalHidePasswordSingleQuote() {
        String testData = " DC=hortonworks,DC=com '--ldap-manager-PASSWORD=2#KQ01DLbUdljJ!AVs' --ldap-sync-usern' sd dsds '";
        Assert.assertEquals(" DC=hortonworks,DC=com '--ldap-manager-PASSWORD=" + REPLACEMENT + "' --ldap-sync-usern' sd dsds '",
                anonymize(testData));
    }

    @Test
    public void testWithBothSimpleAndDoubleQuotes() {
        String testData = ""
                + "export test1_password=pwd\n"
                + "export test1_pass=pwd\n"
                + "export test1_secret=pwd\n"
                + "export test1_key=pwd\n"
                + "export test1_credential=pwd\n"

                + "export test2_password:pwd\n"
                + "export test2_pass:pwd\n"
                + "export test2_secret:pwd\n"
                + "export test2_key:pwd\n"
                + "export test1_credential:pwd\n"

                + "export test3_password:'pwd'\n"
                + "export test3_pass:'pwd'\n"
                + "export test3_secret:'pwd'\n"
                + "export test3_key:'pwd'\n"
                + "export test3_credential:'pwd'\n"

                + "export test4_password='pwd'\n"
                + "export test4_pass='pwd'\n"
                + "export test4_secret='pwd'\n"
                + "export test4_key='pwd'\n"
                + "export test4_credential='pwd'\n"

                + "export test5_password=\"pwd\"\n"
                + "export test5_pass=\"pwd\"\n"
                + "export test5_secret=\"pwd\"\n"
                + "export test5_key=\"pwd\"\n"
                + "export test5_credential=\"pwd\"\n"

                + "export \"test6_password\":\"pwd\"\n"
                + "export \"test6_pass\":\"pwd\"\n"
                + "export \"test6_secret\":\"pwd\"\n"
                + "export \"test6_key\":\"pwd\"\n"
                + "export \"test6_credential\":\"pwd\"\n";

        String expected = ""
                + "export test1_password=****\n"
                + "export test1_pass=****\n"
                + "export test1_secret=****\n"
                + "export test1_key=****\n"
                + "export test1_credential=****\n"

                + "export test2_password:****\n"
                + "export test2_pass:****\n"
                + "export test2_secret:****\n"
                + "export test2_key:****\n"
                + "export test1_credential:****\n"

                + "export test3_password:'****'\n"
                + "export test3_pass:'****'\n"
                + "export test3_secret:'****'\n"
                + "export test3_key:'****'\n"
                + "export test3_credential:'****'\n"

                + "export test4_password='****'\n"
                + "export test4_pass='****'\n"
                + "export test4_secret='****'\n"
                + "export test4_key='****'\n"
                + "export test4_credential='****'\n"

                + "export test5_password=\"****\"\n"
                + "export test5_pass=\"****\"\n"
                + "export test5_secret=\"****\"\n"
                + "export test5_key=\"****\"\n"
                + "export test5_credential=\"****\"\n"

                + "export \"test6_password\":\"****\"\n"
                + "export \"test6_pass\":\"****\"\n"
                + "export \"test6_secret\":\"****\"\n"
                + "export \"test6_key\":\"****\"\n"
                + "export \"test6_credential\":\"****\"\n";
        Assert.assertEquals(expected, anonymize(testData));
    }

    @Test
    public void testHidePasswordDoubleQuote() {
        String testData = " DC=hortonworks,DC=com \"--ldap-manager-password=2#KQ01DLbUdljJ!AVs\" --ldap-sync-usern\" sd dsds \"";
        Assert.assertEquals(" DC=hortonworks,DC=com \"--ldap-manager-password=" + REPLACEMENT + "\" --ldap-sync-usern\" sd dsds \"",
                anonymize(testData));
    }

    @Test
    public void testCapitalHidePasswordDoubleQuote() {
        String testData = " DC=hortonworks,DC=com \"--ldap-manager-PASSWORD=2#KQ01DLbUdljJ!AVs\" --ldap-sync-usern\" sd dsds \"";
        Assert.assertEquals(" DC=hortonworks,DC=com \"--ldap-manager-PASSWORD=" + REPLACEMENT + "\" --ldap-sync-usern\" sd dsds \"",
                anonymize(testData));
    }

    @Test
    public void testHidePasswordNoQuotes() {
        String testData = " DC=hortonworks,DC=com --ldap-manager-password=2#KQ01DLbUdljJ!AVs --ldap-sync-usern' sd dsds '";
        Assert.assertEquals(" DC=hortonworks,DC=com --ldap-manager-password=" + REPLACEMENT + " --ldap-sync-usern' sd dsds '",
                anonymize(testData));
    }

    @Test
    public void testCapitalHidePasswordNoQuotes() {
        String testData = " DC=hortonworks,DC=com --ldap-manager-PASSWORD=2#KQ01DLbUdljJ!AVs --ldap-sync-usern' sd dsds '";
        Assert.assertEquals(" DC=hortonworks,DC=com --ldap-manager-PASSWORD=" + REPLACEMENT + " --ldap-sync-usern' sd dsds '",
                anonymize(testData));
    }

    @Test
    public void testCapitalHidePasswordEqualsSingleQuotes() {
        String testData = " DC=hortonworks,DC=com --ldap-manager-PASSWORD='2#KQ01DLbUdljJ!AVs' --ldap-sync-usern' sd dsds '";
        Assert.assertEquals(" DC=hortonworks,DC=com --ldap-manager-PASSWORD='" + REPLACEMENT + "' --ldap-sync-usern' sd dsds '",
                anonymize(testData));
    }

    @Test
    public void testHideInJson() {
        String testData = "username\":\"admin\",\"external_ranger_admin_password\":\"rangerpassword123\"}}},"
                + "{\"admin-properties\":{\"properties\":{\"db_root_user\":\"rangerroot\",\"db_root_password\":\"rangerpass\",\"db_host\":\"localhost\"}}}";
        Assert.assertEquals("username\":\"admin\",\"external_ranger_admin_password\":\"" + REPLACEMENT + "\"}}},"
                        + "{\"admin-properties\":{\"properties\":{\"db_root_user\":\"rangerroot\",\"db_root_password\":\""
                        + REPLACEMENT + "\",\"db_host\":\"localhost\"}}}",
                anonymize(testData));
    }

    @Test
    public void testAwsKeybasedJson() {
        String testData = "{\"cloudPlatform\":\"AWS\",\"description\":\"\",\"name\":\"anyad\",\"parameters\":{\"selector\":\"key-based\","
                + "\"secretKey\":\"dontshowup\",\"accessKey\":\"dontshowup\"}}";
        Assert.assertEquals("{\"cloudPlatform\":\"AWS\",\"description\":\"\",\"name\":\"anyad\",\"parameters\":{\"selector\":\"key-based\","
                        + "\"secretKey\":\"" + REPLACEMENT + "\",\"accessKey\":\"" + REPLACEMENT + "\"}}",
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
                        + ":\"org.apache.hadoop.fs.azure.Wasbs\",\"fs.azure.account.key.mmolnar.blob.core.windows.net\":\"" + REPLACEMENT + "\","
                        + "\"fs.azure.selfthrottling.read.factor\":\"1.0\",\"fs.azure.selfthrottling.write.factor\":\"1.0\"}}",
                anonymize(testData));
    }

    @Test
    public void testLdapPassword() {
        String testData = "ambari-server sync-ldap --all --ldap-sync-admin-name cloudbreak --ldap-sync-admin-password thisismypassword";
        Assert.assertEquals("ambari-server sync-ldap --all --ldap-sync-admin-name cloudbreak --ldap-sync-admin-password " + REPLACEMENT,
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
                + "        \"value\": \"" + REPLACEMENT + "\"\n"
                + "      },";
        Assert.assertEquals(expectedData, anonymize(testData));
    }

    @Test
    public void testCmTemplatePasswordOneLine() {
        String testData = "{\"name\": \"hive-hive_metastore_database_password\",\"value\": \"Horton01\"},";
        String expectedData = "{\"name\": \"hive-hive_metastore_database_password\",\"value\": \"" + REPLACEMENT + "\"},";
        Assert.assertEquals(expectedData, anonymize(testData));

    }

    @Test
    public void testHideSecretInJson() {
        String testData = "{\"name\":\"gateway_master_secret\",\"value\":\"7hiihnuqtlthgp57o6otvf04im\",\"ref\":null,\"variable\":null,\"autoConfig\":null}";
        Assert.assertEquals("{\"name\":\"gateway_master_secret\",\"value\":\"" + REPLACEMENT + "\",\"ref\":null,\"variable\":null,\"autoConfig\":null}",
                anonymize(testData));
    }

    @Test
    public void testHideAzureDbArmTemplateSecretsInJson() {
        String testData = "{\"type\": \"securestring\",\"defaultValue\" : \"7hiihnuqtlthgp57o6otvf04im\",\"minLength\"1}";
        Assert.assertEquals("{\"type\": \"securestring\",\"defaultValue\" : \"" + REPLACEMENT + "\",\"minLength\"1}",
                anonymize(testData));
    }

    @Test
    public void testHideSecretSingleQuote() {
        String testData = " DC=hortonworks,DC=com '--ldap-manager-secret=2#KQ01DLbUdljJ!AVs' --ldap-sync-usern' sd dsds '";
        Assert.assertEquals(" DC=hortonworks,DC=com '--ldap-manager-secret=" + REPLACEMENT + "' --ldap-sync-usern' sd dsds '",
                anonymize(testData));
    }

    @Test
    public void testCapitalHideSecretSingleQuote() {
        String testData = " DC=hortonworks,DC=com '--ldap-manager-SECRET=2#KQ01DLbUdljJ!AVs' --ldap-sync-usern' sd dsds '";
        Assert.assertEquals(" DC=hortonworks,DC=com '--ldap-manager-SECRET=" + REPLACEMENT + "' --ldap-sync-usern' sd dsds '",
                anonymize(testData));
    }

    @Test
    public void testHideSecretDoubleQuote() {
        String testData = " DC=hortonworks,DC=com \"--ldap-manager-secret=2#KQ01DLbUdljJ!AVs\" --ldap-sync-usern\" sd dsds \"";
        Assert.assertEquals(" DC=hortonworks,DC=com \"--ldap-manager-secret=" + REPLACEMENT + "\" --ldap-sync-usern\" sd dsds \"",
                anonymize(testData));
    }

    @Test
    public void testCapitalHideSecretDoubleQuote() {
        String testData = " DC=hortonworks,DC=com \"--ldap-manager-SECRET=2#KQ01DLbUdljJ!AVs\" --ldap-sync-usern\" sd dsds \"";
        Assert.assertEquals(" DC=hortonworks,DC=com \"--ldap-manager-SECRET=" + REPLACEMENT + "\" --ldap-sync-usern\" sd dsds \"",
                anonymize(testData));
    }

    @Test
    public void testHideSecretNoQuotes() {
        String testData = " DC=hortonworks,DC=com --ldap-manager-secret=2#KQ01DLbUdljJ!AVs --ldap-sync-usern' sd dsds '";
        Assert.assertEquals(" DC=hortonworks,DC=com --ldap-manager-secret=" + REPLACEMENT + " --ldap-sync-usern' sd dsds '",
                anonymize(testData));
    }

    @Test
    public void testCapitalHideSecretNoQuotes() {
        String testData = " DC=hortonworks,DC=com --ldap-manager-SECRET=2#KQ01DLbUdljJ!AVs --ldap-sync-usern' sd dsds '";
        Assert.assertEquals(" DC=hortonworks,DC=com --ldap-manager-SECRET=" + REPLACEMENT + " --ldap-sync-usern' sd dsds '",
                anonymize(testData));
    }

    @Test
    public void testFreeIpaLdapModifyPassword() {
        String testData = "ldapmodify x -D 'cn=directory manager' -w 'Cloudera01' -h localhost";
        String expectedData = "ldapmodify x -D 'cn=directory manager' -w '" + REPLACEMENT + "' -h localhost";
        Assert.assertEquals(expectedData, anonymize(testData));
    }

    @Test
    public void testFreeIpaMagBearerToken() {
        String testData = "IPASESSION: MagBearerToken=abcdefghijklmnopqrstuvwxyz%2f%2bABCDEFGHIJKLMNOPQRSTUVWXYZ3d%3d, " +
                "Set-Cookie: ipa_session=MagBearerToken=abcdefghijklmnopqrstuvwxyz%2f%2bABCDEFGHIJKLMNOPQRSTUVWXYZ3d%3d;path=/ipa;httponly;secure;, " +
                "X-Frame-Options: DENY, Content-Security-Policy: frame-ancestors 'none', Cache-Control: no-cache]";
        String expectedData = "IPASESSION: MagBearerToken=" + REPLACEMENT + ", Set-Cookie: ipa_session=MagBearerToken=" + REPLACEMENT +
                ";path=/ipa;httponly;secure;, X-Frame-Options: DENY, Content-Security-Policy: frame-ancestors 'none', Cache-Control: no-cache]";
        Assert.assertEquals(expectedData, anonymize(testData));
    }
}