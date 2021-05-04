package com.sequenceiq.it.cloudbreak.testcase.e2e.sdx;

import javax.inject.Inject;

import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.util.spot.UseSpotInstances;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;
import com.sequenceiq.sdx.api.model.SdxDatabaseAvailabilityType;
import com.sequenceiq.sdx.api.model.SdxDatabaseRequest;

public class InternalSdxTest extends PreconditionSdxE2ETest {

    @Inject
    private SdxTestClient sdxTestClient;

    @Test(dataProvider = TEST_CONTEXT)
    @UseSpotInstances
    @Description(
            given = "an SDX internal request",
            when = "a SDX internal create request is sent",
            then = "SDX is created")
    public void testCreateInternalSdx(TestContext testContext) {
        SdxDatabaseRequest sdxDatabaseRequest = new SdxDatabaseRequest();
        sdxDatabaseRequest.setAvailabilityType(SdxDatabaseAvailabilityType.NONE);

        testContext
                .given(SdxInternalTestDto.class)
                    .withDatabase(sdxDatabaseRequest)
                    .withCloudStorage(getCloudStorageRequest(testContext))
                .when(sdxTestClient.createInternal())
                .await(SdxClusterStatusResponse.RUNNING)
                .awaitForInstance(getSdxInstancesHealthyState())
                .when(sdxTestClient.describeInternal())
                .validate();
    }
}
