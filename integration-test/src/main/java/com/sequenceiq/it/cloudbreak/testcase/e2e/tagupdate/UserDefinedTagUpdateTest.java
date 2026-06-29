package com.sequenceiq.it.cloudbreak.testcase.e2e.tagupdate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ITestContext;
import org.testng.annotations.Test;

import com.sequenceiq.distrox.api.v1.distrox.model.database.DistroXDatabaseAvailabilityType;
import com.sequenceiq.distrox.api.v1.distrox.model.database.DistroXDatabaseRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentEditRequest;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;
import com.sequenceiq.it.cloudbreak.assertion.util.CloudProviderSideTagAssertion;
import com.sequenceiq.it.cloudbreak.client.DistroXTestClient;
import com.sequenceiq.it.cloudbreak.client.EnvironmentTestClient;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.cloud.v4.CommonClusterManagerProperties;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.instancegroup.DistroXInstanceGroupTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.testcase.e2e.AbstractE2ETest;
import com.sequenceiq.sdx.api.model.SdxClusterShape;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;
import com.sequenceiq.sdx.api.model.SdxDatabaseAvailabilityType;
import com.sequenceiq.sdx.api.model.SdxDatabaseRequest;

public class UserDefinedTagUpdateTest extends AbstractE2ETest {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserDefinedTagUpdateTest.class);

    private static final Map<String, String> USER_DEFINED_TAGS = Map.of("test-user-defined-tag", "test-value");

    private List<String> datahubKeys = new ArrayList<>();

    @Inject
    private CommonClusterManagerProperties commonClusterManagerProperties;

    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private DistroXTestClient distroXTestClient;

    @Inject
    private EnvironmentTestClient environmentTestClient;

    @Inject
    private CloudProviderSideTagAssertion cloudProviderSideTagAssertion;

    @Override
    protected void setupTest(TestContext testContext) {
        testContext.getCloudProvider().getCloudFunctionality().cloudStorageInitialize();
        createDefaultUser(testContext);
        initializeDefaultBlueprints(testContext);
        createDefaultCredential(testContext);
        createDefaultEnvironment(testContext);

        SdxDatabaseRequest sdxDatabaseRequest = new SdxDatabaseRequest();
        sdxDatabaseRequest.setAvailabilityType(SdxDatabaseAvailabilityType.HA);

        testContext
                .given(SdxInternalTestDto.class)
                .withClusterShape(SdxClusterShape.ENTERPRISE)
                .withEnableMultiAz(true)
                .withDatabase(sdxDatabaseRequest)
                .withCloudStorage()
                .when(sdxTestClient.createInternal())
                .await(SdxClusterStatusResponse.RUNNING)
                .awaitForHealthyInstances()
                .validate();

        createDatahubsForEveryVariant(testContext);
    }

    private void createDatahubsForEveryVariant(TestContext testContext) {
        DistroXDatabaseRequest distroxDatabaseRequest = new DistroXDatabaseRequest();
        distroxDatabaseRequest.setAvailabilityType(DistroXDatabaseAvailabilityType.HA);

        List<String> variants = testContext.getCloudProvider().getDistroXVariants();
        if (variants == null || variants.isEmpty()) {
            String key = DistroXTestDto.class.getSimpleName();
            datahubKeys = List.of(key);
            initiateDatahubCreation(testContext, distroxDatabaseRequest, key, null);
            awaitDatahub(testContext, key);
        } else {
            datahubKeys = variants.stream()
                    .map(v -> DistroXTestDto.class.getSimpleName() + "-" + v.toLowerCase().replace("_", "-"))
                    .toList();
            for (int i = 0; i < variants.size(); i++) {
                initiateDatahubCreation(testContext, distroxDatabaseRequest, datahubKeys.get(i), variants.get(i));
            }
            datahubKeys.forEach(key -> awaitDatahub(testContext, key));
        }
    }

    private void initiateDatahubCreation(TestContext testContext, DistroXDatabaseRequest databaseRequest, String key, String variant) {
        DistroXTestDto dto = (DistroXTestDto) testContext
                .given(key, DistroXTestDto.class)
                .withTemplate(commonClusterManagerProperties.getDataEngDistroXBlueprintNameForCurrentRuntime())
                .withEnableMultiAz(true)
                .withExternalDatabase(databaseRequest)
                .withInstanceGroupsEntity(DistroXInstanceGroupTestDto.dataEngHostGroups(testContext, testContext.getCloudPlatform()))
                .withLoadBalancer();
        if (variant != null) {
            dto.withVariant(variant);
        }
        dto.when(distroXTestClient.create());
    }

    private void awaitDatahub(TestContext testContext, String key) {
        testContext
                .given(key, DistroXTestDto.class)
                .await(STACK_AVAILABLE)
                .awaitForHealthyInstances()
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT)
    @Description(
            given = "there is a running Cloudbreak, an SDX cluster and a DistroX for every variant in available state",
            when = "user defined tags are updated on the environment",
            then = "the tags are updated on the Environment, SDX and DistroX resources in the cloud provider side")
    public void testUserDefinedTagUpdate(TestContext testContext, ITestContext iTestContext) {
        EnvironmentEditRequest environmentEditRequest = new EnvironmentEditRequest();
        environmentEditRequest.setTags(USER_DEFINED_TAGS);
        testContext
                .given(EnvironmentTestDto.class)
                .when(environmentTestClient.triggerEnvironmentEdit(environmentEditRequest))
                .await(EnvironmentStatus.AVAILABLE)
                .given(SdxInternalTestDto.class)
                .await(SdxClusterStatusResponse.RUNNING);
        datahubKeys.forEach(key -> testContext
                .given(key, DistroXTestDto.class)
                .await(STACK_AVAILABLE));
        testContext
                .given(EnvironmentTestDto.class)
                .then(cloudProviderSideTagAssertion.verifyUserDefinedTags(USER_DEFINED_TAGS))
                .validate();
    }
}
