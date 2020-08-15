package com.sequenceiq.it.cloudbreak.testcase.mock;

import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status.AVAILABLE;
import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Collections;

import javax.inject.Inject;

import org.testng.annotations.Test;

import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;
import com.sequenceiq.it.cloudbreak.client.DistroXTestClient;
import com.sequenceiq.it.cloudbreak.client.EnvironmentTestClient;
import com.sequenceiq.it.cloudbreak.client.FreeIpaTestClient;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentNetworkTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.mock.freeipa.ServerConnCheckFreeipaRpcResponse;
import com.sequenceiq.it.cloudbreak.testcase.AbstractIntegrationTest;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

public class EnvironmentStartStopTest extends AbstractIntegrationTest {

    private static final Duration POLLING_INTERVAL = Duration.of(3000, ChronoUnit.MILLIS);

    @Inject
    private EnvironmentTestClient environmentTestClient;

    @Inject
    private FreeIpaTestClient freeIpaTestClient;

    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private DistroXTestClient distroXTestClient;

    @Override
    protected void setupTest(TestContext testContext) {
        createDefaultUser(testContext);
        createDefaultCredential(testContext);
        createDefaultImageCatalog(testContext);
        initializeDefaultBlueprints(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running cloudbreak",
            when = "create an attached SDX and Datahub",
            then = "should be stopped first and started after it")
    public void testCreateStopStartEnvironment(MockedTestContext testContext) {
        MockedTestContext mockedTestContext = (MockedTestContext) testContext;
        setUpFreeIpaRouteStubbing(mockedTestContext);
        testContext
                .given(EnvironmentNetworkTestDto.class)
                .given(EnvironmentTestDto.class).withNetwork().withCreateFreeIpa(false)
                .when(environmentTestClient.create())
                .await(EnvironmentStatus.AVAILABLE)
                .given(FreeIpaTestDto.class)
                .withCatalog(mockedTestContext
                .getImageCatalogMockServerSetup()
                        .getFreeIpaImageCatalogUrl())
                .when(freeIpaTestClient.create())
                .await(AVAILABLE)
                .given(SdxInternalTestDto.class)
                .when(sdxTestClient.createInternal())
                .awaitForFlow(key(resourcePropertyProvider().getName()))
                .await(SdxClusterStatusResponse.RUNNING)
                .given("dx1", DistroXTestDto.class)
                .when(distroXTestClient.create(), key("dx1"))
                .given("dx2", DistroXTestDto.class)
                .when(distroXTestClient.create(), key("dx2"))
                .given("dx1", DistroXTestDto.class)
                .await(STACK_AVAILABLE, key("dx1"))
                .given("dx2", DistroXTestDto.class)
                .await(STACK_AVAILABLE, key("dx2"))
                .given(EnvironmentTestDto.class)
                .when(environmentTestClient.stop())
                .await(EnvironmentStatus.ENV_STOPPED, POLLING_INTERVAL);
        getFreeIpaRouteHandler().updateResponse("server_conncheck", new ServerConnCheckFreeipaRpcResponse(false, Collections.emptyList()));
        testContext.given(EnvironmentTestDto.class)
                .when(environmentTestClient.start())
                .await(EnvironmentStatus.AVAILABLE, POLLING_INTERVAL)
                .validate();
        getFreeIpaRouteHandler().updateResponse("server_conncheck", new ServerConnCheckFreeipaRpcResponse());
    }
}