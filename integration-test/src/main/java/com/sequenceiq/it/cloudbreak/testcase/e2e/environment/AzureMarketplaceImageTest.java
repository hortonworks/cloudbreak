package com.sequenceiq.it.cloudbreak.testcase.e2e.environment;

import static com.sequenceiq.it.cloudbreak.config.azure.AzureMarketplaceImageProperties.AZURE_MARKETPLACE_FREEIPA_CATALOG_URL;
import static com.sequenceiq.it.cloudbreak.config.azure.AzureMarketplaceImageProperties.AZURE_MARKETPLACE_FREEIPA_IMAGE_UUID;

import javax.inject.Inject;

import org.testng.annotations.Ignore;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;
import com.sequenceiq.environment.api.v1.environment.model.response.FreeIpaImageResponse;
import com.sequenceiq.it.TestParameter;
import com.sequenceiq.it.cloudbreak.EnvironmentClient;
import com.sequenceiq.it.cloudbreak.assertion.Assertion;
import com.sequenceiq.it.cloudbreak.client.CredentialTestClient;
import com.sequenceiq.it.cloudbreak.client.EnvironmentTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.credential.CredentialTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.telemetry.TelemetryTestDto;
import com.sequenceiq.it.cloudbreak.testcase.e2e.AbstractE2ETest;
import com.sequenceiq.it.cloudbreak.util.spot.UseSpotInstances;

@Ignore("This test case should be re-enabled when azure marketplace images are recovered")
public class AzureMarketplaceImageTest extends AbstractE2ETest {

    @Inject
    private EnvironmentTestClient environmentTestClient;

    @Inject
    private CredentialTestClient credentialTestClient;

    @Override
    protected void setupTest(TestContext testContext) {
        checkCloudPlatform(CloudPlatform.AZURE);
        createDefaultUser(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT)
    @UseSpotInstances
    @Description(
            given = "there is a running cloudbreak",
            when = "create an Environment with FreeIPA",
            then = "should use Marketplace image catalog and Marketplace image UUID")
    public void testCreateNewEnvironmentWithMarketplaceImage(TestContext testContext) {

        testContext
                .given(CredentialTestDto.class)
                .when(credentialTestClient.create())
                .given("telemetry", TelemetryTestDto.class)
                .withLogging()
                .withReportClusterLogs()
                .given(EnvironmentTestDto.class)
                .withNetwork()
                .withTelemetry("telemetry")

                .withCreateFreeIpa(Boolean.TRUE)
                .withMarketplaceFreeIpaImage()
                .when(environmentTestClient.create())
                .await(EnvironmentStatus.AVAILABLE)
                .then((tc, testDto, cc) -> environmentTestClient.describe().action(tc, testDto, cc))
                .then(assertMarketplaceImage())

                .validate();
    }

    public static Assertion<EnvironmentTestDto, EnvironmentClient> assertMarketplaceImage() {
        return (testContext, testDto, environmentClient) -> {
            TestParameter testParameter = testContext
                    .given(EnvironmentTestDto.class)
                    .getTestParameter();
            String catalogUrl = testParameter.get(AZURE_MARKETPLACE_FREEIPA_CATALOG_URL);
            String imageUuid = testParameter.get(AZURE_MARKETPLACE_FREEIPA_IMAGE_UUID);

            DetailedEnvironmentResponse environment = environmentClient.getDefaultClient().environmentV1Endpoint().getByName(testDto.getName());
            FreeIpaImageResponse image = environment.getFreeIpa().getImage();
            if (!image.getCatalog().equals(catalogUrl)) {
                throw new IllegalArgumentException("Image catalog does not match the Marketplace image catalog!");
            }
            if (!image.getId().equals(imageUuid)) {
                throw new IllegalArgumentException("Image uuid does not match the Marketplace image uuid!");
            }
            return testDto;
        };
    }
}
