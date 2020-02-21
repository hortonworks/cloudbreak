package com.sequenceiq.it.cloudbreak.testcase.e2e.sdx;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;

import javax.inject.Inject;

import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxTestDto;
import com.sequenceiq.it.cloudbreak.testcase.e2e.BasicSdxTests;
import com.sequenceiq.it.cloudbreak.util.CloudFunctionality;
import com.sequenceiq.it.cloudbreak.util.aws.amazons3.AmazonS3Util;
import com.sequenceiq.it.cloudbreak.util.wait.WaitUtil;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

public class SdxCloudStorageTests extends BasicSdxTests {

    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private AmazonS3Util amazonS3Util;

    @Inject
    private WaitUtil waitUtil;

    @Test(dataProvider = TEST_CONTEXT)
    @Description(
            given = "there is a running Cloudbreak",
            when = "a basic SDX create request with FreeIPA and DataLake Cloud Storage has been sent",
            then = "SDX should be available along with the created Cloud storage objects"
    )
    public void testSDXWithDataLakeAndFreeIPAStorageCanBeCreatedSuccessfully(TestContext testContext) {
        CloudFunctionality cloudFunctionality = testContext.getCloudProvider().getCloudFunctionality();
        String sdx = resourcePropertyProvider().getName();

        testContext
                .given(sdx, SdxTestDto.class).withCloudStorage()
                .when(sdxTestClient.create(), key(sdx))
                .awaitForFlow(key(sdx))
                .await(SdxClusterStatusResponse.RUNNING)
                .then((tc, testDto, client) -> {
                    return waitUtil.waitForSdxInstancesStatus(testDto, client, getSdxInstancesHealthyState());
                })
                .then((tc, testDto, client) -> {
                    cloudFunctionality.cloudStorageListContainerDataLake(getBaseLocation(testDto));
                    return testDto;
                })
                .then((tc, testDto, client) -> {
                    cloudFunctionality.cloudStorageListContainerFreeIPA(getBaseLocation(testDto));
                    return testDto;
                })
                .validate();
    }

    private String getBaseLocation(SdxTestDto testDto) {
        return testDto.getRequest().getCloudStorage().getBaseLocation();
    }
}
