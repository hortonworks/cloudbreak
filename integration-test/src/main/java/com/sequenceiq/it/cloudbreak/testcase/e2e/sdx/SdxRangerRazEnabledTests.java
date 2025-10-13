package com.sequenceiq.it.cloudbreak.testcase.e2e.sdx;


import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;

import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;

import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

public class SdxRangerRazEnabledTests extends PreconditionSdxE2ETest {

    @Inject
    private SdxTestClient sdxTestClient;

    @Test(dataProvider = TEST_CONTEXT)
    @Description(
            given = "there is a running Cloudbreak",
            when = "enableRangerRaz is called when Raz is not installed",
            then = "Exception is thrown"
    )
    public void testCreateSdxWithoutRangerRaz(TestContext testContext) {
        String sdx = resourcePropertyProvider().getName();

        testContext
                .given(SdxTestDto.class)
                .withCloudStorage(getCloudStorageRequest(testContext))
                .when(sdxTestClient.create(), key(sdx))
                .await(SdxClusterStatusResponse.RUNNING)
                .awaitForHealthyInstances()
                .whenException(sdxTestClient.enableRangerRaz(), BadRequestException.class)
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT)
    @Description(
            given = "there is a running Cloudbreak",
            when = "enableRangerRaz is called when Raz is installed",
            then = "rangerRazEnabled is set for Sdx"
    )
    public void testCreateSdxWithRangerRaz(TestContext testContext) {
        String sdx = resourcePropertyProvider().getName();

        testContext
                .given(SdxTestDto.class)
                .withCloudStorage(getCloudStorageRequest(testContext))
                .withRangerRazEnabled(Boolean.TRUE)
                .when(sdxTestClient.create(), key(sdx))
                .await(SdxClusterStatusResponse.RUNNING)
                .awaitForHealthyInstances()
                .when(sdxTestClient.enableRangerRaz())
                .then((tc, testDto, client) -> {
                    final boolean rangerRazEnabled = testDto.getResponse().getRangerRazEnabled();
                    if (!rangerRazEnabled) {
                        throw new TestFailException("Ranger raz was not enabled!");
                    }
                    return testDto;
                })
                .validate();
    }
}
