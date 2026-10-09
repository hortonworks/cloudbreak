package com.sequenceiq.it.cloudbreak.testcase.e2e.sdx.sdxpro;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.inject.Inject;

import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.auth.altus.model.Entitlement;
import com.sequenceiq.it.cloudbreak.client.RestartTestClient;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.client.UmsTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.restart.RestartInstancesTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.dto.telemetry.TelemetryTestDto;
import com.sequenceiq.it.cloudbreak.dto.ums.UmsTestDto;
import com.sequenceiq.it.cloudbreak.testcase.e2e.sdx.PreconditionSdxE2ETest;
import com.sequenceiq.it.cloudbreak.util.SdxUtil;
import com.sequenceiq.it.cloudbreak.util.spot.UseSpotInstances;
import com.sequenceiq.sdx.api.model.SdxClusterShape;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

public class SdxEnterpriseProLifecycleTest extends PreconditionSdxE2ETest {

    private static final String INSTANCE_GROUP = "core";

    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private RestartTestClient restartTestClient;

    @Inject
    private SdxUtil sdxUtil;

    @Inject
    private UmsTestClient umsTestClient;

    private String runtimeVersion;

    private String qualifiedImageId;

    @Override
    protected void setupTest(TestContext testContext) {
        createDefaultUser(testContext);
        runtimeVersion = commonClusterManagerProperties().getProShapeRuntimeVersion();
        qualifiedImageId = getQualifiedPrewarmedImageId(testContext, runtimeVersion,
                commonClusterManagerProperties().getProShapeMinimumQualifiedRuntimeVersion());
        testContext.getCloudProvider().getCloudFunctionality().cloudStorageInitialize();
        createDefaultCredential(testContext);
        initializeAzureMarketplaceTermsPolicy(testContext);
        createEnvironmentWithFreeIpa(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT, timeOut = 9000000)
    @UseSpotInstances
    @Description(
            given = "there is an available environment and the account is entitled for shapes without HBase/HDFS",
            when = "an SDX Enterprise Pro cluster is created, its instances are restarted, then the cluster is deleted",
            then = "each step should succeed and the cluster should end up DELETED"
    )
    public void testSdxEnterpriseProCreateRestartDelete(TestContext testContext) {
        AtomicReference<String> dataLakeName = new AtomicReference<>();
        AtomicReference<String> dataLakeCrn = new AtomicReference<>();
        List<String> dataLakeInstancesToRestart = new ArrayList<>();

        testContext
                .given(UmsTestDto.class)
                .when(umsTestClient.grantEntitlement(testContext.getActingUserCrn().getAccountId(),
                        Entitlement.CDP_DATALAKE_SHAPES_WITHOUT_HBASE_AND_HDFS.name()))
                .given("telemetry", TelemetryTestDto.class)
                .withLogging()
                .withReportClusterLogs()
                .given(SdxInternalTestDto.class)
                .withTelemetry("telemetry")
                .withCloudStorage(getCloudStorageRequest(testContext))
                .withRuntimeVersion(runtimeVersion)
                .withImageCatalogNameAndImageId(commonCloudProperties().getImageCatalogName(), qualifiedImageId)
                .withClusterShape(SdxClusterShape.ENTERPRISE_PRO)
                .when(sdxTestClient.createInternal())
                .await(SdxClusterStatusResponse.RUNNING)
                .awaitForHealthyInstances()
                .then((tc, testDto, client) -> {
                        dataLakeName.set(testDto.getName());
                        dataLakeCrn.set(testDto.getCrn());
                        dataLakeInstancesToRestart.addAll(sdxUtil.getInstanceIds(testDto, client, INSTANCE_GROUP));
                        return testDto;
                })
                .given("restartSdx", RestartInstancesTestDto.class)
                .withName(dataLakeName.get())
                .withCrn(dataLakeCrn.get())
                .when(restartTestClient.restartInstances(dataLakeInstancesToRestart))
                .awaitForFlow()
                .given(SdxInternalTestDto.class)
                .when(sdxTestClient.describeInternal())
                .await(SdxClusterStatusResponse.RUNNING)
                .awaitForHealthyInstances()
                .when(sdxTestClient.deleteInternal())
                .await(SdxClusterStatusResponse.DELETED)
                .validate();
    }
}
