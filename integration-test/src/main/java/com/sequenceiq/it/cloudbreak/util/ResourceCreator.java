package com.sequenceiq.it.cloudbreak.util;

import static com.sequenceiq.it.cloudbreak.testcase.AbstractMinimalTest.STACK_AVAILABLE;

import java.io.IOException;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;
import com.sequenceiq.it.cloudbreak.ResourcePropertyProvider;
import com.sequenceiq.it.cloudbreak.action.v4.imagecatalog.ImageCatalogCreateRetryAction;
import com.sequenceiq.it.cloudbreak.actor.CloudbreakUser;
import com.sequenceiq.it.cloudbreak.client.BlueprintTestClient;
import com.sequenceiq.it.cloudbreak.client.CredentialTestClient;
import com.sequenceiq.it.cloudbreak.client.DistroXTestClient;
import com.sequenceiq.it.cloudbreak.client.EnvironmentTestClient;
import com.sequenceiq.it.cloudbreak.client.FreeIpaTestClient;
import com.sequenceiq.it.cloudbreak.client.RecipeTestClient;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.context.RunningParameter;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.blueprint.BlueprintTestDto;
import com.sequenceiq.it.cloudbreak.dto.credential.CredentialTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDtoBase;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.dto.imagecatalog.ImageCatalogTestDto;
import com.sequenceiq.it.cloudbreak.dto.recipe.RecipeTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxCloudStorageTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.mock.ImageCatalogMockServerSetup;
import com.sequenceiq.it.util.ResourceUtil;
import com.sequenceiq.sdx.api.model.SdxCloudStorageRequest;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;
import com.sequenceiq.sdx.api.model.SdxDatabaseAvailabilityType;
import com.sequenceiq.sdx.api.model.SdxDatabaseRequest;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@Component
public class ResourceCreator {

    @Inject
    private ResourcePropertyProvider resourcePropertyProvider;

    @Inject
    private CredentialTestClient credentialTestClient;

    @Inject
    private EnvironmentTestClient environmentTestClient;

    @Inject
    private FreeIpaTestClient freeIpaTestClient;

    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private RecipeTestClient recipeTestClient;

    @Inject
    private DistroXTestClient distroXTestClient;

    @Inject
    private BlueprintTestClient blueprintTestClient;

    @Inject
    private ImageCatalogMockServerSetup imageCatalogMockServerSetup;

    public CredentialTestDto createDefaultCredential(TestContext testContext) {
        return create(testContext
                .given(CredentialTestDto.class));
    }

    public CredentialTestDto createNewCredential(TestContext testContext) {
        String name = resourcePropertyProvider.getName();
        return create(testContext.given(name, CredentialTestDto.class).withName(name));
    }

    public EnvironmentTestDto createDefaultEnvironment(TestContext testContext) {
        return create(testContext.given(EnvironmentTestDto.class));
    }

    public EnvironmentTestDto createDefaultEnvironmentWithTelemetryFeaturesDisabled(TestContext testContext) {
        return create(testContext.given(EnvironmentTestDto.class).withTelemetryDisabled());
    }

    public EnvironmentTestDto createNewEnvironment(TestContext testContext) {
        String name = resourcePropertyProvider.getEnvironmentName();
        return create(testContext.given(name, EnvironmentTestDto.class)
                .withName(name));
    }

    public EnvironmentTestDto createNewEnvironment(TestContext testContext, CredentialTestDto credential) {
        String name = resourcePropertyProvider.getEnvironmentName();
        return create(testContext.given(name, EnvironmentTestDto.class)
                .withName(name)
                .withCredentialName(credential.getName()));
    }

    public FreeIpaTestDto createDefaultFreeIpa(TestContext testContext) {
        return create(testContext.given(FreeIpaTestDto.class));
    }

    public FreeIpaTestDto createNewFreeIpa(TestContext testContext, EnvironmentTestDto environment) {
        return create(testContext.given(FreeIpaTestDto.class).withEnvironment(environment));
    }

    public SdxInternalTestDto createDefaultDataLake(TestContext testContext) {
        SdxDatabaseRequest sdxDatabaseRequest = new SdxDatabaseRequest();
        sdxDatabaseRequest.setAvailabilityType(SdxDatabaseAvailabilityType.NONE);

        return create(testContext
                .given(SdxInternalTestDto.class)
                .withDatabase(sdxDatabaseRequest)
                .withCloudStorage(getCloudStorageRequest(testContext)));
    }

    public SdxInternalTestDto createNewDataLake(TestContext testContext, EnvironmentTestDto environment) {
        SdxDatabaseRequest sdxDatabaseRequest = new SdxDatabaseRequest();
        sdxDatabaseRequest.setAvailabilityType(SdxDatabaseAvailabilityType.NONE);
        String name = resourcePropertyProvider.getName();

        return create(testContext
                .given(name, SdxInternalTestDto.class)
                .withName(name)
                .withEnvironmentName(environment.getName())
                .withDatabase(sdxDatabaseRequest)
                .withCloudStorage(getCloudStorageRequest(testContext)));
    }

    public RecipeTestDto createDefaultRecipe(TestContext testContext) {
        return create(testContext.given(RecipeTestDto.class));
    }

    public RecipeTestDto createDefaultRecipeInternal(TestContext testContext, String accountId) {
        return createInternal(testContext.given(RecipeTestDto.class).withAccountId(accountId));
    }

    public BlueprintTestDto createDefaultBlueprintInternal(TestContext testContext, String accountId, String runtimeVersion) {
        try {
            String bluepint = ResourceUtil
                    .readResourceAsString(testContext.getApplicationContext(), "classpath:/blueprint/clouderamanager.bp")
                    .replaceAll("CDH_RUNTIME", runtimeVersion);
            BlueprintTestDto testDto = testContext.given(BlueprintTestDto.class)
                    .withAccountId(accountId)
                    .withBlueprint(bluepint)
                    .when(blueprintTestClient.createInternalV4());
            testDto.validate();
            return testDto;
        } catch (IOException e) {
            throw new TestFailException(e.getMessage());
        }
    }

    public RecipeTestDto createNewRecipe(TestContext testContext) {
        String name = resourcePropertyProvider.getName();
        return create(testContext.given(name, RecipeTestDto.class).withName(name));
    }

    public ImageCatalogTestDto createDefaultImageCatalog(TestContext testContext) {
        return create(testContext.given(ImageCatalogTestDto.class));
    }

    public ImageCatalogTestDto createNewImageCatalog(TestContext testContext) {
        String name = resourcePropertyProvider.getName();
        return create(testContext.given(name, ImageCatalogTestDto.class).withName(name));
    }

    public DistroXTestDto createDefaultDataHubAndWaitAs(TestContext testContext, CloudbreakUser waitingUser) {
        return createAndWaitAs(testContext.given(DistroXTestDto.class), waitingUser);
    }

    public DistroXTestDto createNewDataHubAndWaitAs(TestContext testContext, CloudbreakUser waitingUser) {
        String name = resourcePropertyProvider.getName();
        return createAndWaitAs(testContext.given(name, DistroXTestDto.class), waitingUser);
    }

    public DistroXTestDto createNewDataHubAndWaitAs(TestContext testContext, EnvironmentTestDto environment, CloudbreakUser waitingUser) {
        String name = resourcePropertyProvider.getName();
        return createAndWaitAs(testContext.given(name, DistroXTestDto.class).withEnvironmentName(environment.getName()), waitingUser);
    }

    public CredentialTestDto create(CredentialTestDto testDto) {
        testDto.when(credentialTestClient.create())
                .validate();
        return testDto;
    }

    public EnvironmentTestDto create(EnvironmentTestDto testDto) {
        testDto.withCreateFreeIpa(Boolean.FALSE)
                .when(environmentTestClient.create())
                .await(EnvironmentStatus.AVAILABLE)
                .when(environmentTestClient.describe())
                .validate();
        return testDto;
    }

    public FreeIpaTestDto create(FreeIpaTestDto testDto) {
        testDto.withCatalog(imageCatalogMockServerSetup.getFreeIpaImageCatalogUrl())
                .when(freeIpaTestClient.create())
                .awaitForCreationFlow()
                .validate();
        return testDto;
    }

    public SdxInternalTestDto create(SdxInternalTestDto testDto) {
        testDto.when(sdxTestClient.createInternal())
                .await(SdxClusterStatusResponse.RUNNING)
                .validate();
        return testDto;
    }

    public RecipeTestDto create(RecipeTestDto testDto) {
        testDto.when(recipeTestClient.createV4())
                .validate();
        return testDto;
    }

    public RecipeTestDto createInternal(RecipeTestDto testDto) {
        testDto.when(recipeTestClient.createInternalV4())
                .validate();
        return testDto;
    }

    public ImageCatalogTestDto create(ImageCatalogTestDto testDto) {
        testDto.when(new ImageCatalogCreateRetryAction())
                .validate();
        return testDto;
    }

    @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
    public DistroXTestDto createAndWaitAs(DistroXTestDtoBase<DistroXTestDto> testDto, CloudbreakUser waitingUser) {
        testDto.when(distroXTestClient.create());
        return waitForDistroX(testDto, waitingUser);
    }

    @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
    public DistroXTestDto createInternalAndWaitAs(DistroXTestDtoBase<DistroXTestDto> testDto, CloudbreakUser waitingUser) {
        testDto.when(distroXTestClient.createInternal());
        return waitForDistroX(testDto, waitingUser);
    }

    private DistroXTestDto waitForDistroX(DistroXTestDtoBase<DistroXTestDto> testDto, CloudbreakUser waitingUser) {
        testDto.await(STACK_AVAILABLE, RunningParameter.who(waitingUser));
        testDto.validate();
        return (DistroXTestDto) testDto;
    }

    private SdxCloudStorageRequest getCloudStorageRequest(TestContext testContext) {
        String storage = resourcePropertyProvider.getName();
        testContext.given(storage, SdxCloudStorageTestDto.class);

        SdxCloudStorageTestDto cloudStorage = testContext.getCloudProvider().cloudStorage(testContext.get(storage));
        if (cloudStorage == null) {
            throw new IllegalArgumentException("SDX Cloud Storage does not exist!");
        }

        return cloudStorage.getRequest();
    }

}
