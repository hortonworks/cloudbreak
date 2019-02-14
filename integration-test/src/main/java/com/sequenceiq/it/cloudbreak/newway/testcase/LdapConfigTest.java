package com.sequenceiq.it.cloudbreak.newway.testcase;

import static com.sequenceiq.it.cloudbreak.newway.context.RunningParameter.exceptionConsumer;
import static com.sequenceiq.it.cloudbreak.newway.context.RunningParameter.key;
import static com.sequenceiq.it.cloudbreak.newway.util.ResponseUtil.getErrorMessage;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import java.util.UnknownFormatConversionException;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.ldaps.DirectoryType;
import com.sequenceiq.it.cloudbreak.newway.RandomNameCreator;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.ldap.LdapConfig;
import com.sequenceiq.it.cloudbreak.newway.entity.ldap.LdapConfigEntity;
import com.sequenceiq.it.util.LongStringGeneratorUtil;

public class LdapConfigTest extends AbstractIntegrationTest {

    private static final String TEST_CONTEXT = "testContext";

    private static final String INVALID_LDAP_NAME = "a-@#$%|:&*;";

    @Inject
    private LongStringGeneratorUtil longStringGenerator;

    @Inject
    private RandomNameCreator randomNameCreator;

    @BeforeMethod
    public void beforeMethod(Object[] data) {
        minimalSetupForClusterCreation((TestContext) data[0]);
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown(Object[] data) {
        ((TestContext) data[0]).cleanupTestContextEntity();
    }

    @Test(dataProvider = TEST_CONTEXT)
    public void testCreateValidLdap(TestContext testContext) {
        String name = randomNameCreator.getRandomNameForMock();
        testContext
                .given(LdapConfigEntity.class)
                .valid()
                .withName(name)
                .when(LdapConfig.postV4())
                .then((tc, entity, cc) -> {
                    assertNotNull(entity);
                    assertNotNull(entity.getResponse());
                    return entity;
                })
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT)
    public void testCreateValidAd(TestContext testContext) {
        String name = randomNameCreator.getRandomNameForMock();
        testContext
                .given(LdapConfigEntity.class)
                .valid()
                .withName(name)
                .withDirectoryType(DirectoryType.ACTIVE_DIRECTORY)
                .when(LdapConfig.postV4())
                .then((tc, entity, cc) -> {
                    assertNotNull(entity);
                    assertNotNull(entity.getResponse());
                    return entity;
                })
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT)
    public void testCreateLdapWithMissingName(TestContext testContext) {
        String key = randomNameCreator.getRandomNameForMock();
        testContext
                .given(LdapConfigEntity.class)
                .valid()
                .withName("")
                .when(LdapConfig.postV4(), key(key))
                .expect(BadRequestException.class, exceptionConsumer(e -> {
                    assertThat(getErrorMessage(e), containsString("The length of the ldap config's name has to be in range of 1 to 100"));
                }).withKey(key))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT)
    public void testCreateLdapWithInvalidName(TestContext testContext) {
        testContext
                .given(LdapConfigEntity.class)
                .valid()
                .withName(INVALID_LDAP_NAME)
                .when(LdapConfig.postV4(), key(INVALID_LDAP_NAME))
                .expect(UnknownFormatConversionException.class, exceptionConsumer(e -> {
                    assertThat(getErrorMessage(e), containsString("Conversion = '|'"));
                }).withKey(INVALID_LDAP_NAME))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT)
    public void testCreateLdapWithLongName(TestContext testContext) {
        String longName = longStringGenerator.stringGenerator(101);
        testContext
                .given(LdapConfigEntity.class)
                .valid()
                .withName(longName)
                .when(LdapConfig.postV4(), key(longName))
                .expect(BadRequestException.class, exceptionConsumer(e -> {
                    assertThat(getErrorMessage(e), containsString("The length of the ldap config's name has to be in range of 1 to 100"));
                }).withKey(longName))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT)
    public void testCreateLdapWithLongDesc(TestContext testContext) {
        String name = randomNameCreator.getRandomNameForMock();
        String longDesc = longStringGenerator.stringGenerator(1001);
        testContext
                .given(LdapConfigEntity.class)
                .valid()
                .withName(name)
                .withDescription(longDesc)
                .when(LdapConfig.postV4(), key(longDesc))
                .expect(BadRequestException.class, exceptionConsumer(e -> {
                    assertThat(getErrorMessage(e), containsString("The length of the ldap config's description has to be in range of 0 to 1000"));
                }).withKey(longDesc))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT)
    public void testCreateDeleteCreateAgain(TestContext testContext) {
        String name = randomNameCreator.getRandomNameForMock();
        testContext
                .given(name, LdapConfigEntity.class)
                .valid()
                .withName(name)
                .when(LdapConfig.postV4(), key(name))
                .when(LdapConfig.deleteV4(), key(name))
                .when(LdapConfig.postV4(), key(name))
                .then((tc, entity, cc) -> {
                    assertNotNull(entity);
                    assertNotNull(entity.getResponse());
                    return entity;
                }, key(name))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT)
    public void testCreateLdapWithSameName(TestContext testContext) {
        String name = randomNameCreator.getRandomNameForMock();
        testContext
                .given(name, LdapConfigEntity.class)
                .valid()
                .withName(name)
                .when(LdapConfig.postV4(), key(name))
                .then((tc, entity, cc) -> {
                    assertNotNull(entity);
                    assertNotNull(entity.getResponse());
                    return entity;
                }, key(name))

                .given(name, LdapConfigEntity.class)
                .valid()
                .withName(name)
                .when(LdapConfig.postV4(), key(name))
                .expect(BadRequestException.class, exceptionConsumer(e -> {
                    assertThat(getErrorMessage(e), containsString("dap already exists with name"));
                }).withKey(name))
                .validate();
    }
}