package com.sequenceiq.it.cloudbreak.testcase.e2e.freeipa;

import static com.sequenceiq.freeipa.api.v1.operation.model.OperationState.COMPLETED;
import static com.sequenceiq.it.cloudbreak.context.RunningParameter.waitForFlow;

import java.util.Set;

import jakarta.inject.Inject;

import org.testng.annotations.Test;

import com.sequenceiq.environment.api.v1.environment.model.EnvironmentNetworkAwsParams;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentNetworkRequest;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.network.AwsNetworkParameters;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.network.NetworkRequest;
import com.sequenceiq.it.cloudbreak.assertion.Assertion;
import com.sequenceiq.it.cloudbreak.client.FreeIpaTestClient;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.cloud.v4.aws.AwsCloudProvider;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaOperationStatusTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.dto.telemetry.TelemetryTestDto;
import com.sequenceiq.it.cloudbreak.microservice.FreeIpaClient;
import com.sequenceiq.it.cloudbreak.testcase.e2e.AbstractE2ETest;
import com.sequenceiq.it.cloudbreak.util.aws.AwsCloudFunctionality;
import com.sequenceiq.sdx.api.model.SdxDatabaseAvailabilityType;
import com.sequenceiq.sdx.api.model.SdxDatabaseRequest;

public class FreeIpaUpgradeNativeTests extends AbstractE2ETest {

    protected static final Status FREEIPA_AVAILABLE = Status.AVAILABLE;

    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private FreeIpaTestClient freeIpaTestClient;

    @Inject
    private AwsCloudFunctionality cloudFunctionality;

    @Inject
    private AwsCloudProvider awsCloudProvider;

    @Override
    protected void setupTest(TestContext testContext) {
        initializeTest(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT)
    @Description(
            given = "there is a running cloudbreak, and freeipa with cloudformation",
            when = "upgrade",
            then = "migration happens into native")
    public void testSingleFreeIpaNativeUpgrade(TestContext testContext) {
        String subnet = awsCloudProvider.getSubnetId();

        testContext
                .given("telemetry", TelemetryTestDto.class)
                .withLogging()
                .withReportClusterLogs();

        EnvironmentNetworkRequest environmentNetworkRequest = new EnvironmentNetworkRequest();
        EnvironmentNetworkAwsParams awsParams = new EnvironmentNetworkAwsParams();
        environmentNetworkRequest.setAws(awsParams);
        environmentNetworkRequest.setSubnetIds(Set.of(subnet));
        setUpEnvironmentTestDto(testContext, Boolean.TRUE, 1)
                .withTelemetry("telemetry")
                .withFreeIpaImage(testContext.getCloudProvider().getFreeIpaUpgradeImageCatalog(), testContext.getCloudProvider().getFreeIpaUpgradeImageId())
                .withNetwork(environmentNetworkRequest)
                .when(getEnvironmentTestClient().create())
                .await(EnvironmentStatus.AVAILABLE)
                .given(FreeIpaTestDto.class)
                .when(freeIpaTestClient.describe())
                .await(FREEIPA_AVAILABLE)
                .then(freeIpaCloudFormationStackDoesExist())
                .when(freeIpaTestClient.checkVariant("AWS"))
                .when(freeIpaTestClient.upgrade())
                .await(Status.UPDATE_IN_PROGRESS, waitForFlow().withWaitForFlow(Boolean.FALSE))
                .given(FreeIpaOperationStatusTestDto.class)
                .withOperationId(((FreeIpaTestDto) testContext.get(FreeIpaTestDto.class)).getOperationId())
                .await(COMPLETED)
                .given(FreeIpaTestDto.class)
                .await(FREEIPA_AVAILABLE, waitForFlow().withWaitForFlow(Boolean.FALSE))
                .then(freeIpaCloudFromationStackDoesNotExist())
                .when(freeIpaTestClient.checkVariant("AWS_NATIVE"))
                .validate();
    }

    private SdxDatabaseRequest getDatabaseRequest() {
        SdxDatabaseRequest sdxDatabaseRequest = new SdxDatabaseRequest();
        sdxDatabaseRequest.setAvailabilityType(SdxDatabaseAvailabilityType.NONE);
        sdxDatabaseRequest.setCreate(false);

        return sdxDatabaseRequest;
    }

    private NetworkRequest getNetworkRequest() {
        NetworkRequest networkRequest = new NetworkRequest();
        AwsNetworkParameters params = new AwsNetworkParameters();
        networkRequest.setAws(params);
        params.setVpcId(awsCloudProvider.getVpcId());
        String subnet = awsCloudProvider.getSubnetId();
        params.setSubnetId(subnet);

        return networkRequest;
    }

    private Assertion<FreeIpaTestDto, FreeIpaClient> freeIpaCloudFormationStackDoesExist() {
        return isFreeIpaCloudFromationStackDoesExist(true);
    }

    private Assertion<FreeIpaTestDto, FreeIpaClient> freeIpaCloudFromationStackDoesNotExist() {
        return isFreeIpaCloudFromationStackDoesExist(false);
    }

    private Assertion<FreeIpaTestDto, FreeIpaClient> isFreeIpaCloudFromationStackDoesExist(boolean exist) {
        return (tc, freeipaTestDto, client) -> {
            Boolean res = cloudFunctionality.isFreeipaCfStackExistForEnvironment(freeipaTestDto.getEnvironmentCrn());
            org.assertj.core.api.Assertions.assertThat(res).as("freeipa cloudformation template for environment should "
                    + (exist ? "exist." : "not exist.")).isEqualTo(exist);

            return freeipaTestDto;
        };
    }
}
