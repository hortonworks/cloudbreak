package com.sequenceiq.cloudbreak.service.image;

import static com.sequenceiq.cloudbreak.service.image.ImageTestUtil.PLATFORM;
import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.convert.ConversionService;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.image.ImageSettingsV4Request;
import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cluster.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.cmtemplate.utils.BlueprintUtils;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.domain.ImageCatalog;
import com.sequenceiq.cloudbreak.exception.NotFoundException;
import com.sequenceiq.cloudbreak.service.ComponentConfigProviderService;
import com.sequenceiq.cloudbreak.service.StackMatrixService;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;

@RunWith(MockitoJUnitRunner.class)
public class ImageServiceTest {

    private static final Long WORKSPACE_ID = 1L;

    private static final String STACK_VERSION = "7.1.0";

    private static final String CUSTOM_IMAGE_CATALOG_URL = "http://localhost/custom-imagecatalog-url";

    private static final long ORG_ID = 100L;

    private static final String OS = "anOS";

    private static final long USER_ID = 1000L;

    private static final String USER_ID_STRING = "aUserId";

    @Mock
    private ImageCatalogService imageCatalogService;

    @Mock
    private StackMatrixService stackMatrixService;

    @Mock
    private BlueprintUtils blueprintUtils;

    @Mock
    private CloudConnector cloudConnector;

    @Mock
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Mock
    private ConversionService conversionService;

    @Mock
    private ComponentConfigProviderService componentConfigProviderService;

    @Mock
    private ClusterComponentConfigProvider clusterComponentConfigProvider;

    @InjectMocks
    private ImageService underTest;

    private ImageSettingsV4Request imageSettingsV4Request;

    @Before
    public void setUp() {
        imageSettingsV4Request = new ImageSettingsV4Request();
        imageSettingsV4Request.setCatalog("aCatalog");
        imageSettingsV4Request.setId("anImageId");
        imageSettingsV4Request.setOs(OS);
        when(blueprintUtils.getCDHStackVersion(any())).thenReturn(STACK_VERSION);
        when(imageCatalogService.get(WORKSPACE_ID, "aCatalog")).thenReturn(getImageCatalog());
    }

    @Test
    public void testUseBaseImageAndDisabledBaseImageShouldReturnError() {
        CloudbreakImageCatalogException exception = assertThrows(CloudbreakImageCatalogException.class, () ->
                underTest.determineImageFromCatalog(
                        WORKSPACE_ID,
                        imageSettingsV4Request,
                        PLATFORM,
                        TestUtil.blueprint(),
                        true,
                        false,
                        TestUtil.user(USER_ID, USER_ID_STRING),
                        image -> true));
        assertEquals("Inconsistent request, base images are disabled but custom repo information is submitted!", exception.getMessage());
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
                        TestUtil.blueprint(),
                        true,
                        true,
                        TestUtil.user(USER_ID, USER_ID_STRING),
                        image -> true));

        assertEquals("Image catalog not found with name: aCatalog", exception.getMessage());
    }

    @Test
    public void testDetermineImageFromCatalogWithNonExistingCatalogName() {
        when(imageCatalogService.get(WORKSPACE_ID, "aCatalog")).thenThrow(new NotFoundException("Image catalog not found with name: aCatalog"));
        ImageSettingsV4Request imageRequest = new ImageSettingsV4Request();
        imageRequest.setCatalog("aCatalog");
        imageRequest.setOs(OS);

        CloudbreakImageCatalogException exception = assertThrows(CloudbreakImageCatalogException.class, () ->
                underTest.determineImageFromCatalog(
                        WORKSPACE_ID,
                        imageRequest,
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
        StatedImage statedImage = underTest.determineImageFromCatalog(
                WORKSPACE_ID,
                imageSettingsV4Request,
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
        StatedImage statedImage = underTest.determineImageFromCatalog(
                WORKSPACE_ID,
                imageSettingsV4Request,
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
        StatedImage statedImage = underTest.determineImageFromCatalog(
                WORKSPACE_ID,
                imageSettingsV4Request,
                PLATFORM,
                TestUtil.blueprint(),
                false,
                false,
                TestUtil.user(USER_ID, USER_ID_STRING),
                image -> true);
        assertEquals("uuid", statedImage.getImage().getUuid());
        assertTrue(statedImage.getImage().isPrewarmed());
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