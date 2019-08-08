package com.sequenceiq.it.cloudbreak.testcase.mock.environment;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;

import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.EnvironmentClient;
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
import com.sequenceiq.it.cloudbreak.dto.credential.CredentialTestDto;
import com.sequenceiq.it.cloudbreak.dto.database.DatabaseTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.stack.StackTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.testcase.AbstractIntegrationTest;

public class EnvironmentClusterTest extends AbstractIntegrationTest {

    private static final String NEW_CREDENTIAL_KEY = "newCred";

    private static final String FORBIDDEN_KEY = "forbiddenPost";

    @Inject
    private LdapTestClient ldapTestClient;

    @Inject
    private StackTestClient stackTestClient;

    @Inject
    private DatabaseTestClient databaseTestClient;

    @Inject
    private EnvironmentTestClient environmentTestClient;

    @Inject
    private CredentialTestClient credentialTestClient;

    @Inject
    private ProxyTestClient proxyTestClient;

    @Override
    protected void setupTest(TestContext testContext) {
        createDefaultUser(testContext);
        createDefaultCredential(testContext);
        createDefaultImageCatalog(testContext);
        initializeDefaultBlueprints(testContext);
    }
/*
    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is an environment with a attached shared resources and a running cluster that is using these resources",
            when = "the resource delete endpoints and environment delete endpoints are called",
            then = "non of the operations should succeed")
    public void testCreateWlClusterDeleteFails(MockedTestContext testContext) {
        createEnvWithResources(testContext);
//        createDefaultRdsConfig(testContext);
//        createDefaultLdapConfig(testContext);
        createDefaultProxyConfig(testContext);

        testContext.given(StackTestDto.class)
                .withEnvironment(EnvironmentTestDto.class)
                .withCluster(
                        setResources(
                                testContext,
//                                testContext.get(DatabaseTestDto.class).getName(),
//                                testContext.get(LdapTestDto.class).getName(),
                                "",
                                "",
                                testContext.get(ProxyTestDto.class).getResponse().getCrn()
                        )
                )
                .when(stackTestClient.createV4())
                .await(STACK_AVAILABLE)

                .given(ProxyTestDto.class)
                .deleteGiven(ProxyTestDto.class, proxyTestClient.delete(), RunningParameter.key(FORBIDDEN_KEY))
//                .given(LdapTestDto.class)
//                .deleteGiven(LdapTestDto.class, ldapTestClient.deleteV4(), RunningParameter.key(FORBIDDEN_KEY))
//                .given(DatabaseTestDto.class)
//                .deleteGiven(DatabaseTestDto.class, databaseTestClient.deleteV4(), RunningParameter.key(FORBIDDEN_KEY))
                .given(CredentialTestDto.class)
                .deleteGiven(CredentialTestDto.class, credentialTestClient.delete(), RunningParameter.key(FORBIDDEN_KEY))
                .given(EnvironmentTestDto.class)
                .deleteGiven(EnvironmentTestDto.class, environmentTestClient.delete(), RunningParameter.key(FORBIDDEN_KEY))
                .validate();
    }
*/

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is an environment with a cluster in it",
            when = "calling create cluster with a different cluster name",
            then = "the second cluster should be created")
    public void testSameEnvironmentWithDifferentClusters(TestContext testContext) {
        createDefaultEnvironment(testContext);

        String newStack = resourcePropertyProvider().getName();
        testContext
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
                .when(environmentTestClient.create())
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
                .when(environmentTestClient.create())
                .when(environmentTestClient.describe())
                .given(StackTestDto.class)
                .withEnvironment(EnvironmentTestDto.class)
                .when(stackTestClient.createV4())
                .await(STACK_AVAILABLE)
                .given(NEW_CREDENTIAL_KEY, CredentialTestDto.class)
                .when(credentialTestClient.create())
                .given(EnvironmentTestDto.class)
                .withName(testContext.get(EnvironmentTestDto.class).getName())
                .withChangeCredentialName(testContext.get(NEW_CREDENTIAL_KEY).getName())
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
                .when(environmentTestClient.create())
                .when(environmentTestClient.describe())
                .given(StackTestDto.class)
                .withEnvironmentCrn("")
                .when(stackTestClient.createV4(), RunningParameter.key("badRequest"))
                .expect(BadRequestException.class, RunningParameter.key("badRequest")
                        .withExpectedMessage("1. Environment CRN cannot be null or empty."))
                .validate();
    }

    private void createEnvWithResources(TestContext testContext) {
        testContext.given(EnvironmentTestDto.class)
                .when(environmentTestClient.create())
                .when(environmentTestClient.describe());
    }

    private void checkCredentialAttachedToCluster(TestContext testContext) {
        testContext.given(StackTestDto.class)
                .withName(testContext.get(StackTestDto.class).getName())
                .when(stackTestClient.getV4())
                .then(EnvironmentClusterTest::checkNewCredentialInStack)
                .validate();
    }

    private ClusterTestDto setResources(TestContext testContext, String rdsName, String ldapName, String proxyName) {
        ClusterTestDto cluster = testContext.given(ClusterTestDto.class)
                .valid();
//        if (rdsName != null) {
//            Set<String> rdsSet = new LinkedHashSet<>();
//            rdsSet.add(rdsName);
//            cluster.withRdsConfigNames(rdsSet);
//        }
//        if (ldapName != null) {
//            cluster.withLdapConfigName(ldapName);
//        }
        if (rdsName != null) {
            cluster.withProxyConfigName(proxyName);
        }
        return cluster;
    }

    private static StackTestDto checkNewCredentialInStack(TestContext testContext, StackTestDto stack, CloudbreakClient client) {
        String credentialName = stack.getResponse().getCredentialName();
        if (!credentialName.equals(testContext.get(NEW_CREDENTIAL_KEY).getName())) {
            throw new TestFailException("Credential is not attached to cluster");
        }
        return stack;
    }

    private static EnvironmentTestDto checkNewCredentialAttachedToEnv(
            TestContext testContext,
            EnvironmentTestDto environment, EnvironmentClient client) {

        String credentialName = environment.getResponse().getCredential().getName();
        if (!credentialName.equals(testContext.get(NEW_CREDENTIAL_KEY).getName())) {
            throw new TestFailException("Credential is not attached to environment");
        }
        return environment;
    }
}