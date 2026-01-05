package com.sequenceiq.it.cloudbreak.testcase.mock;

import jakarta.inject.Inject;

import org.testng.annotations.Test;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationState;
import com.sequenceiq.it.cloudbreak.assertion.freeipa.FreeIpaUserSyncSuccess;
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
        createDefaultEnvironment(testContext);
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
                .given(FreeIpaTestDto.class)
                .when(freeIpaTestClient.create())
                .awaitForCreationFlow()
                .given(FreeIpaUserSyncTestDto.class)
                .when(freeIpaTestClient.getLastSyncOperationStatus())
                .await(OperationState.COMPLETED)
                .given(FreeIpaUserSyncTestDto.class)
                .when(freeIpaTestClient.syncAllInternal())
                .await(OperationState.COMPLETED)
                .then(new FreeIpaUserSyncSuccess())
                .given(FreeIpaTestDto.class)
                .when(freeIpaTestClient.delete())
                .await(Status.DELETE_COMPLETED)
                .validate();
    }
}
