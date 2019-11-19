package com.sequenceiq.it.cloudbreak.testcase.e2e.aws;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;

import javax.inject.Inject;

import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxTestDto;
import com.sequenceiq.it.cloudbreak.testcase.e2e.BasicSdxTests;
import com.sequenceiq.it.cloudbreak.util.amazons3.AmazonS3Util;
import com.sequenceiq.it.cloudbreak.util.wait.WaitUtil;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

public class AwsSdxCloudStorageTests extends BasicSdxTests {

    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private AmazonS3Util amazonS3Util;

    @Inject
    private WaitUtil waitUtil;

    @Test(dataProvider = TEST_CONTEXT)
    @Description(
            given = "there is a running Cloudbreak",
            when = "a basic SDX create request with S3 Cloud Storage has been sent",
            then = "SDX should be available AND deletable along with the created S3 objects"
    )
    public void testSDXWithCloudStorageCanBeCreatedThenDeletedSuccessfully(TestContext testContext) {
        String sdx = resourcePropertyProvider().getName();

        testContext
                .given(sdx, SdxTestDto.class).withCloudStorage()
                .when(sdxTestClient.create(), key(sdx))
                .await(SdxClusterStatusResponse.RUNNING)
                .then((tc, testDto, client) -> {
                    return waitUtil.waitForSdxInstancesStatus(testDto, client, getSdxInstancesHealthyState());
                })
                .then((tc, testDto, client) -> {
                    return amazonS3Util.list(tc, testDto, client);
                })
                .then((tc, testDto, client) -> {
                    return amazonS3Util.delete(tc, testDto, client);
                })
                .validate();
    }
}
