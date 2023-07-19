package com.sequenceiq.it.cloudbreak.testcase.e2e.distrox;


import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType.CLUSTER_CB_CM_ADMIN_PASSWORD;
import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType.CLUSTER_CM_DB_PASSWORD;
import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType.CLUSTER_CM_SERVICES_DB_PASSWORD;
import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType.CLUSTER_MGMT_CM_ADMIN_PASSWORD;
import static com.sequenceiq.sdx.rotation.DatalakeMultiSecretType.CM_SERVICE_SHARED_DB;
import static com.sequenceiq.sdx.rotation.DatalakeSecretType.DATALAKE_CB_CM_ADMIN_PASSWORD;
import static com.sequenceiq.sdx.rotation.DatalakeSecretType.DATALAKE_MGMT_CM_ADMIN_PASSWORD;

import java.util.Set;

import javax.inject.Inject;

import org.testng.ITestContext;
import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.client.DistroXTestClient;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.testcase.e2e.AbstractE2ETest;
import com.sequenceiq.it.cloudbreak.util.clouderamanager.ClouderaManagerUtil;

public class DistroXSecretRotationTest extends AbstractE2ETest {

    @Inject
    private DistroXTestClient distroXTestClient;

    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private ClouderaManagerUtil clouderaManagerUtil;

    @Override
    protected void setupTest(TestContext testContext) {
        testContext.getCloudProvider().getCloudFunctionality().cloudStorageInitialize();
        createDefaultUser(testContext);
        createDefaultCredential(testContext);
        initializeDefaultBlueprints(testContext);
        createDefaultDatahub(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT)
    @Description(
            given = "there is a running default Distrox cluster",
            when = "secrets are getting rotated",
            then = "rotation should be successful and cluster should be available")
    public void testSecretRotation(TestContext testContext, ITestContext iTestContext) {
        testContext
                .given(SdxInternalTestDto.class)
                .when(sdxTestClient.rotateSecret(Set.of(DATALAKE_MGMT_CM_ADMIN_PASSWORD, DATALAKE_CB_CM_ADMIN_PASSWORD)))
                .awaitForFlow()
                .given(DistroXTestDto.class)
                .when(distroXTestClient.rotateSecret(Set.of(CLUSTER_MGMT_CM_ADMIN_PASSWORD, CLUSTER_CB_CM_ADMIN_PASSWORD,
                        CLUSTER_CM_SERVICES_DB_PASSWORD, CLUSTER_CM_DB_PASSWORD)))
                .awaitForFlow()
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT)
    @Description(
            given = "there is a running default Distrox cluster",
            when = "multi secrets are getting rotated",
            then = "rotation should be successful and cluster should be available")
    public void testMultiSecretRotation(TestContext testContext, ITestContext iTestContext) {
        testContext
                .given(SdxInternalTestDto.class)
                .when(sdxTestClient.rotateMultiSecret(CM_SERVICE_SHARED_DB))
                .awaitForFlow()
                .given(DistroXTestDto.class)
                .when(distroXTestClient.rotateMultiSecret(CM_SERVICE_SHARED_DB))
                .awaitForFlow()
                .given(SdxInternalTestDto.class)
                .when(sdxTestClient.rotateMultiSecret(CM_SERVICE_SHARED_DB))
                .awaitForFlow()
                .validate();
    }
}
