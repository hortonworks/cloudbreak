package com.sequenceiq.it.cloudbreak.testcase.mock;

import javax.inject.Inject;

import org.testng.annotations.Test;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.UserSyncState;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationState;
import com.sequenceiq.it.cloudbreak.actor.Actor;
import com.sequenceiq.it.cloudbreak.client.FreeIpaTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaUserSyncTestDto;

public class FreeIpaSyncTest extends AbstractMockTest {

    @Inject
    private FreeIpaTestClient freeIpaTestClient;

    @Override
    protected void setupTest(TestContext testContext) {
        createDefaultUser(testContext);
        createDefaultCredential(testContext);
        createDefaultEnvironmentWithNetwork(testContext);
        createDefaultImageCatalog(testContext);
        initializeDefaultBlueprints(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "environment is present",
            when = "calling a freeipe start",
            then = "freeipa sould be available")
    public void testSyncFreeIpaWithInternalActor(MockedTestContext testContext) {
        testContext
                .given(FreeIpaTestDto.class).withCatalog(getImageCatalogMockServerSetup().getFreeIpaImageCatalogUrl())
                .when(freeIpaTestClient.create())
                .await(Status.AVAILABLE)
                .given(FreeIpaUserSyncTestDto.class)
                .await(UserSyncState.UP_TO_DATE);
        Actor internalActor = Actor.create(testContext.getActingUserCrn().getAccountId(), "__internal__actor__");
        testContext
                .as(internalActor)
                .given(FreeIpaUserSyncTestDto.class)
                .when(freeIpaTestClient.sync())
                .await(OperationState.COMPLETED)
                .validate();
        testContext.given(FreeIpaTestDto.class)
                .when(freeIpaTestClient.delete())
                .await(Status.DELETE_COMPLETED);
    }
}
