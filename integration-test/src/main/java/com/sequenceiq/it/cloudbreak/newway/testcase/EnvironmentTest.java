package com.sequenceiq.it.cloudbreak.newway.testcase;

import static com.sequenceiq.it.cloudbreak.newway.context.RunningParameter.key;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.ForbiddenException;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.model.CredentialRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.environment.responses.SimpleEnvironmentV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.ldaps.responses.LdapV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.responses.DatabaseV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.proxies.responses.ProxyV4Response;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.CredentialEntity;
import com.sequenceiq.it.cloudbreak.newway.Environment;
import com.sequenceiq.it.cloudbreak.newway.EnvironmentEntity;
import com.sequenceiq.it.cloudbreak.newway.LdapConfigEntity;
import com.sequenceiq.it.cloudbreak.newway.ProxyConfigEntity;
import com.sequenceiq.it.cloudbreak.newway.RdsConfigEntity;
import com.sequenceiq.it.cloudbreak.newway.action.CredentialCreateAction;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;

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
        TestContext testContext = (TestContext) data[0];
        createDefaultUser(testContext);
        createDefaultCredential(testContext);
        createDefaultImageCatalog(testContext);
        initializeDefaultBlueprints(testContext);
    }

    @AfterMethod(alwaysRun = true)
    public void tear(Object[] data) {
        ((TestContext) data[0]).cleanupTestContextEntity();
    }

    @Test(dataProvider = "testContext")
    public void testCreateEnvironment(TestContext testContext) {
        testContext
                .given(EnvironmentEntity.class)
                .when(Environment::post)
                .then(EnvironmentTest::checkCredentialAttachedToEnv)
                .when(Environment::getAll)
                .then(EnvironmentTest::checkEnvIsListed)
                .validate();
    }

    @Test(dataProvider = "testContext")
    public void testCreateEnvironmenWithProxy(TestContext testContext) {
        createDefaultProxyConfig(testContext);
        Set<String> validProxy = new HashSet<>();
        validProxy.add(testContext.get(ProxyConfigEntity.class).getName());
        testContext
                .given(EnvironmentEntity.class)
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
        Set<String> validLdap = new HashSet<>();
        validLdap.add(testContext.get(LdapConfigEntity.class).getName());
        testContext
                .given(EnvironmentEntity.class)
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
        Set<String> validRds = new HashSet<>();
        validRds.add(testContext.get(RdsConfigEntity.class).getName());
        testContext
                .given(EnvironmentEntity.class)
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
                .when(Environment::post, key(FORBIDDEN_KEY))
                .except(BadRequestException.class, key(FORBIDDEN_KEY))
                .validate();
    }

    @Test(dataProvider = "testContext")
    public void testCreateEnvironmentNoRegion(TestContext testContext) {
        testContext
                .given(EnvironmentEntity.class)
                .withRegions(null)
                .when(Environment::post, key(FORBIDDEN_KEY))
                .except(BadRequestException.class, key(FORBIDDEN_KEY))
                .validate();
    }

    @Test(dataProvider = "testContext")
    public void testCreateEnvironmentNotExistCredential(TestContext testContext) {
        testContext
                .given(EnvironmentEntity.class)
                .withCredentialName("notexistingcredendital")
                .when(Environment::post, key(FORBIDDEN_KEY))
                .except(BadRequestException.class, key(FORBIDDEN_KEY))
                .validate();
    }

    @Test(dataProvider = "testContext")
    public void testCreateEnvironmentNotExistProxy(TestContext testContext) {
        testContext
                .given(EnvironmentEntity.class)
                .withProxyConfigs(INVALID_PROXY)
                .when(Environment::post, key(FORBIDDEN_KEY))
                .except(BadRequestException.class, key(FORBIDDEN_KEY))
                .validate();
    }

    @Test(dataProvider = "testContext")
    public void testCreateEnvironmentNotExistLdap(TestContext testContext) {
        testContext
                .given(EnvironmentEntity.class)
                .withLdapConfigs(INVALID_LDAP)
                .when(Environment::post, key(FORBIDDEN_KEY))
                .except(BadRequestException.class, key(FORBIDDEN_KEY))
                .validate();
    }

    @Test(dataProvider = "testContext")
    public void testCreateEnvironmentNotExistRds(TestContext testContext) {
        testContext
                .given(EnvironmentEntity.class)
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
                .withLdapConfigs(mixedLdap)
                .when(Environment::post, key(FORBIDDEN_KEY))
                .except(BadRequestException.class, key(FORBIDDEN_KEY))
                .validate();
    }

    @Test(dataProvider = "testContext")
    public void testDeleteEnvironment(TestContext testContext) {
        testContext
                .given(EnvironmentEntity.class)
                .when(Environment::post)
                .then(EnvironmentTest::checkCredentialAttachedToEnv)
                .when(Environment::getAll)
                .then(EnvironmentTest::checkEnvIsListed)
                .when(Environment::delete)
                .validate();
    }

    @Test(dataProvider = "testContext")
    public void testDeleteEnvWithProxy(TestContext testContext) {
        createDefaultProxyConfig(testContext);
        Set<String> validProxy = new HashSet<>();
        validProxy.add(testContext.get(ProxyConfigEntity.class).getName());
        testContext
                .given(EnvironmentEntity.class)
                .withProxyConfigs(validProxy)
                .when(Environment::post)
                .then(EnvironmentTest::checkCredentialAttachedToEnv)
                .when(Environment::getAll)
                .then(EnvironmentTest::checkEnvIsListed)
                .when(Environment::delete)
                .validate();
    }

    @Test(dataProvider = "testContext")
    public void testDeleteEnvWithLdap(TestContext testContext) {
        createDefaultLdapConfig(testContext);
        Set<String> validLdap = new HashSet<>();
        validLdap.add(testContext.get(LdapConfigEntity.class).getName());
        testContext
                .given(EnvironmentEntity.class)
                .withLdapConfigs(validLdap)
                .when(Environment::post)
                .then(EnvironmentTest::checkCredentialAttachedToEnv)
                .when(Environment::getAll)
                .then(EnvironmentTest::checkEnvIsListed)
                .when(Environment::delete)
                .validate();
    }

    @Test(dataProvider = "testContext")
    public void testDeleteEnvWithRds(TestContext testContext) {
        createDefaultRdsConfig(testContext);
        Set<String> validRds = new HashSet<>();
        validRds.add(testContext.get(RdsConfigEntity.class).getName());
        testContext
                .given(EnvironmentEntity.class)
                .withRdsConfigs(validRds)
                .when(Environment::post)
                .then(EnvironmentTest::checkCredentialAttachedToEnv)
                .when(Environment::getAll)
                .then(EnvironmentTest::checkEnvIsListed)
                .when(Environment::delete)
                .validate();
    }

    @Test(dataProvider = "testContext")
    public void testDeleteEnvironmentNotExist(TestContext testContext) {
        testContext
                .given(EnvironmentEntity.class)
                .when(Environment::delete, key(FORBIDDEN_KEY))
                .except(ForbiddenException.class, key(FORBIDDEN_KEY))
                .validate();
    }

    @Test(dataProvider = "testContext")
    public void testCreateEnvAttachRds(TestContext testContext) {
        createDefaultRdsConfig(testContext);
        Set<String> validRds = new HashSet<>();
        validRds.add(testContext.get(RdsConfigEntity.class).getName());
        testContext
                .given(EnvironmentEntity.class)
                .withName("int-rds-attach")
                .when(Environment::post)
                .when(Environment::getAll)
                .given(EnvironmentEntity.class)
                .withRdsConfigs(validRds)
                .when(Environment::putAttachResources)
                .then(EnvironmentTest::checkRdsAttachedToEnv)
                .validate();
    }

    @Test(dataProvider = "testContext")
    public void testCreateEnvAttachLdap(TestContext testContext) {
        createDefaultLdapConfig(testContext);
        Set<String> validLdap = new HashSet<>();
        validLdap.add(testContext.get(LdapConfigEntity.class).getName());
        testContext
                .given(EnvironmentEntity.class)
                .withName("int-ldap-attach")
                .when(Environment::post)
                .given(EnvironmentEntity.class)
                .withLdapConfigs(validLdap)
                .when(Environment::putAttachResources)
                .then(EnvironmentTest::checkLdapAttachedToEnv)
                .validate();
    }

    @Test(dataProvider = "testContext")
    public void testCreateEnvAttachProxy(TestContext testContext) {
        createDefaultProxyConfig(testContext);
        Set<String> validProxy = new HashSet<>();
        validProxy.add(testContext.get(ProxyConfigEntity.class).getName());
        testContext
                .given(EnvironmentEntity.class)
                .withName("int-proxy-attach")
                .when(Environment::post)
                .given(EnvironmentEntity.class)
                .withProxyConfigs(validProxy)
                .when(Environment::putAttachResources)
                .then(EnvironmentTest::checkProxyAttachedToEnv)
                .validate();
    }

    @Test(dataProvider = "testContext")
    public void testCreateEnvDetachRds(TestContext testContext) {
        createDefaultRdsConfig(testContext);
        Set<String> validRds = new HashSet<>();
        validRds.add(testContext.get(RdsConfigEntity.class).getName());
        testContext
                .given(EnvironmentEntity.class)
                .withName("int-rds-detach")
                .when(Environment::post)
                .when(Environment::getAll)
                .given(EnvironmentEntity.class)
                .withRdsConfigs(validRds)
                .when(Environment::putAttachResources)
                .then(EnvironmentTest::checkRdsAttachedToEnv)
                .when(Environment::putDetachResources)
                .validate();
    }

    @Test(dataProvider = "testContext")
    public void testCreateEnvDetachLdap(TestContext testContext) {
        createDefaultLdapConfig(testContext);
        Set<String> validLdap = new HashSet<>();
        validLdap.add(testContext.get(LdapConfigEntity.class).getName());
        testContext
                .given(EnvironmentEntity.class)
                .withName("int-ldap-detach")
                .when(Environment::post)
                .when(Environment::getAll)
                .given(EnvironmentEntity.class)
                .withLdapConfigs(validLdap)
                .when(Environment::putAttachResources)
                .then(EnvironmentTest::checkLdapAttachedToEnv)
                .when(Environment::putDetachResources)
                .validate();
    }

    @Test(dataProvider = "testContext")
    public void testCreateEnvDetachProxy(TestContext testContext) {
        createDefaultProxyConfig(testContext);
        Set<String> validProxy = new HashSet<>();
        validProxy.add(testContext.get(ProxyConfigEntity.class).getName());
        testContext
                .given(EnvironmentEntity.class)
                .withName("int-proxy-detach")
                .when(Environment::post)
                .when(Environment::getAll)
                .given(EnvironmentEntity.class)
                .withProxyConfigs(validProxy)
                .when(Environment::putAttachResources)
                .then(EnvironmentTest::checkProxyAttachedToEnv)
                .when(Environment::putDetachResources)
                .validate();
    }

    @Test(dataProvider = "testContext")
    public void testAttachRdsToMoreEnvs(TestContext testContext) {
        createDefaultRdsConfig(testContext);
        Set<String> validRds = new HashSet<>();
        validRds.add(testContext.get(RdsConfigEntity.class).getName());
        attachRdsToEnv(testContext, "int-rds-attach-envs", validRds);
        attachRdsToEnv(testContext, "int-rds-attach-envs2", validRds);
    }

    @Test(dataProvider = "testContext")
    public void testAttachLdapToMoreEnvs(TestContext testContext) {
        createDefaultLdapConfig(testContext);
        Set<String> validLdap = new HashSet<>();
        validLdap.add(testContext.get(LdapConfigEntity.class).getName());
        attachLdapToEnv(testContext, "int-ldap-attach-envs", validLdap);
        attachLdapToEnv(testContext, "int-ldap-attach-envs2", validLdap);
    }

    @Test(dataProvider = "testContext")
    public void testAttachProxyToMoreEnvs(TestContext testContext) {
        createDefaultProxyConfig(testContext);
        Set<String> validProxy = new HashSet<>();
        validProxy.add(testContext.get(ProxyConfigEntity.class).getName());
        attachProxyToEnv(testContext, "int-proxy-attach-envs", validProxy);
        attachProxyToEnv(testContext, "int-proxy-attach-envs2", validProxy);
    }

    @Test(dataProvider = "testContext")
    public void testAttachRdsToNotExistEnv(TestContext testContext) {
        createDefaultRdsConfig(testContext);
        Set<String> validRds = new HashSet<>();
        validRds.add(testContext.get(RdsConfigEntity.class).getName());
        testContext
                .given(EnvironmentEntity.class)
                .withName("int-no-env")
                .withRdsConfigs(validRds)
                .when(Environment::putAttachResources, key(FORBIDDEN_KEY))
                .except(ForbiddenException.class, key(FORBIDDEN_KEY))
                .validate();
    }

    @Test(dataProvider = "testContext")
    public void testRdsAttachDetachOther(TestContext testContext) {
        createDefaultRdsConfig(testContext);
        Set<String> validRds = new HashSet<>();
        Set<String> notValidRds = new HashSet<>();
        validRds.add(testContext.get(RdsConfigEntity.class).getName());
        notValidRds.add("not-existing-rds");
        testContext
                .given(EnvironmentEntity.class)
                .withName("int-rds-attach-1")
                .when(Environment::post)
                .when(Environment::getAll)
                .given(EnvironmentEntity.class)
                .withRdsConfigs(validRds)
                .when(Environment::putAttachResources)
                .then(EnvironmentTest::checkRdsAttachedToEnv)
                .given(EnvironmentEntity.class)
                .withName("int-rds-attach-1")
                .withRdsConfigs(notValidRds)
                .when(Environment::putDetachResources)
                .validate();
    }

    @Test(dataProvider = "testContext")
    public void testCreateEnvironmentChangeCredWithCredName(TestContext testContext) {
        testContext
                .given(EnvironmentEntity.class)
                .when(Environment::post)

                .given(CredentialEntity.class)
                .withName("int-env-change-cred")
                .when(new CredentialCreateAction())

                .given(EnvironmentEntity.class)
                .withCredentialName(testContext.get(CredentialEntity.class).getName())
                .then(Environment::changeCredential)
                .then(EnvironmentTest::checkCredentialAttachedToEnv)
                .validate();
    }

    @Test(dataProvider = "testContext")
    public void testCreateEnvironmentChangeCredWithCredRequest(TestContext testContext) {
        Map<String, Object> parameters = new HashMap<>();
        testContext
                .given(EnvironmentEntity.class)
                .when(Environment::post)

                .given(CredentialEntity.class)
                .withName("int-change-cred")

                .given(EnvironmentEntity.class)
                .withCredentialName(null)
                .withCredential(createCredentialRequest("MOCK", "Change credential", "int-change-cred", parameters))
                .then(Environment::changeCredential)
                .then(EnvironmentTest::checkCredentialAttachedToEnv)
                .validate();
    }

    @Test(dataProvider = "testContext")
    public void testCreateEnvironmentChangeCredForSame(TestContext testContext) {
        testContext
                .given(EnvironmentEntity.class)
                .when(Environment::post)
                .withCredentialName(testContext.get(CredentialEntity.class).getName())
                .then(Environment::changeCredential)
                .then(EnvironmentTest::checkCredentialAttachedToEnv)
                .validate();
    }

    @Test(dataProvider = "testContext")
    public void testCreateEnvironmentChangeCredNotExistingName(TestContext testContext) {
        testContext
                .given(EnvironmentEntity.class)
                .when(Environment::post)

                .given(EnvironmentEntity.class)
                .withCredentialName("not-existing-cred")
                .then(Environment::changeCredential, key(FORBIDDEN_KEY))
                .except(ForbiddenException.class, key(FORBIDDEN_KEY))
                .validate();
    }

    private static void attachRdsToEnv(TestContext testContext, String name, Set<String> validRds) {
        testContext
                .given(EnvironmentEntity.class)
                .withName(name)
                .when(Environment::post)

                .given(EnvironmentEntity.class)
                .withRdsConfigs(validRds)
                .when(Environment::putAttachResources)
                .then(EnvironmentTest::checkRdsAttachedToEnv)
                .validate();
    }

    private static void attachLdapToEnv(TestContext testContext, String name, Set<String> validLdap) {
        testContext
                .given(EnvironmentEntity.class)
                .withName(name)
                .when(Environment::post)

                .given(EnvironmentEntity.class)
                .withLdapConfigs(validLdap)
                .when(Environment::putAttachResources)
                .then(EnvironmentTest::checkLdapAttachedToEnv)
                .validate();
    }

    private static void attachProxyToEnv(TestContext testContext, String name, Set<String> validLdap) {
        testContext
                .given(EnvironmentEntity.class)
                .withName(name)
                .when(Environment::post)

                .given(EnvironmentEntity.class)
                .withProxyConfigs(validLdap)
                .when(Environment::putAttachResources)
                .then(EnvironmentTest::checkProxyAttachedToEnv)
                .validate();
    }

    protected static EnvironmentEntity checkCredentialAttachedToEnv(TestContext testContext, EnvironmentEntity environment, CloudbreakClient cloudbreakClient) {
        String credentialName = environment.getResponse().getCredentialName();
        if (!credentialName.equals(testContext.get(CredentialEntity.class).getName())) {
            throw new TestFailException("Credential is not attached to environment");
        }
        return environment;
    }

    protected static EnvironmentEntity checkRdsAttachedToEnv(TestContext testContext, EnvironmentEntity environment, CloudbreakClient cloudbreakClient) {
        Set<String> rdsConfigs = new HashSet<>();
        Set<DatabaseV4Response> rdsConfigResponseSet = environment.getResponse().getDatabases();
        for (DatabaseV4Response rdsConfigResponse : rdsConfigResponseSet) {
            rdsConfigs.add(rdsConfigResponse.getName());
        }
        if (!rdsConfigs.contains(testContext.get(RdsConfigEntity.class).getName())) {
            throw new TestFailException("Rds is not attached to environment");
        }
        return environment;
    }

    protected static EnvironmentEntity checkLdapAttachedToEnv(TestContext testContext, EnvironmentEntity environment, CloudbreakClient cloudbreakClient) {
        Set<String> ldapConfigs = new HashSet<>();
        Set<LdapV4Response> ldapV4ResponseSet = environment.getResponse().getLdaps();
        for (LdapV4Response ldapV4Response : ldapV4ResponseSet) {
            ldapConfigs.add(ldapV4Response.getName());
        }
        if (!ldapConfigs.contains(testContext.get(LdapConfigEntity.class).getName())) {
            throw new TestFailException("Ldap is not attached to environment");
        }
        return environment;
    }

    protected static EnvironmentEntity checkProxyAttachedToEnv(TestContext testContext, EnvironmentEntity environment, CloudbreakClient cloudbreakClient) {
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

    private static EnvironmentEntity checkEnvIsListed(TestContext testContext, EnvironmentEntity environment, CloudbreakClient cloudbreakClient) {
        Set<SimpleEnvironmentV4Response> simpleEnvironmentV4Respons = testContext.get(EnvironmentEntity.class).getResponseSimpleEnvSet();
        List<SimpleEnvironmentV4Response> result = simpleEnvironmentV4Respons.stream()
                .filter(env -> environment.getName().equals(env.getName()))
                .collect(Collectors.toList());
        if (result.isEmpty()) {
            throw new TestFailException("Environment is not listed");
        }
        return environment;
    }

    private CredentialRequest createCredentialRequest(String cloudPlatform, String description, String name, Map<String, Object> parameters) {
        CredentialRequest credentialRequest = new CredentialRequest();
        credentialRequest.setCloudPlatform(cloudPlatform);
        credentialRequest.setDescription(description);
        credentialRequest.setName(name);
        credentialRequest.setParameters(parameters);
        return credentialRequest;
    }
}