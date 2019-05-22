package com.sequenceiq.it.cloudbreak.testcase.mock;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;

import org.testng.annotations.Test;

<<<<<<< HEAD
import com.sequenceiq.cloudbreak.api.endpoint.v4.ldaps.responses.LdapV4Response;
=======
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseV4Base;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.responses.DatabaseV4Response;
>>>>>>> CB-1516 eliminate relation between Environment and LDAP
import com.sequenceiq.cloudbreak.api.endpoint.v4.proxies.responses.ProxyV4Response;
import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.client.CredentialTestClient;
import com.sequenceiq.it.cloudbreak.client.DatabaseTestClient;
import com.sequenceiq.it.cloudbreak.client.EnvironmentTestClient;
import com.sequenceiq.it.cloudbreak.client.LdapTestClient;
import com.sequenceiq.it.cloudbreak.client.ProxyTestClient;
import com.sequenceiq.it.cloudbreak.client.StackTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.context.RunningParameter;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.ClusterTestDto;
import com.sequenceiq.it.cloudbreak.dto.EnvironmentSettingsV4TestDto;
import com.sequenceiq.it.cloudbreak.dto.credential.CredentialTestDto;
import com.sequenceiq.it.cloudbreak.dto.database.DatabaseTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.ldap.LdapTestDto;
import com.sequenceiq.it.cloudbreak.dto.proxy.ProxyTestDto;
import com.sequenceiq.it.cloudbreak.dto.stack.StackTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.testcase.AbstractIntegrationTest;

public class EnvironmentClusterTest extends AbstractIntegrationTest {

    private static final String NEW_CREDENTIAL_KEY = "newCred";

    private static final String FORBIDDEN_KEY = "forbiddenPost";

    private static final String CD_NAME = "Data Science: Apache Spark 2, Apache Zeppelin";

    @Inject
    private LdapTestClient ldapTestClient;

    @Inject
    private StackTestClient stackTestClient;

    @Inject
    private DatabaseTestClient databaseTestClient;

    @Inject
    private CredentialTestClient credentialTestClient;

    @Inject
    private EnvironmentTestClient environmentTestClient;

    @Inject
    private ProxyTestClient proxyTestClient;

    @Override
    protected void setupTest(TestContext testContext) {
        createDefaultUser(testContext);
        createDefaultCredential(testContext);
        createDefaultImageCatalog(testContext);
        initializeDefaultBlueprints(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is an available environment with attached rds, ldap and proxy configs",
            when = "a cluster is created and deleted in the env and detach environment endpoint is called for the attached resources",
            then = "all of the three resources should be detached")
    public void testDetachFromEnvWithDeletedCluster(MockedTestContext testContext) {
        createEnvWithResources(testContext);
        testContext
                .given(ClusterTestDto.class)
                .withDatabase(testContext.get(DatabaseTestDto.class).getName())
                .withLdapConfigName(testContext.get(LdapTestDto.class).getName())
                .withProxyConfigName(testContext.get(ProxyTestDto.class).getName())
                .given(StackTestDto.class)
                .withEnvironment(EnvironmentTestDto.class)
                .when(stackTestClient.createV4())
                .await(STACK_AVAILABLE)
                .when(stackTestClient.deleteV4(), RunningParameter.withoutLogError())
                .await(STACK_DELETED)

                .given(EnvironmentTestDto.class)
                .withName(testContext.get(EnvironmentTestDto.class).getName())
                .withLdapConfigs(getLdapAsList(testContext))
                .withProxyConfigs(getProxyAsList(testContext))
                .when(environmentTestClient.detachV4())
                .when(environmentTestClient.getV4())
<<<<<<< HEAD
                .then(EnvironmentClusterTest::checkEnvHasNoLdap)
=======
                .then(EnvironmentClusterTest::checkEnvHasNoRds)
>>>>>>> CB-1516 eliminate relation between Environment and LDAP
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
                .withEnvironment(EnvironmentTestDto.class)
                .when(stackTestClient.createV4())
                .await(STACK_AVAILABLE)

                .given(EnvironmentTestDto.class)
                .when(environmentTestClient.detachV4())
                .given(DatabaseTestDto.class)
                .when(databaseTestClient.deleteV4())
                .given(LdapTestDto.class)
                .when(ldapTestClient.deleteV4())
                .given(ProxyTestDto.class)
                .when(proxyTestClient.deleteV4())
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is an environment with a attached shared resources and a running cluster that is using these resources",
            when = "the resource delete endpoints and environment delete endpoints are called",
            then = "non of the operations should succeed")
    public void testCreateWlClusterDeleteFails(MockedTestContext testContext) {
        createEnvWithResources(testContext);
        testContext.given(StackTestDto.class)
                .withEnvironment(EnvironmentTestDto.class)
                .withCluster(
                        setResources(
                                testContext,
                                testContext.get(DatabaseTestDto.class).getName(),
                                testContext.get(LdapTestDto.class).getName(),
                                testContext.get(ProxyTestDto.class).getName()
                        )
                )
                .when(stackTestClient.createV4())
                .await(STACK_AVAILABLE)

                .deleteGiven(ProxyTestDto.class, proxyTestClient.deleteV4(), RunningParameter.key(FORBIDDEN_KEY))
                .deleteGiven(LdapTestDto.class, ldapTestClient.deleteV4(), RunningParameter.key(FORBIDDEN_KEY))
                .deleteGiven(DatabaseTestDto.class, databaseTestClient.deleteV4(), RunningParameter.key(FORBIDDEN_KEY))
                .deleteGiven(CredentialTestDto.class, credentialTestClient.deleteV4(), RunningParameter.key(FORBIDDEN_KEY))
                .deleteGiven(EnvironmentTestDto.class, environmentTestClient.deleteV4(), RunningParameter.key(FORBIDDEN_KEY))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is an environment with a attached shared resources and a running cluster that is using these resources",
            when = "the detach resources from environment endpoint is called",
            then = "the resources should not be detached from the environment")
    public void testCreateWlClusterDetachFails(MockedTestContext testContext) {
        createEnvWithResources(testContext);
        testContext
                .given(ClusterTestDto.class)
                .given(StackTestDto.class)
                .withEnvironment(EnvironmentTestDto.class)
                .withCluster(
                        setResources(
                                testContext,
                                testContext.get(DatabaseTestDto.class).getName(),
                                testContext.get(LdapTestDto.class).getName(),
                                testContext.get(ProxyTestDto.class).getName()
                        )
                )
                .when(stackTestClient.createV4())
                .await(STACK_AVAILABLE)
                .given(EnvironmentTestDto.class)
                .withLdapConfigs(getLdapAsList(testContext))
                .withProxyConfigs(getProxyAsList(testContext))
                .when(environmentTestClient.detachV4(), RunningParameter.key(FORBIDDEN_KEY))
                .expect(BadRequestException.class, RunningParameter.key(FORBIDDEN_KEY))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is an environment with a cluster in it",
            when = "calling create cluster with a different cluster name",
            then = "the second cluster should be created")
    public void testSameEnvironmentWithDifferentClusters(TestContext testContext) {
        String newStack = resourcePropertyProvider().getName();
        testContext.given(EnvironmentTestDto.class)
                .when(environmentTestClient.createV4())
                .given(StackTestDto.class)
                .withEnvironment(EnvironmentTestDto.class)
                .when(stackTestClient.createV4())
                .await(STACK_AVAILABLE)
                .given(newStack, StackTestDto.class)
                .when(stackTestClient.createV4(), RunningParameter.key(newStack))
                .await(STACK_AVAILABLE, RunningParameter.key(newStack))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is an environment and a database which in not attached to the environment",
            when = "a cluster is created in the environment and with the non-attached database",
            then = "the cluster create should succeed")
    public void testClusterWithRdsWithoutEnvironment(TestContext testContext) {
        createDefaultRdsConfig(testContext);
        testContext
                .given(EnvironmentTestDto.class)
                .when(environmentTestClient.createV4())
                .given(StackTestDto.class)
                .withEnvironment(EnvironmentTestDto.class)
                .withCluster(setResources(testContext, testContext.get(DatabaseTestDto.class).getName(),
                        null, null))
                .when(stackTestClient.createV4())
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is an environment with a cluster in it",
            when = "a change credential request is sent for the environment",
            then = "the credential of the cluster should be changed too")
    public void testWlClusterChangeCred(MockedTestContext testContext) {
        testContext
                .given(EnvironmentTestDto.class)
                .when(environmentTestClient.createV4())
                .given(StackTestDto.class)
                .withEnvironment(EnvironmentTestDto.class)
                .when(stackTestClient.createV4())
                .await(STACK_AVAILABLE)
                .given(NEW_CREDENTIAL_KEY, CredentialTestDto.class)
                .given(EnvironmentTestDto.class)
                .withName(testContext.get(EnvironmentTestDto.class).getName())
                .withCredentialName(null)
                .withCredential(NEW_CREDENTIAL_KEY)
                .when(environmentTestClient.changeCredential())
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
                .given(EnvironmentTestDto.class)
                .when(environmentTestClient.createV4())
                .given("invalidEnvironmentSettingsRequest", EnvironmentSettingsV4TestDto.class)
                .withName(null)
                .withCredentialName(null)
                .given(StackTestDto.class)
                .withEnvironmentSettings("invalidEnvironmentSettingsRequest")
                .when(stackTestClient.createV4(), RunningParameter.key("badRequest"))
                .expect(BadRequestException.class, RunningParameter.key("badRequest")
                        .withExpectedMessage(".*CredentialName or EnvironmentName is mandatory"))
                .validate();
    }

    private void createEnvWithResources(TestContext testContext) {
        testContext.given(EnvironmentTestDto.class)
                .withLdapConfigs(createDefaultLdapConfig(testContext))
                .withProxyConfigs(createDefaultProxyConfig(testContext))
                .when(environmentTestClient.createV4());
        createDefaultRdsConfig(testContext);
    }

    private void checkCredentialAttachedToCluster(TestContext testContext) {
        testContext.given(StackTestDto.class)
                .withName(testContext.get(StackTestDto.class).getName())
                .when(stackTestClient.getV4())
                .then(EnvironmentClusterTest::checkNewCredentialInStack)
                .validate();
    }

    private Set<String> getProxyAsList(TestContext testContext) {
        return new HashSet<>(Collections.singletonList(testContext.get(ProxyTestDto.class).getName()));
    }

    private Set<String> getLdapAsList(TestContext testContext) {
        return new HashSet<>(Collections.singletonList(testContext.get(LdapTestDto.class).getName()));
    }

    private ClusterTestDto setResources(TestContext testContext, String rdsName, String ldapName, String proxyName) {
        ClusterTestDto cluster = testContext.given(ClusterTestDto.class)
                .valid();
        if (rdsName != null) {
            Set<String> rdsSet = new LinkedHashSet<>();
            rdsSet.add(rdsName);
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

<<<<<<< HEAD
    static EnvironmentTestDto checkEnvHasNoLdap(TestContext testContext, EnvironmentTestDto environment, CloudbreakClient cloudbreakClient) {
        Set<LdapV4Response> ldapV4ResponseSet = environment.getResponse().getLdaps();
        if (!ldapV4ResponseSet.isEmpty()) {
            throw new TestFailException("Environment has attached ldap");
=======
    static EnvironmentTestDto checkEnvHasNoRds(TestContext testContext, EnvironmentTestDto environment, CloudbreakClient cloudbreakClient) {
        Set<DatabaseV4Response> rdsConfigResponseSet = environment.getResponse().getDatabases();
        if (!rdsConfigResponseSet.isEmpty()) {
            throw new TestFailException("Environment has attached rds");
>>>>>>> CB-1516 eliminate relation between Environment and LDAP
        }
        return environment;
    }

    private static EnvironmentTestDto checkEnvHasNoProxy(TestContext testContext, EnvironmentTestDto environment, CloudbreakClient cloudbreakClient) {
        Set<ProxyV4Response> proxyV4ResponseSet = environment.getResponse().getProxies();
        if (!proxyV4ResponseSet.isEmpty()) {
            throw new TestFailException("Environment has attached proxy");
        }
        return environment;
    }

    private static EnvironmentTestDto checkNewCredentialAttachedToEnv(
            TestContext testContext,
            EnvironmentTestDto environment, CloudbreakClient cloudbreakClient) {

        String credentialName = environment.getResponse().getCredentialName();
        if (!credentialName.equals(testContext.get(NEW_CREDENTIAL_KEY).getName())) {
            throw new TestFailException("Credential is not attached to environment");
        }
        return environment;
    }
}