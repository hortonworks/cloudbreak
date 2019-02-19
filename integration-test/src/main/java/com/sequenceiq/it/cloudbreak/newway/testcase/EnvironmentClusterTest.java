package com.sequenceiq.it.cloudbreak.newway.testcase;

import static com.sequenceiq.it.cloudbreak.newway.context.RunningParameter.key;
import static com.sequenceiq.it.cloudbreak.newway.context.RunningParameter.withoutLogError;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.database.responses.DatabaseV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.ldaps.responses.LdapV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.proxies.responses.ProxyV4Response;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.Credential;
import com.sequenceiq.it.cloudbreak.newway.entity.credential.CredentialTestDto;
import com.sequenceiq.it.cloudbreak.newway.Environment;
import com.sequenceiq.it.cloudbreak.newway.EnvironmentEntity;
import com.sequenceiq.it.cloudbreak.newway.Stack;
import com.sequenceiq.it.cloudbreak.newway.StackEntity;
import com.sequenceiq.it.cloudbreak.newway.client.LdapConfigTestClient;
import com.sequenceiq.it.cloudbreak.newway.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.AmbariEntity;
import com.sequenceiq.it.cloudbreak.newway.entity.ClusterEntity;
import com.sequenceiq.it.cloudbreak.newway.entity.EnvironmentSettingsV4Entity;
import com.sequenceiq.it.cloudbreak.newway.entity.database.DatabaseEntity;
import com.sequenceiq.it.cloudbreak.newway.entity.ldap.LdapConfigTestDto;
import com.sequenceiq.it.cloudbreak.newway.entity.proxy.ProxyConfig;
import com.sequenceiq.it.cloudbreak.newway.entity.proxy.ProxyConfigEntity;
import com.sequenceiq.it.cloudbreak.newway.v3.StackActionV4;

public class EnvironmentClusterTest extends AbstractIntegrationTest {

    public static final String NEW_CREDENTIAL_KEY = "newCred";

    private static final String FORBIDDEN_KEY = "forbiddenPost";

    private static final String BP_NAME = "Data Science: Apache Spark 2, Apache Zeppelin";

    @Inject
    private LdapConfigTestClient ldapConfigTestClient;

    @Override
    protected void minimalSetupForClusterCreation(TestContext testContext) {
        createDefaultUser(testContext);
        createDefaultCredential(testContext);
        createDefaultImageCatalog(testContext);
        initializeDefaultClusterDefinitions(testContext);
    }

    @BeforeMethod
    public void beforeMethod(Object[] data) {
        MockedTestContext testContext = (MockedTestContext) data[0];
        minimalSetupForClusterCreation(testContext);
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown(Object[] data) {
        ((MockedTestContext) data[0]).cleanupTestContextEntity();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    public void testDetachFromEnvWithDeletedCluster(TestContext testContext) {
        createEnvWithResources(testContext);
        testContext.given(StackEntity.class)
                .withEnvironment(EnvironmentEntity.class)
                .withCluster(setResources(testContext, testContext.get(DatabaseEntity.class).getName(),
                        testContext.get(LdapConfigTestDto.class).getName(), testContext.get(ProxyConfigEntity.class).getName()))
                .when(Stack.postV4())
                .await(STACK_AVAILABLE)
                .when(StackActionV4::delete, withoutLogError())
                .await(STACK_DELETED)

                .given(EnvironmentEntity.class)
                .withName(testContext.get(EnvironmentEntity.class).getName())
                .withRdsConfigs(getRdsAsList(testContext))
                .withLdapConfigs(getLdapAsList(testContext))
                .withProxyConfigs(getProxyAsList(testContext))
                .when(Environment::putDetachResources)
                .when(Environment::get)
                .then(EnvironmentClusterTest::checkEnvHasNoRds)
                .then(EnvironmentClusterTest::checkEnvHasNoLdap)
                .then(EnvironmentClusterTest::checkEnvHasNoProxy)
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    public void testWlClusterNotAttachResourceDetachDeleteOk(TestContext testContext) {
        createEnvWithResources(testContext);
        testContext.given(StackEntity.class)
                .withEnvironment(EnvironmentEntity.class)
                .when(Stack.postV4())
                .await(STACK_AVAILABLE)

                .given(EnvironmentEntity.class)
                .when(Environment::putDetachResources)
                .given(DatabaseEntity.class)
                .when(DatabaseEntity::delete)
                .given(LdapConfigTestDto.class)
                .when(ldapConfigTestClient.delete())
                .given(ProxyConfigEntity.class)
                .when(ProxyConfig::delete)
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    public void testCreateWlClusterDeleteFails(TestContext testContext) {
        createEnvWithResources(testContext);
        testContext.given(StackEntity.class)
                .withEnvironment(EnvironmentEntity.class)
                .withCluster(setResources(testContext, testContext.get(DatabaseEntity.class).getName(),
                        testContext.get(LdapConfigTestDto.class).getName(), testContext.get(ProxyConfigEntity.class).getName()))
                .when(Stack.postV4())
                .await(STACK_AVAILABLE)

                .deleteGiven(ProxyConfigEntity.class, ProxyConfig::delete, key(FORBIDDEN_KEY))
                .deleteGiven(LdapConfigTestDto.class, ldapConfigTestClient.delete(), key(FORBIDDEN_KEY))
                .deleteGiven(DatabaseEntity.class, DatabaseEntity::delete, key(FORBIDDEN_KEY))
                .deleteGiven(CredentialTestDto.class, Credential::delete, key(FORBIDDEN_KEY))
                .deleteGiven(EnvironmentEntity.class, Environment::delete, key(FORBIDDEN_KEY))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    public void testCreateWlClusterDetachFails(TestContext testContext) {
        createEnvWithResources(testContext);
        testContext
                .given(StackEntity.class)
                .withEnvironment(EnvironmentEntity.class)
                .withCluster(setResources(testContext, testContext.get(DatabaseEntity.class).getName(),
                        testContext.get(LdapConfigTestDto.class).getName(), testContext.get(ProxyConfigEntity.class).getName()))
                .when(Stack.postV4())
                .await(STACK_AVAILABLE)
                .given(EnvironmentEntity.class)
                .withRdsConfigs(getRdsAsList(testContext))
                .withLdapConfigs(getLdapAsList(testContext))
                .withProxyConfigs(getProxyAsList(testContext))
                .when(Environment::putDetachResources, key(FORBIDDEN_KEY))
                .expect(BadRequestException.class, key(FORBIDDEN_KEY))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    public void testSameEnvironmentWithDifferentClusters(TestContext testContext) {
        String newStack = "newStack";
        testContext.given(EnvironmentEntity.class)
                .when(Environment::post)
                .given(StackEntity.class)
                .withEnvironment(EnvironmentEntity.class)
                .when(Stack.postV4())
                .await(STACK_AVAILABLE)
                .given(newStack, StackEntity.class)
                .when(Stack.postV4(), key(newStack))
                .await(STACK_AVAILABLE, key(newStack))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    public void testSameEnvironmentAttachRdsToDifferentClusters(TestContext testContext) {
        createDefaultRdsConfig(testContext);
        Set<String> validRds = new HashSet<>();
        validRds.add(testContext.get(DatabaseEntity.class).getName());

        String newStack = "newStack";
        testContext.given(EnvironmentEntity.class)
                .withRdsConfigs(validRds)
                .when(Environment::post)
                .then(EnvironmentTest::checkRdsAttachedToEnv)

                .given(StackEntity.class).given(EnvironmentSettingsV4Entity.class)
                .given(StackEntity.class)
                .withEnvironment(EnvironmentEntity.class)
                .withCluster(setResources(testContext, testContext.get(DatabaseEntity.class).getName(),
                        null, null))
                .when(Stack.postV4())
                .await(STACK_AVAILABLE)

                .given(newStack, StackEntity.class)
                .withEnvironment(EnvironmentEntity.class)
                .withCluster(setResources(testContext, testContext.get(DatabaseEntity.class).getName(),
                        null, null))
                .when(Stack.postV4(), key(newStack))
                .await(STACK_AVAILABLE, key(newStack))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    public void testReuseRdsWithDifferentClustersInDifferentEnvs(TestContext testContext) {
        createDefaultRdsConfig(testContext);
        Set<String> validRds = Set.of(testContext.get(DatabaseEntity.class).getName());
        String newEnv = "newEnv";
        String newStack = "newStack";

        testContext.given(EnvironmentEntity.class)
                .withRdsConfigs(validRds)
                .when(Environment::post)
                .then(EnvironmentTest::checkRdsAttachedToEnv)

                .given(StackEntity.class)
                .withEnvironment(EnvironmentEntity.class)
                .withCluster(setResources(testContext, testContext.get(DatabaseEntity.class).getName(),
                        null, null))
                .when(Stack.postV4())
                .await(STACK_AVAILABLE)

                .given(newEnv, EnvironmentEntity.class)
                .withRdsConfigs(validRds)
                .when(Environment::post)
                .then(EnvironmentTest::checkRdsAttachedToEnv)

                .given(newStack, StackEntity.class)
                .withEnvironment(newEnv)
                .withCluster(setResources(testContext, testContext.get(DatabaseEntity.class).getName(),
                        null, null))
                .when(Stack.postV4(), key(newStack))
                .await(STACK_AVAILABLE, key(newStack))
                .when(StackActionV4::delete, key(newStack))
                .await(STACK_DELETED, key(newStack))

                .given(newEnv, EnvironmentEntity.class)
                .when(Environment::putDetachResources, key(newEnv))
                .then((tc, env, cbClient) -> EnvironmentTest.checkRdsDetachedFromEnv(tc, env, DatabaseEntity.class, cbClient))

                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    public void testClusterWithRdsWithoutEnvironment(TestContext testContext) {
        createDefaultRdsConfig(testContext);
        testContext.given(EnvironmentEntity.class)
                .given(StackEntity.class)
                .withEnvironment(EnvironmentEntity.class)
                .withCluster(setResources(testContext, testContext.get(DatabaseEntity.class).getName(),
                        null, null))
                .when(Stack.postV4(), key(FORBIDDEN_KEY))
                .expect(BadRequestException.class, key(FORBIDDEN_KEY))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    public void testWlClusterChangeCred(MockedTestContext testContext) {
        testContext.given(EnvironmentEntity.class)
                .when(Environment::post)
                .given(StackEntity.class)
                .withEnvironment(EnvironmentEntity.class)
                .when(Stack.postV4())
                .await(STACK_AVAILABLE)
                .given(NEW_CREDENTIAL_KEY, CredentialTestDto.class)
                .withName("int-change-cred-cl")
                .given(EnvironmentEntity.class)
                .withName(testContext.get(EnvironmentEntity.class).getName())
                .withCredentialName(null)
                .withCredential(NEW_CREDENTIAL_KEY)
                .when(Environment::changeCredential)
                .then(EnvironmentClusterTest::checkNewCredentialAttachedToEnv)
                .validate();
        checkCredentialAttachedToCluster(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    public void testClusterWithEmptyEnvironmentRequest(TestContext testContext) {
        testContext
                .given(EnvironmentEntity.class)
                .when(Environment::post)
                .given("invalidEnvironmentSettingsRequest", EnvironmentSettingsV4Entity.class).withName(null).withCredentialName(null)
                .given(StackEntity.class)
                .withEnvironmentSettings("invalidEnvironmentSettingsRequest")
                .when(Stack.postV4(), key("badRequest"))
                .expect(BadRequestException.class, key("badRequest").withExpectedMessage(".*CredentialName or EnvironmentName is mandatory"))
                .validate();
    }

    private void createEnvWithResources(TestContext testContext) {
        testContext.given(EnvironmentEntity.class)
                .withRdsConfigs(createDefaultRdsConfig(testContext))
                .withLdapConfigs(createDefaultLdapConfig(testContext))
                .withProxyConfigs(createDefaultProxyConfig(testContext))
                .when(Environment::post);
    }

    private void checkCredentialAttachedToCluster(TestContext testContext) {
        testContext.given(StackEntity.class)
                .withName(testContext.get(StackEntity.class).getName())
                .when(Stack::getByName)
                .then(EnvironmentClusterTest::checkNewCredentialInStack)
                .validate();
    }

    private Set<String> getProxyAsList(TestContext testContext) {
        return new HashSet<>(Collections.singletonList(testContext.get(ProxyConfigEntity.class).getName()));
    }

    private Set<String> getRdsAsList(TestContext testContext) {
        return new HashSet<>(Collections.singletonList(testContext.get(DatabaseEntity.class).getName()));
    }

    private Set<String> getLdapAsList(TestContext testContext) {
        return new HashSet<>(Collections.singletonList(testContext.get(LdapConfigTestDto.class).getName()));
    }

    private ClusterEntity setResources(TestContext testContext, String rdsName, String ldapName, String proxyName) {
        Set<String> rdsSet = new LinkedHashSet<>();
        rdsSet.add(rdsName);
        ClusterEntity cluster = new ClusterEntity(testContext)
                .valid()
                .withRdsConfigNames(rdsSet)
                .withAmbari(new AmbariEntity(testContext).valid().withClusterDefinitionName(BP_NAME));
        if (rdsName != null) {
            cluster.withRdsConfigNames(rdsSet);
        }
        if (ldapName != null) {
            cluster.withLdapConfigName(ldapName);
        }
        if (rdsName != null) {
            cluster.withProxyConfigName(proxyName);
        }
        return cluster;
    }

    private static StackEntity checkNewCredentialInStack(TestContext testContext, StackEntity stack, CloudbreakClient cloudbreakClient) {
        String credentialName = stack.getResponse().getEnvironment().getCredential().getName();
        if (!credentialName.equals(testContext.get(NEW_CREDENTIAL_KEY).getName())) {
            throw new TestFailException("Credential is not attached to cluster");
        }
        return stack;
    }

    static EnvironmentEntity checkEnvHasNoRds(TestContext testContext, EnvironmentEntity environment, CloudbreakClient cloudbreakClient) {
        Set<DatabaseV4Response> rdsConfigResponseSet = environment.getResponse().getDatabases();
        if (!rdsConfigResponseSet.isEmpty()) {
            throw new TestFailException("Environment has attached rds");
        }
        return environment;
    }

    static EnvironmentEntity checkEnvHasNoLdap(TestContext testContext, EnvironmentEntity environment, CloudbreakClient cloudbreakClient) {
        Set<LdapV4Response> ldapV4ResponseSet = environment.getResponse().getLdaps();
        if (!ldapV4ResponseSet.isEmpty()) {
            throw new TestFailException("Environment has attached ldap");
        }
        return environment;
    }

    private static EnvironmentEntity checkEnvHasNoProxy(TestContext testContext, EnvironmentEntity environment, CloudbreakClient cloudbreakClient) {
        Set<ProxyV4Response> proxyV4ResponseSet = environment.getResponse().getProxies();
        if (!proxyV4ResponseSet.isEmpty()) {
            throw new TestFailException("Environment has attached proxy");
        }
        return environment;
    }

    protected static EnvironmentEntity checkNewCredentialAttachedToEnv(TestContext testContext,
            EnvironmentEntity environment, CloudbreakClient cloudbreakClient) {
        String credentialName = environment.getResponse().getCredentialName();
        if (!credentialName.equals(testContext.get(NEW_CREDENTIAL_KEY).getName())) {
            throw new TestFailException("Credential is not attached to environment");
        }
        return environment;
    }
}