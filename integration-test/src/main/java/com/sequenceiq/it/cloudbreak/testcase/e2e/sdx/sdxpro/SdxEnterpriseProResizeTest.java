package com.sequenceiq.it.cloudbreak.testcase.e2e.sdx.sdxpro;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;

import jakarta.inject.Inject;

import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.auth.altus.model.Entitlement;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.client.UmsTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.ums.UmsTestDto;
import com.sequenceiq.it.cloudbreak.testcase.e2e.sdx.PreconditionSdxE2ETest;
import com.sequenceiq.it.cloudbreak.util.resize.SdxResizeTestUtil;
import com.sequenceiq.it.cloudbreak.util.resize.SdxResizeTestValidator;
import com.sequenceiq.it.cloudbreak.util.spot.UseSpotInstances;
import com.sequenceiq.sdx.api.model.SdxClusterResizeRequest;
import com.sequenceiq.sdx.api.model.SdxClusterShape;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

public class SdxEnterpriseProResizeTest extends PreconditionSdxE2ETest {

    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private SdxResizeTestUtil sdxResizeTestUtil;

    @Inject
    private SdxResizeTestValidator sdxResizeTestValidator;

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

    @Test(dataProvider = TEST_CONTEXT)
    @UseSpotInstances
    @Description(
            given = "there is an available environment with a running SDX cluster (ENTERPRISE_PRO shape, single-az) " +
                    "and the account is entitled for shapes without HBase/HDFS",
            when = "resize is called on the SDX cluster (target shape: ENTERPRISE_PRO multi-az)",
            then = "SDX resize should be successful, the new cluster should be up and running"
    )
    public void testSdxEnterpriseProSameShapeResize(TestContext testContext) {
        String sdxKey = resourcePropertyProvider().getName();

        testContext
                .given(UmsTestDto.class)
                .when(umsTestClient.grantEntitlement(testContext.getActingUserCrn().getAccountId(),
                        Entitlement.CDP_DATALAKE_SHAPES_WITHOUT_HBASE_AND_HDFS.name()));

        sdxResizeTestUtil
                .givenProvisionEnvironmentAndDatalake(testContext, sdxKey, runtimeVersion, SdxClusterShape.ENTERPRISE_PRO, sdxResizeTestValidator,
                        commonCloudProperties().getImageCatalogName(), qualifiedImageId)
                .then((tc, testDto, client) -> {
                    SdxClusterResizeRequest sdxClusterResizeRequest = testDto.getSdxResizeRequest();
                    sdxClusterResizeRequest.setClusterShape(SdxClusterShape.ENTERPRISE_PRO);
                    sdxClusterResizeRequest.setEnableMultiAz(true);
                    sdxResizeTestValidator.setExpectedShape(SdxClusterShape.ENTERPRISE_PRO);
                    sdxResizeTestValidator.setExpectedMultiAzDatalake(true);
                    sdxResizeTestValidator.setExpectedSameShapeResize(true);
                    sdxClusterResizeRequest.setSkipValidation(true);
                    return testDto;
                })
                .when(sdxTestClient.resize(), key(sdxKey))
                .await(SdxClusterStatusResponse.STOP_IN_PROGRESS, key(sdxKey).withoutWaitForFlow())
                .await(SdxClusterStatusResponse.STACK_CREATION_IN_PROGRESS, key(sdxKey).withoutWaitForFlow())
                .await(SdxClusterStatusResponse.RUNNING, key(sdxKey))
                .awaitForHealthyInstances()
                .then((tc, dto, client) -> sdxResizeTestValidator.validateResizedCluster(dto, tc))
                .validate();
    }
}
