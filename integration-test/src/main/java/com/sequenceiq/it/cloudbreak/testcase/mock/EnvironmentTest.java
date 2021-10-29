package com.sequenceiq.it.cloudbreak.testcase.mock;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.expectedMessage;
import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;

import org.apache.commons.lang3.StringUtils;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.auth.crn.TestCrnGenerator;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;
import com.sequenceiq.environment.api.v1.environment.model.response.SimpleEnvironmentResponse;
import com.sequenceiq.it.cloudbreak.EnvironmentClient;
import com.sequenceiq.it.cloudbreak.client.CredentialTestClient;
import com.sequenceiq.it.cloudbreak.client.EnvironmentTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.credential.CredentialTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;

public class EnvironmentTest extends AbstractMockTest {

    private static final Set<String> INVALID_REGION = new HashSet<>(Collections.singletonList("MockRegion"));

    @Inject
    private EnvironmentTestClient environmentTestClient;

    @Inject
    private CredentialTestClient credentialTestClient;

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
                .await(EnvironmentStatus.AVAILABLE)
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