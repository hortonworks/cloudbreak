package com.sequenceiq.it.cloudbreak;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;

import org.testng.Assert;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakTest;
import com.sequenceiq.it.cloudbreak.newway.RdsConfig;
import com.sequenceiq.it.util.LongStringGeneratorUtil;

public class RdsConfigTests extends CloudbreakTest {
    private static final String VALID_RDS_CONFIG = "e2e-rds";

    private static final String HIVE = "HIVE_CONFIG_NAME";

    private static final String SPECIAL_RDS_NAME = "a-@#$%|:&*;";

    private static final String RDS_PROTOCOL = "jdbc:postgresql://";

    private static final String[] TYPES = {"hive", "ranger", "superset", "oozie", "druid"};

    private List<String> rdsConfigsToDelete = new ArrayList<>();

    private String rdsUser;

    private String rdsPassword;

    private String rdsConnectionUrl;

    @Inject
    private LongStringGeneratorUtil longStringGeneratorUtil;

    @BeforeTest
    public void setup() throws Exception {
        given(CloudbreakClient.created());
        rdsUser = getTestParameter().get("integrationtest.rdsconfig.rdsUser");
        rdsPassword = getTestParameter().get("integrationtest.rdsconfig.rdsPassword");
        rdsConnectionUrl = getTestParameter().get("integrationtest.rdsconfig.rdsConnectionUrl");
    }

    @Test
    public void testCreateRdsWithAllTypes() throws Exception {
        for (String type : TYPES) {
            given(RdsConfig.request()
                    .withName(VALID_RDS_CONFIG + '-' + type)
                    .withConnectionPassword(rdsPassword)
                    .withConnectionURL(rdsConnectionUrl)
                    .withConnectionUserName(rdsUser)
                    .withType(type), "create valid rds config with " + type
            );
            when(RdsConfig.post(), "post the request");
            then(RdsConfig.assertThis(
                    (rdsconfig, t) -> Assert.assertNotNull(rdsconfig.getResponse().getId(), "rds config id must not be null"))
            );
            rdsConfigsToDelete.add(type);
        }
    }

    @Test(expectedExceptions = BadRequestException.class)
    public void testCreateInvalidRdsWithLongName() throws Exception {
        given(RdsConfig.request()
                .withName(longStringGeneratorUtil.stringGenerator(51))
                .withConnectionPassword(rdsPassword)
                .withConnectionURL(rdsConnectionUrl)
                .withConnectionUserName(rdsUser)
                .withType(HIVE), "create rds config with long name"
            );
            when(RdsConfig.post(), "post the request");
            then(RdsConfig.assertThis(
                    (rdsconfig, t) -> Assert.assertNull(rdsconfig.getResponse().getId(), "rds config id must be null"))
            );
    }

    @Test(expectedExceptions = BadRequestException.class)
    public void testCreateInvalidRdsWithShortName() throws Exception {
        given(RdsConfig.request()
                .withName("abc")
                .withConnectionPassword(rdsPassword)
                .withConnectionURL(rdsConnectionUrl)
                .withConnectionUserName(rdsUser)
                .withType(HIVE), "create rds config with short name"
        );
        when(RdsConfig.post(), "post the request");
        then(RdsConfig.assertThis(
                (rdsconfig, t) -> Assert.assertNull(rdsconfig.getResponse().getId(), "rds config id must be null"))
        );
    }

    @Test(expectedExceptions = BadRequestException.class)
    public void testCreateInvalidRdsWithSpecialName() throws Exception {
        given(RdsConfig.request()
                .withName(SPECIAL_RDS_NAME)
                .withConnectionPassword(rdsPassword)
                .withConnectionURL(rdsConnectionUrl)
                .withConnectionUserName(rdsUser)
                .withType(HIVE), "create rds config with special name"
        );
        when(RdsConfig.post(), "post the request");
        then(RdsConfig.assertThis(
                (rdsconfig, t) -> Assert.assertNull(rdsconfig.getResponse().getId(), "rds config id must be null"))
        );
    }

    @Test()
    public void testCreateRdsWithInvalidUserName() throws Exception {
        given(RdsConfig.request()
                .withName(VALID_RDS_CONFIG + "-name")
                .withConnectionPassword(rdsPassword)
                .withConnectionURL(rdsConnectionUrl)
                .withConnectionUserName("abcde1234")
                .withType(HIVE), "create rds config with invalid user name"
        );
        when(RdsConfig.post(), "post the request");
        then(RdsConfig.assertThis(
                (rdsconfig, t) -> Assert.assertNotNull(rdsconfig.getResponse().getId(), "rds config id must not be null"))
        );
        rdsConfigsToDelete.add("name");
    }

    @Test()
    public void testCreateRdsWithInvalidPwd() throws Exception {
        given(RdsConfig.request()
                .withName(VALID_RDS_CONFIG + "-pwd")
                .withConnectionPassword("abcd1234")
                .withConnectionURL(rdsConnectionUrl)
                .withConnectionUserName(rdsUser)
                .withType(HIVE), "create rds config with invalid password"
        );
        when(RdsConfig.post(), "post the request");
        then(RdsConfig.assertThis(
                (rdsconfig, t) -> Assert.assertNotNull(rdsconfig.getResponse().getId(), "rds config id must not be null"))
        );
        rdsConfigsToDelete.add("pwd");
    }

    @Test(expectedExceptions = BadRequestException.class)
    public void testCreateRdsWithInvalidUrlNoProt() throws Exception {
        given(RdsConfig.request()
                .withName(VALID_RDS_CONFIG + "-urlnp")
                .withConnectionPassword(rdsPassword)
                .withConnectionURL(rdsConnectionUrl.split(RDS_PROTOCOL)[1])
                .withConnectionUserName(rdsUser)
                .withType(HIVE), "create rds config with invalid connection url no protocol"
        );
        when(RdsConfig.post(), "post the request");
        then(RdsConfig.assertThis(
                (rdsconfig, t) -> Assert.assertNull(rdsconfig.getResponse().getId(), "rds config id must be null"))
        );
    }

    @Test()
    public void testCreateRdsWithUrlNotExists() throws Exception {
        given(RdsConfig.request()
                .withName(VALID_RDS_CONFIG + "-url")
                .withConnectionPassword(rdsPassword)
                .withConnectionURL(RDS_PROTOCOL + "www.google.com:1/test")
                .withConnectionUserName(rdsUser)
                .withType(HIVE), "create rds config with connection url not exits"
        );
        when(RdsConfig.post(), "post the request");
        then(RdsConfig.assertThis(
                (rdsconfig, t) -> Assert.assertNotNull(rdsconfig.getResponse().getId(), "rds config id must not be null"))
        );
        rdsConfigsToDelete.add("url");
    }

    @Test
    public void testCreateDeleteCreateAgain() throws Exception {
        given(RdsConfig.isCreatedDeleted()
                .withName(VALID_RDS_CONFIG + "-again")
                .withConnectionPassword(rdsPassword)
                .withConnectionURL(rdsConnectionUrl)
                .withConnectionUserName(rdsUser)
                .withType(HIVE), "create rds config, then delete"
        );

        given(RdsConfig.request()
                .withName(VALID_RDS_CONFIG + "-again")
                .withConnectionPassword(rdsPassword)
                .withConnectionURL(rdsConnectionUrl)
                .withConnectionUserName(rdsUser)
                .withType(HIVE), "create rds config with same name again"
        );
        when(RdsConfig.post(), "post the request");
        then(RdsConfig.assertThis(
                (rdsconfig, t) -> Assert.assertNotNull(rdsconfig.getResponse().getId(), "rds config id must not be null"))
        );
        rdsConfigsToDelete.add("again");
    }

    @Test(expectedExceptions = BadRequestException.class)
    public void testCreateRdsWithSameName() throws Exception {
        given(RdsConfig.isCreated()
                .withName(VALID_RDS_CONFIG + "-same")
                .withConnectionPassword(rdsPassword)
                .withConnectionURL(rdsConnectionUrl)
                .withConnectionUserName(rdsUser)
                .withType(HIVE), "create rds config with long name"
        );

        given(RdsConfig.request()
                .withName(VALID_RDS_CONFIG + "-same")
                .withConnectionPassword(rdsPassword)
                .withConnectionURL(rdsConnectionUrl)
                .withConnectionUserName(rdsUser)
                .withType(HIVE), "create rds with name already exists"
        );
        try {
            when(RdsConfig.post(), "post the request");
            then(RdsConfig.assertThis(
                    (rdsconfig, t) -> Assert.assertNull(rdsconfig.getResponse().getId(), "rds config id must be null"))
            );
        } finally {
            rdsConfigsToDelete.add("same");
        }
    }

    @Test
    public void testRdsConnectOk() throws Exception {
        given(RdsConfig.request()
                .withName(VALID_RDS_CONFIG)
                .withConnectionPassword(rdsPassword)
                .withConnectionURL(rdsConnectionUrl)
                .withConnectionUserName(rdsUser)
                .withType(HIVE), "create valid rds connection test"
        );
        when(RdsConfig.testConnect(), "post the request");
        then(RdsConfig.assertThis(
                (rdsconfig, t) -> Assert.assertTrue(rdsconfig.getResponseTestResult().getResult().contains("connected"),
                        "rds connection test should be OK"))
        );
    }

    @Test
    public void testRdsConnectOkNoValidation() throws Exception {
        given(RdsConfig.request()
                .withName(VALID_RDS_CONFIG)
                .withConnectionPassword(rdsPassword)
                .withConnectionURL(rdsConnectionUrl)
                .withConnectionUserName(rdsUser)
                .withType("RANGER_CONFIG_NAME"), "create valid rds connection test no validation"
        );
        when(RdsConfig.testConnect(), "post the request");
        then(RdsConfig.assertThis(
                (rdsconfig, t) -> Assert.assertTrue(rdsconfig.getResponseTestResult().getResult().contains("connected"),
                        "rds connection test should be OK"))
        );
    }

    @Test
    public void testRdsConnectNotOkInvalidPwd() throws Exception {
        given(RdsConfig.request()
                .withName(VALID_RDS_CONFIG)
                .withConnectionPassword("")
                .withConnectionURL(rdsConnectionUrl)
                .withConnectionUserName(rdsUser)
                .withType(HIVE), "create rds connection test with invalid password"
        );
        when(RdsConfig.testConnect(), "post the request");
        then(RdsConfig.assertThis(
                (rdsconfig, t) -> Assert.assertFalse(rdsconfig.getResponseTestResult().getResult().contains("connected"),
                        "rds connection test should not be OK"))
        );
    }

    @Test
    public void testRdsConnectWithUrlNotExists() throws Exception {
        given(RdsConfig.request()
                .withName(VALID_RDS_CONFIG)
                .withConnectionPassword(rdsPassword)
                .withConnectionURL(RDS_PROTOCOL + "www.google.com:1/test")
                .withConnectionUserName(rdsUser)
                .withType(HIVE), "create rds connection test with url not exits"
        );
        when(RdsConfig.testConnect(), "post the request");
        then(RdsConfig.assertThis(
                (rdsconfig, t) -> Assert.assertFalse(rdsconfig.getResponseTestResult().getResult().contains("connected"),
                        "rds connection test should not be OK"))
        );
    }

    @Test
    public void testRdsConnectWithInvalidUserName() throws Exception {
        given(RdsConfig.request()
                .withName(VALID_RDS_CONFIG)
                .withConnectionPassword(rdsPassword)
                .withConnectionURL(rdsConnectionUrl)
                .withConnectionUserName("abcde1234")
                .withType(HIVE), "create rds connnection test with invalid user name"
        );
        when(RdsConfig.testConnect(), "post the request");
        then(RdsConfig.assertThis(
                (rdsconfig, t) -> Assert.assertFalse(rdsconfig.getResponseTestResult().getResult().contains("connected"),
                        "rds connection test should not be OK"))
        );
    }

    @Test(expectedExceptions = BadRequestException.class)
    public void testRdsConnectWithInvalidUrlNoProt() throws Exception {
        given(RdsConfig.request()
                .withName(VALID_RDS_CONFIG)
                .withConnectionPassword(rdsPassword)
                .withConnectionURL(rdsConnectionUrl.split(RDS_PROTOCOL)[1])
                .withConnectionUserName(rdsUser)
                .withType(HIVE), "create rds connection test with invalid connection url no protocol"
        );
        when(RdsConfig.testConnect(), "post the request");
        then(RdsConfig.assertThis(
                (rdsconfig, t) -> Assert.assertFalse(rdsconfig.getResponseTestResult().getResult().contains("connected"),
                        "rds connection test should not be OK"))
        );
    }

    @AfterSuite()
    public void cleanAll() throws Exception {
        for (String rdsConfig : rdsConfigsToDelete) {
            given(RdsConfig.request()
                    .withName(VALID_RDS_CONFIG + '-' + rdsConfig)
            );
            when(RdsConfig.delete());
        }
    }
}