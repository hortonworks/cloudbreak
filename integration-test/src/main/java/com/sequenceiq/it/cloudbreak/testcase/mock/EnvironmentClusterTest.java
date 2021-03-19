package com.sequenceiq.it.cloudbreak.testcase.mock;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.expectedMessage;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;

import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.EnvironmentClient;
import com.sequenceiq.it.cloudbreak.client.CredentialTestClient;
import com.sequenceiq.it.cloudbreak.client.EnvironmentTestClient;
import com.sequenceiq.it.cloudbreak.client.LdapTestClient;
import com.sequenceiq.it.cloudbreak.client.ProxyTestClient;
import com.sequenceiq.it.cloudbreak.client.StackTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.context.RunningParameter;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.credential.CredentialTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.stack.StackTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;

public class EnvironmentClusterTest extends AbstractMockTest {

    private static final String NEW_CREDENTIAL_KEY = "newCred";

    private static final String FORBIDDEN_KEY = "forbiddenPost";

    @Inject
    private LdapTestClient ldapTestClient;

    @Inject
    private StackTestClient stackTestClient;

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

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is an environment with a cluster in it",
            when = "calling create cluster with a different cluster name",
            then = "the second cluster should be created")
    public void testSameEnvironmentWithDifferentClusters(MockedTestContext testContext) {
        createDefaultEnvironment(testContext);

        String newStack = resourcePropertyProvider().getName();
        testContext
                .given(StackTestDto.class)
                .withEnvironmentClass(EnvironmentTestDto.class)
                .when(stackTestClient.createV4())
                .await(STACK_AVAILABLE)
                .given(newStack, StackTestDto.class)
                .when(stackTestClient.createV4(), RunningParameter.key(newStack))
                .await(STACK_AVAILABLE, RunningParameter.key(newStack))
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
                .withEnvironmentClass(EnvironmentTestDto.class)
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
    public void testClusterWithEmptyEnvironmentRequest(MockedTestContext testContext) {
        testContext
                .given(EnvironmentTestDto.class)
                .when(environmentTestClient.create())
                .when(environmentTestClient.describe())
                .given(StackTestDto.class)
                .withEnvironmentCrn("")
                .whenException(stackTestClient.createV4(), BadRequestException.class, expectedMessage("Environment CRN cannot be null or empty."))
                .validate();
    }

    private void createEnvWithResources(MockedTestContext testContext) {
        testContext.given(EnvironmentTestDto.class)
                .when(environmentTestClient.create())
                .when(environmentTestClient.describe());
    }

    private void checkCredentialAttachedToCluster(MockedTestContext testContext) {
        testContext.given(StackTestDto.class)
                .withName(testContext.get(StackTestDto.class).getName())
                .when(stackTestClient.getV4())
                .then(EnvironmentClusterTest::checkNewCredentialInStack)
                .validate();
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