package com.sequenceiq.it.cloudbreak.testcase.e2e.sdx;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;

import javax.inject.Inject;

import org.testng.annotations.Test;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.DescribeFreeIpaResponse;
import com.sequenceiq.it.cloudbreak.client.FreeIpaTestClient;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxTestDto;
import com.sequenceiq.it.cloudbreak.util.aws.amazons3.AmazonS3Util;
import com.sequenceiq.it.cloudbreak.util.spot.UseSpotInstances;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;
import com.sequenceiq.sdx.api.model.SdxDatabaseAvailabilityType;
import com.sequenceiq.sdx.api.model.SdxDatabaseRequest;

public class SdxCloudStorageTest extends PreconditionSdxE2ETest {

    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private FreeIpaTestClient freeIpaTestClient;

    @Inject
    private AmazonS3Util amazonS3Util;

    @Test(dataProvider = TEST_CONTEXT)
    @UseSpotInstances
    @Description(
            given = "there is a running Cloudbreak",
            when = "a basic SDX create request with FreeIPA and DataLake Cloud Storage has been sent",
            then = "SDX should be available along with the created Cloud storage objects"
    )
    public void testSDXWithDataLakeAndFreeIpaStorageCanBeCreatedSuccessfully(TestContext testContext) {
        String sdx = resourcePropertyProvider().getName();

        SdxDatabaseRequest sdxDatabaseRequest = new SdxDatabaseRequest();
        sdxDatabaseRequest.setAvailabilityType(SdxDatabaseAvailabilityType.NONE);

        DescribeFreeIpaResponse describeFreeIpaResponse = testContext.given(FreeIpaTestDto.class)
                .when(freeIpaTestClient.describe())
                .getResponse();

        testContext
                .given(sdx, SdxTestDto.class)
                .withCloudStorage()
                .withExternalDatabase(sdxDatabaseRequest)
                .when(sdxTestClient.create(), key(sdx))
                .await(SdxClusterStatusResponse.RUNNING)
                .awaitForInstance(getSdxInstancesHealthyState())
                .then((tc, testDto, client) -> {
                    getCloudFunctionality(tc).cloudStorageListContainerDataLake(getBaseLocation(testDto),
                            testDto.getResponse().getName(), testDto.getResponse().getStackCrn());
                    return testDto;
                })
                .then((tc, testDto, client) -> {
                    getCloudFunctionality(tc).cloudStorageListContainerFreeIpa(getBaseLocation(testDto),
                            describeFreeIpaResponse.getName(), describeFreeIpaResponse.getCrn());
                    return testDto;
                })
                .validate();
    }
}
