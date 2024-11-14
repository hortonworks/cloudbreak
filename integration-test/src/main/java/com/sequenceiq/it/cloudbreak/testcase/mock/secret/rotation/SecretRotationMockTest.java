package com.sequenceiq.it.cloudbreak.testcase.mock.secret.rotation;

import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.inject.Inject;

import org.testng.ITestContext;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType;
import com.sequenceiq.freeipa.rotation.FreeIpaSecretType;
import com.sequenceiq.it.cloudbreak.client.DistroXTestClient;
import com.sequenceiq.it.cloudbreak.client.FreeIpaTestClient;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaRotationTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.testcase.mock.AbstractMockTest;
import com.sequenceiq.sdx.rotation.DatalakeSecretType;

public class SecretRotationMockTest extends AbstractMockTest {

    public static final String ROTATION_FAILURE_KEY = "rotation_failure";

    public static final String ROLLBACK_FAILURE_KEY = "rollback_failure";

    public static final String FINALIZE_FAILURE_KEY = "finalize_failure";

    public static final String PREVALIDATE_FAILURE_KEY = "prevalidate_failure";

    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private DistroXTestClient distroXTestClient;

    @Inject
    private FreeIpaTestClient freeIpaTestClient;

    @Override
    protected void setupTest(TestContext testContext) {
        createDefaultUser(testContext);
        createDefaultCredential(testContext);
        createDefaultImageCatalog(testContext);
        initializeDefaultBlueprints(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running default Distrox cluster",
            when = "secrets are getting rotated",
            then = "rotation should be successful and cluster should be available")
    public void testSecretRotation(TestContext testContext, ITestContext iTestContext) {
        createDefaultDatahub(testContext);
        testContext
                .given(FreeIpaRotationTestDto.class)
                .withSecrets(List.of(FreeIpaSecretType.DEMO_SECRET))
                .when(freeIpaTestClient.rotateSecretInternal())
                .awaitForFlow()
                .given(SdxInternalTestDto.class)
                .when(sdxTestClient.rotateSecretInternal(List.of(DatalakeSecretType.DEMO_SECRET)))
                .awaitForFlow()
                .given(DistroXTestDto.class)
                .when(distroXTestClient.rotateSecretInternal(List.of(CloudbreakSecretType.DEMO_SECRET)))
                .awaitForFlow()
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running Data Lake cluster",
            when = "both rotation and rollback fails",
            then = "retrying rollback succeeds")
    public void testFailedDataLakeSecretRotationRollbackCanBeRestarted(TestContext testContext, ITestContext iTestContext) {
        createDefaultDatalake(testContext);
        executeDataLakeDemoRotation(testContext, Map.of(ROTATION_FAILURE_KEY, "", ROLLBACK_FAILURE_KEY, ""))
                .awaitForFlowFail()
                .validate();
        executeDataLakeDemoRotation(testContext)
                .awaitForFlow()
                .validate();
        executeDataLakeDemoRotation(testContext)
                .awaitForFlow()
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running Data Lake cluster",
            when = "both rotation and rollback fails",
            then = "retrying rollback succeeds")
    public void testFailedDataLakeSecretRotationPreValidateCanBeRestarted(TestContext testContext, ITestContext iTestContext) {
        createDefaultDatalake(testContext);
        executeDataLakeDemoRotation(testContext, Map.of(PREVALIDATE_FAILURE_KEY, ""))
                .awaitForFlowFail()
                .validate();
        executeDataLakeDemoRotation(testContext)
                .awaitForFlow()
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running Data Lake cluster",
            when = "secret rotation finalize fails",
            then = "retrying finalize succeeds")
    public void testFailedDataLakeSecretRotationFinalizationCanBeRestarted(TestContext testContext, ITestContext iTestContext) {
        createDefaultDatalake(testContext);
        executeDataLakeDemoRotation(testContext, Map.of(FINALIZE_FAILURE_KEY, ""))
                .awaitForFlowFail()
                .validate();
        executeDataLakeDemoRotation(testContext)
                .awaitForFlow()
                .validate();
    }

    private FreeIpaRotationTestDto executeFreeIpaDemoRotation(TestContext testContext) {
        return testContext
                .given(FreeIpaRotationTestDto.class)
                .withSecrets(List.of(FreeIpaSecretType.DEMO_SECRET))
                .when(freeIpaTestClient.rotateSecretInternal());
    }

    private SdxInternalTestDto executeDataLakeDemoRotation(TestContext testContext) {
        return executeDataLakeDemoRotation(testContext, Map.of());
    }

    private SdxInternalTestDto executeDataLakeDemoRotation(TestContext testContext, Map<String, String> additionalArgs) {
        return testContext
                .given(SdxInternalTestDto.class)
                .when(sdxTestClient.rotateSecretInternal(Set.of(DatalakeSecretType.DEMO_SECRET), additionalArgs));
    }

    private DistroXTestDto executeDataHubDemoRotation(TestContext testContext) {
        return executeDataHubDemoRotation(testContext, Map.of());
    }

    private DistroXTestDto executeDataHubDemoRotation(TestContext testContext, Map<String, String> additionalArgs) {
        return testContext
                .given(DistroXTestDto.class)
                .when(distroXTestClient.rotateSecretInternal(Set.of(CloudbreakSecretType.DEMO_SECRET), additionalArgs));
    }
}
