package com.sequenceiq.it.cloudbreak.newway.testcase;

import static com.sequenceiq.it.cloudbreak.newway.context.RunningParameter.key;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.ws.rs.BadRequestException;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.model.environment.response.SimpleEnvironmentResponse;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.CredentialEntity;
import com.sequenceiq.it.cloudbreak.newway.Environment;
import com.sequenceiq.it.cloudbreak.newway.EnvironmentEntity;
import com.sequenceiq.it.cloudbreak.newway.LdapConfigEntity;
import com.sequenceiq.it.cloudbreak.newway.ProxyConfigEntity;
import com.sequenceiq.it.cloudbreak.newway.RdsConfigEntity;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;

public class EnvironmentTest extends AbstractIntegrationTest  {
    private static final String FORBIDDEN_KEY = "forbiddenPost";

    private static final Set<String> VALID_REGION = new HashSet<>(Collections.singletonList("Europe"));

    private static final String VALID_LOCATION = "London";

    private static final Set<String> INVALID_REGION = new HashSet<>(Collections.singletonList("MockRegion"));

    private static final Set<String> INVALID_PROXY = new HashSet<>(Collections.singletonList("InvalidProxy"));

    private static final Set<String> INVALID_LDAP = new HashSet<>(Collections.singletonList("InvalidLdap"));

    private static final Set<String> INVALID_RDS = new HashSet<>(Collections.singletonList("InvalidRds"));

    private Set<String> validProxy = new HashSet<>();

    private Set<String> validLdap = new HashSet<>();

    private Set<String> validRds = new HashSet<>();

    private Set<String> mixedProxy = new HashSet<>();

    private Set<String> mixedLdap = new HashSet<>();

    private Set<String> mixedRds = new HashSet<>();

    @BeforeMethod
    public void beforeMethod(Object[] data) {
        TestContext testContext = (TestContext) data[0];
        createDefaultUser(testContext);
        createDefaultCredential(testContext);
        createDefaultImageCatalog(testContext);
    }

    @AfterMethod(alwaysRun = true)
    public void tear(Object[] data) {
        ((TestContext) data[0]).cleanupTestContextEntity();
    }

    @Test(dataProvider = "testContext")
    public void testCreateEnvironment(TestContext testContext) {
        testContext
                .given(EnvironmentEntity.class)
                .withRegions(VALID_REGION)
                .withLocation(VALID_LOCATION)
                .when(Environment::post)
                .then(EnvironmentTest::checkCredentialAttachedToEnv)
                .when(Environment::getAll)
                .then(EnvironmentTest::checkEnvIsListed)
                .validate();
    }

    @Test(dataProvider = "testContext")
    public void testCreateEnvironmenWithProxy(TestContext testContext) {
        createDefaultProxyConfig(testContext);
        validProxy.add(testContext.get(ProxyConfigEntity.class).getName());
        testContext
                .given(EnvironmentEntity.class)
                .withRegions(VALID_REGION)
                .withLocation(VALID_LOCATION)
                .withProxyConfigs(validProxy)
                .when(Environment::post)
                .then(EnvironmentTest::checkCredentialAttachedToEnv)
                .when(Environment::getAll)
                .then(EnvironmentTest::checkEnvIsListed)
                .validate();
    }

    @Test(dataProvider = "testContext")
    public void testCreateEnvironmenWithLdap(TestContext testContext) {
        createDefaultLdapConfig(testContext);
        validLdap.add(testContext.get(LdapConfigEntity.class).getName());
        testContext
                .given(EnvironmentEntity.class)
                .withRegions(VALID_REGION)
                .withLocation(VALID_LOCATION)
                .withLdapConfigs(validLdap)
                .when(Environment::post)
                .then(EnvironmentTest::checkCredentialAttachedToEnv)
                .when(Environment::getAll)
                .then(EnvironmentTest::checkEnvIsListed)
                .validate();
    }

    @Test(dataProvider = "testContext")
    public void testCreateEnvironmenWithRds(TestContext testContext) {
        createDefaultRdsConfig(testContext);
        validRds.add(testContext.get(RdsConfigEntity.class).getName());
        testContext
                .given(EnvironmentEntity.class)
                .withRegions(VALID_REGION)
                .withLocation(VALID_LOCATION)
                .withRdsConfigs(validRds)
                .when(Environment::post)
                .then(EnvironmentTest::checkCredentialAttachedToEnv)
                .when(Environment::getAll)
                .then(EnvironmentTest::checkEnvIsListed)
                .validate();
    }

    @Test(dataProvider = "testContext")
    public void testCreateEnvironmentInvalidRegion(TestContext testContext) {
        testContext
                .given(EnvironmentEntity.class)
                .withRegions(INVALID_REGION)
                .withLocation(VALID_LOCATION)
                .when(Environment::post, key(FORBIDDEN_KEY))
                .except(BadRequestException.class, key(FORBIDDEN_KEY))
                .validate();
    }

    // new
    @Test(dataProvider = "testContext")
    public void testCreateEnvironmentNoRegion(TestContext testContext) {
        testContext
                .given(EnvironmentEntity.class)
                .when(Environment::post, key(FORBIDDEN_KEY))
                .except(BadRequestException.class, key(FORBIDDEN_KEY))
                .validate();
    }

    @Test(dataProvider = "testContext")
    public void testCreateEnvironmentNotExistCredential(TestContext testContext) {
        testContext
                .given(EnvironmentEntity.class)
                .withCredentialName("notexistingcredendital")
                .withRegions(VALID_REGION)
                .withLocation(VALID_LOCATION)
                .when(Environment::post, key(FORBIDDEN_KEY))
                .except(BadRequestException.class, key(FORBIDDEN_KEY))
                .validate();
    }

    @Test(dataProvider = "testContext")
    public void testCreateEnvironmentNotExistProxy(TestContext testContext) {
        testContext
                .given(EnvironmentEntity.class)
                .withRegions(VALID_REGION)
                .withLocation(VALID_LOCATION)
                .withProxyConfigs(INVALID_PROXY)
                .when(Environment::post, key(FORBIDDEN_KEY))
                .except(BadRequestException.class, key(FORBIDDEN_KEY))
                .validate();
    }

    @Test(dataProvider = "testContext")
    public void testCreateEnvironmentNotExistLdap(TestContext testContext) {
        testContext
                .given(EnvironmentEntity.class)
                .withRegions(VALID_REGION)
                .withLocation(VALID_LOCATION)
                .withLdapConfigs(INVALID_LDAP)
                .when(Environment::post, key(FORBIDDEN_KEY))
                .except(BadRequestException.class, key(FORBIDDEN_KEY))
                .validate();
    }

    @Test(dataProvider = "testContext")
    public void testCreateEnvironmentNotExistRds(TestContext testContext) {
        testContext
                .given(EnvironmentEntity.class)
                .withRegions(VALID_REGION)
                .withLocation(VALID_LOCATION)
                .withRdsConfigs(INVALID_RDS)
                .when(Environment::post, key(FORBIDDEN_KEY))
                .except(BadRequestException.class, key(FORBIDDEN_KEY))
                .validate();
    }

    @Test(dataProvider = "testContext")
    public void testCreateEnvWithExistingAndNotExistingRds(TestContext testContext) {
        createDefaultRdsConfig(testContext);
        mixedRds.add(testContext.get(RdsConfigEntity.class).getName());
        mixedRds.add("invalidRds");
        testContext
                .given(EnvironmentEntity.class)
                .withRegions(VALID_REGION)
                .withLocation(VALID_LOCATION)
                .withRdsConfigs(mixedRds)
                .when(Environment::post, key(FORBIDDEN_KEY))
                .except(BadRequestException.class, key(FORBIDDEN_KEY))
                .validate();
    }

    @Test(dataProvider = "testContext")
    public void testCreateEnvWithExistingAndNotExistingProxy(TestContext testContext) {
        createDefaultProxyConfig(testContext);
        mixedProxy.add(testContext.get(ProxyConfigEntity.class).getName());
        mixedProxy.add("invalidProxy");
        testContext
                .given(EnvironmentEntity.class)
                .withRegions(VALID_REGION)
                .withLocation(VALID_LOCATION)
                .withProxyConfigs(mixedProxy)
                .when(Environment::post, key(FORBIDDEN_KEY))
                .except(BadRequestException.class, key(FORBIDDEN_KEY))
                .validate();
    }

    @Test(dataProvider = "testContext")
    public void testCreateEnvWithExistingAndNotExistingLdap(TestContext testContext) {
        createDefaultLdapConfig(testContext);
        mixedLdap.add(testContext.get(LdapConfigEntity.class).getName());
        mixedLdap.add("invalidLdap");
        testContext
                .given(EnvironmentEntity.class)
                .withRegions(VALID_REGION)
                .withLocation(VALID_LOCATION)
                .withLdapConfigs(mixedLdap)
                .when(Environment::post, key(FORBIDDEN_KEY))
                .except(BadRequestException.class, key(FORBIDDEN_KEY))
                .validate();
    }

    private static EnvironmentEntity checkCredentialAttachedToEnv(TestContext testContext, EnvironmentEntity environment, CloudbreakClient cloudbreakClient) {
        String credentialName = environment.getResponse().getCredentialName();
        if (!credentialName.equals(testContext.get(CredentialEntity.class).getName())) {
            throw new TestFailException("Credential is not attached to environment");
        }
        return environment;
    }

    private static EnvironmentEntity checkEnvIsListed(TestContext testContext, EnvironmentEntity environment, CloudbreakClient cloudbreakClient) {
        Set<SimpleEnvironmentResponse> simpleEnvironmentResponses = testContext.get(EnvironmentEntity.class).getResponseSimpleEnv();
        List<SimpleEnvironmentResponse> result = simpleEnvironmentResponses.stream()
                .filter(env -> environment.getName().equals(env.getName()))
                .collect(Collectors.toList());
        if (result.isEmpty()) {
            throw new TestFailException("Environment is not listed");
        }
        return environment;
    }
}