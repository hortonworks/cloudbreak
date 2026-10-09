package com.sequenceiq.it.cloudbreak.testcase.e2e.sdx;

import java.util.Comparator;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.BaseStackDetailsV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.BaseStackRepoDetailsV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.ImageV4Response;
import com.sequenceiq.cloudbreak.cloud.model.component.StackRepoDetails;
import com.sequenceiq.cloudbreak.util.CdhVersionProvider;
import com.sequenceiq.cloudbreak.util.VersionComparator;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.cloud.v4.CommonClusterManagerProperties;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.imagecatalog.ImageCatalogTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.testcase.e2e.AbstractE2ETest;
import com.sequenceiq.it.cloudbreak.util.CloudFunctionality;
import com.sequenceiq.it.cloudbreak.util.SdxUtil;

public class PreconditionSdxE2ETest extends AbstractE2ETest {

    private static final Logger LOGGER = LoggerFactory.getLogger(PreconditionSdxE2ETest.class);

    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private SdxUtil sdxUtil;

    @Inject
    private CommonClusterManagerProperties commonClusterManagerProperties;

    @Override
    protected void setupTest(TestContext testContext) {
        testContext.getCloudProvider().getCloudFunctionality().cloudStorageInitialize();
        createDefaultUser(testContext);
        createDefaultCredential(testContext);
        initializeAzureMarketplaceTermsPolicy(testContext);
        createEnvironmentWithFreeIpa(testContext);
    }

    protected SdxTestClient sdxTestClient() {
        return sdxTestClient;
    }

    protected SdxUtil sdxUtil() {
        return sdxUtil;
    }

    protected CommonClusterManagerProperties commonClusterManagerProperties() {
        return commonClusterManagerProperties;
    }

    protected String getDefaultSDXBlueprintName() {
        return commonClusterManagerProperties().getInternalSdxBlueprintName();
    }

    protected String getBaseLocation(SdxTestDto testDto) {
        return testDto.getRequest().getCloudStorage().getBaseLocation();
    }

    protected CloudFunctionality getCloudFunctionality(TestContext testContext) {
        return testContext.getCloudProvider().getCloudFunctionality();
    }

    protected String getLatestPrewarmedImageId(TestContext testContext) {
        AtomicReference<String> selectedImageID = new AtomicReference<>();
        testContext
                .given(ImageCatalogTestDto.class)
                .when((tc, dto, client) -> {
                    selectedImageID.set(tc.getCloudProvider().getLatestPreWarmedImageID(tc, dto, client));
                    return dto;
                });
        return selectedImageID.get();
    }

    protected String getQualifiedPrewarmedImageId(TestContext testContext, String runtimeVersion, String minimumQualifiedVersion) {
        AtomicReference<String> selectedImageId = new AtomicReference<>();
        testContext
                .given(ImageCatalogTestDto.class)
                .when((tc, dto, client) -> {
                    selectedImageId.set(findQualifiedPrewarmedImageId(tc, dto, client, runtimeVersion, minimumQualifiedVersion));
                    return dto;
                });
        return selectedImageId.get();
    }

    private String findQualifiedPrewarmedImageId(TestContext testContext, ImageCatalogTestDto imageCatalogTestDto, CloudbreakClient cloudbreakClient,
            String runtimeVersion, String minimumQualifiedVersion) {
        String platform = testContext.getCloudPlatform().name();
        try {
            return cloudbreakClient
                    .getDefaultClient(testContext)
                    .imageCatalogV4Endpoint()
                    .getImagesByName(cloudbreakClient.getWorkspaceId(), imageCatalogTestDto.getRequest().getName(), null,
                            platform, runtimeVersion, null, testContext.getCloudProvider().getGovCloud(), false, null)
                    .getCdhImages().stream()
                    .filter(image -> isQualified(image, minimumQualifiedVersion))
                    .max(Comparator.comparing(ImageV4Response::getPublished))
                    .map(ImageV4Response::getUuid)
                    .orElseThrow(() -> new TestFailException(String.format(
                            "Cannot find a pre-warmed image at '%s' provider for runtime '%s' qualified to at least '%s'!",
                            platform, runtimeVersion, minimumQualifiedVersion)));
        } catch (TestFailException e) {
            throw e;
        } catch (Exception e) {
            throw new TestFailException(String.format("Cannot fetch pre-warmed images from '%s' image catalog!", imageCatalogTestDto.getRequest().getName()),
                    e);
        }
    }

    private boolean isQualified(ImageV4Response image, String minimumQualifiedVersion) {
        String repositoryVersion = Optional.ofNullable(image.getStackDetails())
                .map(BaseStackDetailsV4Response::getRepository)
                .map(BaseStackRepoDetailsV4Response::getStack)
                .map(stack -> stack.get(StackRepoDetails.REPOSITORY_VERSION))
                .orElse(null);
        String qualifiedVersion = CdhVersionProvider.getCdhFullVersionFromVersionString(repositoryVersion);
        return StringUtils.isNotBlank(qualifiedVersion)
                && new VersionComparator().compare(() -> qualifiedVersion, () -> minimumQualifiedVersion) >= 0;
    }
}
