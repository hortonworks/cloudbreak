package com.sequenceiq.cloudbreak.service.image;

import static com.sequenceiq.cloudbreak.common.type.ComponentType.CDH_PRODUCT_DETAILS;
import static com.sequenceiq.cloudbreak.common.type.ComponentType.CM_REPO_DETAILS;
import static com.sequenceiq.cloudbreak.common.type.ComponentType.IMAGE;
import static com.sequenceiq.cloudbreak.service.image.ImageTestUtil.DEFAULT_REGION;
import static com.sequenceiq.cloudbreak.service.image.ImageTestUtil.INVALID_PLATFORM;
import static com.sequenceiq.cloudbreak.service.image.ImageTestUtil.OTHER_REGION;
import static com.sequenceiq.cloudbreak.service.image.ImageTestUtil.PLATFORM;
import static com.sequenceiq.cloudbreak.service.image.ImageTestUtil.REGION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.image.ImageSettingsV4Request;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.ImageStackDetails;
import com.sequenceiq.cloudbreak.cloud.model.catalog.StackRepoDetails;
import com.sequenceiq.cloudbreak.cloud.model.component.StackType;
import com.sequenceiq.cloudbreak.cluster.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.cmtemplate.utils.BlueprintUtils;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.service.PlatformStringTransformer;
import com.sequenceiq.cloudbreak.common.type.ComponentType;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.domain.ImageCatalog;
import com.sequenceiq.cloudbreak.domain.stack.Component;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.ComponentConfigProviderService;
import com.sequenceiq.cloudbreak.service.DefaultClouderaManagerRepoService;
import com.sequenceiq.cloudbreak.service.StackMatrixService;
import com.sequenceiq.cloudbreak.service.StackTypeResolver;
import com.sequenceiq.cloudbreak.service.parcel.ClouderaManagerProductTransformer;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.common.model.Architecture;
import com.sequenceiq.common.model.ImageCatalogPlatform;

public class ImageServiceTest {

    private static final Long WORKSPACE_ID = 1L;

    private static final Long STACK_ID = 100L;

    private static final String STACK_VERSION = "7.1.0";

    private static final String CUSTOM_IMAGE_CATALOG_URL = "http://localhost/custom-imagecatalog-url";

    private static final long ORG_ID = 100L;

    private static final String OS = "centos7";

    private static final long USER_ID = 1000L;

    private static final String USER_ID_STRING = "aUserId";

    private static final String EXISTING_ID = "ami-09fea90f257c85513";

    private static final String DEFAULT_REGION_EXISTING_ID = "ami-09fea90f257c85514";

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:1234:user:1";

    private static final String IMAGE_CATALOG_NAME = "aCatalog";

    private static final ImageCatalogPlatform IMAGE_CATALOG_PLATFORM = new ImageCatalogPlatform(PLATFORM);

    @Mock
    private ImageCatalogService imageCatalogService;

    @Mock
    private StackMatrixService stackMatrixService;

    @Mock
    private BlueprintUtils blueprintUtils;

    @Mock
    private CloudConnector cloudConnector;

    @Mock
    private PlatformStringTransformer platformStringTransformer;

    @Mock
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Mock
    private ComponentConfigProviderService componentConfigProviderService;

    @Mock
    private ClusterComponentConfigProvider clusterComponentConfigProvider;

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private StackTypeResolver stackTypeResolver;

    @Mock
    private ClouderaManagerProductTransformer clouderaManagerProductTransformer;

    @Mock
    private ComponentConverter componentConverter;

    @Mock
    private DefaultClouderaManagerRepoService clouderaManagerRepoService;

    @InjectMocks
    private ImageService underTest;

    @Captor
    private ArgumentCaptor<ImageFilter> imageFilterCaptor;

    private ImageSettingsV4Request imageSettingsV4Request;

    private static Stream<Arguments> baseImageFlags() {
        return Stream.of(
                Arguments.of(true, true),
                Arguments.of(true, false),
                Arguments.of(false, true),
                Arguments.of(false, false)
        );
    }

    @BeforeEach
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        imageSettingsV4Request = new ImageSettingsV4Request();
        imageSettingsV4Request.setCatalog(IMAGE_CATALOG_NAME);
        imageSettingsV4Request.setId("anImageId");
        imageSettingsV4Request.setOs(OS);
        when(blueprintUtils.getCDHStackVersion(any())).thenReturn(STACK_VERSION);
        when(imageCatalogService.getImageCatalogByName(WORKSPACE_ID, IMAGE_CATALOG_NAME)).thenReturn(getImageCatalog());
        CloudConnector connector = mock(CloudConnector.class);
        when(cloudPlatformConnectors.getDefault(Platform.platform(PLATFORM))).thenReturn(connector);
        when(cloudPlatformConnectors.getDefault(Platform.platform(CloudPlatform.YARN.name()))).thenReturn(connector);
        when(connector.regionToDisplayName(REGION)).thenReturn(REGION);
        when(platformStringTransformer.getPlatformStringForImageCatalog(PLATFORM, PLATFORM)).thenReturn(IMAGE_CATALOG_PLATFORM);
        when(stackMatrixService.getSupportedOperatingSystems(WORKSPACE_ID, STACK_VERSION, IMAGE_CATALOG_PLATFORM, OS, Architecture.X86_64, IMAGE_CATALOG_NAME))
                .thenReturn(Set.of(OS));
    }

    @Test
    public void testUseBaseImageAndDisabledBaseImageShouldReturnError() {
        imageSettingsV4Request.setId(null);
        CloudbreakImageCatalogException exception = assertThrows(CloudbreakImageCatalogException.class, () ->
                underTest.determineImageFromCatalog(
                        WORKSPACE_ID,
                        imageSettingsV4Request,
                        Architecture.X86_64,
                        PLATFORM,
                        PLATFORM,
                        TestUtil.blueprint(),
                        true,
                        false,
                        TestUtil.user(USER_ID, USER_ID_STRING),
                        image -> true));
        assertEquals("Inconsistent request, base images are disabled but custom repo information is submitted!", exception.getMessage());
    }

    @ParameterizedTest
    @MethodSource("baseImageFlags")
    public void theProvidedPrewarmedImageShouldBeUsedRegardlessBaseImageFlags(boolean useBaseImage, boolean baseImageEnabled)
            throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        StatedImage expected = ImageTestUtil.getImageFromCatalog(true, "uuid", STACK_VERSION);
        when(imageCatalogService.getImageByCatalogName(anyLong(), anyString(), anyString()))
                .thenReturn(expected);
        StatedImage actual = underTest.determineImageFromCatalog(
                WORKSPACE_ID,
                imageSettingsV4Request,
                Architecture.X86_64,
                PLATFORM,
                PLATFORM,
                TestUtil.blueprint(),
                useBaseImage,
                baseImageEnabled,
                TestUtil.user(USER_ID, USER_ID_STRING),
                image -> true);
        assertEquals(expected, actual);
    }

    @Test
    public void testDetermineImageFromCatalogWithNonExistingCatalogNameAndIdSpecified()
            throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        when(imageCatalogService.getImageByCatalogName(WORKSPACE_ID, "anImageId", IMAGE_CATALOG_NAME))
                .thenThrow(new CloudbreakImageCatalogException("Image catalog not found with name: aCatalog"));

        CloudbreakImageCatalogException exception = assertThrows(CloudbreakImageCatalogException.class, () ->
                underTest.determineImageFromCatalog(
                        WORKSPACE_ID,
                        imageSettingsV4Request,
                        Architecture.X86_64,
                        PLATFORM,
                        PLATFORM,
                        TestUtil.blueprint(),
                        true,
                        true,
                        TestUtil.user(USER_ID, USER_ID_STRING),
                        image -> true));

        assertEquals("Image catalog not found with name: aCatalog", exception.getMessage());
    }

    @Test
    public void testDetermineImageFromCatalogWithNonExistingCatalogName() {
        when(imageCatalogService.getImageCatalogByName(WORKSPACE_ID, IMAGE_CATALOG_NAME))
                .thenThrow(new NotFoundException("Image catalog not found with name: aCatalog"));
        ImageSettingsV4Request imageRequest = new ImageSettingsV4Request();
        imageRequest.setCatalog(IMAGE_CATALOG_NAME);
        imageRequest.setOs(OS);

        CloudbreakImageCatalogException exception = assertThrows(CloudbreakImageCatalogException.class, () ->
                underTest.determineImageFromCatalog(
                        WORKSPACE_ID,
                        imageRequest,
                        Architecture.X86_64,
                        PLATFORM,
                        PLATFORM,
                        TestUtil.blueprint(),
                        true,
                        true,
                        TestUtil.user(USER_ID, USER_ID_STRING),
                        image -> true));

        assertEquals("Image catalog not found with name: aCatalog", exception.getMessage());
    }

    @Test
    public void testGivenBaseImageIdAndDisabledBaseImageShouldReturnError() throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        when(imageCatalogService.getImageByCatalogName(anyLong(), anyString(), anyString()))
                .thenReturn(ImageTestUtil.getImageFromCatalog(false, "uuid", STACK_VERSION));
        CloudbreakImageCatalogException exception = assertThrows(CloudbreakImageCatalogException.class, () ->
                underTest.determineImageFromCatalog(
                        WORKSPACE_ID,
                        imageSettingsV4Request,
                        Architecture.X86_64,
                        PLATFORM,
                        PLATFORM,
                        TestUtil.blueprint(),
                        false,
                        false,
                        TestUtil.user(USER_ID, USER_ID_STRING),
                        image -> true));
        assertEquals("Inconsistent request, base images are disabled but image with id uuid is base image!", exception.getMessage());
    }

    @Test
    public void testGivenNoImageIdAndNoArchitectureShouldReturnX86Image() throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        imageSettingsV4Request.setId(null);
        when(imageCatalogService.getLatestImageDefaultPreferred(imageFilterCaptor.capture(), eq(false)))
                .thenReturn(ImageTestUtil.getImageFromCatalog(true, "uuid", STACK_VERSION));
        when(platformStringTransformer.getPlatformStringForImageCatalog(anyString(), anyString())).thenReturn(IMAGE_CATALOG_PLATFORM);
        StatedImage statedImage = underTest.determineImageFromCatalog(
                WORKSPACE_ID,
                imageSettingsV4Request,
                null,
                PLATFORM,
                PLATFORM,
                TestUtil.blueprint(),
                false,
                false,
                TestUtil.user(USER_ID, USER_ID_STRING),
                image -> true);
        assertEquals("uuid", statedImage.getImage().getUuid());
        assertTrue(statedImage.getImage().isPrewarmed());
        assertEquals(Architecture.X86_64, imageFilterCaptor.getValue().getArchitecture());
    }

    @Test
    public void testGivenBaseImageIdAndTheOSIsNotSupportedByTheTemplateShouldReturnError() throws Exception {
        imageSettingsV4Request.setOs(null);
        StatedImage image = ImageTestUtil.getImageFromCatalog(false, "uuid", STACK_VERSION);
        when(stackMatrixService.getSupportedOperatingSystems(WORKSPACE_ID, STACK_VERSION, IMAGE_CATALOG_PLATFORM,
                image.getImage().getOs(), Architecture.X86_64, image.getImageCatalogName()))
                .thenReturn(Set.of("redhat8"));
        when(imageCatalogService.getImageByCatalogName(anyLong(), anyString(), anyString()))
                .thenReturn(image);
        CloudbreakImageCatalogException exception = assertThrows(CloudbreakImageCatalogException.class, () ->
                underTest.determineImageFromCatalog(
                        WORKSPACE_ID,
                        imageSettingsV4Request,
                        Architecture.X86_64,
                        PLATFORM,
                        PLATFORM,
                        TestUtil.blueprint(),
                        true,
                        true,
                        TestUtil.user(USER_ID, USER_ID_STRING),
                        img -> true));
        assertEquals("The centos7 OS of the selected base image (uuid) is not compatible with the runtime version 7.1.0.", exception.getMessage());
    }

    @Test
    public void testGivenImageIdAndTheOSIsSameShouldReturnImage() throws Exception {
        imageSettingsV4Request.setOs("centos7");
        StatedImage image = ImageTestUtil.getImageFromCatalog(false, "uuid", STACK_VERSION);
        when(stackMatrixService.getSupportedOperatingSystems(WORKSPACE_ID, STACK_VERSION, IMAGE_CATALOG_PLATFORM,
                image.getImage().getOs(), Architecture.X86_64, image.getImageCatalogName()))
                .thenReturn(Set.of("centos7"));
        when(imageCatalogService.getImageByCatalogName(anyLong(), anyString(), anyString()))
                .thenReturn(image);
        StatedImage statedImage = underTest.determineImageFromCatalog(
                WORKSPACE_ID,
                imageSettingsV4Request,
                Architecture.X86_64,
                PLATFORM,
                PLATFORM,
                TestUtil.blueprint(),
                true,
                true,
                TestUtil.user(USER_ID, USER_ID_STRING),
                img -> true);

        assertEquals("uuid", statedImage.getImage().getUuid());
        assertEquals("centos7", statedImage.getImage().getOs());
    }

    @Test
    public void testGivenImageIdAndTheOSIsDifferentShouldReturnError() throws Exception {
        imageSettingsV4Request.setOs("redhat8");
        StatedImage image = ImageTestUtil.getImageFromCatalog(false, "uuid", STACK_VERSION);
        when(stackMatrixService.getSupportedOperatingSystems(WORKSPACE_ID, STACK_VERSION, IMAGE_CATALOG_PLATFORM,
                image.getImage().getOs(), Architecture.X86_64, image.getImageCatalogName()))
                .thenReturn(Set.of("centos7", "redhat8"));
        when(imageCatalogService.getImageByCatalogName(anyLong(), anyString(), anyString()))
                .thenReturn(image);
        CloudbreakImageCatalogException exception = assertThrows(CloudbreakImageCatalogException.class, () ->
                underTest.determineImageFromCatalog(
                        WORKSPACE_ID,
                        imageSettingsV4Request,
                        Architecture.X86_64,
                        PLATFORM,
                        PLATFORM,
                        TestUtil.blueprint(),
                        true,
                        true,
                        TestUtil.user(USER_ID, USER_ID_STRING),
                        img -> true));
        assertEquals("Image with requested id has different os than requested.", exception.getMessage());
    }

    @Test
    public void testGivenPrewarmedImageIdAndDisabledBaseImageShouldReturnOk() throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        when(imageCatalogService.getImageByCatalogName(anyLong(), anyString(), anyString()))
                .thenReturn(ImageTestUtil.getImageFromCatalog(true, "uuid", STACK_VERSION));
        StatedImage statedImage = underTest.determineImageFromCatalog(
                WORKSPACE_ID,
                imageSettingsV4Request,
                Architecture.X86_64,
                PLATFORM,
                PLATFORM,
                TestUtil.blueprint(),
                false,
                false,
                TestUtil.user(USER_ID, USER_ID_STRING),
                image -> true);
        assertEquals("uuid", statedImage.getImage().getUuid());
        assertTrue(statedImage.getImage().isPrewarmed());
    }

    @Test
    public void testGivenBaseImageIdAndEnabledBaseImageShouldReturnOk() throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        when(imageCatalogService.getImageByCatalogName(anyLong(), anyString(), anyString()))
                .thenReturn(ImageTestUtil.getImageFromCatalog(false, "uuid", STACK_VERSION));
        StatedImage statedImage = underTest.determineImageFromCatalog(
                WORKSPACE_ID,
                imageSettingsV4Request,
                Architecture.X86_64,
                PLATFORM,
                PLATFORM,
                TestUtil.blueprint(),
                false,
                true,
                TestUtil.user(USER_ID, USER_ID_STRING),
                image -> true);
        assertEquals("uuid", statedImage.getImage().getUuid());
        assertFalse(statedImage.getImage().isPrewarmed());
    }

    @Test
    public void testGivenPrewarmedImageIdAndEnabledBaseImageShouldReturnOk() throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        when(imageCatalogService.getImageByCatalogName(anyLong(), anyString(), anyString()))
                .thenReturn(ImageTestUtil.getImageFromCatalog(true, "uuid", STACK_VERSION));
        StatedImage statedImage = underTest.determineImageFromCatalog(
                WORKSPACE_ID,
                imageSettingsV4Request,
                Architecture.X86_64,
                PLATFORM,
                PLATFORM,
                TestUtil.blueprint(),
                false,
                true,
                TestUtil.user(USER_ID, USER_ID_STRING),
                image -> true);
        assertEquals("uuid", statedImage.getImage().getUuid());
        assertTrue(statedImage.getImage().isPrewarmed());
    }

    @Test
    public void testUseBaseImageAndEnabledBaseImageShouldReturnCorrectImage() throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        imageSettingsV4Request.setId(null);
        when(imageCatalogService.getLatestImageDefaultPreferred(imageFilterCaptor.capture(), eq(true)))
                .thenReturn(ImageTestUtil.getImageFromCatalog(false, "uuid", STACK_VERSION));
        when(platformStringTransformer.getPlatformStringForImageCatalog(anyString(), anyString())).thenReturn(IMAGE_CATALOG_PLATFORM);
        StatedImage statedImage = underTest.determineImageFromCatalog(
                WORKSPACE_ID,
                imageSettingsV4Request,
                Architecture.X86_64,
                PLATFORM,
                PLATFORM,
                TestUtil.blueprint(),
                true,
                true,
                TestUtil.user(USER_ID, USER_ID_STRING),
                image -> true);
        assertEquals("uuid", statedImage.getImage().getUuid());
        assertFalse(statedImage.getImage().isPrewarmed());
        assertNull(imageFilterCaptor.getValue().getClusterVersion());
    }

    @Test
    public void testNoUseBaseImageAndEnabledBaseImageShouldReturnCorrectImage() throws CloudbreakImageCatalogException, CloudbreakImageNotFoundException {
        imageSettingsV4Request.setId(null);
        when(imageCatalogService.getLatestImageDefaultPreferred(imageFilterCaptor.capture(), eq(false)))
                .thenReturn(ImageTestUtil.getImageFromCatalog(false, "uuid", STACK_VERSION));
        when(platformStringTransformer.getPlatformStringForImageCatalog(anyString(), anyString())).thenReturn(IMAGE_CATALOG_PLATFORM);
        StatedImage statedImage = underTest.determineImageFromCatalog(
                WORKSPACE_ID,
                imageSettingsV4Request,
                Architecture.X86_64,
                PLATFORM,
                PLATFORM,
                TestUtil.blueprint(),
                false,
                true,
                TestUtil.user(USER_ID, USER_ID_STRING),
                image -> true);
        assertEquals("uuid", statedImage.getImage().getUuid());
        assertFalse(statedImage.getImage().isPrewarmed());
        assertEquals(STACK_VERSION, imageFilterCaptor.getValue().getClusterVersion());
    }

    @Test
    public void testDisabledBaseImageShouldReturnCorrectImage() throws CloudbreakImageCatalogException, CloudbreakImageNotFoundException {
        imageSettingsV4Request.setId(null);
        when(imageCatalogService.getLatestImageDefaultPreferred(imageFilterCaptor.capture(), eq(false)))
                .thenReturn(ImageTestUtil.getImageFromCatalog(true, "uuid", STACK_VERSION));
        when(platformStringTransformer.getPlatformStringForImageCatalog(anyString(), anyString())).thenReturn(IMAGE_CATALOG_PLATFORM);
        StatedImage statedImage = underTest.determineImageFromCatalog(
                WORKSPACE_ID,
                imageSettingsV4Request,
                Architecture.X86_64,
                PLATFORM,
                PLATFORM,
                TestUtil.blueprint(),
                false,
                false,
                TestUtil.user(USER_ID, USER_ID_STRING),
                image -> true);
        assertEquals("uuid", statedImage.getImage().getUuid());
        assertTrue(statedImage.getImage().isPrewarmed());
        assertEquals(STACK_VERSION, imageFilterCaptor.getValue().getClusterVersion());
    }

    @Test
    public void testUpdateImageName() throws CloudbreakImageNotFoundException, IOException {
        com.sequenceiq.cloudbreak.cloud.model.Image imageModel = new com.sequenceiq.cloudbreak.cloud.model.Image(
                "anImage", new HashMap<>(), "centos7", "redhat7", "arch", "", "default", "default-id", new HashMap<>(), "2019-10-24", 1571884856L);
        Component imageComponent = createImageComponent(imageModel);
        when(componentConfigProviderService.getImageComponent(STACK_ID)).thenReturn(imageComponent);

        underTest.updateImageNameForImageComponent(STACK_ID, "aNewImage");

        ArgumentCaptor<Component> componentCaptor = ArgumentCaptor.forClass(Component.class);
        verify(componentConfigProviderService).store(componentCaptor.capture());
        assertEquals("aNewImage", componentCaptor.getValue().getAttributes().get(com.sequenceiq.cloudbreak.cloud.model.Image.class).getImageName());
    }

    private Component createImageComponent(com.sequenceiq.cloudbreak.cloud.model.Image image) {
        return new Component(ComponentType.IMAGE, "target", new Json(image), null);
    }

    @Test
    public void testDetermineImageNameFound() {
        Image image = mock(Image.class);
        when(image.getImageSetsByProvider()).thenReturn(Collections.singletonMap(PLATFORM, Collections.singletonMap(REGION, EXISTING_ID)));

        String imageName = ThreadBasedUserCrnProvider.doAs(USER_CRN,
                () -> {
                    try {
                        return underTest.determineImageName(PLATFORM, IMAGE_CATALOG_PLATFORM, REGION, image);
                    } catch (CloudbreakImageNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                });
        assertEquals("ami-09fea90f257c85513", imageName);
    }

    @Test
    public void testDetermineImageNameFoundYCloud() {
        Image image = mock(Image.class);
        String platform = CloudPlatform.YARN.name().toLowerCase(Locale.ROOT);
        when(entitlementService.azureMarketplaceImagesEnabled(any())).thenReturn(false);

        when(image.getImageSetsByProvider()).thenReturn(Collections.singletonMap(platform,
                Map.of(
                        REGION, EXISTING_ID,
                        DEFAULT_REGION, DEFAULT_REGION_EXISTING_ID)));

        String imageName = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            try {
                return underTest.determineImageName(platform, new ImageCatalogPlatform(platform), null, image);
            } catch (CloudbreakImageNotFoundException e) {
                throw new RuntimeException(e);
            }
        });
        assertEquals("ami-09fea90f257c85514", imageName);
    }

    @Test
    public void testDetermineImageNameFoundDefaultPreferred() {
        Image image = mock(Image.class);
        when(entitlementService.azureMarketplaceImagesEnabled(any())).thenReturn(true);

        when(image.getImageSetsByProvider()).thenReturn(Collections.singletonMap(PLATFORM,
                Map.of(
                        REGION, EXISTING_ID,
                        DEFAULT_REGION, DEFAULT_REGION_EXISTING_ID)));

        String imageName = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            try {
                return underTest.determineImageName(PLATFORM, IMAGE_CATALOG_PLATFORM, REGION, image);
            } catch (CloudbreakImageNotFoundException e) {
                throw new RuntimeException(e);
            }
        });
        assertEquals("ami-09fea90f257c85514", imageName);
    }

    @Test
    public void testDetermineImageNameNotDefaultPreferredFallbackToRegion() {
        Image image = mock(Image.class);
        when(entitlementService.azureMarketplaceImagesEnabled(any())).thenReturn(true);

        when(image.getImageSetsByProvider()).thenReturn(Collections.singletonMap(PLATFORM,
                Map.of(REGION, EXISTING_ID)));

        String imageName = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            try {
                return underTest.determineImageName(PLATFORM, IMAGE_CATALOG_PLATFORM, REGION, image);
            } catch (CloudbreakImageNotFoundException e) {
                throw new RuntimeException(e);
            }
        });
        assertEquals("ami-09fea90f257c85513", imageName);
    }

    @Test
    public void testDetermineImageNameNotFoundDefaultNorFallback() {
        Image image = mock(Image.class);
        when(entitlementService.azureMarketplaceImagesEnabled(any())).thenReturn(true);

        when(image.getImageSetsByProvider()).thenReturn(Collections.singletonMap(PLATFORM,
                Map.of(OTHER_REGION, EXISTING_ID)));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> ThreadBasedUserCrnProvider.doAs(USER_CRN,
                        () -> {
                            try {
                                return underTest.determineImageName(PLATFORM, IMAGE_CATALOG_PLATFORM, REGION, image);
                            } catch (CloudbreakImageNotFoundException e) {
                                throw new RuntimeException(e);
                            }
                        }));
        assertEquals("com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException: " +
                "The preferred Azure Marketplace image is not present and virtual machine image could not be found in the West US 2 region. " +
                "Available images are as follows: {West US 3=ami-09fea90f257c85513}.", exception.getMessage());
    }

    @Test
    public void testDetermineImageNameFoundNoMpEntitlement() {
        Image image = mock(Image.class);
        when(entitlementService.azureMarketplaceImagesEnabled(any())).thenReturn(false);

        when(image.getImageSetsByProvider()).thenReturn(Collections.singletonMap(PLATFORM,
                Map.of(
                        REGION, EXISTING_ID,
                        DEFAULT_REGION, DEFAULT_REGION_EXISTING_ID)));

        String imageName = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            try {
                return underTest.determineImageName(PLATFORM, IMAGE_CATALOG_PLATFORM, REGION, image);
            } catch (CloudbreakImageNotFoundException e) {
                throw new RuntimeException(e);
            }
        });
        assertEquals("ami-09fea90f257c85513", imageName);
    }

    @Test
    public void testDetermineImageNameNotFound() {
        Image image = mock(Image.class);
        when(image.getImageSetsByProvider()).thenReturn(Collections.singletonMap(PLATFORM, Collections.singletonMap(REGION, EXISTING_ID)));

        Exception exception = ThreadBasedUserCrnProvider.doAs(USER_CRN,
                () -> assertThrows(CloudbreakImageNotFoundException.class,
                        () -> underTest.determineImageName(PLATFORM, IMAGE_CATALOG_PLATFORM, "fake-region", image)));
        String exceptionMessage = "The virtual machine image couldn't be found for azure";
        MatcherAssert.assertThat(exception.getMessage(), CoreMatchers.containsString(exceptionMessage));
    }

    @Test
    public void testDetermineImageNameImageForPlatformNotFound() {
        Image image = mock(Image.class);
        when(image.getImageSetsByProvider()).thenReturn(Collections.singletonMap(INVALID_PLATFORM, Collections.singletonMap(REGION, EXISTING_ID)));
        when(image.toString()).thenReturn("Image");

        Exception exception = assertThrows(CloudbreakImageNotFoundException.class, () ->
                underTest.determineImageName(PLATFORM, IMAGE_CATALOG_PLATFORM, REGION, image));
        String exceptionMessage = "The selected image: 'Image' "
                + "doesn't contain virtual machine image for the selected platform: 'ImageCatalogPlatform{platform='azure'}'.";
        MatcherAssert.assertThat(exception.getMessage(), CoreMatchers.containsString(exceptionMessage));
    }

    @Test
    public void testGetComponents() throws CloudbreakImageCatalogException {
        Stack stack = new Stack();
        stack.setCloudPlatform(PLATFORM);
        stack.setPlatformVariant("VARIANT");
        stack.setRegion(REGION);


        when(platformStringTransformer.getPlatformStringForImageCatalog(anyString(), anyString())).thenReturn(IMAGE_CATALOG_PLATFORM);

        CloudConnector connector = mock(CloudConnector.class);
        when(cloudPlatformConnectors.getDefault(Platform.platform(PLATFORM))).thenReturn(connector);
        when(connector.regionToDisplayName(REGION)).thenReturn(REGION);

        Image image = mock(Image.class);
        when(image.getImageSetsByProvider()).thenReturn(Collections.singletonMap(PLATFORM, Collections.singletonMap(REGION, EXISTING_ID)));

        StackRepoDetails stackRepoDetails = new StackRepoDetails(Map.of(StackRepoDetails.REPO_ID_TAG, "CDH"), null);
        ImageStackDetails imageStackDetails = new ImageStackDetails("7.2.17", stackRepoDetails, "123");
        when(image.getStackDetails()).thenReturn(imageStackDetails);
        when(image.getOsType()).thenReturn("redhat8");
        when(stackTypeResolver.determineStackType(any())).thenReturn(StackType.CDH);


        StatedImage statedImage = StatedImage.statedImage(image, "https://url.com", "my-cayalog");

        Set<Component> components = ThreadBasedUserCrnProvider.doAs(USER_CRN,
                () -> {
                    try {
                        return underTest.getComponents(stack, statedImage, EnumSet.of(IMAGE, CDH_PRODUCT_DETAILS, CM_REPO_DETAILS));
                    } catch (CloudbreakImageNotFoundException | CloudbreakImageCatalogException e) {
                        throw new RuntimeException(e);
                    }
                });

        assertEquals(3, components.size());
        for (Component component : components) {
            ComponentType componentType = component.getComponentType();
            assertTrue(EnumSet.of(IMAGE, CDH_PRODUCT_DETAILS, CM_REPO_DETAILS).contains(componentType));
            if (componentType == IMAGE) {
                assertEquals(EXISTING_ID, component.getAttributes().getValue("imageName"));
            }
        }
    }

    @Test
    public void testGetSupportedImdsIfPlatformNotAws() {
        assertFalse(underTest.getSupportedImdsVersion("AZURE", StatedImage.statedImage(null, null, null)).isPresent());
    }

    @Test
    public void testGetSupportedImdsIfPkgVersionMissing() {
        Optional<String> supportedImdsVersion = underTest.getSupportedImdsVersion("AWS", StatedImage.statedImage(Image.builder().build(), null, null));
        assertTrue(supportedImdsVersion.isPresent());
        assertEquals("v1", supportedImdsVersion.get());
    }

    @Test
    public void testGetSupportedImds() {
        Image image = Image.builder()
                .withPackageVersions(Map.of("imds", "v2"))
                .build();
        Optional<String> supportedImdsVersion = underTest.getSupportedImdsVersion("AWS", StatedImage.statedImage(image, null, null));
        assertTrue(supportedImdsVersion.isPresent());
        assertEquals("v2", supportedImdsVersion.get());
    }

    private ImageCatalog getImageCatalog() {
        ImageCatalog imageCatalog = new ImageCatalog();
        imageCatalog.setImageCatalogUrl(CUSTOM_IMAGE_CATALOG_URL);
        imageCatalog.setName("default");
        Workspace ws = new Workspace();
        ws.setId(ORG_ID);
        imageCatalog.setWorkspace(ws);
        imageCatalog.setCreator("someone");
        imageCatalog.setResourceCrn("someCrn");
        return imageCatalog;
    }

}