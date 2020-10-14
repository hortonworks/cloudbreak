package com.sequenceiq.it.cloudbreak.testcase.authorization;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;

import javax.inject.Inject;
import javax.ws.rs.ForbiddenException;

import org.testng.annotations.Test;

import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;
import com.sequenceiq.it.cloudbreak.actor.Actor;
import com.sequenceiq.it.cloudbreak.client.CredentialTestClient;
import com.sequenceiq.it.cloudbreak.client.EnvironmentTestClient;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
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
import com.sequenceiq.it.cloudbreak.dto.util.RenewDatalakeCertificateTestDto;
import com.sequenceiq.it.cloudbreak.mock.freeipa.FreeIpaRouteHandler;
import com.sequenceiq.it.cloudbreak.testcase.AbstractIntegrationTest;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

public class DatalakeDatahubCreateAuthTest extends AbstractIntegrationTest {

    @Inject
    private EnvironmentTestClient environmentTestClient;

    @Inject
    private CredentialTestClient credentialTestClient;

    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private FreeIpaRouteHandler freeIpaRouteHandler;

    @Override
    protected void setupTest(TestContext testContext) {
        useRealUmsUser(testContext, AuthUserKeys.ACCOUNT_ADMIN);
        useRealUmsUser(testContext, AuthUserKeys.ENV_CREATOR_B);
        useRealUmsUser(testContext, AuthUserKeys.ENV_CREATOR_A);
        useRealUmsUser(testContext, AuthUserKeys.ZERO_RIGHTS);
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running env service",
            when = "valid create environment request is sent",
            then = "environment should be created but unauthorized users should not be able to access it")
    public void testCreateEnvironment(MockedTestContext testContext) {
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
                .withDatahubCreator()
                .when(environmentTestClient.assignResourceRole(AuthUserKeys.ENV_CREATOR_B))
                .withEnvironmentUser()
                .when(environmentTestClient.assignResourceRole(AuthUserKeys.ENV_CREATOR_B))
                .given(clouderaManager, ClouderaManagerTestDto.class)
                .given(cluster, ClusterTestDto.class)
                .withClouderaManager(clouderaManager)
                .given(stack, StackTestDto.class).withCluster(cluster)
                .withGatewayPort(testContext.getSparkServer().getPort())
                .given(sdxInternal, SdxInternalTestDto.class)
                .withStackRequest(key(cluster), key(stack))
                .when(sdxTestClient.createInternal(), key(sdxInternal))
                .awaitForFlow(key(sdxInternal))
                .await(SdxClusterStatusResponse.RUNNING)
                .when(sdxTestClient.detailedDescribeInternal(), RunningParameter.who(Actor.useRealUmsUser(AuthUserKeys.ENV_CREATOR_A)))
                .when(sdxTestClient.detailedDescribeInternal(), RunningParameter.who(Actor.useRealUmsUser(AuthUserKeys.ENV_CREATOR_B)))
                .when(sdxTestClient.detailedDescribeInternal(), RunningParameter.who(Actor.useRealUmsUser(AuthUserKeys.ZERO_RIGHTS)))
                .expect(ForbiddenException.class,
                        RunningParameter.expectedMessage("You have insufficient rights to perform the following action[(]s[)]: " +
                                "'datalake/describeDetailedDatalake'.*")
                                .withKey("SdxDetailedDescribeInternalAction"))
                .given(RenewDatalakeCertificateTestDto.class)
                .withStackCrn(testContext.get(sdxInternal).getCrn())
                .when(sdxTestClient.renewDatalakeCertificateV4(), RunningParameter.who(Actor.useRealUmsUser(AuthUserKeys.ZERO_RIGHTS)))
                .expect(ForbiddenException.class,
                        RunningParameter.expectedMessage("You have insufficient rights to perform the following action[(]s[)]: 'datalake/repairDatalake'.*")
                                .withKey("RenewDatalakeCertificateAction"))
                .validate();
    }

}