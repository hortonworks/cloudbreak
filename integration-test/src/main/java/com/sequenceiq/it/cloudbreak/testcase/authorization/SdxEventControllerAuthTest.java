package com.sequenceiq.it.cloudbreak.testcase.authorization;

import static com.sequenceiq.cloudbreak.structuredevent.event.StructuredEventType.NOTIFICATION;
import static com.sequenceiq.it.cloudbreak.context.RunningParameter.expectedMessage;
import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;
import static com.sequenceiq.it.cloudbreak.util.AuthorizationTestUtil.environmentPattern;

import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.ForbiddenException;

import org.jetbrains.annotations.NotNull;
import org.testng.annotations.Test;

import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;
import com.sequenceiq.it.cloudbreak.SdxClient;
import com.sequenceiq.it.cloudbreak.actor.CloudbreakActor;
import com.sequenceiq.it.cloudbreak.assertion.Assertion;
import com.sequenceiq.it.cloudbreak.client.CredentialTestClient;
import com.sequenceiq.it.cloudbreak.client.EnvironmentTestClient;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.client.UmsTestClient;
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
import com.sequenceiq.it.cloudbreak.testcase.AbstractIntegrationTest;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

public class SdxEventControllerAuthTest extends AbstractIntegrationTest {
    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private UmsTestClient umsTestClient;

    @Inject
    private CloudbreakActor cloudbreakActor;

    @Inject
    private EnvironmentTestClient environmentTestClient;

    @Inject
    private CredentialTestClient credentialTestClient;

    @Override
    protected void setupTest(TestContext testContext) {
        useRealUmsUser(testContext, AuthUserKeys.ACCOUNT_ADMIN);
        useRealUmsUser(testContext, AuthUserKeys.ENV_CREATOR_B);
        useRealUmsUser(testContext, AuthUserKeys.ENV_CREATOR_A);
        useRealUmsUser(testContext, AuthUserKeys.ZERO_RIGHTS);
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

        useRealUmsUser(testContext, AuthUserKeys.ENV_CREATOR_A);
        createDefaultImageCatalog(testContext);
        testContext
                .given(CredentialTestDto.class)
                .when(credentialTestClient.create())
                .given(EnvironmentTestDto.class)
                .when(environmentTestClient.create())
                .await(EnvironmentStatus.AVAILABLE)
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
                .when(sdxTestClient.getAuditEvents(), RunningParameter.who(cloudbreakActor.useRealUmsUser(AuthUserKeys.ENV_CREATOR_A)))
                .when(sdxTestClient.getAuditEvents(), RunningParameter.who(cloudbreakActor.useRealUmsUser(AuthUserKeys.ENV_CREATOR_B)))
                .whenException(
                        sdxTestClient.getAuditEvents(), ForbiddenException.class, expectedMessage("Doesn't have 'environments/describeEnvironment' " +
                                "right on any of the environment[(]s[)] " + environmentPattern(testContext)
                ).withWho(cloudbreakActor.useRealUmsUser(AuthUserKeys.ZERO_RIGHTS)))
                .when(sdxTestClient.getDatalakeEventsZip(), RunningParameter.who(cloudbreakActor.useRealUmsUser(AuthUserKeys.ENV_CREATOR_A)))
                .then(checkZipEndpointStatusManually(200))
                .when(sdxTestClient.getDatalakeEventsZip(), RunningParameter.who(cloudbreakActor.useRealUmsUser(AuthUserKeys.ENV_CREATOR_B)))
                .then(checkZipEndpointStatusManually(200))
                // CB-16345 to fix http 500 in this case
                .when(sdxTestClient.getDatalakeEventsZip(), RunningParameter.who(cloudbreakActor.useRealUmsUser(AuthUserKeys.ZERO_RIGHTS)))
                .then(checkZipEndpointStatusManually(500))
                .validate();
    }

    @NotNull
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
