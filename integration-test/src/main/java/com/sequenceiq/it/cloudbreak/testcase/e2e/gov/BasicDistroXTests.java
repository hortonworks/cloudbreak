package com.sequenceiq.it.cloudbreak.testcase.e2e.gov;

import org.testng.annotations.Test;

import com.sequenceiq.distrox.api.v1.distrox.model.database.DistroXDatabaseAvailabilityType;
import com.sequenceiq.distrox.api.v1.distrox.model.database.DistroXDatabaseRequest;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.util.spot.UseSpotInstances;

public class BasicDistroXTests extends PreconditionGovTest {

    @Override
    protected void setupTest(TestContext testContext) {
        super.setupTest(testContext);
        createDefaultDatalake(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT)
    @UseSpotInstances
    @Description(
            given = "there is an available environment with CCM2 and FreeIpa",
            when = "creating a new DistroX with default parameters",
            then = "DistroX should be created successfuly and get in RUNNING state")
    public void testCreateDistroX(TestContext testContext) {
        // This is needed until CB-16092 Support DB SSL in AWS GovCloud for DL and DH clusters using RDS
        // is going to be implemented
        // OR
        // /mock-thunderhead/src/main/resources/application.yml
        //    database.wire.encryption.enable: true
        // is going to be set to 'false'
        DistroXDatabaseRequest database = new DistroXDatabaseRequest();
        database.setAvailabilityType(DistroXDatabaseAvailabilityType.NONE);

        testContext
                .given(DistroXTestDto.class)
                    .withExternalDatabase(database)
                .when(getDistroXTestClient().create())
                .await(STACK_AVAILABLE)
                .awaitForHealthyInstances()
                .when(getDistroXTestClient().get())
                .validate();
    }
}
