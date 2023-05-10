package com.sequenceiq.cloudbreak.service.image;

import static com.sequenceiq.cloudbreak.service.image.ImageTestUtil.DEFAULT_REGION;
import static com.sequenceiq.cloudbreak.service.image.ImageTestUtil.INVALID_PLATFORM;
import static com.sequenceiq.cloudbreak.service.image.ImageTestUtil.PLATFORM;
import static com.sequenceiq.cloudbreak.service.image.ImageTestUtil.REGION;
import static com.sequenceiq.common.model.ImageCatalogPlatform.imageCatalogPlatform;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Map;
import java.util.stream.Stream;

import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
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
import com.sequenceiq.cloudbreak.cluster.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.cmtemplate.utils.BlueprintUtils;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.service.PlatformStringTransformer;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.domain.ImageCatalog;
import com.sequenceiq.cloudbreak.service.ComponentConfigProviderService;
import com.sequenceiq.cloudbreak.service.StackMatrixService;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.common.model.ImageCatalogPlatform;

public class ImageServiceTest {

    private static final Long WORKSPACE_ID = 1L;

    private static final String STACK_VERSION = "7.1.0";

    private static final String CUSTOM_IMAGE_CATALOG_URL = "http://localhost/custom-imagecatalog-url";

    private static final long ORG_ID = 100L;

    private static final String OS = "anOS";

    private static final long USER_ID = 1000L;

    private static final String USER_ID_STRING = "aUserId";

    private static final String EXISTING_ID = "ami-09fea90f257c85513";

    private static final String DEFAULT_REGION_EXISTING_ID = "ami-09fea90f257c85514";

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:1234:user:1";

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

    @InjectMocks
    private ImageService underTest;

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
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        imageSettingsV4Request = new ImageSettingsV4Request();
        imageSettingsV4Request.setCatalog("aCatalog");
        imageSettingsV4Request.setId("anImageId");
        imageSettingsV4Request.setOs(OS);
        when(blueprintUtils.getCDHStackVersion(any())).thenReturn(STACK_VERSION);
        when(imageCatalogService.getImageCatalogByName(WORKSPACE_ID, "aCatalog")).thenReturn(getImageCatalog());
        CloudConnector connector = mock(CloudConnector.class);
        when(cloudPlatformConnectors.getDefault(Platform.platform(PLATFORM))).thenReturn(connector);
        when(cloudPlatformConnectors.getDefault(Platform.platform(CloudPlatform.YARN.name()))).thenReturn(connector);
        when(connector.regionToDisplayName(REGION)).thenReturn(REGION);
    }

    @Test
    public void testUseBaseImageAndDisabledBaseImageShouldReturnError() {
        imageSettingsV4Request.setId(null);
        CloudbreakImageCatalogException exception = assertThrows(CloudbreakImageCatalogException.class, () ->
                underTest.determineImageFromCatalog(
                        WORKSPACE_ID,
                        imageSettingsV4Request,
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
        when(imageCatalogService.getImageByCatalogName(WORKSPACE_ID, "anImageId", "aCatalog"))
                .thenThrow(new CloudbreakImageCatalogException("Image catalog not found with name: aCatalog"));

        CloudbreakImageCatalogException exception = assertThrows(CloudbreakImageCatalogException.class, () ->
                underTest.determineImageFromCatalog(
                        WORKSPACE_ID,
                        imageSettingsV4Request,
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
        when(imageCatalogService.getImageCatalogByName(WORKSPACE_ID, "aCatalog"))
                .thenThrow(new NotFoundException("Image catalog not found with name: aCatalog"));
        ImageSettingsV4Request imageRequest = new ImageSettingsV4Request();
        imageRequest.setCatalog("aCatalog");
        imageRequest.setOs(OS);

        CloudbreakImageCatalogException exception = assertThrows(CloudbreakImageCatalogException.class, () ->
                underTest.determineImageFromCatalog(
                        WORKSPACE_ID,
                        imageRequest,
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
    public void testGivenPrewarmedImageIdAndDisabledBaseImageShouldReturnOk() throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        when(imageCatalogService.getImageByCatalogName(anyLong(), anyString(), anyString()))
                .thenReturn(ImageTestUtil.getImageFromCatalog(true, "uuid", STACK_VERSION));
        StatedImage statedImage = underTest.determineImageFromCatalog(
                WORKSPACE_ID,
                imageSettingsV4Request,
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
        when(imageCatalogService.getLatestBaseImageDefaultPreferred(any(), any()))
                .thenReturn(ImageTestUtil.getImageFromCatalog(false, "uuid", STACK_VERSION));
        when(platformStringTransformer.getPlatformStringForImageCatalog(anyString(), anyString())).thenReturn(imageCatalogPlatform(PLATFORM));
        StatedImage statedImage = underTest.determineImageFromCatalog(
                WORKSPACE_ID,
                imageSettingsV4Request,
                PLATFORM,
                PLATFORM,
                TestUtil.blueprint(),
                true,
                true,
                TestUtil.user(USER_ID, USER_ID_STRING),
                image -> true);
        assertEquals("uuid", statedImage.getImage().getUuid());
        assertFalse(statedImage.getImage().isPrewarmed());
    }

    @Test
    public void testNoUseBaseImageAndEnabledBaseImageShouldReturnCorrectImage() throws CloudbreakImageCatalogException, CloudbreakImageNotFoundException {
        imageSettingsV4Request.setId(null);
        when(imageCatalogService.getImagePrewarmedDefaultPreferred(any(), any()))
                .thenReturn(ImageTestUtil.getImageFromCatalog(false, "uuid", STACK_VERSION));
        when(platformStringTransformer.getPlatformStringForImageCatalog(anyString(), anyString())).thenReturn(imageCatalogPlatform(PLATFORM));
        StatedImage statedImage = underTest.determineImageFromCatalog(
                WORKSPACE_ID,
                imageSettingsV4Request,
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
    public void testDisabledBaseImageShouldReturnCorrectImage() throws CloudbreakImageCatalogException, CloudbreakImageNotFoundException {
        imageSettingsV4Request.setId(null);
        when(imageCatalogService.getImagePrewarmedDefaultPreferred(any(), any()))
                .thenReturn(ImageTestUtil.getImageFromCatalog(true, "uuid", STACK_VERSION));
        when(platformStringTransformer.getPlatformStringForImageCatalog(anyString(), anyString())).thenReturn(imageCatalogPlatform(PLATFORM));
        StatedImage statedImage = underTest.determineImageFromCatalog(
                WORKSPACE_ID,
                imageSettingsV4Request,
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
    public void testDetermineImageNameFound() {
        Image image = mock(Image.class);
        ImageCatalogPlatform imageCatalogPlatform = imageCatalogPlatform(PLATFORM);

        when(image.getImageSetsByProvider()).thenReturn(Collections.singletonMap(PLATFORM, Collections.singletonMap(REGION, EXISTING_ID)));

        String imageName = ThreadBasedUserCrnProvider.doAs(USER_CRN,
                () -> {
                    try {
                        return underTest.determineImageName(PLATFORM, imageCatalogPlatform, REGION, image);
                    } catch (CloudbreakImageNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                });
        assertEquals("ami-09fea90f257c85513", imageName);
    }

    @Test
    public void testDetermineImageNameFoundYCloud() {
        Image image = mock(Image.class);
        String platform = CloudPlatform.YARN.name().toLowerCase();
        ImageCatalogPlatform imageCatalogPlatform = imageCatalogPlatform(platform);
        when(entitlementService.azureMarketplaceImagesEnabled(any())).thenReturn(false);

        when(image.getImageSetsByProvider()).thenReturn(Collections.singletonMap(platform,
                Map.of(
                        REGION, EXISTING_ID,
                        DEFAULT_REGION, DEFAULT_REGION_EXISTING_ID)));

        String imageName = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            try {
                return underTest.determineImageName(platform, imageCatalogPlatform, null, image);
            } catch (CloudbreakImageNotFoundException e) {
                throw new RuntimeException(e);
            }
        });
        assertEquals("ami-09fea90f257c85514", imageName);
    }

    @Test
    public void testDetermineImageNameFoundDefaultPreferred() {
        Image image = mock(Image.class);
        ImageCatalogPlatform imageCatalogPlatform = imageCatalogPlatform(PLATFORM);
        when(entitlementService.azureMarketplaceImagesEnabled(any())).thenReturn(true);

        when(image.getImageSetsByProvider()).thenReturn(Collections.singletonMap(PLATFORM,
                Map.of(
                        REGION, EXISTING_ID,
                        DEFAULT_REGION, DEFAULT_REGION_EXISTING_ID)));

        String imageName = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            try {
                return underTest.determineImageName(PLATFORM, imageCatalogPlatform, REGION, image);
            } catch (CloudbreakImageNotFoundException e) {
                throw new RuntimeException(e);
            }
        });
        assertEquals("ami-09fea90f257c85514", imageName);
    }

    @Test
    public void testDetermineImageNameFoundNoMpEntitlement() {
        Image image = mock(Image.class);
        ImageCatalogPlatform imageCatalogPlatform = imageCatalogPlatform(PLATFORM);
        when(entitlementService.azureMarketplaceImagesEnabled(any())).thenReturn(false);

        when(image.getImageSetsByProvider()).thenReturn(Collections.singletonMap(PLATFORM,
                Map.of(
                        REGION, EXISTING_ID,
                        DEFAULT_REGION, DEFAULT_REGION_EXISTING_ID)));

        String imageName = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            try {
                return underTest.determineImageName(PLATFORM, imageCatalogPlatform, REGION, image);
            } catch (CloudbreakImageNotFoundException e) {
                throw new RuntimeException(e);
            }
        });
        assertEquals("ami-09fea90f257c85513", imageName);
    }

    @Test
    public void testDetermineImageNameNotFound() {
        Image image = mock(Image.class);
        ImageCatalogPlatform imageCatalogPlatform = imageCatalogPlatform(PLATFORM);

        when(image.getImageSetsByProvider()).thenReturn(Collections.singletonMap(PLATFORM, Collections.singletonMap(REGION, EXISTING_ID)));

        Exception exception = ThreadBasedUserCrnProvider.doAs(USER_CRN,
                        () -> assertThrows(CloudbreakImageNotFoundException.class,
                                () -> underTest.determineImageName(PLATFORM, imageCatalogPlatform, "fake-region", image)));
        String exceptionMessage = "Virtual machine image couldn't be found in image";
        MatcherAssert.assertThat(exception.getMessage(), CoreMatchers.containsString(exceptionMessage));
    }

    @Test
    public void testDetermineImageNameImageForPlatformNotFound() {
        Image image = mock(Image.class);
        ImageCatalogPlatform imageCatalogPlatform = imageCatalogPlatform(PLATFORM);

        when(image.getImageSetsByProvider()).thenReturn(Collections.singletonMap(INVALID_PLATFORM, Collections.singletonMap(REGION, EXISTING_ID)));
        when(image.toString()).thenReturn("Image");

        Exception exception = assertThrows(CloudbreakImageNotFoundException.class, () ->
                underTest.determineImageName(PLATFORM, imageCatalogPlatform, REGION, image));
        String exceptionMessage = "The selected image: 'Image' "
                + "doesn't contain virtual machine image for the selected platform: 'ImageCatalogPlatform{platform='azure'}'.";
        MatcherAssert.assertThat(exception.getMessage(), CoreMatchers.containsString(exceptionMessage));
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
