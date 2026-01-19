package com.sequenceiq.it.cloudbreak.testcase.e2e.environment;

import jakarta.inject.Inject;

import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.common.api.type.ServiceEndpointCreation;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.FreeIpaImageResponse;
import com.sequenceiq.it.cloudbreak.ResourceGroupTest;
import com.sequenceiq.it.cloudbreak.assertion.Assertion;
import com.sequenceiq.it.cloudbreak.client.CredentialTestClient;
import com.sequenceiq.it.cloudbreak.client.EnvironmentTestClient;
import com.sequenceiq.it.cloudbreak.cloud.v4.azure.AzureProperties;
import com.sequenceiq.it.cloudbreak.config.azure.AzureMarketplaceImageProperties;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.credential.CredentialTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentNetworkTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.telemetry.TelemetryTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.microservice.EnvironmentClient;
import com.sequenceiq.it.cloudbreak.testcase.e2e.AbstractE2ETest;
import com.sequenceiq.it.cloudbreak.util.spot.UseSpotInstances;

public class AzureMarketplaceImageTest extends AbstractE2ETest {
    @Inject
    private EnvironmentTestClient environmentTestClient;

    @Inject
    private CredentialTestClient credentialTestClient;

    @Inject
    private AzureMarketplaceImageProperties azureMarketplaceImageProperties;

    @Inject
    private AzureProperties azureProperties;

    @Override
    protected void setupTest(TestContext testContext) {
        assertSupportedCloudPlatform(CloudPlatform.AZURE);
        createDefaultUser(testContext);
        initializeAzureMarketplaceTermsPolicy(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT)
    @UseSpotInstances
    @Description(
            given = "there is a running cloudbreak",
            when = "create an Environment with FreeIPA",
            then = "should use Marketplace image catalog and Marketplace image UUID")
    public void testCreateNewEnvironmentWithMarketplaceImage(TestContext testContext) {
        EnvironmentNetworkTestDto environmentNetworkTestDto = testContext.given("network", EnvironmentNetworkTestDto.class)
                    .withServiceEndpoints(ServiceEndpointCreation.DISABLED);
        environmentNetworkTestDto.getRequest().getAzure().setFlexibleServerSubnetIds(azureProperties.getNetwork().getFlexibleServerSubnetIds());
        environmentNetworkTestDto.getRequest().getAzure().setDatabasePrivateDnsZoneId(azureProperties.getNetwork().getDatabasePrivateDnsZoneId());
        testContext
                .given(CredentialTestDto.class)
                .when(credentialTestClient.create())
                .given("telemetry", TelemetryTestDto.class)
                    .withLogging()
                    .withReportClusterLogs()
                .given(EnvironmentTestDto.class)
                    .withResourceGroup(ResourceGroupTest.AZURE_RESOURCE_GROUP_USAGE_MULTIPLE, "")
                    .withNetwork("network")
                    .withTelemetry("telemetry")
                    .withCreateFreeIpa(Boolean.TRUE)
                    .withOneFreeIpaNode()
                    .withMarketplaceFreeIpaImage()
                .when(environmentTestClient.create())
                .awaitForCreationFlow()
                .then((tc, testDto, cc) -> environmentTestClient.describe().action(tc, testDto, cc))
                .then(assertMarketplaceImage())

                .validate();
    }

    public Assertion<EnvironmentTestDto, EnvironmentClient> assertMarketplaceImage() {
        return (testContext, testDto, environmentClient) -> {
            String catalogUrl = azureMarketplaceImageProperties.getCatalogUrl();
            String imageUuid = azureMarketplaceImageProperties.getImageUuid();

            DetailedEnvironmentResponse environment = environmentClient.getDefaultClient(testContext).environmentV1Endpoint().getByName(testDto.getName());
            FreeIpaImageResponse image = environment.getFreeIpa().getImage();
            if (!image.getCatalog().equals(catalogUrl)) {
                throw new IllegalArgumentException("Image catalog does not match the Marketplace image catalog!");
            }
            if (!image.getId().equals(imageUuid)) {
                throw new IllegalArgumentException("Image uuid does not match the Marketplace image uuid!");
            }
            if (environment.getTags().getUserDefined().isEmpty()) {
                throw new TestFailException("User defined tags aren't being set in the environment!");
            }
            return testDto;
        };
    }
}
