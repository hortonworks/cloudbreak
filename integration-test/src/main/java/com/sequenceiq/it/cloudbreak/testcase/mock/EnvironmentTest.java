package com.sequenceiq.it.cloudbreak.testcase.mock;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.requests.RecipeV4Type.PRE_SERVICE_DEPLOYMENT;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.requests.RecipeV4Type.PRE_TERMINATION;
import static com.sequenceiq.it.cloudbreak.context.RunningParameter.expectedMessage;
import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;

import org.apache.commons.lang3.StringUtils;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareCrnGenerator;
import com.sequenceiq.cloudbreak.auth.crn.TestCrnGenerator;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;
import com.sequenceiq.environment.api.v1.environment.model.response.SimpleEnvironmentResponse;
import com.sequenceiq.it.cloudbreak.client.CredentialTestClient;
import com.sequenceiq.it.cloudbreak.client.EnvironmentTestClient;
import com.sequenceiq.it.cloudbreak.client.FreeIpaTestClient;
import com.sequenceiq.it.cloudbreak.client.RecipeTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.credential.CredentialTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.dto.recipe.RecipeTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.microservice.EnvironmentClient;
import com.sequenceiq.it.cloudbreak.mock.ImageCatalogMockServerSetup;
import com.sequenceiq.it.cloudbreak.util.RecipeUtil;

public class EnvironmentTest extends AbstractMockTest {

    private static final Set<String> INVALID_REGION = new HashSet<>(Collections.singletonList("MockRegion"));

    @Inject
    private EnvironmentTestClient environmentTestClient;

    @Inject
    private CredentialTestClient credentialTestClient;

    @Inject
    private RecipeTestClient recipeTestClient;

    @Inject
    private FreeIpaTestClient freeIpaTestClient;

    @Inject
    private RecipeUtil recipeUtil;

    @Inject
    private RegionAwareCrnGenerator regionAwareCrnGenerator;

    @Inject
    private ImageCatalogMockServerSetup imageCatalogMockServerSetup;

    @Override
    protected void setupTest(TestContext testContext) {
        createDefaultUser(testContext);
        initializeDefaultBlueprints(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running cloudbreak",
            when = "valid create environment request is sent",
            then = "environment should be created")
    public void testCreateEnvironment(MockedTestContext testContext) {
        testContext
                .given(CredentialTestDto.class)
                .when(credentialTestClient.create())
                .given(EnvironmentTestDto.class)
                .when(environmentTestClient.create())
                .when(environmentTestClient.list())
                .then(this::checkEnvIsListed)
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running cloudbreak",
            when = "valid create environment request is sent with wrong docker registry config",
            then = "environment should be failed state")
    public void testCreateEnvironmentWithWrongDockerRegistryConfig(MockedTestContext testContext) {
        testContext
                .given(CredentialTestDto.class)
                .when(credentialTestClient.create())
                .given(EnvironmentTestDto.class)
                .withDockerRegistryConfig(regionAwareCrnGenerator.generateCrn(
                        CrnResourceDescriptor.COMPUTE_DOCKER_CONFIG,
                        "wrong-crn",
                        "cloudera").toString())
                .when(environmentTestClient.create())
                .await(EnvironmentStatus.CREATE_FAILED)
                .when(environmentTestClient.list())
                .then(this::checkEnvAboutDockerRegistryValidationFailure)
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running cloudbreak",
            when = "valid create environment request is sent with docker registry config",
            then = "environment should be valid state")
    public void testCreateEnvironmentWithValidDockerRegistryConfig(MockedTestContext testContext) {
        testContext
                .given(CredentialTestDto.class)
                .when(credentialTestClient.create())
                .given(EnvironmentTestDto.class)
                .withDockerRegistryConfig(regionAwareCrnGenerator.generateCrn(
                        CrnResourceDescriptor.COMPUTE_DOCKER_CONFIG,
                        "9d74eee4-1cad-45d7-b645-7csdfsdfsdfds",
                        "cloudera").toString())
                .when(environmentTestClient.create())
                .await(EnvironmentStatus.CREATE_FAILED)
                .when(environmentTestClient.list())
                .then(this::checkEnvAboutDockerRegistryValidationFailure)
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running cloudbreak",
            when = "a create environment request with reference to a non-existing credential is sent",
            then = "a BadRequestException should be returned")
    public void testCreateEnvironmentNotExistCredential(MockedTestContext testContext) {
        testContext
                .given(CredentialTestDto.class)
                .init(EnvironmentTestDto.class)
                .whenException(environmentTestClient.create(), BadRequestException.class)
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is an available environment",
            when = "a delete request is sent for the environment",
            then = "the environment should be deleted")
    public void testDeleteEnvironment(MockedTestContext testContext) {
        testContext
                .given(CredentialTestDto.class)
                .when(credentialTestClient.create())
                .init(EnvironmentTestDto.class)
                .when(environmentTestClient.create())
                .when(environmentTestClient.describe())
                .when(environmentTestClient.list())
                .then(this::checkEnvIsListed)
                .when(environmentTestClient.delete())
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is an available environment",
            when = "a get and delete request is sent for the environment with invalid crn",
            then = "requests should fail with validation error")
    public void testGetAndDeleteEnvironmentWithInvalidCrn(MockedTestContext testContext) {
        String invalidCrn = TestCrnGenerator.getDatalakeCrn("dl", "acc");
        String otherInvalidCrn = TestCrnGenerator.getDatahubCrn();
        testContext
                .given(CredentialTestDto.class)
                .when(credentialTestClient.create())
                .given(EnvironmentTestDto.class)
                .when(environmentTestClient.create())
                .awaitForCreationFlow()
                .getResponse().setCrn(invalidCrn);
        testContext
                .given(EnvironmentTestDto.class)
                .whenException(environmentTestClient.describeByCrn(), BadRequestException.class,
                        expectedMessage(".*Crn provided: " +
                                "crn:cdp:datalake:us-west-1:acc:datalake:dl has invalid resource type or service type. " +
                                "Accepted service type / resource type pairs: [(]environments,environment[)].*"))
                .whenException(environmentTestClient.deleteMultipleByCrns(invalidCrn, otherInvalidCrn), BadRequestException.class,
                        expectedMessage(".*Crns provided: \\[crn:cdp:datalake:us-west-1:acc:datalake:dl," +
                                "crn:cdp:datahub:us-west-1:acc:cluster:dh\\] have invalid resource type or service type. " +
                                "Accepted service type / resource type pairs: [(]environments,environment[)].*"))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running cloudbreak",
            when = "a delete request is sent for a non-existing environment",
            then = "a NotFoundException should be returned")
    public void testDeleteEnvironmentNotExist(MockedTestContext testContext) {
        testContext
                .given(CredentialTestDto.class)
                .init(EnvironmentTestDto.class)
                .whenException(environmentTestClient.deleteByName(), NotFoundException.class)
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is an environment resource created",
            when = "obtaining environment with internal actor ",
            then = "the environment response by crn should exist.")
    public void testWlClusterWithInternalGetRequest(MockedTestContext testContext) {
        testContext
                .given(CredentialTestDto.class)
                .when(credentialTestClient.create())
                .given(EnvironmentTestDto.class)
                .when(environmentTestClient.create())
                .when(environmentTestClient.get())
                .then(EnvironmentTest::checkEnvironmentCrnIsNotEmpty)
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is an environment resource created",
            when = "obtaining the environment with crn via name API",
            then = "invalid crn error occurred")
    public void testEnvCrnDescribeWithName(MockedTestContext testContext) {
        testContext
                .given(CredentialTestDto.class)
                .when(credentialTestClient.create())
                .given(EnvironmentTestDto.class)
                .when((testContext1, testDto, client) -> {
                    testDto.setResponse(
                            client.getInternalClient(testContext)
                                    .environmentV1Endpoint()
                                    .getByCrn("someName"));
                    return testDto;
                }, key("wrongCrn"))
                .expect(BadRequestException.class, expectedMessage("Invalid Crn was provided. 'someName' does not match the Crn pattern")
                        .withKey("wrongCrn"))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running cloudbreak",
            when = "valid create environment request is sent with freeipa recipe",
            then = "environment should be created with freeipa and recipes was passed")
    public void testCreateEnvironmentAndFreeIpaRecipesPassed(MockedTestContext testContext) {
        String preCMRecipeName = resourcePropertyProvider().getName();
        String preTerminationRecipeName = resourcePropertyProvider().getName();
        testContext
                .given(RecipeTestDto.class)
                    .withName(preCMRecipeName)
                    .withContent(recipeUtil.generatePreDeploymentRecipeContent(applicationContext))
                    .withRecipeType(PRE_SERVICE_DEPLOYMENT)
                .when(recipeTestClient.createV4())
                .given(RecipeTestDto.class)
                    .withName(preTerminationRecipeName)
                    .withContent(recipeUtil.generatePreTerminationRecipeContent(applicationContext))
                    .withRecipeType(PRE_TERMINATION)
                .when(recipeTestClient.createV4())
                .given(CredentialTestDto.class)
                .when(credentialTestClient.create())
                .given(EnvironmentTestDto.class)
                .withCreateFreeIpa(true)
                .withOneFreeIpaNode()
                .withFreeIpaRecipe(Set.of(preCMRecipeName, preTerminationRecipeName))
                .withFreeIpaImage(imageCatalogMockServerSetup.getFreeIpaImageCatalogUrl(), "f6e778fc-7f17-4535-9021-515351df3691")
                .when(environmentTestClient.create())
                .awaitForCreationFlow()
                .given(FreeIpaTestDto.class)
                .when(freeIpaTestClient.describe())
                .then((testContext1, testDto, client) -> {
                    if (!testDto.getResponse().getRecipes().containsAll(Set.of(preCMRecipeName, preTerminationRecipeName))) {
                        throw new TestFailException("Necessary recipes are not present on FreeIpa stack!");
                    }
                    return testDto;
                })
                .validate();
    }

    private EnvironmentTestDto checkEnvIsListed(TestContext testContext, EnvironmentTestDto environment, EnvironmentClient environmentClient) {
        Collection<SimpleEnvironmentResponse> simpleEnvironmentV4Respons = environment.getResponseSimpleEnvSet();
        List<SimpleEnvironmentResponse> result = simpleEnvironmentV4Respons.stream()
                .filter(env -> environment.getName().equals(env.getName()))
                .collect(Collectors.toList());
        if (result.isEmpty()) {
            throw new TestFailException("Environment is not listed");
        }
        return environment;
    }

    private EnvironmentTestDto checkEnvAboutDockerRegistryValidationFailure(TestContext testContext, EnvironmentTestDto environment,
        EnvironmentClient environmentClient) {
        Collection<SimpleEnvironmentResponse> simpleEnvironmentV4Respons = environment.getResponseSimpleEnvSet();
        List<SimpleEnvironmentResponse> result = simpleEnvironmentV4Respons.stream()
                .filter(env -> environment.getName().equals(env.getName()))
                .collect(Collectors.toList());
        if (result.isEmpty()) {
            throw new TestFailException("Environment is not listed");
        } else {
            String reason = result.get(0).getStatusReason();
            if (!reason.startsWith("The validation of the specified custom docker registry config with CRN")) {
                throw new TestFailException("Environment failed but not with Liftie validation as we expected. " +
                        "The error reason was " + reason);
            }
        }
        return environment;
    }

    private static EnvironmentTestDto checkEnvironmentCrnIsNotEmpty(TestContext testContext,
            EnvironmentTestDto testDto, EnvironmentClient client) {
        if (testDto.getResponse() == null) {
            throw new TestFailException("Environment response by internal actor cannot be empty.");
        }
        if (StringUtils.isBlank(testDto.getResponse().getCrn())) {
            throw new TestFailException("Environment resource crn in environment response " +
                    "by internal actor cannot be empty.");
        }
        return testDto;
    }
}
