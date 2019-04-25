package com.sequenceiq.it.cloudbreak.newway.testcase.mock;

import static com.sequenceiq.it.cloudbreak.newway.context.RunningParameter.key;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.ForbiddenException;

import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.environment.responses.SimpleEnvironmentV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.ldaps.responses.LdapV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.proxies.responses.ProxyV4Response;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.assertion.environment.EnvironmentTestAssertion;
import com.sequenceiq.it.cloudbreak.newway.client.CredentialTestClient;
import com.sequenceiq.it.cloudbreak.newway.client.EnvironmentTestClient;
import com.sequenceiq.it.cloudbreak.newway.context.Description;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.dto.credential.CredentialTestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.database.DatabaseTestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.ldap.LdapTestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.proxy.ProxyTestDto;
import com.sequenceiq.it.cloudbreak.newway.testcase.AbstractIntegrationTest;
import com.sequenceiq.it.cloudbreak.newway.util.EnvironmentTestUtils;

public class EnvironmentTest extends AbstractIntegrationTest {

    private static final Set<String> INVALID_REGION = new HashSet<>(Collections.singletonList("MockRegion"));

    private static final Set<String> INVALID_PROXY = new HashSet<>(Collections.singletonList("InvalidProxy"));

    private static final Set<String> INVALID_LDAP = new HashSet<>(Collections.singletonList("InvalidLdap"));

    private static final Set<String> INVALID_RDS = new HashSet<>(Collections.singletonList("InvalidRds"));

    private Set<String> mixedProxy = new HashSet<>();

    private Set<String> mixedLdap = new HashSet<>();

    private Set<String> mixedRds = new HashSet<>();

    @Inject
    private CredentialTestClient credentialTestClient;

    @Inject
    private EnvironmentTestClient environmentTestClient;

    @Override
    protected void setupTest(TestContext testContext) {
        createDefaultUser(testContext);
        createDefaultCredential(testContext);
        initializeDefaultBlueprints(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running cloudbreak",
            when = "valid create environment request is sent",
            then = "environment should be created")
    public void testCreateEnvironment(TestContext testContext) {
        testContext
                .given(EnvironmentTestDto.class)
                .when(environmentTestClient.createV4())
                .then(this::checkCredentialAttachedToEnv)
                .when(environmentTestClient.listV4())
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
        validProxy.add(testContext.get(ProxyTestDto.class).getName());
        testContext
                .given(EnvironmentTestDto.class)
                .withProxyConfigs(validProxy)
                .when(environmentTestClient.createV4())
                .then(this::checkCredentialAttachedToEnv)
                .when(environmentTestClient.listV4())
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
        validLdap.add(testContext.get(LdapTestDto.class).getName());
        testContext
                .given(EnvironmentTestDto.class)
                .withLdapConfigs(validLdap)
                .when(environmentTestClient.createV4())
                .then(this::checkCredentialAttachedToEnv)
                .when(environmentTestClient.listV4())
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
        validRds.add(testContext.get(DatabaseTestDto.class).getName());
        testContext
                .given(EnvironmentTestDto.class)
                .withRdsConfigs(validRds)
                .when(environmentTestClient.createV4())
                .then(this::checkCredentialAttachedToEnv)
                .when(environmentTestClient.listV4())
                .then(this::checkEnvIsListed)
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running cloudbreak",
            when = "a create environment request is sent with an invalid region in it",
            then = "a BadRequestException should be returned")
    public void testCreateEnvironmentInvalidRegion(TestContext testContext) {
        String forbiddenKey = resourcePropertyProvider().getName();
        testContext
                .init(EnvironmentTestDto.class)
                .withRegions(INVALID_REGION)
                .when(environmentTestClient.createV4(), key(forbiddenKey))
                .expect(BadRequestException.class, key(forbiddenKey))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running cloudbreak",
            when = "a create environment request is sent with no region in it",
            then = "a BadRequestException should be returned")
    public void testCreateEnvironmentNoRegion(TestContext testContext) {
        String forbiddenKey = resourcePropertyProvider().getName();
        testContext
                .init(EnvironmentTestDto.class)
                .withRegions(null)
                .when(environmentTestClient.createV4(), key(forbiddenKey))
                .expect(BadRequestException.class, key(forbiddenKey))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running cloudbreak",
            when = "a create environment request with reference to a non-existing credential is sent",
            then = "a BadRequestException should be returned")
    public void testCreateEnvironmentNotExistCredential(TestContext testContext) {
        String forbiddenKey = resourcePropertyProvider().getName();
        testContext
                .init(EnvironmentTestDto.class)
                .withCredentialName("notexistingcredendital")
                .when(environmentTestClient.createV4(), key(forbiddenKey))
                .expect(BadRequestException.class, key(forbiddenKey))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running cloudbreak",
            when = "a create environment request with reference to a non-existing proxy is sent",
            then = "a BadRequestException should be returned")
    public void testCreateEnvironmentNotExistProxy(TestContext testContext) {
        String forbiddenKey = resourcePropertyProvider().getName();
        testContext
                .init(EnvironmentTestDto.class)
                .withProxyConfigs(INVALID_PROXY)
                .when(environmentTestClient.createV4(), key(forbiddenKey))
                .expect(BadRequestException.class, key(forbiddenKey))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running cloudbreak",
            when = "a create environment request with reference to a non-existing ldap is sent",
            then = "a BadRequestException should be returned")
    public void testCreateEnvironmentNotExistLdap(TestContext testContext) {
        String forbiddenKey = resourcePropertyProvider().getName();
        testContext
                .init(EnvironmentTestDto.class)
                .withLdapConfigs(INVALID_LDAP)
                .when(environmentTestClient.createV4(), key(forbiddenKey))
                .expect(BadRequestException.class, key(forbiddenKey))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running cloudbreak",
            when = "a create environment request with reference to a non-existing ldap is sent",
            then = "a BadRequestException should be returned")
    public void testCreateEnvironmentNotExistRds(TestContext testContext) {
        String forbiddenKey = resourcePropertyProvider().getName();
        testContext
                .init(EnvironmentTestDto.class)
                .withRdsConfigs(INVALID_RDS)
                .when(environmentTestClient.createV4(), key(forbiddenKey))
                .expect(BadRequestException.class, key(forbiddenKey))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running cloudbreak",
            when = "a create environment request with reference to an existing and a non-existing proxy is sent",
            then = "a BadRequestException should be returned")
    public void testCreateEnvWithExistingAndNotExistingProxy(TestContext testContext) {
        String forbiddenKey = resourcePropertyProvider().getName();
        createDefaultProxyConfig(testContext);
        mixedProxy.add(testContext.get(ProxyTestDto.class).getName());
        mixedProxy.add("invalidProxy");
        testContext
                .init(EnvironmentTestDto.class)
                .withProxyConfigs(mixedProxy)
                .when(environmentTestClient.createV4(), key(forbiddenKey))
                .expect(BadRequestException.class, key(forbiddenKey))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running cloudbreak",
            when = "a create environment request with reference to an existing and a non-existing database is sent",
            then = "a BadRequestException should be returned")
    public void testCreateEnvWithExistingAndNotExistingRds(TestContext testContext) {
        String forbiddenKey = resourcePropertyProvider().getName();
        createDefaultRdsConfig(testContext);
        mixedRds.add(testContext.get(DatabaseTestDto.class).getName());
        mixedRds.add("invalidRds");
        testContext
                .init(EnvironmentTestDto.class)
                .withRdsConfigs(mixedRds)
                .when(environmentTestClient.createV4(), key(forbiddenKey))
                .expect(BadRequestException.class, key(forbiddenKey))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running cloudbreak",
            when = "a create environment request with reference to an existing and a non-existing ldap is sent",
            then = "a BadRequestException should be returned")
    public void testCreateEnvWithExistingAndNotExistingLdap(TestContext testContext) {
        String forbiddenKey = resourcePropertyProvider().getName();
        createDefaultLdapConfig(testContext);
        mixedLdap.add(testContext.get(LdapTestDto.class).getName());
        mixedLdap.add("invalidLdap");
        testContext
                .init(EnvironmentTestDto.class)
                .withLdapConfigs(mixedLdap)
                .when(environmentTestClient.createV4(), key(forbiddenKey))
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
                .init(EnvironmentTestDto.class)
                .when(environmentTestClient.createV4())
                .then(this::checkCredentialAttachedToEnv)
                .when(environmentTestClient.listV4())
                .then(this::checkEnvIsListed)
                .when(environmentTestClient.deleteV4())
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
        validProxy.add(testContext.get(ProxyTestDto.class).getName());
        testContext
                .init(EnvironmentTestDto.class)
                .withProxyConfigs(validProxy)
                .when(environmentTestClient.createV4())
                .then(this::checkCredentialAttachedToEnv)
                .when(environmentTestClient.listV4())
                .then(this::checkEnvIsListed)
                .when(environmentTestClient.deleteV4())
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
        validLdap.add(testContext.get(LdapTestDto.class).getName());
        testContext
                .init(EnvironmentTestDto.class)
                .withLdapConfigs(validLdap)
                .when(environmentTestClient.createV4())
                .then(this::checkCredentialAttachedToEnv)
                .when(environmentTestClient.listV4())
                .then(this::checkEnvIsListed)
                .when(environmentTestClient.deleteV4())
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
        validRds.add(testContext.get(DatabaseTestDto.class).getName());
        testContext
                .init(EnvironmentTestDto.class)
                .withRdsConfigs(validRds)
                .when(environmentTestClient.createV4())
                .then(this::checkCredentialAttachedToEnv)
                .when(environmentTestClient.listV4())
                .then(this::checkEnvIsListed)
                .when(environmentTestClient.deleteV4())
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running cloudbreak",
            when = "a delete request is sent for a non-existing environment",
            then = "a ForbiddenException should be returned")
    public void testDeleteEnvironmentNotExist(TestContext testContext) {
        String forbiddenKey = resourcePropertyProvider().getName();
        testContext
                .init(EnvironmentTestDto.class)
                .when(environmentTestClient.deleteV4(), key(forbiddenKey))
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
        validRds.add(testContext.get(DatabaseTestDto.class).getName());
        testContext
                .given(EnvironmentTestDto.class)
                .when(environmentTestClient.createV4())
                .when(environmentTestClient.listV4())
                .given(EnvironmentTestDto.class)
                .withRdsConfigs(validRds)
                .when(environmentTestClient.attachV4())
                .then(EnvironmentTestUtils::checkRdsAttachedToEnv)
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is an available environment",
            when = "an ldap attach request is sent for that environment",
            then = "the ldap should be attached to the environment")
    public void testCreateEnvAttachLdap(TestContext testContext) {
        String env = resourcePropertyProvider().getName();
        createDefaultLdapConfig(testContext);
        Set<String> validLdap = new HashSet<>();
        validLdap.add(testContext.get(LdapTestDto.class).getName());
        testContext
                .given(EnvironmentTestDto.class)
                .withName(env)
                .when(environmentTestClient.createV4())
                .given(EnvironmentTestDto.class)
                .withLdapConfigs(validLdap)
                .when(environmentTestClient.attachV4())
                .then(this::checkLdapAttachedToEnv)
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is an available environment",
            when = "an proxy attach request is sent for that environment",
            then = "the proxy should be attached to the environment")
    public void testCreateEnvAttachProxy(TestContext testContext) {
        String env = resourcePropertyProvider().getName();
        createDefaultProxyConfig(testContext);
        Set<String> validProxy = new HashSet<>();
        validProxy.add(testContext.get(ProxyTestDto.class).getName());
        testContext
                .given(EnvironmentTestDto.class)
                .withName(env)
                .when(environmentTestClient.createV4())
                .given(EnvironmentTestDto.class)
                .withProxyConfigs(validProxy)
                .when(environmentTestClient.attachV4())
                .then(this::checkProxyAttachedToEnv)
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is an available environment with an attached rds config",
            when = "an rds detach request is sent for that environment and rds config",
            then = "the rds config should be detached from the environment")
    public void testCreateEnvDetachRds(TestContext testContext) {
        String env = resourcePropertyProvider().getName();
        createDefaultRdsConfig(testContext);
        Set<String> validRds = new HashSet<>();
        validRds.add(testContext.get(DatabaseTestDto.class).getName());
        testContext
                .given(EnvironmentTestDto.class)
                .withName(env)
                .when(environmentTestClient.createV4())
                .when(environmentTestClient.listV4())
                .given(EnvironmentTestDto.class)
                .withRdsConfigs(validRds)
                .when(environmentTestClient.attachV4())
                .then(EnvironmentTestUtils::checkRdsAttachedToEnv)
                .when(environmentTestClient.detachV4())
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is an available environment with an attached ldap config",
            when = "an ldap detach request is sent for that environment and ldap config",
            then = "the ldap config should be detached from the environment")
    public void testCreateEnvDetachLdap(TestContext testContext) {
        String env = resourcePropertyProvider().getName();
        createDefaultLdapConfig(testContext);
        Set<String> validLdap = new HashSet<>();
        validLdap.add(testContext.get(LdapTestDto.class).getName());
        testContext
                .given(EnvironmentTestDto.class)
                .withName(env)
                .when(environmentTestClient.createV4())
                .when(environmentTestClient.listV4())
                .given(EnvironmentTestDto.class)
                .withLdapConfigs(validLdap)
                .when(environmentTestClient.attachV4())
                .then(this::checkLdapAttachedToEnv)
                .when(environmentTestClient.detachV4())
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is an available environment with an attached proxy config",
            when = "an proxy detach request is sent for that environment and proxy config",
            then = "the proxy config should be detached from the environment")
    public void testCreateEnvDetachProxy(TestContext testContext) {
        String env = resourcePropertyProvider().getName();
        createDefaultProxyConfig(testContext);
        Set<String> validProxy = new HashSet<>();
        validProxy.add(testContext.get(ProxyTestDto.class).getName());
        testContext
                .given(EnvironmentTestDto.class)
                .withName(env)
                .when(environmentTestClient.createV4())
                .when(environmentTestClient.listV4())
                .given(EnvironmentTestDto.class)
                .withProxyConfigs(validProxy)
                .when(environmentTestClient.attachV4())
                .then(this::checkProxyAttachedToEnv)
                .when(environmentTestClient.detachV4())
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there are two available environments and an rds config",
            when = "an rds attach requests sent for each of the environments",
            then = "rds should be attached to both environments")
    public void testAttachRdsToMoreEnvs(TestContext testContext) {
        String env1 = resourcePropertyProvider().getName();
        String env2 = resourcePropertyProvider().getName();
        createDefaultRdsConfig(testContext);
        Set<String> validRds = new HashSet<>();
        validRds.add(testContext.get(DatabaseTestDto.class).getName());
        attachRdsToEnv(testContext, env1, validRds);
        attachRdsToEnv(testContext, env2, validRds);
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there are two available environments and an ldap config",
            when = "an ldap attach requests sent for each of the environments",
            then = "ldap should be attached to both environments")
    public void testAttachLdapToMoreEnvs(TestContext testContext) {
        String env1 = resourcePropertyProvider().getName();
        String env2 = resourcePropertyProvider().getName();
        createDefaultLdapConfig(testContext);
        Set<String> validLdap = new HashSet<>();
        validLdap.add(testContext.get(LdapTestDto.class).getName());
        attachLdapToEnv(testContext, env1, validLdap);
        attachLdapToEnv(testContext, env2, validLdap);
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there are two available environments and an proxy config",
            when = "an proxy attach requests sent for each of the environments",
            then = "proxy should be attached to both environments")
    public void testAttachProxyToMoreEnvs(TestContext testContext) {
        String env1 = resourcePropertyProvider().getName();
        String env2 = resourcePropertyProvider().getName();
        createDefaultProxyConfig(testContext);
        Set<String> validProxy = new HashSet<>();
        validProxy.add(testContext.get(ProxyTestDto.class).getName());
        attachProxyToEnv(testContext, env1, validProxy);
        attachProxyToEnv(testContext, env2, validProxy);
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is an rds config",
            when = "an rds config attach request is sent for a non-existing environment",
            then = "a ForbiddenException should be returned")
    public void testAttachRdsToNonExistingEnv(TestContext testContext) {
        String env = resourcePropertyProvider().getName();
        String forbiddenKey = resourcePropertyProvider().getName();
        createDefaultRdsConfig(testContext);
        Set<String> validRds = new HashSet<>();
        validRds.add(testContext.get(DatabaseTestDto.class).getName());
        testContext
                .init(EnvironmentTestDto.class)
                .withName(env)
                .withRdsConfigs(validRds)
                .when(environmentTestClient.attachV4(), key(forbiddenKey))
                .expect(ForbiddenException.class, key(forbiddenKey))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there az environment with an attached rds config",
            when = "when an rds detach request is sent for that environment but with a different rds config name",
            then = "nothing should happen with the environment")
    public void testAttachAnRdsThenDetachAnotherOther(TestContext testContext) {
        String notExistingRds = resourcePropertyProvider().getName();
        String rds1 = resourcePropertyProvider().getName();
        createDefaultRdsConfig(testContext);
        Set<String> validRds = new HashSet<>();
        Set<String> notValidRds = new HashSet<>();
        validRds.add(testContext.get(DatabaseTestDto.class).getName());
        notValidRds.add(notExistingRds);
        testContext
                .given(EnvironmentTestDto.class)
                .withName(rds1)
                .when(environmentTestClient.createV4())
                .when(environmentTestClient.listV4())
                .given(EnvironmentTestDto.class)
                .withRdsConfigs(validRds)
                .when(environmentTestClient.attachV4())
                .then(EnvironmentTestUtils::checkRdsAttachedToEnv)
                .given(EnvironmentTestDto.class)
                .withName(rds1)
                .withRdsConfigs(notValidRds)
                .when(environmentTestClient.detachV4())
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is an available environment",
            when = "a modifyV4 credential request (with cred name) is sent for the environment",
            then = "the credential should change to the new credential in the environment")
    public void testCreateEnvironmentChangeCredWithCredName(TestContext testContext) {
        String cred1 = resourcePropertyProvider().getName();
        testContext
                .given(EnvironmentTestDto.class)
                .when(environmentTestClient.createV4())
                .given(cred1, CredentialTestDto.class)
                .withName(cred1)
                .when(credentialTestClient.createV4())
                .given(EnvironmentTestDto.class)
                .withCredentialName(cred1)
                .when(environmentTestClient.changeCredential())
                .then(new EnvironmentTestAssertion(cred1))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is an available environment",
            when = "a modifyV4 credential request (with cred request) is sent for the environment",
            then = "the credential should change to the new credential in the environment")
    public void testCreateEnvironmentChangeCredWithCredRequest(TestContext testContext) {
        String cred1 = resourcePropertyProvider().getName();
        testContext
                .given(EnvironmentTestDto.class)
                .when(environmentTestClient.createV4())
                .given(cred1, CredentialTestDto.class)
                .withName(cred1)
                .given(EnvironmentTestDto.class)
                .withCredentialName(null)
                .withCredential(cred1)
                .when(environmentTestClient.changeCredential())
                .then(new EnvironmentTestAssertion(cred1))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is an available environment",
            when = "a modifyV4 credential request is sent for the environment with the same credential",
            then = "the credential should not change in the environment")
    public void testCreateEnvironmentChangeCredForSame(TestContext testContext) {
        testContext
                .given(EnvironmentTestDto.class)
                .when(environmentTestClient.createV4())
                .withCredentialName(testContext.get(CredentialTestDto.class).getName())
                .when(environmentTestClient.changeCredential())
                .then(this::checkCredentialAttachedToEnv)
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is an available environment",
            when = "a modifyV4 credential request is sent for the environment with a non-existing credential",
            then = "a ForbiddenException should be returned")
    public void testCreateEnvironmentChangeCredNonExistingName(TestContext testContext) {
        String notExistingCred = resourcePropertyProvider().getName();
        String forbiddenKey = resourcePropertyProvider().getName();
        testContext
                .given(EnvironmentTestDto.class)
                .when(environmentTestClient.createV4())
                .given(EnvironmentTestDto.class)
                .withCredentialName(notExistingCred)
                .when(environmentTestClient.changeCredential(), key(forbiddenKey))
                .expect(ForbiddenException.class, key(forbiddenKey))
                .validate();
    }

    private void attachRdsToEnv(TestContext testContext, String envName, Set<String> validRds) {
        testContext
                .given(envName, EnvironmentTestDto.class)
                .withName(envName)
                .when(environmentTestClient.createV4())

                .given(envName, EnvironmentTestDto.class)
                .withRdsConfigs(validRds)
                .when(environmentTestClient.attachV4())
                .then(EnvironmentTestUtils::checkRdsAttachedToEnv)
                .validate();
    }

    private void attachLdapToEnv(TestContext testContext, String envName, Set<String> validLdap) {
        testContext
                .given(envName, EnvironmentTestDto.class)
                .withName(envName)
                .when(environmentTestClient.createV4())

                .given(envName, EnvironmentTestDto.class)
                .withLdapConfigs(validLdap)
                .when(environmentTestClient.attachV4())
                .then(this::checkLdapAttachedToEnv)
                .validate();
    }

    private void attachProxyToEnv(TestContext testContext, String envName, Set<String> validLdap) {
        testContext
                .given(envName, EnvironmentTestDto.class)
                .withName(envName)
                .when(environmentTestClient.createV4())

                .given(envName, EnvironmentTestDto.class)
                .withProxyConfigs(validLdap)
                .when(environmentTestClient.attachV4())
                .then(this::checkProxyAttachedToEnv)
                .validate();
    }

    private EnvironmentTestDto checkCredentialAttachedToEnv(TestContext testContext, EnvironmentTestDto environment, CloudbreakClient cloudbreakClient) {
        String credentialName = environment.getResponse().getCredentialName();
        if (!credentialName.equals(testContext.get(CredentialTestDto.class).getName())) {
            throw new TestFailException("Credential is not attached to environment");
        }
        return environment;
    }

    private EnvironmentTestDto checkLdapAttachedToEnv(TestContext testContext, EnvironmentTestDto environment, CloudbreakClient cloudbreakClient) {
        Set<String> ldapConfigs = new HashSet<>();
        Set<LdapV4Response> ldapV4ResponseSet = environment.getResponse().getLdaps();
        for (LdapV4Response ldapV4Response : ldapV4ResponseSet) {
            ldapConfigs.add(ldapV4Response.getName());
        }
        if (!ldapConfigs.contains(testContext.get(LdapTestDto.class).getName())) {
            throw new TestFailException("Ldap is not attached to environment");
        }
        return environment;
    }

    private EnvironmentTestDto checkProxyAttachedToEnv(TestContext testContext, EnvironmentTestDto environment, CloudbreakClient cloudbreakClient) {
        Set<String> proxyConfigs = new HashSet<>();
        Set<ProxyV4Response> proxyV4ResponseSet = environment.getResponse().getProxies();
        for (ProxyV4Response proxyV4Response : proxyV4ResponseSet) {
            proxyConfigs.add(proxyV4Response.getName());
        }
        if (!proxyConfigs.contains(testContext.get(ProxyTestDto.class).getName())) {
            throw new TestFailException("Proxy is not attached to environment");
        }
        return environment;
    }

    private EnvironmentTestDto checkEnvIsListed(TestContext testContext, EnvironmentTestDto environment, CloudbreakClient cloudbreakClient) {
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