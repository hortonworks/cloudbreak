package com.sequenceiq.it.cloudbreak.testcase.mock;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;

import org.testng.annotations.Test;

import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;
import com.sequenceiq.environment.api.v1.environment.model.response.SimpleEnvironmentResponse;
import com.sequenceiq.it.cloudbreak.EnvironmentClient;
import com.sequenceiq.it.cloudbreak.SdxClient;
import com.sequenceiq.it.cloudbreak.client.EnvironmentTestClient;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.testcase.AbstractIntegrationTest;

public class EnvironmentDeleteTest extends AbstractIntegrationTest {

    private static final Set<String> INVALID_REGION = new HashSet<>(Collections.singletonList("MockRegion"));

    @Inject
    private EnvironmentTestClient environmentTestClient;

    @Inject
    private SdxTestClient sdxTestClient;

    @Override
    protected void setupTest(TestContext testContext) {
        super.setupTest(testContext);
        createDatalake(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "an environment with an SDX",
            when = "an environment delete request is sent",
            then = "we should receive a bad request response")
    public void testDeleteEnvironmentWithSdx(MockedTestContext testContext) {
        testContext
                .given(EnvironmentTestDto.class)
                .then((testContext1, testDto, client) -> {
                    try {
                        environmentTestClient.delete(false, false).action(testContext, testDto, client);
                    } catch (BadRequestException expected) {
                        return testDto;
                    }
                    throw new TestFailException("Environment delete should have responded with bad request.");
                })
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "an environment with an SDX",
            when = "an environment delete request with cascade is sent",
            then = "environment should be deleted")
    public void testCascadingDeleteEnvironmentWithSdx(MockedTestContext testContext) {
        testContext
                .given(EnvironmentTestDto.class)
                .when(environmentTestClient.delete(true, false))
                .await(EnvironmentStatus.ARCHIVED)
                .when(environmentTestClient.list())
                .then(this::checkEnvIsNotListed)
                .given(SdxInternalTestDto.class)
                .then(this::checkSdxIsNotListed)
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "an environment with an SDX",
            when = "an environment delete request with force is sent",
            then = "we should receive a bad request response")
    public void testForceDeleteEnvironmentWithSdx(MockedTestContext testContext) {
        testContext
                .given(EnvironmentTestDto.class)
                .then((testContext1, testDto, client) -> {
                    try {
                        environmentTestClient.delete(false, true).action(testContext, testDto, client);
                    } catch (BadRequestException expected) {
                        return testDto;
                    }
                    throw new TestFailException("Environment delete should have responded with bad request.");
                })
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "an environment with an SDX",
            when = "an environment delete request with cascade and force is sent",
            then = "we should receive a bad request response")
    public void testCascadeAndForceDeleteEnvironmentWithSdx(MockedTestContext testContext) {
        testContext
                .given(EnvironmentTestDto.class)
                .when(environmentTestClient.delete(true, true))
                .await(EnvironmentStatus.ARCHIVED)
                .when(environmentTestClient.list())
                .then(this::checkEnvIsNotListed)
                .given(SdxInternalTestDto.class)
                .then(this::checkSdxIsNotListed)
                .validate();
    }

    private SdxInternalTestDto checkSdxIsNotListed(TestContext testContext, SdxInternalTestDto sdxInternalTestDto, SdxClient sdxClient) {
        boolean listed = true;
        try {
            sdxTestClient.describeInternal().action(testContext, sdxInternalTestDto, sdxClient);
        } catch (NotFoundException e) {
            listed = false;
        } catch (Exception e) {
            throw new TestFailException("Failed to describe SDX.", e);
        }
        if (listed) {
            throw new TestFailException("SDX is listed");
        }
        return sdxInternalTestDto;
    }

    private EnvironmentTestDto checkEnvIsNotListed(TestContext testContext, EnvironmentTestDto environment, EnvironmentClient environmentClient) {
        Collection<SimpleEnvironmentResponse> simpleEnvironmentV4Respons = environment.getResponseSimpleEnvSet();
        boolean envIsListed = simpleEnvironmentV4Respons.stream()
                .anyMatch(env -> environment.getName().equals(env.getName()));
        if (envIsListed) {
            throw new TestFailException("Environment is listed");
        }
        return environment;
    }
}
