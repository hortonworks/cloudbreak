package com.sequenceiq.it.cloudbreak.newway.testcase.mock;

import static com.sequenceiq.it.cloudbreak.newway.context.RunningParameter.key;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.ForbiddenException;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.environment.responses.SimpleEnvironmentV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.ldaps.responses.LdapV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.proxies.responses.ProxyV4Response;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.Environment;
import com.sequenceiq.it.cloudbreak.newway.EnvironmentEntity;
import com.sequenceiq.it.cloudbreak.newway.assertion.CheckEnvironmentCredential;
import com.sequenceiq.it.cloudbreak.newway.client.CredentialTestClient;
import com.sequenceiq.it.cloudbreak.newway.context.Description;
import com.sequenceiq.it.cloudbreak.newway.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.credential.CredentialTestDto;
import com.sequenceiq.it.cloudbreak.newway.entity.database.DatabaseEntity;
import com.sequenceiq.it.cloudbreak.newway.entity.ldap.LdapConfigTestDto;
import com.sequenceiq.it.cloudbreak.newway.entity.proxy.ProxyConfigEntity;
import com.sequenceiq.it.cloudbreak.newway.testcase.AbstractIntegrationTest;
import com.sequenceiq.it.cloudbreak.newway.util.EnvironmentTestUtils;

public class EnvironmentTest extends AbstractIntegrationTest {
    private static final String FORBIDDEN_KEY = "forbiddenPost";

    private static final Set<String> INVALID_REGION = new HashSet<>(Collections.singletonList("MockRegion"));

    private static final Set<String> INVALID_PROXY = new HashSet<>(Collections.singletonList("InvalidProxy"));

    private static final Set<String> INVALID_LDAP = new HashSet<>(Collections.singletonList("InvalidLdap"));

    private static final Set<String> INVALID_RDS = new HashSet<>(Collections.singletonList("InvalidRds"));

    private Set<String> mixedProxy = new HashSet<>();

    private Set<String> mixedLdap = new HashSet<>();

    private Set<String> mixedRds = new HashSet<>();

    @BeforeMethod
    public void beforeMethod(Object[] data) {
        MockedTestContext testContext = (MockedTestContext) data[0];
        createDefaultUser(testContext);
        createDefaultCredential(testContext);
        initializeDefaultClusterDefinitions(testContext);
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown(Object[] data) {
        ((MockedTestContext) data[0]).cleanupTestContextEntity();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running cloudbreak",
            when = "valid create environment request is sent",
            then = "environment should be created")
    public void testCreateEnvironment(TestContext testContext) {
        testContext
                .given(EnvironmentEntity.class)
                .when(Environment::post)
                .then(this::checkCredentialAttachedToEnv)
                .when(Environment::getAll)
                .then(this::checkEnvIsListed)
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running cloudbreak",
            when = "valid create environment request is sent with attached proxy config in it",
            then = "environment should be created and the proxy should be attached")
    public void testCreateEnvironmenWithProxy(TestContext testContext) {
        createDefaultProxyConfig(testContext);
        Set<String> validProxy = new HashSet<>();
        validProxy.add(testContext.get(ProxyConfigEntity.class).getName());
        testContext
                .given(EnvironmentEntity.class)
                .withProxyConfigs(validProxy)
                .when(Environment::post)
                .then(this::checkCredentialAttachedToEnv)
                .when(Environment::getAll)
                .then(this::checkEnvIsListed)
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running cloudbreak",
            when = "valid create environment request is sent with attached ldap config in it",
            then = "environment should be created and the ldap should be attached")
    public void testCreateEnvironmenWithLdap(TestContext testContext) {
        createDefaultLdapConfig(testContext);
        Set<String> validLdap = new HashSet<>();
        validLdap.add(testContext.get(LdapConfigTestDto.class).getName());
        testContext
                .given(EnvironmentEntity.class)
                .withLdapConfigs(validLdap)
                .when(Environment::post)
                .then(this::checkCredentialAttachedToEnv)
                .when(Environment::getAll)
                .then(this::checkEnvIsListed)
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running cloudbreak",
            when = "valid create environment request is sent with attached database config in it",
            then = "environment should be created and the database should be attached")
    public void testCreateEnvironmenWithDatabase(TestContext testContext) {
        createDefaultRdsConfig(testContext);
        Set<String> validRds = new HashSet<>();
        validRds.add(testContext.get(DatabaseEntity.class).getName());
        testContext
                .given(EnvironmentEntity.class)
                .withRdsConfigs(validRds)
                .when(Environment::post)
                .then(this::checkCredentialAttachedToEnv)
                .when(Environment::getAll)
                .then(this::checkEnvIsListed)
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running cloudbreak",
            when = "a create environment request is sent with an invalid region in it",
            then = "a BadRequestException should be returned")
    public void testCreateEnvironmentInvalidRegion(TestContext testContext) {
        String forbiddenKey = getNameGenerator().getRandomNameForResource();
        testContext
                .init(EnvironmentEntity.class)
                .withRegions(INVALID_REGION)
                .when(Environment::post, key(forbiddenKey))
                .expect(BadRequestException.class, key(forbiddenKey))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running cloudbreak",
            when = "a create environment request is sent with no region in it",
            then = "a BadRequestException should be returned")
    public void testCreateEnvironmentNoRegion(TestContext testContext) {
        String forbiddenKey = getNameGenerator().getRandomNameForResource();
        testContext
                .init(EnvironmentEntity.class)
                .withRegions(null)
                .when(Environment::post, key(forbiddenKey))
                .expect(BadRequestException.class, key(forbiddenKey))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running cloudbreak",
            when = "a create environment request with reference to a non-existing credential is sent",
            then = "a BadRequestException should be returned")
    public void testCreateEnvironmentNotExistCredential(TestContext testContext) {
        String forbiddenKey = getNameGenerator().getRandomNameForResource();
        testContext
                .init(EnvironmentEntity.class)
                .withCredentialName("notexistingcredendital")
                .when(Environment::post, key(forbiddenKey))
                .expect(BadRequestException.class, key(forbiddenKey))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running cloudbreak",
            when = "a create environment request with reference to a non-existing proxy is sent",
            then = "a BadRequestException should be returned")
    public void testCreateEnvironmentNotExistProxy(TestContext testContext) {
        String forbiddenKey = getNameGenerator().getRandomNameForResource();
        testContext
                .init(EnvironmentEntity.class)
                .withProxyConfigs(INVALID_PROXY)
                .when(Environment::post, key(forbiddenKey))
                .expect(BadRequestException.class, key(forbiddenKey))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running cloudbreak",
            when = "a create environment request with reference to a non-existing ldap is sent",
            then = "a BadRequestException should be returned")
    public void testCreateEnvironmentNotExistLdap(TestContext testContext) {
        String forbiddenKey = getNameGenerator().getRandomNameForResource();
        testContext
                .init(EnvironmentEntity.class)
                .withLdapConfigs(INVALID_LDAP)
                .when(Environment::post, key(forbiddenKey))
                .expect(BadRequestException.class, key(forbiddenKey))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running cloudbreak",
            when = "a create environment request with reference to a non-existing ldap is sent",
            then = "a BadRequestException should be returned")
    public void testCreateEnvironmentNotExistRds(TestContext testContext) {
        String forbiddenKey = getNameGenerator().getRandomNameForResource();
        testContext
                .init(EnvironmentEntity.class)
                .withRdsConfigs(INVALID_RDS)
                .when(Environment::post, key(forbiddenKey))
                .expect(BadRequestException.class, key(forbiddenKey))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running cloudbreak",
            when = "a create environment request with reference to an existing and a non-existing proxy is sent",
            then = "a BadRequestException should be returned")
    public void testCreateEnvWithExistingAndNotExistingProxy(TestContext testContext) {
        String forbiddenKey = getNameGenerator().getRandomNameForResource();
        createDefaultProxyConfig(testContext);
        mixedProxy.add(testContext.get(ProxyConfigEntity.class).getName());
        mixedProxy.add("invalidProxy");
        testContext
                .init(EnvironmentEntity.class)
                .withProxyConfigs(mixedProxy)
                .when(Environment::post, key(forbiddenKey))
                .expect(BadRequestException.class, key(forbiddenKey))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running cloudbreak",
            when = "a create environment request with reference to an existing and a non-existing database is sent",
            then = "a BadRequestException should be returned")
    public void testCreateEnvWithExistingAndNotExistingRds(TestContext testContext) {
        String forbiddenKey = getNameGenerator().getRandomNameForResource();
        createDefaultRdsConfig(testContext);
        mixedRds.add(testContext.get(DatabaseEntity.class).getName());
        mixedRds.add("invalidRds");
        testContext
                .init(EnvironmentEntity.class)
                .withRdsConfigs(mixedRds)
                .when(Environment::post, key(forbiddenKey))
                .expect(BadRequestException.class, key(forbiddenKey))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running cloudbreak",
            when = "a create environment request with reference to an existing and a non-existing ldap is sent",
            then = "a BadRequestException should be returned")
    public void testCreateEnvWithExistingAndNotExistingLdap(TestContext testContext) {
        String forbiddenKey = getNameGenerator().getRandomNameForResource();
        createDefaultLdapConfig(testContext);
        mixedLdap.add(testContext.get(LdapConfigTestDto.class).getName());
        mixedLdap.add("invalidLdap");
        testContext
                .init(EnvironmentEntity.class)
                .withLdapConfigs(mixedLdap)
                .when(Environment::post, key(forbiddenKey))
                .expect(BadRequestException.class, key(forbiddenKey))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is an available environment",
            when = "a delete request is sent for the environment",
            then = "the environment should be deleted")
    public void testDeleteEnvironment(TestContext testContext) {
        testContext
                .init(EnvironmentEntity.class)
                .when(Environment::post)
                .then(this::checkCredentialAttachedToEnv)
                .when(Environment::getAll)
                .then(this::checkEnvIsListed)
                .when(Environment::delete)
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is an available environment with an attached proxy",
            when = "a delete request is sent for the environment",
            then = "the environment should be deleted")
    public void testDeleteEnvWithProxy(TestContext testContext) {
        createDefaultProxyConfig(testContext);
        Set<String> validProxy = new HashSet<>();
        validProxy.add(testContext.get(ProxyConfigEntity.class).getName());
        testContext
                .init(EnvironmentEntity.class)
                .withProxyConfigs(validProxy)
                .when(Environment::post)
                .then(this::checkCredentialAttachedToEnv)
                .when(Environment::getAll)
                .then(this::checkEnvIsListed)
                .when(Environment::delete)
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is an available environment with an attached ldap",
            when = "a delete request is sent for the environment",
            then = "the environment should be deleted")
    public void testDeleteEnvWithLdap(TestContext testContext) {
        createDefaultLdapConfig(testContext);
        Set<String> validLdap = new HashSet<>();
        validLdap.add(testContext.get(LdapConfigTestDto.class).getName());
        testContext
                .init(EnvironmentEntity.class)
                .withLdapConfigs(validLdap)
                .when(Environment::post)
                .then(this::checkCredentialAttachedToEnv)
                .when(Environment::getAll)
                .then(this::checkEnvIsListed)
                .when(Environment::delete)
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is an available environment with an attached database",
            when = "a delete request is sent for the environment",
            then = "the environment should be deleted")
    public void testDeleteEnvWithRds(TestContext testContext) {
        createDefaultRdsConfig(testContext);
        Set<String> validRds = new HashSet<>();
        validRds.add(testContext.get(DatabaseEntity.class).getName());
        testContext
                .init(EnvironmentEntity.class)
                .withRdsConfigs(validRds)
                .when(Environment::post)
                .then(this::checkCredentialAttachedToEnv)
                .when(Environment::getAll)
                .then(this::checkEnvIsListed)
                .when(Environment::delete)
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running cloudbreak",
            when = "a delete request is sent for a non-existing environment",
            then = "a ForbiddenException should be returned")
    public void testDeleteEnvironmentNotExist(TestContext testContext) {
        String forbiddenKey = getNameGenerator().getRandomNameForResource();
        testContext
                .init(EnvironmentEntity.class)
                .when(Environment::delete, key(forbiddenKey))
                .expect(ForbiddenException.class, key(forbiddenKey))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is an available environment",
            when = "an rds attach request is sent for that environment",
            then = "the rds should be attached to the environment")
    public void testCreateEnvAttachRds(TestContext testContext) {
        createDefaultRdsConfig(testContext);
        Set<String> validRds = new HashSet<>();
        validRds.add(testContext.get(DatabaseEntity.class).getName());
        testContext
                .given(EnvironmentEntity.class)
                .when(Environment::post)
                .when(Environment::getAll)
                .given(EnvironmentEntity.class)
                .withRdsConfigs(validRds)
                .when(Environment::putAttachResources)
                .then(EnvironmentTestUtils::checkRdsAttachedToEnv)
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is an available environment",
            when = "an ldap attach request is sent for that environment",
            then = "the ldap should be attached to the environment")
    public void testCreateEnvAttachLdap(TestContext testContext) {
        String env = getNameGenerator().getRandomNameForResource();
        createDefaultLdapConfig(testContext);
        Set<String> validLdap = new HashSet<>();
        validLdap.add(testContext.get(LdapConfigTestDto.class).getName());
        testContext
                .given(EnvironmentEntity.class)
                .withName(env)
                .when(Environment::post)
                .given(EnvironmentEntity.class)
                .withLdapConfigs(validLdap)
                .when(Environment::putAttachResources)
                .then(this::checkLdapAttachedToEnv)
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is an available environment",
            when = "an proxy attach request is sent for that environment",
            then = "the proxy should be attached to the environment")
    public void testCreateEnvAttachProxy(TestContext testContext) {
        String env = getNameGenerator().getRandomNameForResource();
        createDefaultProxyConfig(testContext);
        Set<String> validProxy = new HashSet<>();
        validProxy.add(testContext.get(ProxyConfigEntity.class).getName());
        testContext
                .given(EnvironmentEntity.class)
                .withName(env)
                .when(Environment::post)
                .given(EnvironmentEntity.class)
                .withProxyConfigs(validProxy)
                .when(Environment::putAttachResources)
                .then(this::checkProxyAttachedToEnv)
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is an available environment with an attached rds config",
            when = "an rds detach request is sent for that environment and rds config",
            then = "the rds config should be detached from the environment")
    public void testCreateEnvDetachRds(TestContext testContext) {
        String env = getNameGenerator().getRandomNameForResource();
        createDefaultRdsConfig(testContext);
        Set<String> validRds = new HashSet<>();
        validRds.add(testContext.get(DatabaseEntity.class).getName());
        testContext
                .given(EnvironmentEntity.class)
                .withName(env)
                .when(Environment::post)
                .when(Environment::getAll)
                .given(EnvironmentEntity.class)
                .withRdsConfigs(validRds)
                .when(Environment::putAttachResources)
                .then(EnvironmentTestUtils::checkRdsAttachedToEnv)
                .when(Environment::putDetachResources)
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is an available environment with an attached ldap config",
            when = "an ldap detach request is sent for that environment and ldap config",
            then = "the ldap config should be detached from the environment")
    public void testCreateEnvDetachLdap(TestContext testContext) {
        String env = getNameGenerator().getRandomNameForResource();
        createDefaultLdapConfig(testContext);
        Set<String> validLdap = new HashSet<>();
        validLdap.add(testContext.get(LdapConfigTestDto.class).getName());
        testContext
                .given(EnvironmentEntity.class)
                .withName(env)
                .when(Environment::post)
                .when(Environment::getAll)
                .given(EnvironmentEntity.class)
                .withLdapConfigs(validLdap)
                .when(Environment::putAttachResources)
                .then(this::checkLdapAttachedToEnv)
                .when(Environment::putDetachResources)
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is an available environment with an attached proxy config",
            when = "an proxy detach request is sent for that environment and proxy config",
            then = "the proxy config should be detached from the environment")
    public void testCreateEnvDetachProxy(TestContext testContext) {
        String env = getNameGenerator().getRandomNameForResource();
        createDefaultProxyConfig(testContext);
        Set<String> validProxy = new HashSet<>();
        validProxy.add(testContext.get(ProxyConfigEntity.class).getName());
        testContext
                .given(EnvironmentEntity.class)
                .withName(env)
                .when(Environment::post)
                .when(Environment::getAll)
                .given(EnvironmentEntity.class)
                .withProxyConfigs(validProxy)
                .when(Environment::putAttachResources)
                .then(this::checkProxyAttachedToEnv)
                .when(Environment::putDetachResources)
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there are two available environments and an rds config",
            when = "an rds attach requests sent for each of the environments",
            then = "rds should be attached to both environments")
    public void testAttachRdsToMoreEnvs(TestContext testContext) {
        String env1 = getNameGenerator().getRandomNameForResource();
        String env2 = getNameGenerator().getRandomNameForResource();
        createDefaultRdsConfig(testContext);
        Set<String> validRds = new HashSet<>();
        validRds.add(testContext.get(DatabaseEntity.class).getName());
        attachRdsToEnv(testContext, env1, validRds);
        attachRdsToEnv(testContext, env2, validRds);
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there are two available environments and an ldap config",
            when = "an ldap attach requests sent for each of the environments",
            then = "ldap should be attached to both environments")
    public void testAttachLdapToMoreEnvs(TestContext testContext) {
        String env1 = getNameGenerator().getRandomNameForResource();
        String env2 = getNameGenerator().getRandomNameForResource();
        createDefaultLdapConfig(testContext);
        Set<String> validLdap = new HashSet<>();
        validLdap.add(testContext.get(LdapConfigTestDto.class).getName());
        attachLdapToEnv(testContext, env1, validLdap);
        attachLdapToEnv(testContext, env2, validLdap);
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there are two available environments and an proxy config",
            when = "an proxy attach requests sent for each of the environments",
            then = "proxy should be attached to both environments")
    public void testAttachProxyToMoreEnvs(TestContext testContext) {
        String env1 = getNameGenerator().getRandomNameForResource();
        String env2 = getNameGenerator().getRandomNameForResource();
        createDefaultProxyConfig(testContext);
        Set<String> validProxy = new HashSet<>();
        validProxy.add(testContext.get(ProxyConfigEntity.class).getName());
        attachProxyToEnv(testContext, env1, validProxy);
        attachProxyToEnv(testContext, env2, validProxy);
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is an rds config",
            when = "an rds config attach request is sent for a non-existing environment",
            then = "a ForbiddenException should be returned")
    public void testAttachRdsToNonExistingEnv(TestContext testContext) {
        String env = getNameGenerator().getRandomNameForResource();
        String forbiddenKey = getNameGenerator().getRandomNameForResource();
        createDefaultRdsConfig(testContext);
        Set<String> validRds = new HashSet<>();
        validRds.add(testContext.get(DatabaseEntity.class).getName());
        testContext
                .init(EnvironmentEntity.class)
                .withName(env)
                .withRdsConfigs(validRds)
                .when(Environment::putAttachResources, key(forbiddenKey))
                .expect(ForbiddenException.class, key(forbiddenKey))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there az environment with an attached rds config",
            when = "when an rds detach request is sent for that environment but with a different rds config name",
            then = "nothing should happen with the environment")
    public void testAttachAnRdsThenDetachAnotherOther(TestContext testContext) {
        String notExistingRds = getNameGenerator().getRandomNameForResource();
        String rds1 = getNameGenerator().getRandomNameForResource();
        createDefaultRdsConfig(testContext);
        Set<String> validRds = new HashSet<>();
        Set<String> notValidRds = new HashSet<>();
        validRds.add(testContext.get(DatabaseEntity.class).getName());
        notValidRds.add(notExistingRds);
        testContext
                .given(EnvironmentEntity.class)
                .withName(rds1)
                .when(Environment::post)
                .when(Environment::getAll)
                .given(EnvironmentEntity.class)
                .withRdsConfigs(validRds)
                .when(Environment::putAttachResources)
                .then(EnvironmentTestUtils::checkRdsAttachedToEnv)
                .given(EnvironmentEntity.class)
                .withName(rds1)
                .withRdsConfigs(notValidRds)
                .when(Environment::putDetachResources)
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is an available environment",
            when = "a modify credential request (with cred name) is sent for the environment",
            then = "the credential should change to the new credential in the environment")
    public void testCreateEnvironmentChangeCredWithCredName(TestContext testContext) {
        String cred1 = getNameGenerator().getRandomNameForResource();
        testContext
                .given(EnvironmentEntity.class)
                .when(Environment::post)
                .given(cred1, CredentialTestDto.class)
                .withName(cred1)
                .when(CredentialTestClient::create)
                .given(EnvironmentEntity.class)
                .withCredentialName(cred1)
                .then(Environment::changeCredential)
                .then(new CheckEnvironmentCredential(cred1))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is an available environment",
            when = "a modify credential request (with cred request) is sent for the environment",
            then = "the credential should change to the new credential in the environment")
    public void testCreateEnvironmentChangeCredWithCredRequest(TestContext testContext) {
        String cred1 = getNameGenerator().getRandomNameForResource();
        testContext
                .given(EnvironmentEntity.class)
                .when(Environment::post)
                .given(cred1, CredentialTestDto.class)
                .withName(cred1)
                .given(EnvironmentEntity.class)
                .withCredentialName(null)
                .withCredential(cred1)
                .then(Environment::changeCredential)
                .then(new CheckEnvironmentCredential(cred1))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is an available environment",
            when = "a modify credential request is sent for the environment with the same credential",
            then = "the credential should not change in the environment")
    public void testCreateEnvironmentChangeCredForSame(TestContext testContext) {
        testContext
                .given(EnvironmentEntity.class)
                .when(Environment::post)
                .withCredentialName(testContext.get(CredentialTestDto.class).getName())
                .then(Environment::changeCredential)
                .then(this::checkCredentialAttachedToEnv)
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is an available environment",
            when = "a modify credential request is sent for the environment with a non-existing credential",
            then = "a ForbiddenException should be returned")
    public void testCreateEnvironmentChangeCredNonExistingName(TestContext testContext) {
        String notExistingCred = getNameGenerator().getRandomNameForResource();
        String forbiddenKey = getNameGenerator().getRandomNameForResource();
        testContext
                .given(EnvironmentEntity.class)
                .when(Environment::post)
                .given(EnvironmentEntity.class)
                .withCredentialName(notExistingCred)
                .then(Environment::changeCredential, key(forbiddenKey))
                .expect(ForbiddenException.class, key(forbiddenKey))
                .validate();
    }

    private void attachRdsToEnv(TestContext testContext, String envName, Set<String> validRds) {
        testContext
                .given(envName, EnvironmentEntity.class)
                .withName(envName)
                .when(Environment::post)

                .given(envName, EnvironmentEntity.class)
                .withRdsConfigs(validRds)
                .when(Environment::putAttachResources)
                .then(EnvironmentTestUtils::checkRdsAttachedToEnv)
                .validate();
    }

    private void attachLdapToEnv(TestContext testContext, String envName, Set<String> validLdap) {
        testContext
                .given(envName, EnvironmentEntity.class)
                .withName(envName)
                .when(Environment::post)

                .given(envName, EnvironmentEntity.class)
                .withLdapConfigs(validLdap)
                .when(Environment::putAttachResources)
                .then(this::checkLdapAttachedToEnv)
                .validate();
    }

    private void attachProxyToEnv(TestContext testContext, String envName, Set<String> validLdap) {
        testContext
                .given(envName, EnvironmentEntity.class)
                .withName(envName)
                .when(Environment::post)

                .given(envName, EnvironmentEntity.class)
                .withProxyConfigs(validLdap)
                .when(Environment::putAttachResources)
                .then(this::checkProxyAttachedToEnv)
                .validate();
    }

    private EnvironmentEntity checkCredentialAttachedToEnv(TestContext testContext, EnvironmentEntity environment, CloudbreakClient cloudbreakClient) {
        String credentialName = environment.getResponse().getCredentialName();
        if (!credentialName.equals(testContext.get(CredentialTestDto.class).getName())) {
            throw new TestFailException("Credential is not attached to environment");
        }
        return environment;
    }

    private EnvironmentEntity checkLdapAttachedToEnv(TestContext testContext, EnvironmentEntity environment, CloudbreakClient cloudbreakClient) {
        Set<String> ldapConfigs = new HashSet<>();
        Set<LdapV4Response> ldapV4ResponseSet = environment.getResponse().getLdaps();
        for (LdapV4Response ldapV4Response : ldapV4ResponseSet) {
            ldapConfigs.add(ldapV4Response.getName());
        }
        if (!ldapConfigs.contains(testContext.get(LdapConfigTestDto.class).getName())) {
            throw new TestFailException("Ldap is not attached to environment");
        }
        return environment;
    }

    private EnvironmentEntity checkProxyAttachedToEnv(TestContext testContext, EnvironmentEntity environment, CloudbreakClient cloudbreakClient) {
        Set<String> proxyConfigs = new HashSet<>();
        Set<ProxyV4Response> proxyV4ResponseSet = environment.getResponse().getProxies();
        for (ProxyV4Response proxyV4Response : proxyV4ResponseSet) {
            proxyConfigs.add(proxyV4Response.getName());
        }
        if (!proxyConfigs.contains(testContext.get(ProxyConfigEntity.class).getName())) {
            throw new TestFailException("Proxy is not attached to environment");
        }
        return environment;
    }

    private EnvironmentEntity checkEnvIsListed(TestContext testContext, EnvironmentEntity environment, CloudbreakClient cloudbreakClient) {
        Collection<SimpleEnvironmentV4Response> simpleEnvironmentV4Respons = environment.getResponseSimpleEnvSet();
        List<SimpleEnvironmentV4Response> result = simpleEnvironmentV4Respons.stream()
                .filter(env -> environment.getName().equals(env.getName()))
                .collect(Collectors.toList());
        if (result.isEmpty()) {
            throw new TestFailException("Environment is not listed");
        }
        return environment;
    }
}