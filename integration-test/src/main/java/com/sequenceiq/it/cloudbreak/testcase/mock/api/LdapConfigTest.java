package com.sequenceiq.it.cloudbreak.testcase.mock.api;

import static org.junit.Assert.assertNotNull;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;

import org.testng.annotations.Test;

import com.sequenceiq.freeipa.api.v1.ldap.model.DirectoryType;
import com.sequenceiq.it.cloudbreak.client.LdapTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.RunningParameter;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.ldap.LdapTestDto;
import com.sequenceiq.it.cloudbreak.testcase.AbstractIntegrationTest;

public class LdapConfigTest extends AbstractIntegrationTest {

    private static final String INVALID_LDAP_NAME = "a-@#$%%|:&*;";

    @Inject
    private LdapTestClient ldapTestClient;

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "a valid ldap request",
            when = "calling create ldap",
            then = "the ldap should be created")
    public void testCreateValidLdap(TestContext testContext) {
        String name = resourcePropertyProvider().getName();
        testContext
                .given(LdapTestDto.class)
                .valid()
                .withName(name)
                .when(ldapTestClient.createV1())
                .then((tc, entity, cc) -> {
                    assertNotNull(entity);
                    assertNotNull(entity.getResponse());
                    return entity;
                })
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "a valid active directory request",
            when = "calling create ldap",
            then = "the ldap should be created")
    public void testCreateValidAd(TestContext testContext) {
        String name = resourcePropertyProvider().getName();
        testContext
                .given(LdapTestDto.class)
                .valid()
                .withName(name)
                .withDirectoryType(DirectoryType.ACTIVE_DIRECTORY)
                .when(ldapTestClient.createV1())
                .then((tc, entity, cc) -> {
                    assertNotNull(entity);
                    assertNotNull(entity.getResponse());
                    return entity;
                })
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "an invalid ldap request with empty name",
            when = "calling create ldap",
            then = "getting BadRequestException because ldap needs a valid name")
    public void testCreateLdapWithMissingName(TestContext testContext) {
        String key = resourcePropertyProvider().getName();
        testContext
                .given(LdapTestDto.class)
                .valid()
                .withName("")
                .when(ldapTestClient.createV1(), RunningParameter.key(key))
                .expect(BadRequestException.class,
                        RunningParameter.expectedMessage("The length of the ldap config's name has to be in range of 1 to 100")
                                .withKey(key))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "an invalid ldap request with empty environmentCrn",
            when = "calling create ldap",
            then = "getting BadRequestException because ldap needs a valid environmentCrn")
    public void testCreateLdapWithMissingEnvironmentCrn(TestContext testContext) {
        String key = resourcePropertyProvider().getName();
        testContext
                .given(LdapTestDto.class)
                .valid()
                .withEnvironmentCrn("")
                .when(ldapTestClient.createV1(), RunningParameter.key(key))
                .expect(BadRequestException.class,
                        RunningParameter.expectedMessage(".*environmentCrn.*must not be empty.*")
                                .withKey(key))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "an invalid ldap request with specific characters in the name",
            when = "calling create ldap",
            then = "getting BadRequestException because ldap needs a valid name")
    public void testCreateLdapWithInvalidName(TestContext testContext) {
        testContext
                .given(LdapTestDto.class)
                .valid()
                .withName(INVALID_LDAP_NAME)
                .when(ldapTestClient.createV1(), RunningParameter.key(INVALID_LDAP_NAME))
                .expect(BadRequestException.class, RunningParameter.expectedMessage(
                        "The name can only contain lowercase alphanumeric characters and hyphens and has start with an alphanumeric character")
                                .withKey(INVALID_LDAP_NAME))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "an invalid ldap request with too long name",
            when = "calling create ldap",
            then = "getting BadRequestException because ldap needs a valid name")
    public void testCreateLdapWithLongName(TestContext testContext) {
        String longName = getLongNameGenerator().stringGenerator(101);
        testContext
                .given(LdapTestDto.class)
                .valid()
                .withName(longName)
                .when(ldapTestClient.createV1(), RunningParameter.key(longName))
                .expect(BadRequestException.class,
                        RunningParameter.expectedMessage("The length of the ldap config's name has to be in range of 1 to 100")
                                .withKey(longName))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "an invalid ldap request with too long description",
            when = "calling create ldap",
            then = "getting BadRequestException because ldap needs a valid description")
    public void testCreateLdapWithLongDesc(TestContext testContext) {
        String name = resourcePropertyProvider().getName();
        String longDesc = getLongNameGenerator().stringGenerator(1001);
        testContext
                .given(LdapTestDto.class)
                .valid()
                .withName(name)
                .withDescription(longDesc)
                .when(ldapTestClient.createV1(), RunningParameter.key(longDesc))
                .expect(BadRequestException.class,
                        RunningParameter.expectedMessage("The length of the ldap config's description has to be in range of 0 to 1000")
                                .withKey(longDesc))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "a valid ldap request",
            when = "calling create ldap and delete and create again with the same name",
            then = "ldap should be created")
    public void testCreateDeleteCreateAgain(TestContext testContext) {
        String name = resourcePropertyProvider().getName();
        testContext
                .given(name, LdapTestDto.class)
                .valid()
                .withName(name)
                .when(ldapTestClient.createV1(), RunningParameter.key(name))
                .when(ldapTestClient.deleteV1(), RunningParameter.key(name))
                .when(ldapTestClient.createV1(), RunningParameter.key(name))
                .then((tc, entity, cc) -> {
                    assertNotNull(entity);
                    assertNotNull(entity.getResponse());
                    return entity;
                }, RunningParameter.key(name))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "a valid ldap request",
            when = "calling create ldap and create again with the same environment",
            then = "getting BadRequestException because only one ldap can exist with same environment in an account at the same time")
    public void testCreateLdapWithSameEnvironment(TestContext testContext) {
        String name = resourcePropertyProvider().getName();
        testContext
                .given(name, LdapTestDto.class)
                .valid()
                .withEnvironmentCrn(name)
                .when(ldapTestClient.createV1(), RunningParameter.key(name))
                .then((tc, entity, cc) -> {
                    assertNotNull(entity);
                    assertNotNull(entity.getResponse());
                    return entity;
                }, RunningParameter.key(name))

                .given(name, LdapTestDto.class)
                .valid()
                .withEnvironmentCrn(name)
                .when(ldapTestClient.createV1(), RunningParameter.key(name))
                .expect(BadRequestException.class,
                        RunningParameter.expectedMessage("environment is already exists")
                                .withKey(name))
                .validate();
    }
}
