package com.sequenceiq.it.cloudbreak.testcase.authorization;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.expectedMessage;
import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;
import static com.sequenceiq.it.cloudbreak.util.AuthorizationTestUtil.datalakePattern;
import static com.sequenceiq.it.cloudbreak.util.AuthorizationTestUtil.environmentDatalakePattern;

import jakarta.inject.Inject;
import jakarta.ws.rs.ForbiddenException;

import org.testng.annotations.Test;

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
import com.sequenceiq.it.cloudbreak.dto.util.RenewDatalakeCertificateTestDto;
import com.sequenceiq.it.cloudbreak.testcase.AbstractIntegrationTest;
import com.sequenceiq.it.cloudbreak.util.ResourceCreator;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

public class DatalakeDatahubCreateAuthTest extends AbstractIntegrationTest {

    @Inject
    private EnvironmentTestClient environmentTestClient;

    @Inject
    private CredentialTestClient credentialTestClient;

    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private UmsTestClient umsTestClient;

    @Inject
    private ResourceCreator resourceCreator;

    @Override
    protected void setupTest(TestContext testContext) {
        testContext.getTestUsers().setSelector(TestUserSelectors.UMS_ONLY);
        testContext.as(AuthUserKeys.ACCOUNT_ADMIN);
        testContext.as(AuthUserKeys.ENV_CREATOR_B);
        testContext.as(AuthUserKeys.ENV_CREATOR_A);
        testContext.as(AuthUserKeys.ZERO_RIGHTS);
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
                    .withDatahubCreator()
                .when(umsTestClient.assignResourceRole(AuthUserKeys.ENV_CREATOR_B))
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
                .when(sdxTestClient.detailedDescribeInternal(), RunningParameter.who(testContext.getTestUsers().getUserByLabel(AuthUserKeys.ENV_CREATOR_A)))
                .when(sdxTestClient.detailedDescribeInternal(), RunningParameter.who(testContext.getTestUsers().getUserByLabel(AuthUserKeys.ENV_CREATOR_B)))
                .whenException(sdxTestClient.detailedDescribeInternal(), ForbiddenException.class, expectedMessage("Doesn't have " +
                        "'datalake/describeDetailedDatalake' right on any of the environment[(]s[)] " + environmentDatalakePattern(testContext) +
                        " or on " + datalakePattern(testContext.get(sdxInternal).getName()))
                        .withWho(testContext.getTestUsers().getUserByLabel(AuthUserKeys.ZERO_RIGHTS)))
                .validate();

        testContext
                .given(RenewDatalakeCertificateTestDto.class)
                    .withStackCrn(testContext.get(sdxInternal).getCrn())
                .whenException(sdxTestClient.renewDatalakeCertificateV4(), ForbiddenException.class, expectedMessage("Doesn't have 'datalake/repairDatalake'" +
                        " right on any of the environment[(]s[)] " + environmentDatalakePattern(testContext) + " or on " +
                        datalakePattern(testContext.get(sdxInternal).getName()))
                        .withWho(testContext.getTestUsers().getUserByLabel(AuthUserKeys.ZERO_RIGHTS)))
                .validate();
    }

}
