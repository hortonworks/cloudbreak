package com.sequenceiq.it.cloudbreak.testcase.e2e.distrox;

import javax.inject.Inject;

import org.testng.ITestContext;
import org.testng.annotations.Test;

import com.sequenceiq.distrox.api.v1.distrox.model.database.DistroXDatabaseAvailabilityType;
import com.sequenceiq.it.TestParameter;
import com.sequenceiq.it.cloudbreak.client.DistroXTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.externaldatabase.DistroXExternalDatabaseTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.image.DistroXImageTestDto;
import com.sequenceiq.it.cloudbreak.testcase.e2e.AbstractE2ETest;

public class DistroXScaleTest extends AbstractE2ETest {
    private static final String DIX_EXTDB_KEY = "distroxExternalDatabaseKey";

    @Inject
    private DistroXTestClient distroXTestClient;

    @Inject
    private TestParameter testParameter;

    @Override
    protected void setupTest(TestContext testContext) {
        testContext.getCloudProvider().getCloudFunctionality().cloudStorageInitialize();
        createDefaultUser(testContext);
        createDefaultCredential(testContext);
        createEnvironmentWithNetworkAndFreeIPA(testContext);
        initializeDefaultBlueprints(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT)
    @Description(
            given = "there is a running cloudbreak",
            when = "a valid DistroX create request is sent",
            then = "DistroX cluster with external database is created")
    public void testCreateAndScaleDistroX(TestContext testContext, ITestContext iTestContext) {
        String imageSettings = resourcePropertyProvider().getName();
        DistroXScaleTestParameters params = new DistroXScaleTestParameters(iTestContext.getCurrentXmlTest().getAllParameters());
        DistroXExternalDatabaseTestDto dbTestContext = testContext.given(DIX_EXTDB_KEY, DistroXExternalDatabaseTestDto.class)
                .withAvailabilityType(DistroXDatabaseAvailabilityType.NON_HA);
        if (params.isImageCatalogConfigured()) {
            createImageCatalogWithUrl(testContext, params.getImageCatalogName(), params.getImageCatalogUrl());
            dbTestContext.given(imageSettings, DistroXImageTestDto.class)
                    .withImageCatalog(params.getImageCatalogName())
                    .withImageId(params.getImageId());
        }
        DistroXTestDto currentContext = dbTestContext
                .given(DistroXTestDto.class).withImageSettingsIf(params.isImageCatalogConfigured(), imageSettings)
                .withExternalDatabase(DIX_EXTDB_KEY)
                .when(distroXTestClient.create())
                .await(STACK_AVAILABLE);
        for (int i = 0; i < params.getTimes(); i++) {
            currentContext = currentContext
                    .when(distroXTestClient.scale(params.getHostgroup(), params.getScaleUp()))
                    .await(STACK_AVAILABLE)
                    .when(distroXTestClient.stop())
                    .await(STACK_STOPPED)
                    .when(distroXTestClient.start())
                    .await(STACK_AVAILABLE)
                    .when(distroXTestClient.scale(params.getHostgroup(), params.getScaleDown()))
                    .await(STACK_AVAILABLE);
        }
        currentContext.validate();
    }

}
