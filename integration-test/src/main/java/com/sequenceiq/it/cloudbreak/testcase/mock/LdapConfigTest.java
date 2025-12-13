package com.sequenceiq.it.cloudbreak.testcase.mock;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.expectedMessage;
import static com.sequenceiq.it.cloudbreak.context.RunningParameter.expectedPayload;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;

import org.testng.annotations.Test;

import com.sequenceiq.freeipa.api.v1.ldap.model.DirectoryType;
import com.sequenceiq.it.cloudbreak.assertion.audit.LdapConfigAuditGrpcServiceAssertion;
import com.sequenceiq.it.cloudbreak.assertion.ldap.LdapListStructuredEventAssertions;
import com.sequenceiq.it.cloudbreak.client.LdapTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.context.RunningParameter;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.ldap.LdapTestDto;

public class LdapConfigTest extends AbstractMockTest {

    private static final String INVALID_LDAP_NAME = "a-@#$%%|:&*;";

    @Inject
    private LdapTestClient ldapTestClient;

    @Inject
    private LdapListStructuredEventAssertions ldapListStructuredEventAssertions;

    @Inject
    private LdapConfigAuditGrpcServiceAssertion ldapConfigAuditGrpcServiceAssertion;

    protected void setupTest(TestContext testContext) {
        createDefaultUser(testContext);
        createDefaultCredential(testContext);
        createDefaultEnvironment(testContext);
        createDefaultImageCatalog(testContext);
        initializeDefaultBlueprints(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "a valid ldap request",
            when = "calling create ldap",
            then = "the ldap should be created")
    public void testCreateValidLdap(MockedTestContext testContext) {
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
                .when(ldapTestClient.deleteV1())
                .then(ldapListStructuredEventAssertions::checkCreateEvents)
                .then(ldapListStructuredEventAssertions::checkDeleteEvents)
                .then(ldapConfigAuditGrpcServiceAssertion::create)
                .then(ldapConfigAuditGrpcServiceAssertion::delete)
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "a valid active directory request",
            when = "calling create ldap",
            then = "the ldap should be created")
    public void testCreateValidAd(MockedTestContext testContext) {
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
    public void testCreateLdapWithMissingName(MockedTestContext testContext) {
        testContext
                .given(LdapTestDto.class)
                .valid()
                .withName("")
                .whenException(ldapTestClient.createV1(), BadRequestException.class,
                        expectedMessage("The length of the ldap config's name has to be in range of 1 to 100"))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "an invalid ldap request with empty environmentCrn",
            when = "calling create ldap",
            then = "getting BadRequestException because ldap needs a valid environmentCrn")
    public void testCreateLdapWithMissingEnvironmentCrn(MockedTestContext testContext) {
        testContext
                .given(LdapTestDto.class)
                .valid()
                .withEnvironmentCrn("")
                .whenException(ldapTestClient.createV1(), BadRequestException.class, expectedPayload(".*environmentCrn.*must not be empty.*"))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "an invalid ldap request with specific characters in the name",
            when = "calling create ldap",
            then = "getting BadRequestException because ldap needs a valid name")
    public void testCreateLdapWithInvalidName(MockedTestContext testContext) {
        testContext
                .given(LdapTestDto.class)
                .valid()
                .withName(INVALID_LDAP_NAME)
                .whenException(ldapTestClient.createV1(), BadRequestException.class,
                        expectedMessage("The name can only contain lowercase alphanumeric characters" +
                                " and hyphens and has start with an alphanumeric character"))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "an invalid ldap request with too long name",
            when = "calling create ldap",
            then = "getting BadRequestException because ldap needs a valid name")
    public void testCreateLdapWithLongName(MockedTestContext testContext) {
        String longName = getLongNameGenerator().stringGenerator(101);
        testContext
                .given(LdapTestDto.class)
                .valid()
                .withName(longName)
                .whenException(ldapTestClient.createV1(), BadRequestException.class,
                        expectedMessage("The length of the ldap config's name has to be in range of 1 to 100"))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "an invalid ldap request with too long description",
            when = "calling create ldap",
            then = "getting BadRequestException because ldap needs a valid description")
    public void testCreateLdapWithLongDesc(MockedTestContext testContext) {
        String name = resourcePropertyProvider().getName();
        String longDesc = getLongNameGenerator().stringGenerator(1001);
        testContext
                .given(LdapTestDto.class)
                .valid()
                .withName(name)
                .withDescription(longDesc)
                .whenException(ldapTestClient.createV1(), BadRequestException.class,
                        expectedMessage("The length of the ldap config's description has to be in range of 0 to 1000"))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "a valid ldap request",
            when = "calling create ldap and delete and create again with the same name",
            then = "ldap should be created")
    public void testCreateDeleteCreateAgain(MockedTestContext testContext) {
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
    public void testCreateLdapWithSameEnvironment(MockedTestContext testContext) {
        String name = resourcePropertyProvider().getName();
        String envCrn = testContext.get(EnvironmentTestDto.class).getCrn();
        testContext
                .given(name, LdapTestDto.class)
                .valid()
                .withEnvironmentCrn(envCrn)
                .when(ldapTestClient.createV1(), RunningParameter.key(name))
                .then((tc, entity, cc) -> {
                    assertNotNull(entity);
                    assertNotNull(entity.getResponse());
                    return entity;
                }, RunningParameter.key(name))

                .given(name, LdapTestDto.class)
                .valid()
                .withEnvironmentCrn(envCrn)
                .whenException(ldapTestClient.createV1(), BadRequestException.class, expectedMessage("environment is already exists"))
                .validate();
    }
}
