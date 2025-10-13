package com.sequenceiq.it.cloudbreak.testcase.e2e.gov;

import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxTestDto;
import com.sequenceiq.it.cloudbreak.util.spot.UseSpotInstances;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

public class BasicSdxTests extends PreconditionGovTest {

    @Override
    protected void setupTest(TestContext testContext) {
        super.setupTest(testContext);
        createDefaultEnvironment(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT)
    @UseSpotInstances
    @Description(
            given = "there is an available environment with CCM2 and FreeIpa",
            when = "creating a new DataLake with no database for the environment",
            then = "DataLake should be created successfuly and get in RUNNING state")
    public void testCreateSdx(TestContext testContext) {

        testContext
                .given(SdxTestDto.class)
                    .withCloudStorage(getCloudStorageRequest(testContext))
                .when(getSdxTestClient().create())
                .await(SdxClusterStatusResponse.RUNNING)
                .awaitForHealthyInstances()
                .when(getSdxTestClient().describe())
                .validate();
    }
}
