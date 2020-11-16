package com.sequenceiq.it.cloudbreak.testcase.mock;

import javax.inject.Inject;

import org.testng.annotations.Test;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.it.cloudbreak.assertion.audit.FreeIpaAuditGrpcServiceAssertion;
import com.sequenceiq.it.cloudbreak.assertion.freeipa.FreeIpaListStructuredEventAssertions;
import com.sequenceiq.it.cloudbreak.client.FreeIpaTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;

public class FreeIpaStartStopTest extends AbstractMockTest {

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
        createDefaultEnvironmentWithNetwork(testContext);
        createDefaultImageCatalog(testContext);
        initializeDefaultBlueprints(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "environment is present",
            when = "calling a freeipe start",
            then = "freeipa sould be available")
    public void testStopStartFreeIpa(MockedTestContext testContext) {
        getFreeIpaHealthCheckHandler().setHealthy();
        testContext
                .given(FreeIpaTestDto.class).withCatalog(getImageCatalogMockServerSetup().getFreeIpaImageCatalogUrl())
                .when(freeIpaTestClient.create())
                .await(Status.AVAILABLE);
        getFreeIpaHealthCheckHandler().setUnreachable();
        testContext.given(FreeIpaTestDto.class)
                .when(freeIpaTestClient.stop())
                .await(Status.STOPPED);
        getFreeIpaHealthCheckHandler().setHealthy();
        testContext.given(FreeIpaTestDto.class)
                .when(freeIpaTestClient.start())
                .await(Status.AVAILABLE)
                .when(freeIpaTestClient.delete())
                .await(Status.DELETE_COMPLETED)
                .when(freeIpaAuditGrpcServiceAssertion::create)
                .when(freeIpaAuditGrpcServiceAssertion::delete)
                .when(freeIpaAuditGrpcServiceAssertion::stop)
                .when(freeIpaAuditGrpcServiceAssertion::start)
                .when(freeIpaListStructuredEventAssertions::checkCreateEvents)
                .when(freeIpaListStructuredEventAssertions::checkDeleteEvents)
                .when(freeIpaListStructuredEventAssertions::checkStartEvents)
                .when(freeIpaListStructuredEventAssertions::checkStopEvents)
                .validate();
    }
}
