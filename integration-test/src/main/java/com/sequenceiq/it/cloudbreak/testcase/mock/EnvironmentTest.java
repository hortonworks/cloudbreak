package com.sequenceiq.it.cloudbreak.testcase.mock;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;

import org.apache.commons.lang3.StringUtils;
import org.testng.annotations.Test;

import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;
import com.sequenceiq.environment.api.v1.environment.model.response.SimpleEnvironmentResponse;
import com.sequenceiq.it.cloudbreak.EnvironmentClient;
import com.sequenceiq.it.cloudbreak.client.CredentialTestClient;
import com.sequenceiq.it.cloudbreak.client.EnvironmentTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.context.RunningParameter;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.credential.CredentialTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.testcase.AbstractIntegrationTest;

import static java.util.Objects.isNull;

public class EnvironmentTest extends AbstractIntegrationTest {

    private static final Set<String> INVALID_REGION = new HashSet<>(Collections.singletonList("MockRegion"));

    private static final String PARENT_ENVIRONMENT = "parent";

    private static final String CHILD_ENVIRONMENT = "child";

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
    public void testCreateEnvironment(TestContext testContext) {
        testContext
                .given(CredentialTestDto.class)
                .when(credentialTestClient.create())
                .given(EnvironmentTestDto.class)
                .when(environmentTestClient.create())
                .when(environmentTestClient.list())
                .then(this::checkEnvIsListedByNameAndParentName)
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running cloudbreak",
            when = "a create environment request is sent with an invalid region in it",
            then = "a BadRequestException should be returned")
    public void testCreateEnvironmentInvalidRegion(TestContext testContext) {
        String forbiddenKey = resourcePropertyProvider().getName();
        testContext
                .given(CredentialTestDto.class)
                .when(credentialTestClient.create())
                .init(EnvironmentTestDto.class)
                .withRegions(INVALID_REGION)
                .when(environmentTestClient.create(), RunningParameter.key(forbiddenKey))
                .expect(BadRequestException.class, RunningParameter.key(forbiddenKey))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running cloudbreak",
            when = "a create environment request is sent with no region in it",
            then = "a BadRequestException should be returned")
    public void testCreateEnvironmentNoRegion(TestContext testContext) {
        String forbiddenKey = resourcePropertyProvider().getName();
        testContext
                .given(CredentialTestDto.class)
                .when(credentialTestClient.create())
                .init(EnvironmentTestDto.class)
                .withRegions(null)
                .when(environmentTestClient.create(), RunningParameter.key(forbiddenKey))
                .expect(BadRequestException.class, RunningParameter.key(forbiddenKey))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running cloudbreak",
            when = "a create environment request with reference to a non-existing credential is sent",
            then = "a BadRequestException should be returned")
    public void testCreateEnvironmentNotExistCredential(TestContext testContext) {
        String forbiddenKey = resourcePropertyProvider().getName();
        testContext
                .given(CredentialTestDto.class)
                .init(EnvironmentTestDto.class)
                .when(environmentTestClient.create(), RunningParameter.key(forbiddenKey))
                .expect(BadRequestException.class, RunningParameter.key(forbiddenKey))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is an available environment",
            when = "a delete request is sent for the environment",
            then = "the environment should be deleted")
    public void testDeleteEnvironment(TestContext testContext) {
        testContext
                .given(CredentialTestDto.class)
                .when(credentialTestClient.create())
                .init(EnvironmentTestDto.class)
                .when(environmentTestClient.create())
                .when(environmentTestClient.describe())
                .when(environmentTestClient.list())
                .then(this::checkEnvIsListedByNameAndParentName)
                .when(environmentTestClient.delete())
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running cloudbreak",
            when = "a delete request is sent for a non-existing environment",
            then = "a NotFoundException should be returned")
    public void testDeleteEnvironmentNotExist(TestContext testContext) {
        String forbiddenKey = resourcePropertyProvider().getName();
        testContext
                .given(CredentialTestDto.class)
                .init(EnvironmentTestDto.class)
                .when(environmentTestClient.deleteByName(), RunningParameter.key(forbiddenKey))
                .expect(NotFoundException.class, RunningParameter.key(forbiddenKey))
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
                .when(environmentTestClient.getInternal())
                .then(EnvironmentTest::checkEnvironmentCrnIsNotEmpty)
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running cloudbreak",
            when = "valid create environment requests are sent to create parent environment first and then a child one",
            then = "environments should be created and parent environment should be referenced in the child environment")
    public void testCreateParentChildEnvironment(TestContext testContext) {
        testContext
                .given(CredentialTestDto.class)
                .when(credentialTestClient.create())
                .given(PARENT_ENVIRONMENT, EnvironmentTestDto.class)
                .when(environmentTestClient.create())
                .when(environmentTestClient.list())
                .then(this::checkEnvIsListedByNameAndParentName)
                .await(EnvironmentStatus.AVAILABLE)
                .given(CHILD_ENVIRONMENT, EnvironmentTestDto.class)
                .withParentEnvironmentName(testContext.get(PARENT_ENVIRONMENT).getName())
                .when(environmentTestClient.create())
                .when(environmentTestClient.list())
                .then(this::checkEnvIsListedByNameAndParentName)
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is an available child environment with a referenced available parent environment",
            when = "child create request is sent but parent environment is a child environment",
            then = "a BadRequestException should be returned")
    public void testCreateParentChildEnvironmentWhereParentIsAChild(TestContext testContext) {
        String forbiddenKey = resourcePropertyProvider().getName();
        testContext
                .given(CredentialTestDto.class)
                .when(credentialTestClient.create())
                .given(PARENT_ENVIRONMENT, EnvironmentTestDto.class)
                .when(environmentTestClient.create())
                .await(EnvironmentStatus.AVAILABLE)
                .given(CHILD_ENVIRONMENT, EnvironmentTestDto.class)
                .withParentEnvironmentName(testContext.get(PARENT_ENVIRONMENT).getName())
                .when(environmentTestClient.create())
                .await(EnvironmentStatus.AVAILABLE)
                .when(environmentTestClient.list())
                .given(EnvironmentTestDto.class)
                .withParentEnvironmentName(CHILD_ENVIRONMENT)
                .when(environmentTestClient.create(), RunningParameter.key(forbiddenKey))
                .expect(BadRequestException.class, RunningParameter.key(forbiddenKey))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running cloudbreak",
            when = "child create request is sent for a non-existing parent environment",
            then = "a BadRequestException should be returned")
    public void testCreateChildWithoutParentEnvironment(TestContext testContext) {
        String forbiddenKey = resourcePropertyProvider().getName();
        testContext
                .given(CredentialTestDto.class)
                .when(credentialTestClient.create())
                .given(EnvironmentTestDto.class)
                .withParentEnvironmentName("non existing parent name")
                .when(environmentTestClient.create(), RunningParameter.key(forbiddenKey))
                .expect(BadRequestException.class, RunningParameter.key(forbiddenKey))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is an available child environment with a referenced available parent environment",
            when = "a delete request is sent for the parent environment",
            then = "a BadRequestException should be returned")
    public void testDeleteParentEnvironmentWithExistingChild(TestContext testContext) {
        String forbiddenKey = resourcePropertyProvider().getName();
        testContext
                .given(CredentialTestDto.class)
                .when(credentialTestClient.create())
                .given(PARENT_ENVIRONMENT, EnvironmentTestDto.class)
                .when(environmentTestClient.create())
                .await(EnvironmentStatus.AVAILABLE)
                .given(CHILD_ENVIRONMENT, EnvironmentTestDto.class)
                .withParentEnvironmentName(testContext.get(PARENT_ENVIRONMENT).getName())
                .when(environmentTestClient.create())
                .await(EnvironmentStatus.AVAILABLE)
                .given(PARENT_ENVIRONMENT, EnvironmentTestDto.class)
                .when(environmentTestClient.deleteByName(), RunningParameter.key(forbiddenKey))
                .expect(BadRequestException.class, RunningParameter.key(forbiddenKey))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is an available child environment with a referenced available parent environment",
            when = "a delete request is sent for the child environment",
            then = "the child environment should be deleted")
    public void testDeleteChildEnvironment(TestContext testContext) {
        testContext
                .given(CredentialTestDto.class)
                .when(credentialTestClient.create())
                .given(PARENT_ENVIRONMENT, EnvironmentTestDto.class)
                .when(environmentTestClient.create())
                .await(EnvironmentStatus.AVAILABLE)
                .given(CHILD_ENVIRONMENT, EnvironmentTestDto.class)
                .withParentEnvironmentName(testContext.get(PARENT_ENVIRONMENT).getName())
                .when(environmentTestClient.create())
                .await(EnvironmentStatus.AVAILABLE)
                .when(environmentTestClient.deleteByName())
                .await(EnvironmentStatus.ARCHIVED)
                .when(environmentTestClient.list())
                .then(this::checkEnvIsNotListedByNameAndParentName)
                .validate();
    }

    private EnvironmentTestDto checkEnvIsListedByNameAndParentName(TestContext testContext,
            EnvironmentTestDto environment,
            EnvironmentClient environmentClient) {
        Collection<SimpleEnvironmentResponse> simpleEnvironmentV4Respons = environment.getResponseSimpleEnvSet();
        if (isNull(simpleEnvironmentV4Respons)) {
            throw new TestFailException("Environment list response is missing.");
        }
        boolean listed = simpleEnvironmentV4Respons.stream()
                .anyMatch(environmentResponse -> nameEquals(environment, environmentResponse) && parentNameEquals(environment, environmentResponse));
        if (!listed) {
            throw new TestFailException("Environment is not listed");
        }
        return environment;
    }

    private EnvironmentTestDto checkEnvIsNotListedByNameAndParentName(TestContext testContext,
            EnvironmentTestDto environment,
            EnvironmentClient environmentClient) {
        Collection<SimpleEnvironmentResponse> simpleEnvironmentV4Respons = environment.getResponseSimpleEnvSet();
        if (isNull(simpleEnvironmentV4Respons)) {
            throw new TestFailException("Environment list response is missing.");
        }
        boolean listed = simpleEnvironmentV4Respons.stream()
                .anyMatch(environmentResponse -> nameEquals(environment, environmentResponse) && parentNameEquals(environment, environmentResponse));
        if (listed) {
            throw new TestFailException("Environment is listed");
        }
        return environment;
    }

    private boolean nameEquals(EnvironmentTestDto environment, SimpleEnvironmentResponse environmentResponse) {
        return environment.getName().equals(environmentResponse.getName());
    }

    private boolean parentNameEquals(EnvironmentTestDto environment, SimpleEnvironmentResponse environmentResponse) {
        return isNull(environment.getParentEnvironmentName()) && isNull(environmentResponse.getParentEnvironmentName()) ||
                environment.getParentEnvironmentName().equals(environmentResponse.getParentEnvironmentName());
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