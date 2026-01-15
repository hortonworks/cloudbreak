package com.sequenceiq.it.cloudbreak.testcase.authorization;

import static com.sequenceiq.cloudbreak.structuredevent.event.StructuredEventType.NOTIFICATION;
import static com.sequenceiq.it.cloudbreak.context.RunningParameter.expectedMessage;
import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;
import static com.sequenceiq.it.cloudbreak.testcase.authorization.AuthUserKeys.ENV_CREATOR_A;
import static com.sequenceiq.it.cloudbreak.testcase.authorization.AuthUserKeys.ENV_CREATOR_B;
import static com.sequenceiq.it.cloudbreak.testcase.authorization.AuthUserKeys.ZERO_RIGHTS;
import static com.sequenceiq.it.cloudbreak.util.AuthorizationTestUtil.environmentPattern;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.ws.rs.ForbiddenException;

import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.actor.CloudbreakUser;
import com.sequenceiq.it.cloudbreak.assertion.Assertion;
import com.sequenceiq.it.cloudbreak.client.CredentialTestClient;
import com.sequenceiq.it.cloudbreak.client.EnvironmentTestClient;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.client.UmsTestClient;
import com.sequenceiq.it.cloudbreak.config.user.TestUserSelectors;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.context.RunningParameter;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.ClouderaManagerTestDto;
import com.sequenceiq.it.cloudbreak.dto.ClusterTestDto;
import com.sequenceiq.it.cloudbreak.dto.credential.CredentialTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.dto.stack.StackTestDto;
import com.sequenceiq.it.cloudbreak.dto.ums.UmsTestDto;
import com.sequenceiq.it.cloudbreak.dto.util.SdxEventTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.microservice.SdxClient;
import com.sequenceiq.it.cloudbreak.testcase.AbstractIntegrationTest;
import com.sequenceiq.it.cloudbreak.util.ResourceCreator;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

public class SdxEventControllerAuthTest extends AbstractIntegrationTest {
    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private UmsTestClient umsTestClient;

    @Inject
    private EnvironmentTestClient environmentTestClient;

    @Inject
    private CredentialTestClient credentialTestClient;

    @Inject
    private ResourceCreator resourceCreator;

    @Override
    protected void setupTest(TestContext testContext) {
        testContext.getTestUsers().setSelector(TestUserSelectors.UMS_ONLY);

        testContext.as(AuthUserKeys.ACCOUNT_ADMIN);
        testContext.as(AuthUserKeys.ENV_CREATOR_B);
        testContext.as(AuthUserKeys.ZERO_RIGHTS);
        testContext.as(ENV_CREATOR_A);
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running env service and sdx service",
            when = "valid create environment and create datalake requests sent",
            then = "environment/datalake should be created but unauthorized users should not be able to get events"
    )
    public void testGetSdxEvents(MockedTestContext testContext) {
        String sdxInternal = resourcePropertyProvider().getName();
        String stack = resourcePropertyProvider().getName();
        String clouderaManager = "cm";
        String cluster = "cmcluster";
        CloudbreakUser envCreatorB = testContext.getTestUsers().getUserByLabel(ENV_CREATOR_B);
        CloudbreakUser envCreatorA = testContext.getTestUsers().getUserByLabel(ENV_CREATOR_A);
        CloudbreakUser zeroRights = testContext.getTestUsers().getUserByLabel(ZERO_RIGHTS);

        testContext.as(AuthUserKeys.ENV_CREATOR_A);
        createDefaultImageCatalog(testContext);
        testContext
                .given(CredentialTestDto.class)
                .when(credentialTestClient.create())
                .given(EnvironmentTestDto.class)
                .when(environmentTestClient.create())
                .awaitForCreationFlow()
                .validate();

        EnvironmentTestDto environment = testContext.get(EnvironmentTestDto.class);
        resourceCreator.createNewFreeIpa(testContext, environment);

        testContext
                .given(UmsTestDto.class)
                .assignTarget(EnvironmentTestDto.class.getSimpleName())
                .withEnvironmentUser()
                .when(umsTestClient.assignResourceRole(AuthUserKeys.ENV_CREATOR_B))
                .given(clouderaManager, ClouderaManagerTestDto.class)
                .given(cluster, ClusterTestDto.class)
                .withClouderaManager(clouderaManager)
                .given(stack, StackTestDto.class).withCluster(cluster)
                .given(sdxInternal, SdxInternalTestDto.class)
                .withStackRequest(key(cluster), key(stack))
                .when(sdxTestClient.createInternal(), key(sdxInternal))
                .await(SdxClusterStatusResponse.RUNNING)
                .given(SdxEventTestDto.class)
                .withEnvironmentCrn(testContext.get(EnvironmentTestDto.class).getResponse().getCrn())
                .withTypes(List.of(NOTIFICATION))
                .withPage(0)
                .withSize(1)
                .when(sdxTestClient.getAuditEvents(), RunningParameter.who(envCreatorA))
                .when(sdxTestClient.getAuditEvents(), RunningParameter.who(envCreatorB))
                .whenException(
                        sdxTestClient.getAuditEvents(), ForbiddenException.class, expectedMessage("Doesn't have 'environments/describeEnvironment' " +
                                "right on environment " + environmentPattern(testContext)
                        ).withWho(zeroRights)
                )
                .when(sdxTestClient.getDatalakeEventsZip(), RunningParameter.who(envCreatorA))
                .then(checkZipEndpointStatusManually(200))
                .when(sdxTestClient.getDatalakeEventsZip(), RunningParameter.who(envCreatorB))
                .then(checkZipEndpointStatusManually(200))
                .when(sdxTestClient.getDatalakeEventsZip(), RunningParameter.who(zeroRights))
                .then(checkZipEndpointStatusManually(403))
                .validate();
    }

    private Assertion<SdxEventTestDto, SdxClient> checkZipEndpointStatusManually(int expectedStatus) {
        return (context, testDto, client) -> {
            if (!testDto.getZippedResponseStatus().isPresent()) {
                throw new TestFailException("There is no response status for getDatalakeEventsZip!");
            }
            if (testDto.getZippedResponseStatus().get() != expectedStatus) {
                throw new TestFailException(String.format("getDatalakeEventsZip call should have returned with %s but returned with %s",
                        expectedStatus, testDto.getZippedResponseStatus().get()));
            }
            return testDto;
        };
    }
}
