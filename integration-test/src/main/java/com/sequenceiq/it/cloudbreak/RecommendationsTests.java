package com.sequenceiq.it.cloudbreak;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.ForbiddenException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StreamUtils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequenceiq.cloudbreak.api.model.DiskResponse;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.VmTypeV4Response;
import com.sequenceiq.it.cloudbreak.newway.Blueprint;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakTest;
import com.sequenceiq.it.cloudbreak.newway.Credential;
import com.sequenceiq.it.cloudbreak.newway.Recommendation;
import com.sequenceiq.it.cloudbreak.newway.TestParameter;
import com.sequenceiq.it.cloudbreak.newway.cloud.CloudProvider;
import com.sequenceiq.it.cloudbreak.newway.cloud.CloudProviderHelper;
import com.sequenceiq.it.cloudbreak.newway.cloud.OpenstackCloudProvider;

public class RecommendationsTests extends CloudbreakTest {

    private static final String VALID_BP_NAME = "valid-blueprint";

    private static final String BP_DESCRIPTION = "temporary blueprint for API E2E tests";

    private static final Logger LOGGER = LoggerFactory.getLogger(RecommendationsTests.class);

    private CloudProvider cloudProvider;

    public RecommendationsTests() {
    }

    public RecommendationsTests(TestParameter tp) {
        setTestParameter(tp);
    }

    private String getBlueprintFile() throws IOException {
        return new String(StreamUtils.copyToByteArray(applicationContext.getResource("classpath:/blueprint/multi-node-hdfs-yarn.bp").getInputStream()));
    }

    private String getJsonFile() throws IOException {
        return new String(StreamUtils.copyToByteArray(applicationContext.getResource("classpath:/templates/diskResponses.json").getInputStream()));
    }

    private Set<DiskResponse> getCustomDiskResponses() throws IOException {
        ObjectMapper mapper = new ObjectMapper();

        return new HashSet<>(mapper.readValue(getJsonFile(), mapper.getTypeFactory().constructCollectionType(Set.class, DiskResponse.class)));
    }

    private void createBlueprint() throws Exception {
        given(CloudbreakClient.created());
        given(Blueprint.request().withName(VALID_BP_NAME).withDescription(BP_DESCRIPTION).withAmbariBlueprint(getBlueprintFile()));
        when(Blueprint.post());
    }

    private void deleteBlueprint() throws Exception {
        given(CloudbreakClient.created());
        given(Blueprint.request().withName(VALID_BP_NAME));
        when(Blueprint.delete());
    }

    private void deleteCredential() throws Exception {
        given(CloudbreakClient.created());
        given(Credential.request().withName(cloudProvider.getCredentialName()));
        when(Credential.delete());
    }

    @BeforeTest
    @Parameters("provider")
    public void beforeTest(@Optional(OpenstackCloudProvider.OPENSTACK) String provider) {
        LOGGER.info("Provider: {} Region test setup has been started.", provider);
        if (cloudProvider == null) {
            cloudProvider = CloudProviderHelper.providerFactory(provider, getTestParameter());
        } else {
            LOGGER.info("cloud provider already set - running from factory test");
        }
    }

    @BeforeClass
    public void setUp() throws Exception {
        LOGGER.info("Create blueprint with name ::: {}", VALID_BP_NAME);

        try {
            createBlueprint();
        } catch (ForbiddenException | BadRequestException e) {
            String exceptionMessage = e.getResponse().readEntity(String.class);
            String errorMessage = exceptionMessage.substring(exceptionMessage.lastIndexOf(':') + 1);
            LOGGER.info("Clean Up Exception message ::: {}", errorMessage);
        }
    }

    @AfterClass
    public void cleanUp() throws Exception {
        LOGGER.info("Delete {} blueprint and {} credential.", VALID_BP_NAME, cloudProvider.getCredentialName());

        try {
            deleteBlueprint();
            deleteCredential();
        } catch (ForbiddenException | BadRequestException e) {
            String exceptionMessage = e.getResponse().readEntity(String.class);
            String errorMessage = exceptionMessage.substring(exceptionMessage.lastIndexOf(':') + 1);
            LOGGER.info("Clean Up Exception message ::: {}", errorMessage);
        }
    }

    private String setCustomDiskResponses(Recommendation recommendations) {
        String expectedDisplayName = "";

        try {
            Set<DiskResponse> customDiskResponseSet = getCustomDiskResponses();
            expectedDisplayName = getCustomDiskResponses().iterator().next().getDisplayName();

            recommendations.getResponse().setDiskResponses(customDiskResponseSet);
        } catch (IOException e) {
            LOGGER.info("Set custom Disk Response Exception message ::: {}", e.getMessage());
        }

        return expectedDisplayName;
    }

    @Test(priority = 1, groups = "recommendations")
    public void testListRecommendations() throws Exception {
        given(CloudbreakClient.created());
        given(cloudProvider.aValidCredential(), cloudProvider.getPlatform() + " credential is created.");
        given(Recommendation.request()
                .withRegion(cloudProvider.region())
                .withAvailabilityZone(cloudProvider.availabilityZone()), " recommendation request");
        when(Recommendation.post(), "Recommendations are requested.");
        then(Recommendation.assertThis(
                (recommendations, t) -> {
                    Map<String, VmTypeV4Response> recommendationsForBlueprint = recommendations.getResponse().getRecommendations();

                    for (Entry<String, VmTypeV4Response> recommendationForBlueprint : recommendationsForBlueprint.entrySet()) {
                        LOGGER.debug("{} Recommendations are ::: {}", recommendationForBlueprint.getKey(), recommendationForBlueprint.getValue());
                        Assert.assertFalse(recommendationForBlueprint.getValue().getValue().isEmpty(),
                                "Recommendations should be present in response!");
                    }
                }), "Recommendations should be part of the response."
        );
    }

    @Test(priority = 2, groups = "recommendations")
    public void testListDiskResponses() throws Exception {
        given(CloudbreakClient.created());
        given(cloudProvider.aValidCredential(), cloudProvider.getPlatform() + " credential is created.");
        given(Recommendation.request()
                .withRegion(cloudProvider.region())
                .withAvailabilityZone(cloudProvider.availabilityZone()), " recommendation request");
        when(Recommendation.post(), "Recommendations are requested.");
        then(Recommendation.assertThis(
                (recommendations, t) -> {
                    Set<DiskResponse> diskResponseSet = recommendations.getResponse().getDiskResponses();

                    for (DiskResponse diskResponse : diskResponseSet) {
                        LOGGER.debug("Disk Response is ::: {}", diskResponse.getDisplayName());
                        Assert.assertFalse(diskResponse.getDisplayName().isEmpty(), "Disk Responses should be present in response!");
                    }
                }), "Disk Responses should be part of the response."
        );
    }

    @Test(priority = 3, groups = "recommendations")
    public void testListVirtualMachines() throws Exception {
        given(CloudbreakClient.created());
        given(cloudProvider.aValidCredential(), cloudProvider.getPlatform() + " credential is created.");
        given(Recommendation.request()
                .withRegion(cloudProvider.region())
                .withAvailabilityZone(cloudProvider.availabilityZone()), " recommendation request");
        when(Recommendation.post(), "Recommendations are requested.");
        then(Recommendation.assertThis(
                (recommendations, t) -> {
                    Set<VmTypeV4Response> virtualMachineSet = recommendations.getResponse().getVirtualMachines();

                    for (VmTypeV4Response virtualMachine : virtualMachineSet) {
                        LOGGER.debug("Virtual Machine is ::: {}", virtualMachine.getValue());
                        Assert.assertFalse(virtualMachine.getValue().isEmpty(), "Virtual Machines should be present in response!");
                    }
                }), "Virtual Machines should be part of the response."
        );
    }

    @Test(priority = 4, groups = "recommendations")
    public void testSetDiskResponses() throws Exception {
        given(CloudbreakClient.created());
        given(cloudProvider.aValidCredential(), cloudProvider.getPlatform() + " credential is created.");
        given(Recommendation.request()
                .withRegion(cloudProvider.region())
                .withAvailabilityZone(cloudProvider.availabilityZone()), " recommendation request");
        when(Recommendation.post(), "Recommendations are requested.");
        then(Recommendation.assertThis(
                (recommendations, t) -> {
                    String expectedDisplayName = setCustomDiskResponses(recommendations);
                    Set<DiskResponse> diskResponseSet = recommendations.getResponse().getDiskResponses();

                    for (DiskResponse diskResponse : diskResponseSet) {
                        LOGGER.debug("Custom Disk Response is ::: {}", diskResponse.getDisplayName());
                        Assert.assertEquals(diskResponse.getDisplayName(), expectedDisplayName, "HDD should be the custom displayName value!");
                    }
                }), "HDD should be the custom displayName value."
        );
    }
}