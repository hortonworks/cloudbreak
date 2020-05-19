package com.sequenceiq.it.cloudbreak.testcase.e2e.sdx;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;

import javax.inject.Inject;

import org.testng.annotations.Test;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.DescribeFreeIpaResponse;
import com.sequenceiq.it.cloudbreak.client.FreeIPATestClient;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIPATestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxTestDto;
import com.sequenceiq.it.cloudbreak.util.aws.amazons3.AmazonS3Util;
import com.sequenceiq.it.cloudbreak.util.wait.WaitUtil;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

public class SdxCloudStorageTest extends PreconditionSdxE2ETest {

    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private FreeIPATestClient freeIPATestClient;

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
        String sdx = resourcePropertyProvider().getName();

        DescribeFreeIpaResponse describeFreeIpaResponse = testContext.given(FreeIPATestDto.class)
                .when(freeIPATestClient.describe())
                .getResponse();

        testContext
                .given(sdx, SdxTestDto.class).withCloudStorage()
                .when(sdxTestClient.create(), key(sdx))
                .awaitForFlow(key(sdx))
                .await(SdxClusterStatusResponse.RUNNING)
                .then((tc, testDto, client) -> {
                    waitUtil.waitForSdxInstanceStatus(testDto.getResponse().getName(), tc, getSdxInstancesHealthyState(), true);
                    return testDto;
                })
                .then((tc, testDto, client) -> {
                    getCloudFunctionality(tc).cloudStorageListContainerDataLake(getBaseLocation(testDto),
                            testDto.getResponse().getName(), testDto.getResponse().getStackCrn());
                    return testDto;
                })
                .then((tc, testDto, client) -> {
                    getCloudFunctionality(tc).cloudStorageListContainerFreeIPA(getBaseLocation(testDto),
                            describeFreeIpaResponse.getName(), describeFreeIpaResponse.getCrn());
                    return testDto;
                })
                .validate();
    }
}
