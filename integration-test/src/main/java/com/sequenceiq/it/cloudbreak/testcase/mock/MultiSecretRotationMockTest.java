package com.sequenceiq.it.cloudbreak.testcase.mock;

import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType.DATAHUB_DEMO_SECRET;
import static com.sequenceiq.freeipa.rotation.FreeIpaSecretType.FREEIPA_DEMO_SECRET;
import static com.sequenceiq.sdx.rotation.DatalakeSecretType.DATALAKE_DEMO_SECRET;

import java.util.List;

import javax.inject.Inject;

import org.testng.ITestContext;
import org.testng.annotations.Test;

import com.sequenceiq.distrox.api.v1.distrox.model.DistroXSecretRotationRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.rotate.FreeIpaSecretRotationRequest;
import com.sequenceiq.it.cloudbreak.client.DistroXTestClient;
import com.sequenceiq.it.cloudbreak.client.FreeIpaTestClient;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaRotationTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.microservice.FreeIpaClient;
import com.sequenceiq.it.cloudbreak.microservice.SdxClient;
import com.sequenceiq.sdx.api.model.SdxSecretRotationRequest;

public class MultiSecretRotationMockTest extends AbstractMockTest {

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
        createDefaultDatahub(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running default Distrox cluster",
            when = "multi secrets are getting rotated",
            then = "rotation should be successful and cluster should be available")
    public void testMultiSecretRotation(TestContext testContext, ITestContext iTestContext) {
        testContext
                .given(FreeIpaRotationTestDto.class)
                .when(MultiSecretRotationMockTest::freeipaDemoRotationWithInternalActor)
                .awaitForFlow()
                .given(SdxInternalTestDto.class)
                .when(MultiSecretRotationMockTest::datalakeDemoRotationWithInternalActor)
                .awaitForFlow()
                .given(DistroXTestDto.class)
                .when(MultiSecretRotationMockTest::datahubDemoRotationWithInternalActor)
                .awaitForFlow()
                .given(FreeIpaRotationTestDto.class)
                .when(MultiSecretRotationMockTest::freeipaDemoRotationWithInternalActor)
                .awaitForFlow()
                .validate();
    }

    private static SdxInternalTestDto datalakeDemoRotationWithInternalActor(TestContext testContext1, SdxInternalTestDto dto, SdxClient client) {
        SdxSecretRotationRequest request = new SdxSecretRotationRequest();
        request.setSecrets(List.of(DATALAKE_DEMO_SECRET.value()));
        request.setCrn(dto.getCrn());
        dto.setFlow("SDX secret rotation.", client.getInternalClient(testContext1).sdxRotationEndpoint().rotateSecrets(request));
        return dto;
    }

    private static DistroXTestDto datahubDemoRotationWithInternalActor(TestContext testContext1, DistroXTestDto dto, CloudbreakClient client) {
        DistroXSecretRotationRequest request = new DistroXSecretRotationRequest();
        request.setSecrets(List.of(DATAHUB_DEMO_SECRET.value()));
        request.setCrn(dto.getCrn());
        dto.setFlow("Data Hub secret rotation.", client.getInternalClient(testContext1).distroXV1RotationEndpoint().rotateSecrets(request));
        return dto;
    }

    private static FreeIpaRotationTestDto freeipaDemoRotationWithInternalActor(TestContext testContext1, FreeIpaRotationTestDto dto, FreeIpaClient client) {
        FreeIpaSecretRotationRequest request = new FreeIpaSecretRotationRequest();
        request.setSecrets(List.of(FREEIPA_DEMO_SECRET.value()));
        dto.setFlow("Freeipa secret rotation.",
                client.getInternalClient(testContext1).getFreeipaRotationV1Endpoint().rotateSecretsByCrn(dto.getEnvironmentCrn(), request));
        return dto;
    }
}
