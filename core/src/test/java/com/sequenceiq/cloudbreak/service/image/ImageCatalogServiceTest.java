package com.sequenceiq.cloudbreak.service.image;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.cloud.CloudConstant;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.cloud.model.catalog.CloudbreakImageCatalogV2;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Images;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.core.flow2.stack.image.update.StackImageUpdateService;
import com.sequenceiq.cloudbreak.domain.ImageCatalog;
import com.sequenceiq.cloudbreak.domain.UserProfile;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.repository.ImageCatalogRepository;
import com.sequenceiq.cloudbreak.service.ComponentConfigProviderService;
import com.sequenceiq.cloudbreak.service.RestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.account.PreferencesService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.user.UserProfileHandler;
import com.sequenceiq.cloudbreak.service.user.UserProfileService;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.cloudbreak.util.JsonUtil;

@RunWith(MockitoJUnitRunner.class)
public class ImageCatalogServiceTest {

    public static final String USER_ID = "userId";

    public static final String USERNAME = "username";

    public static final String EMAIL = "email@hwx.com";

    public static final String TENANT = "tenant";

    private static final String GIVEN_CB_VERSION = "2.8.0";

    private static final String DEFAULT_CATALOG_URL = "http://localhost/imagecatalog-url";

    private static final String CUSTOM_IMAGE_CATALOG_URL = "http://localhost/custom-imagecatalog-url";

    private static final String V2_CATALOG_FILE = "com/sequenceiq/cloudbreak/service/image/cb-image-catalog-v2.json";

    private static final String PROD_CATALOG_FILE = "com/sequenceiq/cloudbreak/service/image/cb-prod-image-catalog.json";

    private static final String DEV_CATALOG_FILE = "com/sequenceiq/cloudbreak/service/image/cb-dev-image-catalog.json";

    private static final String RC_CATALOG_FILE = "com/sequenceiq/cloudbreak/service/image/cb-rc-image-catalog.json";

    private static final String PROVIDER_AWS = "AWS";

    private static final String STACK_NAME = "stackName";

    private static final String IMAGE_CATALOG_NAME = "anyImageCatalog";

    private static final String IMAGE_HDP_ID = "hdp-1";

    private static final String IMAGE_BASE_ID = "base-2";

    private static final String IMAGE_HDF_ID = "hdf-3";

    private static final long STACK_ID = 1L;

    private static final long ORG_ID = 100L;

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Mock
    private ImageCatalogProvider imageCatalogProvider;

    @Spy
    private ImageCatalogVersionFilter versionFilter;

    @Mock
    private UserProfileService userProfileService;

    @Mock
    private ImageCatalogRepository imageCatalogRepository;

    @Mock
    private PreferencesService preferencesService;

    @Mock
    private UserProfileHandler userProfileHandler;

    @InjectMocks
    private ImageCatalogService underTest;

    @Spy
    private final List<CloudConstant> constants = new ArrayList<>();

    @Mock
    private StackService stackService;

    @Mock
    private StackImageUpdateService stackImageUpdateService;

    @Mock
    private StackImageFilterService stackImageFilterService;

    @Mock
    private ComponentConfigProviderService componentConfigProviderService;

    @Mock
    private WorkspaceService workspaceService;

    @Mock
    private UserService userService;

    @Mock
    private RestRequestThreadLocalService restRequestThreadLocalService;

    @Mock
    private User user;

    @Before
    public void beforeTest() throws Exception {
        setupImageCatalogProvider(CUSTOM_IMAGE_CATALOG_URL, V2_CATALOG_FILE);

        when(preferencesService.enabledPlatforms()).thenReturn(new HashSet<>(Arrays.asList("AZURE", "AWS", "GCP", "OPENSTACK")));
        when(userService.getOrCreate(any())).thenReturn(user);

        constants.addAll(Collections.singletonList(new AwsCloudConstant()));

        ReflectionTestUtils.setField(underTest, ImageCatalogService.class, "defaultCatalogUrl", DEFAULT_CATALOG_URL, null);
        setMockedCbVersion("cbVersion", "unspecified");
    }

    private void setMockedCbVersion(String cbVersion, String versionValue) {
        ReflectionTestUtils.setField(underTest, ImageCatalogService.class, cbVersion, versionValue, String.class);
    }

    @Test
    public void testGetLatestBaseImageDefaultPreferredWithNoDefaultsLatest() throws Exception {
        setupUserProfileService();
        setupImageCatalogProvider(DEFAULT_CATALOG_URL, V2_CATALOG_FILE);

        StatedImage image = underTest.getLatestBaseImageDefaultPreferred("AWS", null, user);

        assertEquals("7aca1fa6-980c-44e2-a75e-3144b18a5993", image.getImage().getUuid());
        assertFalse(image.getImage().isDefaultImage());
    }

    @Test
    public void testGetLatestBaseImageDefaultPreferredWithNoDefaultsLatestNoVersionMatch() throws Exception {
        setupUserProfileService();
        setupImageCatalogProvider(DEFAULT_CATALOG_URL, V2_CATALOG_FILE);
        ReflectionTestUtils.setField(underTest, ImageCatalogService.class, "cbVersion", "2.1.0-dev.200", null);

        StatedImage image = underTest.getLatestBaseImageDefaultPreferred("AWS", null, user);

        assertEquals("7aca1fa6-980c-44e2-a75e-3144b18a5993", image.getImage().getUuid());
        assertTrue(image.getImage().isDefaultImage());
    }

    @Test
    public void testGetLatestBaseImageDefaultPreferredWithMultipleDefaults() throws Exception {
        setupUserProfileService();
        setupImageCatalogProvider(DEFAULT_CATALOG_URL, V2_CATALOG_FILE);
        ReflectionTestUtils.setField(underTest, ImageCatalogService.class, "cbVersion", "2.1.0-dev.1", null);

        StatedImage image = underTest.getLatestBaseImageDefaultPreferred("AWS", null, user);

        assertEquals("7aca1fa6-980c-44e2-a75e-3144b18a5993", image.getImage().getUuid());
        assertTrue(image.getImage().isDefaultImage());
    }

    @Test
    public void testGetLatestBaseImageDefaultPreferredWenNotLatestSelected() throws Exception {
        setupUserProfileService();
        setupImageCatalogProvider(DEFAULT_CATALOG_URL, V2_CATALOG_FILE);
        ReflectionTestUtils.setField(underTest, ImageCatalogService.class, "cbVersion", "2.1.0-dev.2", null);

        StatedImage image = underTest.getLatestBaseImageDefaultPreferred("AWS", null, user);

        assertEquals("f6e778fc-7f17-4535-9021-515351df3691", image.getImage().getUuid());
        assertTrue(image.getImage().isDefaultImage());
    }

    @Test
    public void testGetStatedImagesFilteredByOperatingSystems() throws CloudbreakImageCatalogException, IOException {
        setupUserProfileService();
        setupImageCatalogProvider(DEFAULT_CATALOG_URL, DEV_CATALOG_FILE);
        Set<String> operatingSystems = new HashSet<>(Arrays.asList("redhat7", "redhat6", "amazonlinux2"));
        StatedImages images = underTest.getStatedImagesFilteredByOperatingSystems("aws", operatingSystems, user);

        Set<Image> allImage = Stream.of(images.getImages().getHdpImages(), images.getImages().getHdpImages(), images.getImages().getBaseImages())
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
        boolean allMatch = allImage.stream().allMatch(image -> operatingSystems.contains(image.getOsType()));
        assertTrue("All images should be based on supported OS", allMatch);
    }

    @Test
    public void testGetImagesWhenExactVersionExistsInCatalog() throws Exception {
        String cbVersion = "1.16.4";
        ImageCatalog imageCatalog = getImageCatalog();

        StatedImages images = underTest.getImages(new ImageFilter(imageCatalog, Collections.singleton("aws"), cbVersion));

        boolean exactImageIdMatch = images.getImages().getHdpImages().stream()
                .anyMatch(img -> "2.5.1.9-4-ccbb32dc-6c9f-43f1-8a09-64b598fda733-2.6.1.4-2".equals(img.getUuid()));
        assertTrue("Result doesn't contain the required Ambari image with id.", exactImageIdMatch);
    }

    @Test
    public void testGetImagesWhenExactVersionExistsInCatalogAndMorePlatformRequested() throws Exception {
        String cbVersion = "1.12.0";
        ImageCatalog imageCatalog = getImageCatalog();
        StatedImages images = underTest.getImages(new ImageFilter(imageCatalog, ImmutableSet.of("aws", "azure"), cbVersion));
        boolean awsAndAzureWerePresentedInTheTest = false;
        assertEquals(2L, images.getImages().getHdpImages().size());
        for (Image image : images.getImages().getHdpImages()) {
            boolean containsAws = images.getImages().getHdpImages().stream()
                    .anyMatch(img -> img.getImageSetsByProvider().entrySet().stream().anyMatch(
                            platformImages -> "aws".equals(platformImages.getKey())));
            boolean containsAzure = images.getImages().getHdpImages().stream()
                    .anyMatch(img -> img.getImageSetsByProvider().entrySet().stream().anyMatch(
                            platformImages -> "azure_rm".equals(platformImages.getKey())));
            if (image.getImageSetsByProvider().size() == 2) {
                awsAndAzureWerePresentedInTheTest = true;
                assertTrue("Result doesn't contain the required Ambari image with id.", containsAws && containsAzure);
            } else if (image.getImageSetsByProvider().size() == 1) {
                assertTrue("Result doesn't contain the required Ambari image with id.", containsAws || containsAzure);

            }
        }
        assertTrue(awsAndAzureWerePresentedInTheTest);
    }

    @Test
    public void testGetImagesWhenLatestVersionDoesntExistInCatalogShouldReturnWithReleasedVersionIfExists() throws Exception {
        setupImageCatalogProvider(CUSTOM_IMAGE_CATALOG_URL, PROD_CATALOG_FILE);
        ImageCatalog imageCatalog = getImageCatalog();

        StatedImages images = underTest.getImages(new ImageFilter(imageCatalog, Collections.singleton("aws"), "2.6.0"));

        boolean match = images.getImages().getHdpImages().stream()
                .anyMatch(img -> "63cdb3bc-28a6-4cea-67e4-9842fdeeaefb".equals(img.getUuid()));
        assertTrue("Result doesn't contain the required base image with id.", match);
    }

    @Test
    public void testGetImagesWhenLatestDevVersionDoesntExistInCatalogShouldReturnWithReleasedVersionIfExists() throws Exception {
        setupImageCatalogProvider(CUSTOM_IMAGE_CATALOG_URL, DEV_CATALOG_FILE);
        ImageCatalog imageCatalog = getImageCatalog();

        StatedImages images = underTest.getImages(new ImageFilter(imageCatalog, Collections.singleton("aws"), "2.6.0-dev.132"));

        boolean match = images.getImages().getHdpImages().stream()
                .anyMatch(img -> "b150efce-33ac-49c9-7206-7f148d162744".equals(img.getUuid()));
        assertTrue("Result doesn't contain the required base image with id.", match);
    }

    @Test
    public void testGetImagesWhenLatestRcVersionDoesntExistInCatalogShouldReturnWithReleasedVersionIfExists() throws Exception {
        setupImageCatalogProvider(CUSTOM_IMAGE_CATALOG_URL, RC_CATALOG_FILE);
        ImageCatalog imageCatalog = getImageCatalog();

        StatedImages images = underTest.getImages(new ImageFilter(imageCatalog, Collections.singleton("aws"), "2.6.0-rc.13"));

        boolean match = images.getImages().getHdpImages().stream()
                .anyMatch(img -> "bbc63453-086c-4bf7-4337-a04c37d51b68".equals(img.getUuid()));
        assertTrue("Result doesn't contain the required base image with id.", match);
    }

    @Test
    public void testGetImagesWhenSimilarRcVersionDoesntExistInDevCatalogShouldReturnWithLatestDevVersionIfExists() throws Exception {
        setupImageCatalogProvider(CUSTOM_IMAGE_CATALOG_URL, DEV_CATALOG_FILE);
        ImageCatalog imageCatalog = getImageCatalog();

        StatedImages images = underTest.getImages(new ImageFilter(imageCatalog, Collections.singleton("aws"), "2.6.0-rc.13"));

        boolean match = images.getImages().getHdpImages().stream()
                .anyMatch(img -> img.getUuid().equals("bbc63453-086c-4bf7-4337-a04c37d51b68"));
        assertTrue("Result doesn't contain the required base image with id.", match);
    }

    @Test
    public void testGetImagesWhenSimilarDevVersionDoesntExistInCatalogShouldReturnWithReleasedVersionIfExists() throws Exception {
        ImageCatalog imageCatalog = getImageCatalog();

        StatedImages images = underTest.getImages(new ImageFilter(imageCatalog, Collections.singleton("aws"), "1.16.4-dev.132"));

        boolean match = images.getImages().getHdpImages().stream()
                .anyMatch(img -> "2.5.1.9-4-ccbb32dc-6c9f-43f1-8a09-64b598fda733-2.6.1.4-2".equals(img.getUuid()));
        assertTrue("Result doesn't contain the required Ambari image with id.", match);
    }

    @Test
    public void testGetImagesWhenSimilarRcVersionDoesntExistInCatalogShouldReturnWithReleasedVersionIfExists() throws Exception {
        ImageCatalog imageCatalog = getImageCatalog();

        StatedImages images = underTest.getImages(new ImageFilter(imageCatalog, Collections.singleton("aws"), "1.16.4-rc.13"));

        boolean match = images.getImages().getHdpImages().stream()
                .anyMatch(img -> "2.5.1.9-4-ccbb32dc-6c9f-43f1-8a09-64b598fda733-2.6.1.4-2".equals(img.getUuid()));
        assertTrue("Result doesn't contain the required Ambari image with id.", match);
    }

    @Test
    public void testGetImagesWhenSimilarDevVersionExistsInCatalog() throws Exception {
        ImageCatalog imageCatalog = getImageCatalog();

        StatedImages images = underTest.getImages(new ImageFilter(imageCatalog, Collections.singleton("aws"), "2.1.0-dev.4000"));

        boolean hdfImgMatch = images.getImages().getHdfImages().stream()
                .anyMatch(ambariImage -> "9958938a-1261-48e2-aff9-dbcb2cebf6cd".equals(ambariImage.getUuid()));
        boolean hdpImgMatch = images.getImages().getHdpImages().stream()
                .anyMatch(ambariImage -> "2.5.0.2-65-5288855d-d7b9-4b90-b326-ab4b168cf581-2.6.0.1-145".equals(ambariImage.getUuid()));
        boolean baseImgMatch = images.getImages().getBaseImages().stream()
                .anyMatch(ambariImage -> "f6e778fc-7f17-4535-9021-515351df3691".equals(ambariImage.getUuid()));
        assertTrue("Result doesn't contain the required Ambari image with id.", hdfImgMatch && hdpImgMatch && baseImgMatch);
    }

    @Test
    public void testGetImagesWhenSimilarRcVersionExistsInCatalog() throws Exception {
        ImageCatalog imageCatalog = getImageCatalog();

        StatedImages images = underTest.getImages(new ImageFilter(imageCatalog, Collections.singleton("aws"), "2.0.0-rc.4"));

        boolean allMatch = images.getImages().getHdpImages().stream()
                .allMatch(img -> "2.4.2.2-1-9e3ccdca-fa64-42eb-ab29-b1450767bbd8-2.5.0.1-265".equals(img.getUuid())
                        || "2.5.1.9-4-ccbb32dc-6c9f-43f1-8a09-64b598fda733-2.6.1.4-2".equals(img.getUuid()));
        assertTrue("Result doesn't contain the required Ambari image with id.", allMatch);
    }

    @Test
    public void testGetImagesWhenExactVersionExistsInCatalogForPlatform() throws Exception {
        ImageCatalog imageCatalog = getImageCatalog();

        StatedImages images = underTest.getImages(new ImageFilter(imageCatalog, Collections.singleton("AWS"), "1.16.4"));

        boolean exactImageIdMatch = images.getImages().getHdpImages().stream()
                .anyMatch(img -> "2.5.1.9-4-ccbb32dc-6c9f-43f1-8a09-64b598fda733-2.6.1.4-2".equals(img.getUuid()));
        assertTrue("Result doesn't contain the required Ambari image with id for the platform.", exactImageIdMatch);
    }

    @Test
    public void testGetImagesWhenExactVersionDoesnotExistInCatalogForPlatform() throws Exception {
        ImageCatalog imageCatalog = getImageCatalog();

        thrown.expectMessage("Platform(s) owncloud are not supported by the current catalog");
        thrown.expect(CloudbreakImageCatalogException.class);

        underTest.getImages(new ImageFilter(imageCatalog, Collections.singleton("owncloud"), "1.16.4"));
    }

    @Test
    public void testGetImagesWhenCustomImageCatalogExists() throws Exception {
        ImageCatalog ret = new ImageCatalog();
        ret.setImageCatalogUrl("");
        when(imageCatalogRepository.findByNameAndWorkspaceId("name", ORG_ID)).thenReturn(Optional.of(ret));
        when(imageCatalogProvider.getImageCatalogV2("")).thenReturn(null);
        underTest.getImages(ORG_ID, "name", "aws");

        verify(imageCatalogProvider, times(1)).getImageCatalogV2("");

    }

    @Test
    public void testGetImagesWhenCustomImageCatalogDoesNotExists() throws Exception {
        when(imageCatalogRepository.findByNameAndWorkspaceId(anyString(), anyLong())).thenThrow(new AccessDeniedException("denied"));

        thrown.expectMessage("The verycool catalog does not exist or does not belongs to your account.");
        thrown.expect(CloudbreakImageCatalogException.class);

        underTest.getImages(ORG_ID, "verycool", "aws").getImages();

        verify(imageCatalogProvider, times(0)).getImageCatalogV2("");
    }

    @Test
    public void testDeleteImageCatalog() {
        String name = "img-name";
        ImageCatalog imageCatalog = new ImageCatalog();
        imageCatalog.setName(name);
        imageCatalog.setArchived(false);
        doNothing().when(userProfileHandler).destroyProfileImageCatalogPreparation(any(ImageCatalog.class));
        when(imageCatalogRepository.findByNameAndWorkspaceId(name, ORG_ID)).thenReturn(Optional.of(imageCatalog));
        setupUserProfileService();

        underTest.delete(ORG_ID, name);

        verify(imageCatalogRepository, times(1)).save(imageCatalog);

        assertTrue(imageCatalog.isArchived());
        assertTrue(imageCatalog.getName().startsWith(name) && imageCatalog.getName().indexOf('_') == name.length());
    }

    @Test
    public void testDeleteImageCatalogWhenEnvDefault() {
        String name = "cloudbreak-default";

        thrown.expectMessage("cloudbreak-default cannot be deleted because it is an environment default image catalog.");
        thrown.expect(BadRequestException.class);

        underTest.delete(ORG_ID, name);
    }

    @Test
    public void testGet() {
        String name = "img-name";
        ImageCatalog imageCatalog = new ImageCatalog();
        when(imageCatalogRepository.findByNameAndWorkspaceId(name, ORG_ID)).thenReturn(Optional.of(imageCatalog));
        ImageCatalog actual = underTest.get(ORG_ID, name);

        assertEquals(actual, imageCatalog);
    }

    @Test
    public void testGetWhenEnvDefault() {
        String name = "cloudbreak-default";
        ImageCatalog actual = underTest.get(ORG_ID, name);

        verify(imageCatalogRepository, times(0)).findByNameAndWorkspace(eq(name), any(Workspace.class));

        assertEquals(actual.getName(), name);
        assertNull(actual.getId());
    }

    @Test
    public void testGetImagesFromDefaultWithEmptyInput() throws CloudbreakImageCatalogException {
        thrown.expect(BadRequestException.class);

        underTest.getImagesFromDefault(ORG_ID, null, null, Collections.emptySet());

        thrown.expectMessage("Either platform or stackName should be filled in request");
    }

    @Test
    public void testGetImagesFromDefaultGivenBothInput() throws CloudbreakImageCatalogException {
        thrown.expect(BadRequestException.class);

        underTest.getImagesFromDefault(ORG_ID, "stack", "AWS", Collections.emptySet());

        thrown.expectMessage("Platform or stackName cannot be filled in the same request");
    }

    @Test
    public void testGetImagesFromDefaultWithStackName() throws CloudbreakImageCatalogException {
        when(stackImageFilterService.getApplicableImages(anyLong(), anyString())).thenReturn(new Images(Lists.newArrayList(), Lists.newArrayList(),
                Lists.newArrayList(), Lists.newArrayList(), Sets.newHashSet()));

        underTest.getImagesFromDefault(ORG_ID, "stack", null, Collections.emptySet());

        verify(stackImageFilterService, never()).getApplicableImages(anyLong(), anyString(), anyString());
        verify(stackImageFilterService, times(1)).getApplicableImages(anyLong(), anyString());
    }

    @Test
    public void testGetImagesFromDefaultWithPlatform() throws CloudbreakImageCatalogException, IOException {
        setupUserProfileService();
        setupImageCatalogProvider(DEFAULT_CATALOG_URL, V2_CATALOG_FILE);

        underTest.getImagesFromDefault(ORG_ID, null, "AWS", Collections.emptySet());

        verify(stackImageFilterService, never()).getApplicableImages(anyLong(), anyString(), anyString());
        verify(stackImageFilterService, never()).getApplicableImages(anyLong(), anyString());
    }

    @Test
    public void testGetImagesWithEmptyInput() throws CloudbreakImageCatalogException {
        thrown.expect(BadRequestException.class);

        underTest.getImagesByCatalogName(ORG_ID, "catalog", null, null);

        thrown.expectMessage("Either platform or stackName should be filled in request");
    }

    @Test
    public void testGetImagesGivenBothInput() throws CloudbreakImageCatalogException {
        thrown.expect(BadRequestException.class);

        underTest.getImagesByCatalogName(ORG_ID, "catalog", "stack", "AWS");

        thrown.expectMessage("Platform or stackName cannot be filled in the same request");
    }

    @Test
    public void testGetImagesWithStackName() throws CloudbreakImageCatalogException {
        when(stackImageFilterService.getApplicableImages(anyLong(), anyString(), anyString())).thenReturn(new Images(Lists.newArrayList(), Lists.newArrayList(),
                Lists.newArrayList(), Lists.newArrayList(), Sets.newHashSet()));

        underTest.getImagesByCatalogName(ORG_ID, "catalog", "stack", null);

        verify(stackImageFilterService, times(1)).getApplicableImages(anyLong(), anyString(), anyString());
        verify(stackImageFilterService, never()).getApplicableImages(anyLong(), anyString());
    }

    @Test
    public void testGetImagesWithPlatform() throws CloudbreakImageCatalogException, IOException {
        setupUserProfileService();
        setupImageCatalogProvider(CUSTOM_IMAGE_CATALOG_URL, V2_CATALOG_FILE);
        when(imageCatalogRepository.findByNameAndWorkspaceId(anyString(), anyLong())).thenReturn(Optional.of(new ImageCatalog()));

        underTest.getImagesByCatalogName(ORG_ID, "catalog", null, "AWS");

        verify(stackImageFilterService, never()).getApplicableImages(anyLong(), anyString(), anyString());
        verify(stackImageFilterService, never()).getApplicableImages(anyLong(), anyString());
    }

    private void setupImageCatalogProvider(String catalogUrl, String catalogFile) throws IOException, CloudbreakImageCatalogException {
        String catalogJson = FileReaderUtils.readFileFromClasspath(catalogFile);
        CloudbreakImageCatalogV2 catalog = JsonUtil.readValue(catalogJson, CloudbreakImageCatalogV2.class);
        when(imageCatalogProvider.getImageCatalogV2(catalogUrl)).thenReturn(catalog);
    }

    private void setupUserProfileService() {
        UserProfile userProfile = new UserProfile();
        when(userProfileService.getOrCreate(any(User.class))).thenReturn(userProfile);
    }

    private ImageCatalog getImageCatalog() {
        ImageCatalog imageCatalog = new ImageCatalog();
        imageCatalog.setImageCatalogUrl(CUSTOM_IMAGE_CATALOG_URL);
        imageCatalog.setName("default");
        return imageCatalog;
    }

    private static class AwsCloudConstant implements CloudConstant {
        @Override
        public Platform platform() {
            return Platform.platform("AWS");
        }

        @Override
        public Variant variant() {
            return Variant.variant("AWS");
        }
    }
}