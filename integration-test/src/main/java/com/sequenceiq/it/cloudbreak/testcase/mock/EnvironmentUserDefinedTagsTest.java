package com.sequenceiq.it.cloudbreak.testcase.mock;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.expectedMessage;

import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.core.GenericType;

import org.testng.annotations.Test;

import com.sequenceiq.common.model.DefaultApplicationTag;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentEditRequest;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;
import com.sequenceiq.it.cloudbreak.client.DistroXTestClient;
import com.sequenceiq.it.cloudbreak.client.EnvironmentTestClient;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.microservice.FreeIpaClient;

public class EnvironmentUserDefinedTagsTest extends AbstractMockTest {

    private static final String FREEIPA_IMAGE_CATALOG_ID = "f6e778fc-7f17-4535-9021-515351df3691";

    private static final String TAG_A = "mock-tag-a";

    private static final String TAG_B = "mock-tag-b";

    private static final String TAG_C = "mock-tag-c";

    @Inject
    private EnvironmentTestClient environmentTestClient;

    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private DistroXTestClient distroXTestClient;

    @Override
    protected void setupTest(TestContext testContext) {
        createDefaultUser(testContext);
        createDefaultCredential(testContext);
        createDefaultImageCatalog(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running cloudbreak",
            when = "an environment with user defined tags is created together with a Data Lake and a Data Hub",
            then = "the user defined and the default tags are present on the mock cloud resources of every stack")
    public void createWithUserDefinedTags(MockedTestContext testContext) {
        Map<String, String> tags = Map.of(TAG_A, "created-a", TAG_B, "created-b");
        createEnvironmentWithDatalakeAndDatahub(testContext, tags)
                .given(EnvironmentTestDto.class)
                .when(environmentTestClient.describe())
                .then((tc, t, c) -> {
                    assertEnvironmentUserDefinedTags(t, tags, Set.of());
                    assertTagsOnAllStacks(tc, t, tags, Set.of());
                    assertDefaultTagsOnAllStacks(tc, t);
                    return t;
                })
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running environment with a user defined tag, a Data Lake and a Data Hub on the mock cloud provider",
            when = "new user defined tags are added and an existing one is overwritten with updating existing resources",
            then = "the environment tag modification flow finishes and every stack holds the merged tags on the mock cloud provider")
    public void updateUserDefinedTagsOnExistingResources(MockedTestContext testContext) {
        createEnvironmentWithDatalakeAndDatahub(testContext, Map.of(TAG_A, "v1"))
                .given(EnvironmentTestDto.class)
                .when(environmentTestClient.triggerEnvironmentEdit(tagEditRequest(Map.of(TAG_B, "v2", TAG_C, "v3"), true)))
                .awaitForFlow()
                .await(EnvironmentStatus.AVAILABLE)
                .when(environmentTestClient.describe())
                .then((tc, t, c) -> {
                    Map<String, String> expected = Map.of(TAG_A, "v1", TAG_B, "v2", TAG_C, "v3");
                    assertEnvironmentUserDefinedTags(t, expected, Set.of());
                    assertTagsOnAllStacks(tc, t, expected, Set.of());
                    return t;
                })

                .when(environmentTestClient.triggerEnvironmentEdit(tagEditRequest(Map.of(TAG_A, "v1-new"), true)))
                .awaitForFlow()
                .await(EnvironmentStatus.AVAILABLE)
                .when(environmentTestClient.describe())
                .then((tc, t, c) -> {
                    Map<String, String> expected = Map.of(TAG_A, "v1-new", TAG_B, "v2", TAG_C, "v3");
                    assertEnvironmentUserDefinedTags(t, expected, Set.of());
                    assertTagsOnAllStacks(tc, t, expected, Set.of());
                    assertDefaultTagsOnAllStacks(tc, t);
                    return t;
                })
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running environment with user defined tags, a Data Lake and a Data Hub on the mock cloud provider",
            when = "some of the user defined tags are deleted from the environment",
            then = "the deleted tags are removed from the environment and from every stack, the remaining and the default tags stay")
    public void deleteUserDefinedTags(MockedTestContext testContext) {
        createEnvironmentWithDatalakeAndDatahub(testContext, Map.of(TAG_A, "v1", TAG_B, "v2", TAG_C, "v3"))
                .given(EnvironmentTestDto.class)
                .when(environmentTestClient.deleteTags(Set.of(TAG_B, TAG_C)))
                .awaitForFlow()
                .await(EnvironmentStatus.AVAILABLE)
                .when(environmentTestClient.describe())
                .then((tc, t, c) -> {
                    assertEnvironmentUserDefinedTags(t, Map.of(TAG_A, "v1"), Set.of(TAG_B, TAG_C));
                    assertTagsOnAllStacks(tc, t, Map.of(TAG_A, "v1"), Set.of(TAG_B, TAG_C));
                    assertDefaultTagsOnAllStacks(tc, t);
                    return t;
                })
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running environment with FreeIPA on the mock cloud provider",
            when = "user defined tags are added, overwritten and then deleted one after the other",
            then = "after every step the FreeIPA stack holds exactly the expected user defined tags on the mock cloud provider")
    public void addOverwriteAndDeleteUserDefinedTagsOnFreeIpa(MockedTestContext testContext) {
        createEnvironmentWithFreeIpa(testContext, Map.of())
                .when(environmentTestClient.triggerEnvironmentEdit(tagEditRequest(Map.of(TAG_A, "v1", TAG_B, "v2"), true)))
                .awaitForFlow()
                .await(EnvironmentStatus.AVAILABLE)
                .then((tc, t, c) -> {
                    assertTags(getFreeIpaCrn(tc, t), Map.of(TAG_A, "v1", TAG_B, "v2"), Set.of());
                    return t;
                })

                .when(environmentTestClient.triggerEnvironmentEdit(tagEditRequest(Map.of(TAG_B, "v2-new"), true)))
                .awaitForFlow()
                .await(EnvironmentStatus.AVAILABLE)
                .then((tc, t, c) -> {
                    assertTags(getFreeIpaCrn(tc, t), Map.of(TAG_A, "v1", TAG_B, "v2-new"), Set.of());
                    return t;
                })

                .when(environmentTestClient.deleteTags(Set.of(TAG_A)))
                .awaitForFlow()
                .await(EnvironmentStatus.AVAILABLE)
                .then((tc, t, c) -> {
                    assertTags(getFreeIpaCrn(tc, t), Map.of(TAG_B, "v2-new"), Set.of(TAG_A));
                    return t;
                })

                .when(environmentTestClient.deleteTags(Set.of(TAG_B)))
                .awaitForFlow()
                .await(EnvironmentStatus.AVAILABLE)
                .when(environmentTestClient.describe())
                .then((tc, t, c) -> {
                    assertEnvironmentUserDefinedTags(t, Map.of(), Set.of(TAG_A, TAG_B));
                    assertTags(getFreeIpaCrn(tc, t), Map.of(), Set.of(TAG_A, TAG_B));
                    return t;
                })
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running environment with FreeIPA on the mock cloud provider",
            when = "user defined tags are edited without updating existing resources",
            then = "the environment stores the new tags but the existing FreeIPA cloud resources are left untouched")
    public void updateUserDefinedTagsWithoutUpdatingExistingResources(MockedTestContext testContext) {
        createEnvironmentWithFreeIpa(testContext, Map.of(TAG_A, "v1"))
                .when(environmentTestClient.triggerEnvironmentEdit(tagEditRequest(Map.of(TAG_A, "v1-new", TAG_B, "v2"), false)))
                .await(EnvironmentStatus.AVAILABLE)
                .when(environmentTestClient.describe())
                .then((tc, t, c) -> {
                    assertEnvironmentUserDefinedTags(t, Map.of(TAG_A, "v1-new", TAG_B, "v2"), Set.of());
                    assertTags(getFreeIpaCrn(tc, t), Map.of(TAG_A, "v1"), Set.of(TAG_B));
                    return t;
                })
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running environment with FreeIPA on the mock cloud provider",
            when = "deletion of a default tag is requested",
            then = "the request is rejected and the default tag stays on the FreeIPA cloud resources")
    public void deleteDefaultTagIsRejected(MockedTestContext testContext) {
        String protectedKey = DefaultApplicationTag.ENVIRONMENT_CRN.key();
        createEnvironmentWithFreeIpa(testContext, Map.of(TAG_A, "v1"))
                .whenException(environmentTestClient.deleteTags(Set.of(protectedKey)), BadRequestException.class,
                        expectedMessage("cannot be removed because they are default tag key\\(s\\)"))
                .then((tc, t, c) -> {
                    assertTags(getFreeIpaCrn(tc, t), Map.of(TAG_A, "v1", protectedKey, t.getResponse().getCrn()), Set.of());
                    return t;
                })
                .validate();
    }

    private EnvironmentTestDto createEnvironmentWithFreeIpa(MockedTestContext testContext, Map<String, String> tags) {
        return testContext
                .given(EnvironmentTestDto.class)
                .addTags(tags)
                .withCreateFreeIpa(true)
                .withOneFreeIpaNode()
                .withFreeIpaImage(getImageCatalogMockServerSetup().getFreeIpaImageCatalogUrl(), FREEIPA_IMAGE_CATALOG_ID)
                .when(environmentTestClient.create())
                .awaitForCreationFlow();
    }

    private DistroXTestDto createEnvironmentWithDatalakeAndDatahub(MockedTestContext testContext, Map<String, String> tags) {
        createEnvironmentWithFreeIpa(testContext, tags);
        return testContext
                .given(SdxInternalTestDto.class)
                .withEnvironment()
                .when(sdxTestClient.createInternal())
                .awaitForFlow()

                .given(DistroXTestDto.class)
                .withEnvironment()
                .when(distroXTestClient.create())
                .awaitForFlow();
    }

    private EnvironmentEditRequest tagEditRequest(Map<String, String> tags, boolean updateTagsOnExistingResources) {
        EnvironmentEditRequest request = new EnvironmentEditRequest();
        request.setUpdateTagsOnExistingResources(updateTagsOnExistingResources);
        request.setTags(tags);
        return request;
    }

    private void assertEnvironmentUserDefinedTags(EnvironmentTestDto environment, Map<String, String> expectedTags, Set<String> absentTagKeys) {
        Map<String, String> actualTags = environment.getResponse().getTags().getUserDefined();
        assertTagsMatch("environment " + environment.getName(), actualTags, expectedTags, absentTagKeys);
    }

    private void assertTagsOnAllStacks(TestContext testContext, EnvironmentTestDto environment, Map<String, String> expectedTags,
            Set<String> absentTagKeys) {
        getStackCrns(testContext, environment).forEach(crn -> assertTags(crn, expectedTags, absentTagKeys));
    }

    private void assertDefaultTagsOnAllStacks(TestContext testContext, EnvironmentTestDto environment) {
        Map<String, String> expectedDefaultTags = Map.of(DefaultApplicationTag.ENVIRONMENT_CRN.key(), environment.getResponse().getCrn());
        getStackCrns(testContext, environment).forEach(crn -> assertTags(crn, expectedDefaultTags, Set.of()));
    }

    private List<String> getStackCrns(TestContext testContext, EnvironmentTestDto environment) {
        return List.of(
                getFreeIpaCrn(testContext, environment),
                testContext.get(SdxInternalTestDto.class).getCrn(),
                testContext.get(DistroXTestDto.class).getCrn());
    }

    private String getFreeIpaCrn(TestContext testContext, EnvironmentTestDto environment) {
        return testContext.getMicroserviceClient(FreeIpaClient.class)
                .getDefaultClient(testContext)
                .getFreeIpaV1Endpoint()
                .describe(environment.getResponse().getCrn())
                .getCrn();
    }

    private void assertTags(String stackCrn, Map<String, String> expectedTags, Set<String> absentTagKeys) {
        Map<String, String> actualTags = getExecuteQueryToMockInfrastructure()
                .execute("/" + stackCrn + "/tags", response -> response.readEntity(new GenericType<Map<String, String>>() { }));
        assertTagsMatch("mock cloud resources of stack " + stackCrn, actualTags, expectedTags, absentTagKeys);
    }

    private void assertTagsMatch(String target, Map<String, String> actualTags, Map<String, String> expectedTags, Set<String> absentTagKeys) {
        expectedTags.forEach((key, value) -> {
            if (!value.equals(actualTags.get(key))) {
                throw new TestFailException(String.format("Tag '%s' should be '%s' on %s, but tags were: %s", key, value, target, actualTags));
            }
        });
        absentTagKeys.forEach(key -> {
            if (actualTags.containsKey(key)) {
                throw new TestFailException(String.format("Tag '%s' should not be present on %s, but tags were: %s", key, target, actualTags));
            }
        });
    }
}
