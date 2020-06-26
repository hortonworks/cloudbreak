package com.sequenceiq.it.cloudbreak.testcase.mock;

import static java.util.Objects.isNull;

import java.util.Collection;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;

import org.springframework.http.HttpMethod;
import org.testng.annotations.Test;

import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;
import com.sequenceiq.environment.api.v1.environment.model.response.SimpleEnvironmentResponse;
import com.sequenceiq.it.cloudbreak.EnvironmentClient;
import com.sequenceiq.it.cloudbreak.assertion.Assertion;
import com.sequenceiq.it.cloudbreak.assertion.MockVerification;
import com.sequenceiq.it.cloudbreak.client.EnvironmentTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.context.RunningParameter;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.mock.ITResponse;
import com.sequenceiq.it.cloudbreak.testcase.AbstractIntegrationTest;

public class EnvironmentChildTest extends AbstractIntegrationTest {

    private static final String PARENT_ENVIRONMENT = "parent";

    private static final String CHILD_ENVIRONMENT = "child";

    @Inject
    private EnvironmentTestClient environmentTestClient;

    @Override
    protected void setupTest(TestContext testContext) {
        createDefaultUser(testContext);
        initializeDefaultBlueprints(testContext);
        createDefaultCredential(testContext);
        createDefaultEnvironmentWithNetwork(testContext);
        createDefaultFreeIPA(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is an available parent environment",
            when = "valid create child environment request is sent",
            then = "environment should be created and parent environment should be referenced in the child environment")
    public void testCreateChildEnvironment(MockedTestContext testContext) {
        testContext
                .given(EnvironmentTestDto.class)
                .when(environmentTestClient.list())
                .then(this::checkEnvIsListedByNameAndParentName)
                .given(CHILD_ENVIRONMENT, EnvironmentTestDto.class)
                    .withParentEnvironment()
                .when(environmentTestClient.create())
                .await(EnvironmentStatus.AVAILABLE)
                .when(environmentTestClient.list())
                .then(this::checkEnvIsListedByNameAndParentName)
                .then(verifyFreeIpaRequest("dnszone_add", 1))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is an available child environment with a referenced available parent environment",
            when = "child create request is sent but parent environment is a child environment",
            then = "a BadRequestException should be returned")
    public void testCreateChildEnvironmentWhereParentIsAChild(TestContext testContext) {
        String forbiddenKey = resourcePropertyProvider().getName();
        testContext
                .given(CHILD_ENVIRONMENT, EnvironmentTestDto.class)
                    .withParentEnvironment()
                .when(environmentTestClient.create())
                .await(EnvironmentStatus.AVAILABLE)
                .given(EnvironmentTestDto.class)
                    .withParentEnvironment()
                .when(environmentTestClient.create(), RunningParameter.key(forbiddenKey))
                .expect(BadRequestException.class, RunningParameter.key(forbiddenKey))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running cloudbreak",
            when = "child create request is sent but parent is not created yet",
            then = "a BadRequestException should be returned")
    public void testCreateChildWithoutParentEnvironment(TestContext testContext) {
        String forbiddenKey = resourcePropertyProvider().getName();
        testContext
                .given(PARENT_ENVIRONMENT, EnvironmentTestDto.class)
                .given(CHILD_ENVIRONMENT, EnvironmentTestDto.class)
                    .withParentEnvironment(RunningParameter.key(PARENT_ENVIRONMENT))
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
                .given(CHILD_ENVIRONMENT, EnvironmentTestDto.class)
                    .withParentEnvironment()
                .when(environmentTestClient.create())
                .await(EnvironmentStatus.AVAILABLE)
                .given(EnvironmentTestDto.class)
                .when(environmentTestClient.deleteByName(), RunningParameter.key(forbiddenKey))
                .expect(BadRequestException.class, RunningParameter.key(forbiddenKey))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is an available child environment with a referenced available parent environment",
            when = "a delete request is sent for the child environment",
            then = "the child environment should be deleted",
            and = "dns zone should be deleted")
    public void testDeleteChildEnvironment(TestContext testContext) {
        testContext
                .given(CHILD_ENVIRONMENT, EnvironmentTestDto.class)
                    .withParentEnvironment()
                .when(environmentTestClient.create())
                .await(EnvironmentStatus.AVAILABLE)
                .when(environmentTestClient.deleteByName())
                .await(EnvironmentStatus.ARCHIVED)
                .when(environmentTestClient.list())
                .then(this::checkEnvIsNotListedByNameAndParentName)
                .then(verifyFreeIpaRequest("dnszone_del", 1))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is an available child environment with a referenced available parent environment",
            when = "a delete request is sent for the child environment",
            then = "the child environment should be deleted, but dns zone should not be removed")
    public void testDeleteChildEnvironmentThatHasSibling(TestContext testContext) {
        testContext
                .given(CHILD_ENVIRONMENT, EnvironmentTestDto.class)
                    .withParentEnvironment()
                .when(environmentTestClient.create())
                .await(EnvironmentStatus.AVAILABLE)
                .given("child2", EnvironmentTestDto.class)
                    .withParentEnvironment()
                .when(environmentTestClient.create())
                .await(EnvironmentStatus.AVAILABLE)
                .when(environmentTestClient.deleteByName())
                .await(EnvironmentStatus.ARCHIVED)
                .when(environmentTestClient.list())
                .then(this::checkEnvIsNotListedByNameAndParentName)
                .then(verifyFreeIpaRequest("dnszone_del", 0))
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

    @SuppressWarnings("unchecked")
    private Assertion<EnvironmentTestDto, EnvironmentClient> verifyFreeIpaRequest(String method, int times) {
        return (testContext1, testDto, client) ->
                testDto.then(
                        MockVerification.verify(HttpMethod.POST, ITResponse.FREEIPA_ROOT + "/session/json")
                                .bodyContains(method)
                                .exactTimes(times));
    }
}
