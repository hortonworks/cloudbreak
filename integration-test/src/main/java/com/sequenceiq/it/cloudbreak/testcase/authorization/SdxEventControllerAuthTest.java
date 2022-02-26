package com.sequenceiq.it.cloudbreak.testcase.authorization;

import static com.sequenceiq.cloudbreak.structuredevent.event.StructuredEventType.NOTIFICATION;
import static com.sequenceiq.it.cloudbreak.context.RunningParameter.expectedMessage;
import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;
import static com.sequenceiq.it.cloudbreak.util.AuthorizationTestUtil.datalakePattern;

import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.ForbiddenException;

import org.testng.annotations.Test;

import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;
import com.sequenceiq.it.cloudbreak.actor.CloudbreakActor;
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
import com.sequenceiq.it.cloudbreak.testcase.AbstractIntegrationTest;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

public class SdxEventControllerAuthTest extends AbstractIntegrationTest {
    private static final String CLOUDERA_MANAGER = "cm";

    private static final String CLUSTER_NAME = "cmcluster";

    private static final String ENVIRONMENT = "environment";

    private String sdxInternal;

    private String stack;

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
        sdxInternal = resourcePropertyProvider().getName();
        stack = resourcePropertyProvider().getName();

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
        useRealUmsUser(testContext, AuthUserKeys.ENV_CREATOR_A);
        createDefaultImageCatalog(testContext);
        testContext
                .given(CredentialTestDto.class)
                .when(credentialTestClient.create())
                .given(ENVIRONMENT, EnvironmentTestDto.class)
                .when(environmentTestClient.create())
                .await(EnvironmentStatus.AVAILABLE)
                .given(UmsTestDto.class)
                .assignTarget(EnvironmentTestDto.class.getSimpleName())
                .withEnvironmentUser()
                .when(umsTestClient.assignResourceRole(AuthUserKeys.ENV_CREATOR_B))
                .given(CLOUDERA_MANAGER, ClouderaManagerTestDto.class)
                .given(CLUSTER_NAME, ClusterTestDto.class)
                .withClouderaManager(CLOUDERA_MANAGER)
                .given(stack, StackTestDto.class).withCluster(CLUSTER_NAME)
                .given(sdxInternal, SdxInternalTestDto.class)
                .withStackRequest(key(CLUSTER_NAME), key(stack))
                .when(sdxTestClient.createInternal(), key(sdxInternal))
                .await(SdxClusterStatusResponse.RUNNING)
                .given(SdxEventTestDto.class)
                .withEnvironmentCrn(testContext.get(ENVIRONMENT).getCrn())
                .withTypes(List.of(NOTIFICATION))
                .withPage(0)
                .withSize(1)
                .when(sdxTestClient.getAuditEvents(), RunningParameter.who(cloudbreakActor.useRealUmsUser(AuthUserKeys.ENV_CREATOR_A)))
                .when(sdxTestClient.getAuditEvents(), RunningParameter.who(cloudbreakActor.useRealUmsUser(AuthUserKeys.ENV_CREATOR_B)))
                .whenException(
                        sdxTestClient.getAuditEvents(), ForbiddenException.class, expectedMessage(
                                "Doesn't have 'datalake/getAuditEvents' right on any of the environment[(]s[)] " +
                                "[\\[]crn: crn:cdp:environments:us-west-1:.*:environment:.*[]] or on " +
                                datalakePattern(testContext.get(sdxInternal).getName())
                        ).withWho(cloudbreakActor.useRealUmsUser(AuthUserKeys.ZERO_RIGHTS))
                ).when(sdxTestClient.getDatalakeEventsZip(), RunningParameter.who(cloudbreakActor.useRealUmsUser(AuthUserKeys.ENV_CREATOR_A)))
                .when(sdxTestClient.getDatalakeEventsZip(), RunningParameter.who(cloudbreakActor.useRealUmsUser(AuthUserKeys.ENV_CREATOR_B)))
                .whenException(
                        sdxTestClient.getDatalakeEventsZip(), ForbiddenException.class, expectedMessage(
                                "Doesn't have 'datalake/getDatalakeEventsZip' right on any of the environment[(]s[)] " +
                                        "[\\[]crn: crn:cdp:environments:us-west-1:.*:environment:.*[]] or on " +
                                        datalakePattern(testContext.get(sdxInternal).getName())
                        ).withWho(cloudbreakActor.useRealUmsUser(AuthUserKeys.ZERO_RIGHTS))
                ).validate();
    }
}
