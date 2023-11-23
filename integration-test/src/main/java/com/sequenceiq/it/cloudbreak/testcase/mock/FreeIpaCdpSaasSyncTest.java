package com.sequenceiq.it.cloudbreak.testcase.mock;

import static java.lang.String.format;

import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.testng.annotations.Test;

import com.cloudera.thunderhead.service.authdistributor.AuthDistributorProto.UserState;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationState;
import com.sequenceiq.it.cloudbreak.assertion.Assertion;
import com.sequenceiq.it.cloudbreak.client.FreeIpaTestClient;
import com.sequenceiq.it.cloudbreak.config.user.TestUserCreator;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.authdistributor.FetchAuthViewTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaUserSyncTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.microservice.AuthDistributorClient;
import com.sequenceiq.it.cloudbreak.microservice.FreeIpaClient;

public class FreeIpaCdpSaasSyncTest extends AbstractMockTest {

    @Inject
    private FreeIpaTestClient freeIpaTestClient;

    @Inject
    private TestUserCreator testUserCreator;

    @Override
    protected void setupTest(TestContext testContext) {
        testContext.as(testUserCreator.create("CDP_SAAS_TENANT", "cdpsaasuser@cloudera.com"));
        createDefaultCredential(testContext);
        createDefaultEnvironment(testContext);
        createDefaultImageCatalog(testContext);
        initializeDefaultBlueprints(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "environment is present",
            when = "calling a freeipe start",
            then = "freeipa sould be available")
    public void testSyncFreeIpaCdpSaasWithInternalActor(MockedTestContext testContext) {
        FreeIpaUserSyncTestDto freeIpaUserSyncTestDto = testContext
                .given(FreeIpaTestDto.class)
                .when(freeIpaTestClient.create())
                .await(Status.AVAILABLE)
                .given(FreeIpaUserSyncTestDto.class)
                .when(freeIpaTestClient.getLastSyncOperationStatus())
                .await(OperationState.COMPLETED);
        String environmentCrn = freeIpaUserSyncTestDto.getEnvironmentCrn();
        testContext.given(FetchAuthViewTestDto.class)
                .withEnvironmentCrn(environmentCrn)
                .then(validateUpdatedAuthView(true))
                .given(FreeIpaUserSyncTestDto.class)
                .when(freeIpaTestClient.syncAllInternal())
                .await(OperationState.COMPLETED)
                .given(FreeIpaTestDto.class)
                .when(freeIpaTestClient.delete())
                .await(Status.DELETE_COMPLETED)
                .given(FetchAuthViewTestDto.class)
                .withEnvironmentCrn(environmentCrn)
                .then(validateUpdatedAuthView(false))
                .given(FreeIpaUserSyncTestDto.class)
                .withUsers(Set.of("user1"))
                .when(freeIpaTestClient.syncAllInternal())
                .then(validatePartialUserSyncFailed())
                .validate();
    }

    public static Assertion<FreeIpaUserSyncTestDto, FreeIpaClient> validatePartialUserSyncFailed() {
        return (testContext, freeIpaUserSyncTestDto, freeIpaClient) -> {
            String errorMessage = freeIpaUserSyncTestDto.getErrorMessage();
            if (StringUtils.isBlank(errorMessage)) {
                throw new TestFailException("Partial user sync should fail if CDP_SAAS entitlement is enabled!");
            }
            return freeIpaUserSyncTestDto;
        };
    }

    public static Assertion<FetchAuthViewTestDto, AuthDistributorClient> validateUpdatedAuthView(boolean exists) {
        return (testContext, fetchAuthViewTestDto, authDistributorClient) -> {
            String environmentCrn = fetchAuthViewTestDto.getRequest().getEnvironmentCrn();
            Optional<UserState> userState = authDistributorClient.getDefaultClient().fetchAuthViewForEnvironment(environmentCrn);

            if (exists && userState.isEmpty()) {
                throw new TestFailException(format("User state is not exists for environment '%s' ", environmentCrn));
            }
            if (!exists && userState.isPresent()) {
                throw new TestFailException(format("User state exists for environment '%s' ", environmentCrn));
            }
            return fetchAuthViewTestDto;
        };
    }
}
