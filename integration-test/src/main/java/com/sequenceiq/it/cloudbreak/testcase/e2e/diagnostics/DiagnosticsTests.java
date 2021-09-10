package com.sequenceiq.it.cloudbreak.testcase.e2e.diagnostics;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;

import javax.inject.Inject;

import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.client.FreeIpaTestClient;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaDiagnosticsTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxCMDiagnosticsTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxDiagnosticsTestDto;
import com.sequenceiq.it.cloudbreak.testcase.e2e.AbstractE2ETest;

public class DiagnosticsTests extends AbstractE2ETest {

    @Inject
    private FreeIpaTestClient freeIpaTestClient;

    @Inject
    private SdxTestClient sdxTestClient;

    @Override
    protected void setupTest(TestContext testContext) {
        testContext.getCloudProvider().getCloudFunctionality().cloudStorageInitialize();
        createDefaultUser(testContext);
        createDefaultCredential(testContext);
        initializeDefaultBlueprints(testContext);
        createEnvironmentWithFreeIpaAndDatalake(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT)
    @Description(
            given = "there is a running cloudbreak",
            when = "a valid SDX create request is sent with FreeIPA instances AND run diagnostics on the instances",
            then = "the diagnostics flows should be executed successfully")
    public void testDiagnosticsOnFreeIpaAndDatalake(TestContext testContext) {
        String freeIpaDiagnostics = resourcePropertyProvider().getName();
        String sdxDiagnostics = resourcePropertyProvider().getName();
        String sdxCMDiagnostics = resourcePropertyProvider().getName();
        testContext
                .given(freeIpaDiagnostics, FreeIpaDiagnosticsTestDto.class)
                .withFreeIpa()
                .when(freeIpaTestClient.collectDiagnostics(), key(freeIpaDiagnostics))
                .given(sdxDiagnostics, SdxDiagnosticsTestDto.class)
                .withSdx()
                .when(sdxTestClient.collectDiagnostics(), key(sdxDiagnostics))
                .given(sdxCMDiagnostics, SdxCMDiagnosticsTestDto.class)
                .withSdx()
                .when(sdxTestClient.collectCMDiagnostics(), key(sdxCMDiagnostics))
                .valid();
    }
}
