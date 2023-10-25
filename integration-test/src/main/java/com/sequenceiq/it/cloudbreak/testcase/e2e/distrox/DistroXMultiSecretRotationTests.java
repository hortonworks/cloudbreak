package com.sequenceiq.it.cloudbreak.testcase.e2e.distrox;


import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType.DATAHUB_CM_INTERMEDIATE_CA_CERT;
import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType.DATAHUB_CM_SERVICE_SHARED_DB;
import static com.sequenceiq.freeipa.rotation.FreeIpaSecretType.FREEIPA_CA_CERT_RENEWAL;
import static com.sequenceiq.sdx.rotation.DatalakeSecretType.DATALAKE_CM_INTERMEDIATE_CA_CERT;
import static com.sequenceiq.sdx.rotation.DatalakeSecretType.DATALAKE_CM_SERVICE_SHARED_DB;

import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.testng.ITestContext;
import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.client.DistroXTestClient;
import com.sequenceiq.it.cloudbreak.client.FreeIpaTestClient;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaRotationTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.testcase.e2e.AbstractE2ETest;

public class DistroXMultiSecretRotationTests extends AbstractE2ETest {

    @Inject
    private DistroXTestClient distroXTestClient;

    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private FreeIpaTestClient freeIpaTestClient;

    @Override
    protected void setupTest(TestContext testContext) {
        testContext.getCloudProvider().getCloudFunctionality().cloudStorageInitialize();
        createDefaultUser(testContext);
        createDefaultCredential(testContext);
        initializeDefaultBlueprints(testContext);
        createDefaultDatahubWithAutoTlsAndExternalDb(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT)
    @Description(
            given = "there is a running default Distrox cluster",
            when = "CM shared DB multi secret are getting rotated",
            then = "rotation should be successful and clusters should be available")
    public void testCMSharedDbMultiSecretRotation(TestContext testContext, ITestContext iTestContext) {
        testContext
                .given(SdxInternalTestDto.class)
                .when(sdxTestClient.rotateSecret(Set.of(DATALAKE_CM_SERVICE_SHARED_DB)))
                .awaitForFlow()
                .given(DistroXTestDto.class)
                .when(distroXTestClient.rotateSecret(Set.of(DATAHUB_CM_SERVICE_SHARED_DB)))
                .awaitForFlow()
                .given(SdxInternalTestDto.class)
                .when(sdxTestClient.rotateSecret(Set.of(DATALAKE_CM_SERVICE_SHARED_DB)))
                .awaitForFlow()
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT)
    @Description(
            given = "there is a running default Distrox cluster",
            when = "root CA cert multi secret are getting rotated",
            then = "rotation should be successful and clusters should be available")
    public void testCacertMultiSecretRotation(TestContext testContext, ITestContext iTestContext) {
        testContext
                .given(FreeIpaRotationTestDto.class)
                .withSecrets(List.of(FREEIPA_CA_CERT_RENEWAL))
                .when(freeIpaTestClient.rotateSecret())
                .awaitForFlow()
                .given(SdxInternalTestDto.class)
                .when(sdxTestClient.rotateSecret(Set.of(DATALAKE_CM_INTERMEDIATE_CA_CERT)))
                .awaitForFlow()
                .given(DistroXTestDto.class)
                .when(distroXTestClient.rotateSecret(Set.of(DATAHUB_CM_INTERMEDIATE_CA_CERT)))
                .awaitForFlow()
                .given(FreeIpaRotationTestDto.class)
                .withSecrets(List.of(FREEIPA_CA_CERT_RENEWAL))
                .when(freeIpaTestClient.rotateSecret())
                .awaitForFlow()
                .validate();
    }
}
