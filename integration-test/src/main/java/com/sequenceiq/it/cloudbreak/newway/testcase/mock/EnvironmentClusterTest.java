package com.sequenceiq.it.cloudbreak.newway.testcase.mock;

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

import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseV4Base;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.responses.DatabaseV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.ldaps.responses.LdapV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.proxies.responses.ProxyV4Response;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.Credential;
import com.sequenceiq.it.cloudbreak.newway.context.Description;
import com.sequenceiq.it.cloudbreak.newway.entity.CloudbreakEntity;
import com.sequenceiq.it.cloudbreak.newway.entity.credential.CredentialTestDto;
import com.sequenceiq.it.cloudbreak.newway.Environment;
import com.sequenceiq.it.cloudbreak.newway.EnvironmentEntity;
import com.sequenceiq.it.cloudbreak.newway.Stack;
import com.sequenceiq.it.cloudbreak.newway.entity.stack.StackTestDto;
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
import com.sequenceiq.it.cloudbreak.newway.testcase.AbstractIntegrationTest;
import com.sequenceiq.it.cloudbreak.newway.util.EnvironmentTestUtils;
import com.sequenceiq.it.cloudbreak.newway.v3.StackActionV4;

public class EnvironmentClusterTest extends AbstractIntegrationTest {

    private static final String NEW_CREDENTIAL_KEY = "newCred";

    private static final String FORBIDDEN_KEY = "forbiddenPost";

    private static final String CD_NAME = "Data Science: Apache Spark 2, Apache Zeppelin";

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
    @Description(
            given = "there is an available environment with attached rds, ldap and proxy configs",
            when = "a cluster is created and deleted in the env and detach environment endpoint is called for the attached resources",
            then = "all of the three resources should be detached")
    public void testDetachFromEnvWithDeletedCluster(TestContext testContext) {
        createEnvWithResources(testContext);
        testContext.given(StackTestDto.class)
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
    @Description(
            given = "there is an environment with a attached shared resources and a running cluster that is not using these resources",
            when = "calling detach environment endpoint for the resources and deleting those",
            then = "all three resources should be be deleted")
    public void testWlClusterNotAttachResourceDetachDeleteOk(TestContext testContext) {
        createEnvWithResources(testContext);
        testContext.given(StackTestDto.class)
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
    @Description(
            given = "there is an environment with a attached shared resources and a running cluster that is using these resources",
            when = "the resource delete endpoints and environment delete endpoints are called",
            then = "non of the operations should succeed")
    public void testCreateWlClusterDeleteFails(TestContext testContext) {
        createEnvWithResources(testContext);
        testContext.given(StackTestDto.class)
                .withEnvironment(EnvironmentEntity.class)
                .withCluster(
                        setResources(
                                testContext,
                                testContext.get(DatabaseEntity.class).getName(),
                                testContext.get(LdapConfigTestDto.class).getName(),
                                testContext.get(ProxyConfigEntity.class).getName()
                        )
                )
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
    @Description(
            given = "there is an environment with a attached shared resources and a running cluster that is using these resources",
            when = "the detach resources from environment endpoint is called",
            then = "the resources should not be detached from the environment")
    public void testCreateWlClusterDetachFails(TestContext testContext) {
        createEnvWithResources(testContext);
        testContext
                .given(StackTestDto.class)
                .withEnvironment(EnvironmentEntity.class)
                .withCluster(
                        setResources(
                                testContext,
                                testContext.get(DatabaseEntity.class).getName(),
                                testContext.get(LdapConfigTestDto.class).getName(),
                                testContext.get(ProxyConfigEntity.class).getName()
                        )
                )
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
    @Description(
            given = "there is an environment with a cluster in it",
            when = "calling create cluster with a different cluster name",
            then = "the second cluster should be created")
    public void testSameEnvironmentWithDifferentClusters(TestContext testContext) {
        String newStack = getNameGenerator().getRandomNameForResource();
        testContext.given(EnvironmentEntity.class)
                .when(Environment::post)
                .given(StackTestDto.class)
                .withEnvironment(EnvironmentEntity.class)
                .when(Stack.postV4())
                .await(STACK_AVAILABLE)
                .given(newStack, StackTestDto.class)
                .when(Stack.postV4(), key(newStack))
                .await(STACK_AVAILABLE, key(newStack))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is an environment and attached database and a cluster in it which uses the database",
            when = "the cluster create endpoint is called with a cluster that is using the same attached database",
            then = "the second cluster should be created, using the same attached database")
    public void testSameEnvironmentAttachRdsToDifferentClusters(TestContext testContext) {
        createDefaultRdsConfig(testContext);
        Set<String> validRds = new HashSet<>();
        validRds.add(testContext.get(DatabaseEntity.class).getName());

        String newStack = getNameGenerator().getRandomNameForResource();
        testContext.given(EnvironmentEntity.class)
                .withRdsConfigs(validRds)
                .when(Environment::post)
                .then(EnvironmentTestUtils::checkRdsAttachedToEnv)

                .given(StackTestDto.class).given(EnvironmentSettingsV4Entity.class)
                .given(StackTestDto.class)
                .withEnvironment(EnvironmentEntity.class)
                .withCluster(setResources(testContext, testContext.get(DatabaseEntity.class).getName(),
                        null, null))
                .when(Stack.postV4())
                .await(STACK_AVAILABLE)

                .given(newStack, StackTestDto.class)
                .withEnvironment(EnvironmentEntity.class)
                .withCluster(setResources(testContext, testContext.get(DatabaseEntity.class).getName(),
                        null, null))
                .when(Stack.postV4(), key(newStack))
                .await(STACK_AVAILABLE, key(newStack))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a database that is attached to 2 environment",
            when = "2 separate clusters are created in the 2 envs, both using the same database",
            then = "both cluster creation should work")
    public void testReuseRdsWithDifferentClustersInDifferentEnvs(TestContext testContext) {
        createDefaultRdsConfig(testContext);
        Set<String> validRds = Set.of(testContext.get(DatabaseEntity.class).getName());
        String newEnv = getNameGenerator().getRandomNameForResource();
        String newStack = getNameGenerator().getRandomNameForResource();

        testContext.given(EnvironmentEntity.class)
                .withRdsConfigs(validRds)
                .when(Environment::post)
                .then(EnvironmentTestUtils::checkRdsAttachedToEnv)

                .given(StackTestDto.class)
                .withEnvironment(EnvironmentEntity.class)
                .withCluster(
                        setResources(
                                testContext,
                                testContext.get(DatabaseEntity.class).getName(),
                                null,
                                null
                        )
                )
                .when(Stack.postV4())
                .await(STACK_AVAILABLE)

                .given(newEnv, EnvironmentEntity.class)
                .withRdsConfigs(validRds)
                .when(Environment::post)
                .then(EnvironmentTestUtils::checkRdsAttachedToEnv)

                .given(newStack, StackTestDto.class)
                .withEnvironmentKey(newEnv)
                .withCluster(
                        setResources(
                                testContext,
                                testContext.get(DatabaseEntity.class).getName(),
                                null,
                                null
                        )
                )
                .when(Stack.postV4(), key(newStack))
                .await(STACK_AVAILABLE, key(newStack))
                .when(StackActionV4::delete, key(newStack))
                .await(STACK_DELETED, key(newStack))

                .given(newEnv, EnvironmentEntity.class)
                .when(Environment::putDetachResources, key(newEnv))
                .then((tc, env, cbClient) -> checkRdsDetachedFromEnv(tc, env, DatabaseEntity.class, cbClient))

                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is an environment and a database which in not attached to the environment",
            when = "a cluster is created in the environment and with the non-attached database",
            then = "the cluster create should succeed")
    public void testClusterWithRdsWithoutEnvironment(TestContext testContext) {
        createDefaultRdsConfig(testContext);
        testContext.given(EnvironmentEntity.class)
                .when(Environment::post)
                .given(StackTestDto.class)
                .withEnvironment(EnvironmentEntity.class)
                .withCluster(setResources(testContext, testContext.get(DatabaseEntity.class).getName(),
                        null, null))
                .when(Stack.postV4())
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is an environment with a cluster in it",
            when = "a change credential request is sent for the environment",
            then = "the credential of the cluster should be changed too")
    public void testWlClusterChangeCred(MockedTestContext testContext) {
        testContext.given(EnvironmentEntity.class)
                .when(Environment::post)
                .given(StackTestDto.class)
                .withEnvironment(EnvironmentEntity.class)
                .when(Stack.postV4())
                .await(STACK_AVAILABLE)
                .given(NEW_CREDENTIAL_KEY, CredentialTestDto.class)
                .given(EnvironmentEntity.class)
                .withName(testContext.get(EnvironmentEntity.class).getName())
                .withCredentialName(null)
                .withCredential(NEW_CREDENTIAL_KEY)
                .when(Environment::changeCredential)
                .then(EnvironmentClusterTest::checkNewCredentialAttachedToEnv);
        checkCredentialAttachedToCluster(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is an available environment",
            when = "create cluster request is sent with missing environment settings",
            then = "a BadRequestException should be returned")
    public void testClusterWithEmptyEnvironmentRequest(TestContext testContext) {
        testContext
                .given(EnvironmentEntity.class)
                .when(Environment::post)
                .given("invalidEnvironmentSettingsRequest", EnvironmentSettingsV4Entity.class).withName(null).withCredentialName(null)
                .given(StackTestDto.class)
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
        testContext.given(StackTestDto.class)
                .withName(testContext.get(StackTestDto.class).getName())
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
                .withAmbari(testContext.given(AmbariEntity.class).withClusterDefinitionName(CD_NAME));
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

    private static StackTestDto checkNewCredentialInStack(TestContext testContext, StackTestDto stack, CloudbreakClient cloudbreakClient) {
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

    private static EnvironmentEntity checkNewCredentialAttachedToEnv(TestContext testContext,
            EnvironmentEntity environment, CloudbreakClient cloudbreakClient) {
        String credentialName = environment.getResponse().getCredentialName();
        if (!credentialName.equals(testContext.get(NEW_CREDENTIAL_KEY).getName())) {
            throw new TestFailException("Credential is not attached to environment");
        }
        return environment;
    }

    private <T extends CloudbreakEntity> EnvironmentEntity checkRdsDetachedFromEnv(TestContext testContext,
            EnvironmentEntity environment, Class<T> rdsKey, CloudbreakClient cloudbreakClient) {
        String rdsName = testContext.get(rdsKey).getName();
        return checkRdsDetachedFromEnv(environment, rdsName);
    }

    private EnvironmentEntity checkRdsDetachedFromEnv(EnvironmentEntity environment, String rdsName) {
        Set<DatabaseV4Response> rdsConfigs = environment.getResponse().getDatabases();
        boolean attached = rdsConfigs.stream().map(DatabaseV4Base::getName)
                .anyMatch(rds -> rds.equals(rdsName));

        if (attached) {
            throw new TestFailException("Rds is attached to environment");
        }
        return environment;
    }
}