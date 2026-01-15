package com.sequenceiq.it.cloudbreak.testcase.mock;

import static java.util.Objects.isNull;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;

import org.testng.annotations.Test;

import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentBaseResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;
import com.sequenceiq.environment.api.v1.environment.model.response.SimpleEnvironmentResponse;
import com.sequenceiq.it.cloudbreak.assertion.Assertion;
import com.sequenceiq.it.cloudbreak.client.EnvironmentTestClient;
import com.sequenceiq.it.cloudbreak.client.FreeIpaTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.context.RunningParameter;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.microservice.EnvironmentClient;

public class EnvironmentChildTest extends AbstractMockTest {

    private static final String PARENT_ENVIRONMENT = "parent";

    private static final String CHILD_ENVIRONMENT = "child";

    @Inject
    private EnvironmentTestClient environmentTestClient;

    @Inject
    private FreeIpaTestClient freeIpaTestClient;

    @Override
    protected void setupTest(TestContext testContext) {
        createDefaultUser(testContext);
        initializeDefaultBlueprints(testContext);
        createDefaultCredential(testContext);
        createDefaultEnvironment(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is an available parent environment",
            when = "valid create child environment request is sent",
            then = "environment should be created and parent environment should be referenced in the child environment")
    public void testCreateChildEnvironment(MockedTestContext testContext) {
        testContext
                .given(FreeIpaTestDto.class)
                .when(freeIpaTestClient.create())
                .enableVerification()
                .awaitForCreationFlow()
                .given(EnvironmentTestDto.class)
                .when(environmentTestClient.list())
                .then(this::checkEnvIsListedByNameAndParentName)
                .given(CHILD_ENVIRONMENT, EnvironmentTestDto.class)
                .withParentEnvironment()
                .when(environmentTestClient.create())
                .awaitForCreationFlow()
                .when(environmentTestClient.list())
                .then(this::checkEnvIsListedByNameAndParentName)
                .given(FreeIpaTestDto.class)
                .mockFreeIpa().session().post().bodyContains("dnszone_add", 1).times(1).verify()
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is an available child environment with a referenced available parent environment",
            when = "child create request is sent but parent environment is a child environment",
            then = "a BadRequestException should be returned")
    public void testCreateChildEnvironmentWhereParentIsAChild(MockedTestContext testContext) {
        testContext
                .given(CHILD_ENVIRONMENT, EnvironmentTestDto.class)
                .withParentEnvironment()
                .when(environmentTestClient.create())
                .awaitForCreationFlow()
                .given(EnvironmentTestDto.class)
                .withParentEnvironment()
                .whenException(environmentTestClient.create(), BadRequestException.class)
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running cloudbreak",
            when = "child create request is sent but parent is not created yet",
            then = "a BadRequestException should be returned")
    public void testCreateChildWithoutParentEnvironment(MockedTestContext testContext) {
        testContext
                .given(PARENT_ENVIRONMENT, EnvironmentTestDto.class)
                .given(CHILD_ENVIRONMENT, EnvironmentTestDto.class)
                .withParentEnvironment(RunningParameter.key(PARENT_ENVIRONMENT))
                .whenException(environmentTestClient.create(), BadRequestException.class)
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is an available child environment with a referenced available parent environment",
            when = "a delete request is sent for the parent environment without cascading",
            then = "a BadRequestException should be returned")
    public void testDeleteParentEnvironmentWithExistingChild(MockedTestContext testContext) {
        testContext
                .given(CHILD_ENVIRONMENT, EnvironmentTestDto.class)
                .withParentEnvironment()
                .when(environmentTestClient.create())
                .awaitForCreationFlow()
                .given(EnvironmentTestDto.class)
                .whenException(environmentTestClient.deleteByName(false), BadRequestException.class)
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is an available child environment with a referenced available parent environment",
            when = "a delete request is sent for the child environment",
            then = "the child environment should be deleted",
            and = "dns zone should be deleted")
    public void testDeleteChildEnvironment(MockedTestContext testContext) {
        testContext
                .given(FreeIpaTestDto.class)
                .when(freeIpaTestClient.create())
                .enableVerification()
                .awaitForCreationFlow()
                .given(CHILD_ENVIRONMENT, EnvironmentTestDto.class)
                .withParentEnvironment()
                .when(environmentTestClient.create())
                .awaitForCreationFlow()
                .when(environmentTestClient.deleteByName())
                .await(EnvironmentStatus.ARCHIVED)
                .when(environmentTestClient.list())
                .then(this::checkEnvIsNotListedByNameAndParentName)
                .given(FreeIpaTestDto.class)
                .mockFreeIpa().session().post().bodyContains("dnszone_del", 1).times(1).verify()
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is an available child environment with a referenced available parent environment",
            when = "a delete multiple request is sent for both environments",
            then = "the child and parent environments should be deleted")
    public void testDeleteChildAndParentEnvironment(MockedTestContext testContext) {
        String parentEnvName = testContext.get(EnvironmentTestDto.class).getName();
        testContext
                .given(EnvironmentTestDto.class)
                .when(environmentTestClient.list())
                .then(this::checkEnvIsListedByNameAndParentName)
                .given(CHILD_ENVIRONMENT, EnvironmentTestDto.class)
                .withParentEnvironment()
                .when(environmentTestClient.create(), RunningParameter.key(CHILD_ENVIRONMENT))
                .await(EnvironmentStatus.AVAILABLE, RunningParameter.key(CHILD_ENVIRONMENT))
                .when(environmentTestClient.describe(), RunningParameter.key(CHILD_ENVIRONMENT))
                .when(environmentTestClient.list())
                .then(this::checkEnvIsListedByNameAndParentName)
                .when(environmentTestClient.deleteMultipleByNames(parentEnvName, testContext.get(CHILD_ENVIRONMENT).getName()))
                .await(EnvironmentStatus.ARCHIVED, RunningParameter.key(CHILD_ENVIRONMENT))
                .given(EnvironmentTestDto.class)
                .await(EnvironmentStatus.ARCHIVED)
                .when(environmentTestClient.list())
                .then(checkEnvsAreNotListedByName(List.of(parentEnvName, testContext.get(CHILD_ENVIRONMENT).getName())))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is an available child environment with a referenced available parent environment",
            when = "a delete request is sent for the child environment",
            then = "the child environment should be deleted, but dns zone should not be removed")
    public void testDeleteChildEnvironmentThatHasSibling(MockedTestContext testContext) {
        testContext
                .given(FreeIpaTestDto.class)
                .when(freeIpaTestClient.create())
                .enableVerification()
                .awaitForCreationFlow()
                .given(CHILD_ENVIRONMENT, EnvironmentTestDto.class)
                .withParentEnvironment()
                .when(environmentTestClient.create())
                .awaitForCreationFlow()
                .given("child2", EnvironmentTestDto.class)
                .withParentEnvironment()
                .when(environmentTestClient.create())
                .awaitForCreationFlow()
                .when(environmentTestClient.deleteByName())
                .await(EnvironmentStatus.ARCHIVED)
                .when(environmentTestClient.list())
                .then(this::checkEnvIsNotListedByNameAndParentName)
                .given(FreeIpaTestDto.class)
                .mockFreeIpa().session().post().bodyContains("dnszone_del", 0).verify()
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

    private Assertion<EnvironmentTestDto, EnvironmentClient> checkEnvsAreNotListedByName(List<String> environmentNames) {
        return (testContext, environmentTestDto, environmentClient) -> {
            Collection<SimpleEnvironmentResponse> simpleEnvironmentV4Respons = environmentTestDto.getResponseSimpleEnvSet();
            if (isNull(simpleEnvironmentV4Respons)) {
                throw new TestFailException("Environment list response is missing.");
            }
            List<String> listedEnvironmentNames = simpleEnvironmentV4Respons.stream()
                    .filter(response -> !response.getEnvironmentStatus().equals(EnvironmentStatus.ARCHIVED))
                    .map(EnvironmentBaseResponse::getName)
                    .collect(Collectors.toList());
            environmentNames.forEach(environmentName -> {
                if (listedEnvironmentNames.contains(environmentName)) {
                    throw new TestFailException(String.format("'%s' environment has not been deleted.", environmentName));
                }
            });
            return environmentTestDto;
        };
    }
}
