package com.sequenceiq.it.cloudbreak.testcase.e2e.sdx;

import jakarta.inject.Inject;

import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.assertion.sdx.SdxAssertion;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxTestDto;
import com.sequenceiq.it.cloudbreak.util.spot.UseSpotInstances;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

public class SdxEncryptionProfileTests extends PreconditionSdxE2ETest {

    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private SdxAssertion sdxAssertion;

    @Test(dataProvider = TEST_CONTEXT)
    @UseSpotInstances
    @Description(
            given = "there is a running Cloudbreak",
            when = "a SDX cluster in available state",
            then = "check default tls version is configured"
    )
    public void testEncryptionProfile(TestContext testContext) {
        SdxTestDto sdxTestDto = testContext
                .given(SdxTestDto.class)
                .withCloudStorage()
                .withRuntimeVersion("7.3.2");
        sdxTestDto
                .when(sdxTestClient.create())
                .await(SdxClusterStatusResponse.RUNNING)
                .awaitForHealthyInstances()
                .then((tc, testDto, client) -> {
                    sdxAssertion.validateFileContentExists(testDto, "/etc/cloudera-scm-server/cm.settings",
                            "SUPPORTED_TLS_VERSIONS\\s*TLSv1.2,TLSv1.3");
                    sdxAssertion.validateFileContentExists(testDto, "/etc/nginx/sites-enabled/ssl.conf",
                            "ssl_protocols\\s*TLSv1.2\\s*TLSv1.3");
                    sdxAssertion.validateFileContentExists(testDto, "/etc/nginx/sites-enabled/ssl-user-facing.conf",
                            "ssl_protocols\\s*TLSv1.2\\s*TLSv1.3");
                    return testDto;
                })
                .validate();
    }
}
