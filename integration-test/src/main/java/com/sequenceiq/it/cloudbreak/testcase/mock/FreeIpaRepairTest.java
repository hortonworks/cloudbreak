package com.sequenceiq.it.cloudbreak.testcase.mock;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.waitForFlow;

import javax.inject.Inject;

import org.testng.annotations.Test;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceMetadataType;
import com.sequenceiq.it.cloudbreak.assertion.audit.FreeIpaAuditGrpcServiceAssertion;
import com.sequenceiq.it.cloudbreak.assertion.freeipa.FreeIpaListStructuredEventAssertions;
import com.sequenceiq.it.cloudbreak.client.FreeIpaTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;

public class FreeIpaRepairTest extends AbstractMockTest {

    @Inject
    private FreeIpaTestClient freeIpaTestClient;

    @Inject
    private FreeIpaListStructuredEventAssertions freeIpaListStructuredEventAssertions;

    @Inject
    private FreeIpaAuditGrpcServiceAssertion freeIpaAuditGrpcServiceAssertion;

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
            when = "calling a freeipe repair on each instances",
            then = "freeipa sould be available")
    public void testStopStartFreeIpa(MockedTestContext testContext) {
        testContext
                .given(FreeIpaTestDto.class)
                .withFreeIpaHa(1, 2)
                .when(freeIpaTestClient.create())
                .await(Status.AVAILABLE)
                .when(freeIpaTestClient.repair(InstanceMetadataType.GATEWAY_PRIMARY))
                .await(Status.UPDATE_IN_PROGRESS, waitForFlow().withWaitForFlow(Boolean.FALSE))
                .await(Status.AVAILABLE)
                .awaitForHealthyInstances()
                .validate();
    }
}
