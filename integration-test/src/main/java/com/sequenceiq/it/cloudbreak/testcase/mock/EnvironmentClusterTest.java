package com.sequenceiq.it.cloudbreak.testcase.mock;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.expectedMessage;

import jakarta.inject.Inject;

import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.client.CredentialTestClient;
import com.sequenceiq.it.cloudbreak.client.DistroXTestClient;
import com.sequenceiq.it.cloudbreak.client.EnvironmentTestClient;
import com.sequenceiq.it.cloudbreak.client.FreeIpaTestClient;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.context.RunningParameter;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.credential.CredentialTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.microservice.EnvironmentClient;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

public class EnvironmentClusterTest extends AbstractMockTest {

    private static final String NEW_CREDENTIAL_KEY = "newCred";

    @Inject
    private EnvironmentTestClient environmentTestClient;

    @Inject
    private CredentialTestClient credentialTestClient;

    @Inject
    private FreeIpaTestClient freeIpaTestClient;

    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private DistroXTestClient distroXTestClient;

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
                .given(FreeIpaTestDto.class)
                .when(freeIpaTestClient.create())
                .awaitForCreationFlow()
                .given(SdxInternalTestDto.class)
                .when(sdxTestClient.createInternal())
                .awaitForFlow()
                .given(DistroXTestDto.class)
                .when(distroXTestClient.create())
                .awaitForFlow()
                .given(newStack, DistroXTestDto.class)
                .when(distroXTestClient.create(), RunningParameter.key(newStack))
                .awaitForFlow(RunningParameter.key(newStack))
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
                .given(FreeIpaTestDto.class)
                .when(freeIpaTestClient.create())
                .awaitForCreationFlow()
                .given(SdxInternalTestDto.class)
                .when(sdxTestClient.createInternal())
                .awaitForFlow()
                .given(DistroXTestDto.class)
                .when(distroXTestClient.create())
                .awaitForFlow()
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
                .withCreateFreeIpa(Boolean.FALSE)
                .when(environmentTestClient.create())
                .when(environmentTestClient.describe())
                .given(FreeIpaTestDto.class)
                .withEnvironment()
                .when(freeIpaTestClient.create())
                .awaitForCreationFlow()
                .given(SdxInternalTestDto.class)
                .withEnvironment()
                .when(sdxTestClient.createInternal())
                .await(SdxClusterStatusResponse.RUNNING)
                .awaitForHealthyInstances()
                .given(DistroXTestDto.class)
                .withEnvironmentName("")
                .whenException(distroXTestClient.create(), TestFailException.class, expectedMessage("Env name cannot be null"))
                .validate();
    }

    private void checkCredentialAttachedToCluster(MockedTestContext testContext) {
        testContext.given(DistroXTestDto.class)
                .withName(testContext.get(DistroXTestDto.class).getName())
                .when(distroXTestClient.get())
                .then(EnvironmentClusterTest::checkNewCredentialInStack)
                .validate();
    }

    private static DistroXTestDto checkNewCredentialInStack(TestContext testContext, DistroXTestDto stack, CloudbreakClient client) {
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
