package com.sequenceiq.cloudbreak.service.image;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.ArgumentMatchers.matches;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.sequenceiq.authorization.service.OwnerAssignmentService;
import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareCrnGenerator;
import com.sequenceiq.cloudbreak.auth.CrnTestUtil;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.CloudConstant;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.cloud.model.catalog.CloudbreakImageCatalogV3;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Images;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.common.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.core.flow2.stack.image.update.StackImageUpdateService;
import com.sequenceiq.cloudbreak.domain.CustomImage;
import com.sequenceiq.cloudbreak.domain.ImageCatalog;
import com.sequenceiq.cloudbreak.domain.UserProfile;
import com.sequenceiq.cloudbreak.repository.ImageCatalogRepository;
import com.sequenceiq.cloudbreak.service.ComponentConfigProviderService;
import com.sequenceiq.cloudbreak.service.account.PreferencesService;
import com.sequenceiq.cloudbreak.service.image.catalog.AdvertisedImageCatalogService;
import com.sequenceiq.cloudbreak.service.image.catalog.AdvertisedImageProvider;
import com.sequenceiq.cloudbreak.service.image.catalog.ImageCatalogServiceProxy;
import com.sequenceiq.cloudbreak.service.image.catalog.VersionBasedImageCatalogService;
import com.sequenceiq.cloudbreak.service.image.catalog.VersionBasedImageProvider;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.user.UserProfileHandler;
import com.sequenceiq.cloudbreak.service.user.UserProfileService;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;
import com.sequenceiq.cloudbreak.structuredevent.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.structuredevent.LegacyRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.cloudbreak.util.TestConstants;
import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.common.api.type.ImageType;

@RunWith(MockitoJUnitRunner.class)
public class ImageCatalogServiceTest {

    private static final String DEFAULT_CATALOG_URL = "http://localhost/imagecatalog-url";

    private static final String DEFAULT_FREEIPA_CATALOG_URL = "http://localhost/freeipaimagecatalog-url";

    private static final String CUSTOM_IMAGE_CATALOG_URL = "http://localhost/custom-imagecatalog-url";

    private static final String V2_CB_CATALOG_FILE = "com/sequenceiq/cloudbreak/service/image/cb-image-catalog-v2.json";

    private static final String V3_CB_CATALOG_FILE = "com/sequenceiq/cloudbreak/service/image/cb-image-catalog-v3.json";

    private static final String V3_FREEIPA_CATALOG_FILE = "com/sequenceiq/cloudbreak/service/image/freeipa-image-catalog-v3.json";

    private static final String PROD_CATALOG_FILE = "com/sequenceiq/cloudbreak/service/image/cb-prod-image-catalog.json";

    private static final String DEV_CATALOG_FILE = "com/sequenceiq/cloudbreak/service/image/cb-dev-image-catalog.json";

    private static final String RC_CATALOG_FILE = "com/sequenceiq/cloudbreak/service/image/cb-rc-image-catalog.json";

    private static final long ORG_ID = 100L;

    private static final Long WORKSPACE_ID = 1L;

    private static final String CDP_DEFAULT_CATALOG_NAME = "cdp-default";

    private static final String CUSTOM_CATALOG_NAME = "custom-catalog";

    private static final String CUSTOM_BASE_PARCEL_URL = "https://myarchive.test.com";

    private static final String CUSTOM_IMAGE_ID = "customImageId";

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
    private LegacyRestRequestThreadLocalService legacyRestRequestThreadLocalService;

    @Mock
    private User user;

    @Mock
    private ImageCatalog imageCatalog;

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private OwnerAssignmentService ownerAssignmentService;

    @Mock
    private TransactionService transactionService;

    @Mock
    private PrefixMatcherService prefixMatcherService;

    @Mock
    private LatestDefaultImageUuidProvider latestDefaultImageUuidProvider;

    @InjectMocks
    private VersionBasedImageProvider versionBasedImageProvider;

    @Mock
    private AdvertisedImageProvider advertisedImageProvider;

    @InjectMocks
    private ImageCatalogServiceProxy imageCatalogServiceProxy;

    @InjectMocks
    private AdvertisedImageCatalogService advertisedImageCatalogService;

    @InjectMocks
    private VersionBasedImageCatalogService versionBasedImageCatalogService;

    @Mock
    private CustomImageProvider customImageProvider;

    @Mock
    private CloudbreakVersionListProvider cloudbreakVersionListProvider;

    @Captor
    private ArgumentCaptor<StatedImage> sourceImageCaptor;

    @Mock
    private CloudbreakRestRequestThreadLocalService restRequestThreadLocalService;

    @Mock
    private RegionAwareCrnGenerator regionAwareCrnGenerator;

    @Before
    public void beforeTest() throws Exception {
        setupImageCatalogProvider(DEFAULT_CATALOG_URL, V2_CB_CATALOG_FILE);

        when(preferencesService.enabledPlatforms()).thenReturn(new HashSet<>(Arrays.asList("AZURE", "AWS", "GCP", "OPENSTACK")));
        lenient().when(user.getUserCrn()).thenReturn(TestConstants.CRN);
        when(userService.getOrCreate(any())).thenReturn(user);
        when(entitlementService.baseImageEnabled(anyString())).thenReturn(true);

        constants.addAll(Collections.singletonList(new AwsCloudConstant()));

        ReflectionTestUtils.setField(underTest, ImageCatalogService.class, "defaultCatalogUrl", DEFAULT_CATALOG_URL, null);
        ReflectionTestUtils.setField(underTest, ImageCatalogService.class, "defaultFreeIpaCatalogUrl", DEFAULT_FREEIPA_CATALOG_URL, null);
        setMockedCbVersion("cbVersion", "unspecified");

        ReflectionTestUtils.setField(underTest, "imageCatalogServiceProxy", imageCatalogServiceProxy);

        ReflectionTestUtils.setField(imageCatalogServiceProxy, "advertisedImageCatalogService", advertisedImageCatalogService);
        ReflectionTestUtils.setField(imageCatalogServiceProxy, "versionBasedImageCatalogService", versionBasedImageCatalogService);

        ReflectionTestUtils.setField(versionBasedImageCatalogService, "versionBasedImageProvider", versionBasedImageProvider);

        CrnTestUtil.mockCrnGenerator(regionAwareCrnGenerator);
    }

    private void setMockedCbVersion(String cbVersion, String versionValue) {
        ReflectionTestUtils.setField(underTest, ImageCatalogService.class, cbVersion, versionValue, String.class);
    }

    @Test
    public void testGetLatestBaseImageDefaultPreferredWithNoDefaultsLatest() throws Exception {
        setupUserProfileService();
        setupImageCatalogProvider(DEFAULT_CATALOG_URL, V2_CB_CATALOG_FILE);

        ImageFilter imageFilter = new ImageFilter(imageCatalog, Set.of("AWS"), null, true, null, null);
        StatedImage image = underTest.getLatestBaseImageDefaultPreferred(imageFilter, i -> true);

        assertEquals("7aca1fa6-980c-44e2-a75e-3144b18a5993", image.getImage().getUuid());
        assertFalse(image.getImage().isDefaultImage());
    }

    @Test
    public void shouldGetStatedImagesFromAdvertisedImageProvider() throws Exception {
        setupUserProfileService();
        setupImageCatalogProviderWithoutVersions(DEFAULT_CATALOG_URL, V2_CB_CATALOG_FILE);
        when(advertisedImageProvider.getImages(any(), any())).thenReturn(
                StatedImages.statedImages(
                        new Images(Collections.singletonList(ImageTestUtil.getImage(false, "uuid", "stack")), null, null, null), null, null));

        ImageFilter imageFilter = new ImageFilter(imageCatalog, Set.of("AWS"), null, true, null, null);
        underTest.getLatestBaseImageDefaultPreferred(imageFilter, i -> true);

        verify(advertisedImageProvider).getImages(any(), any());
    }

    @Test
    public void testGetLatestBaseImageDefaultPreferredWithNoDefaultsLatestNoVersionMatch() throws Exception {
        setupUserProfileService();
        setupImageCatalogProvider(DEFAULT_CATALOG_URL, V2_CB_CATALOG_FILE);
        ReflectionTestUtils.setField(underTest, ImageCatalogService.class, "cbVersion", "2.1.0-dev.200", null);

        Set<String> vMImageUUIDs = Set.of("7aca1fa6-980c-44e2-a75e-3144b18a5993");
        Set<String> defaultVMImageUUIDs = Set.of("7aca1fa6-980c-44e2-a75e-3144b18a5993");
        Set<String> supportedVersions = Set.of("2.1.0-dev.2");
        PrefixMatchImages prefixMatchImages = new PrefixMatchImages(vMImageUUIDs, defaultVMImageUUIDs, supportedVersions);
        when(prefixMatcherService.prefixMatchForCBVersion(any(), any())).thenReturn(prefixMatchImages);
        setupLatestDefaultImageUuidProvider("7aca1fa6-980c-44e2-a75e-3144b18a5993");

        ImageFilter imageFilter = new ImageFilter(imageCatalog, Set.of("AWS"), null, true, null, null);
        StatedImage image = underTest.getLatestBaseImageDefaultPreferred(imageFilter, i -> true);

        assertEquals("7aca1fa6-980c-44e2-a75e-3144b18a5993", image.getImage().getUuid());
        assertTrue(image.getImage().isDefaultImage());
    }

    @Test
    public void testGetLatestBaseImageDefaultPreferredWithMultipleDefaults() throws Exception {
        setupUserProfileService();
        setupImageCatalogProvider(DEFAULT_CATALOG_URL, V2_CB_CATALOG_FILE);
        ReflectionTestUtils.setField(underTest, ImageCatalogService.class, "cbVersion", "2.1.0-dev.1", null);
        setupLatestDefaultImageUuidProvider("7aca1fa6-980c-44e2-a75e-3144b18a5993");

        ImageFilter imageFilter = new ImageFilter(imageCatalog, Set.of("AWS"), null, true, null, null);
        StatedImage image = underTest.getLatestBaseImageDefaultPreferred(imageFilter, i -> true);

        assertEquals("7aca1fa6-980c-44e2-a75e-3144b18a5993", image.getImage().getUuid());
        assertTrue(image.getImage().isDefaultImage());
    }

    @Test
    public void testGetLatestBaseImageDefaultPreferredWenNotLatestSelected() throws Exception {
        setupUserProfileService();
        setupImageCatalogProvider(DEFAULT_CATALOG_URL, V2_CB_CATALOG_FILE);
        ReflectionTestUtils.setField(underTest, ImageCatalogService.class, "cbVersion", "2.1.0-dev.2", null);
        setupLatestDefaultImageUuidProvider("f6e778fc-7f17-4535-9021-515351df3691");

        ImageFilter imageFilter = new ImageFilter(imageCatalog, Set.of("AWS"), null, true, null, null);
        StatedImage image = underTest.getLatestBaseImageDefaultPreferred(imageFilter, i -> true);

        assertEquals("f6e778fc-7f17-4535-9021-515351df3691", image.getImage().getUuid());
        assertTrue(image.getImage().isDefaultImage());
    }

    @Test
    public void testGetStatedImagesFilteredByOperatingSystems() throws CloudbreakImageCatalogException, IOException {
        setupUserProfileService();
        setupImageCatalogProvider(DEFAULT_CATALOG_URL, DEV_CATALOG_FILE);
        Set<String> operatingSystems = new HashSet<>(Arrays.asList("redhat7", "redhat6", "amazonlinux2"));

        ImageFilter imageFilter = new ImageFilter(imageCatalog, Set.of("AWS"), null, true, operatingSystems, null);
        StatedImages images = underTest.getStatedImagesFilteredByOperatingSystems(imageFilter, i -> true);

        boolean allMatch = images.getImages().getBaseImages().stream().allMatch(image -> operatingSystems.contains(image.getOsType()));
        assertTrue("All images should be based on supported OS", allMatch);
    }

    @Test
    public void testGetImagesWhenExactVersionExistsInCatalog() throws Exception {
        String cbVersion = "1.16.2";
        ImageCatalog imageCatalog = getImageCatalog();

        StatedImages images = underTest.getImages(new ImageFilter(imageCatalog, Collections.singleton("azure"), cbVersion));

        boolean exactImageIdMatch = images.getImages().getCdhImages().stream()
                .anyMatch(img -> "666aa8bf-bc1a-4cc6-43f1-427b4432c8c2".equals(img.getUuid()));
        assertTrue("Result doesn't contain the required image with id.", exactImageIdMatch);
    }

    @Test
    public void testGetImagesWhenExactVersionExistsInCatalogAndMorePlatformRequested() throws Exception {
        String cbVersion = "2.0.0";
        ImageCatalog imageCatalog = getImageCatalog();
        StatedImages images = underTest.getImages(
                new ImageFilter(imageCatalog, ImmutableSet.of("aws", "azure"), cbVersion, true, ImmutableSet.of("amazonlinux"), null));
        for (Image image : images.getImages().getBaseImages()) {
            boolean containsAws = image.getImageSetsByProvider().entrySet().stream().anyMatch(platformImages -> "aws".equals(platformImages.getKey()));
            boolean containsAzure = image.getImageSetsByProvider().entrySet().stream().anyMatch(platformImages -> "azure_rm".equals(platformImages.getKey()));
            assertTrue("Result doesn't contain the required image with id.", containsAws || containsAzure);
        }
    }

    @Test
    public void testGetImagesWhenLatestVersionDoesntExistInCatalogShouldReturnWithReleasedVersionIfExists() throws Exception {
        setupImageCatalogProvider(DEFAULT_CATALOG_URL, PROD_CATALOG_FILE);
        ImageCatalog imageCatalog = getImageCatalog();

        StatedImages images = underTest.getImages(
                new ImageFilter(imageCatalog, Collections.singleton("aws"), "2.6.0", true, ImmutableSet.of("amazonlinux"), null));

        boolean match = images.getImages().getBaseImages().stream()
                .anyMatch(img -> "0f575e42-9d90-4f85-5f8a-bdced2221dc3".equals(img.getUuid()));
        assertTrue("Result doesn't contain the required base image with id.", match);
    }

    @Test
    public void testGetImagesWhenLatestDevVersionDoesntExistInCatalogShouldReturnWithReleasedVersionIfExists() throws Exception {
        setupImageCatalogProvider(DEFAULT_CATALOG_URL, DEV_CATALOG_FILE);
        ImageCatalog imageCatalog = getImageCatalog();

        Set<String> vMImageUUIDs = Set.of("cab28152-f5e1-43e1-5107-9e7bbed33eef");
        Set<String> defaultVMImageUUIDs = Set.of("cab28152-f5e1-43e1-5107-9e7bbed33eef");
        Set<String> supportedVersions = Set.of("2.1.0-dev.2");
        PrefixMatchImages prefixMatchImages = new PrefixMatchImages(vMImageUUIDs, defaultVMImageUUIDs, supportedVersions);
        when(prefixMatcherService.prefixMatchForCBVersion(any(), any())).thenReturn(prefixMatchImages);

        StatedImages images = underTest.getImages(
                new ImageFilter(imageCatalog, Collections.singleton("aws"), "2.6.0-dev.132", true, ImmutableSet.of("amazonlinux", "centos7"), null));

        boolean match = images.getImages().getBaseImages().stream()
                .anyMatch(img -> "cab28152-f5e1-43e1-5107-9e7bbed33eef".equals(img.getUuid()));
        assertTrue("Result doesn't contain the required base image with id.", match);
    }

    @Test
    public void testGetImagesWhenLatestRcVersionDoesntExistInCatalogShouldReturnWithReleasedVersionIfExists() throws Exception {
        setupImageCatalogProvider(DEFAULT_CATALOG_URL, RC_CATALOG_FILE);
        ImageCatalog imageCatalog = getImageCatalog();

        StatedImages images = underTest.getImages(
                new ImageFilter(imageCatalog, Collections.singleton("aws"), "2.6.0-rc.13", true, ImmutableSet.of("amazonlinux", "centos7"), null));

        boolean match = images.getImages().getBaseImages().stream()
                .anyMatch(img -> "0f575e42-9d90-4f85-5f8a-bdced2221dc3".equals(img.getUuid()));
        assertTrue("Result doesn't contain the required base image with id.", match);
    }

    @Test
    public void testGetImagesWhenSimilarRcVersionDoesntExistInDevCatalogShouldReturnWithLatestDevVersionIfExists() throws Exception {
        setupImageCatalogProvider(DEFAULT_CATALOG_URL, DEV_CATALOG_FILE);
        ImageCatalog imageCatalog = getImageCatalog();

        Set<String> vMImageUUIDs = Set.of("0f575e42-9d90-4f85-5f8a-bdced2221dc3");
        Set<String> defaultVMImageUUIDs = Set.of("0f575e42-9d90-4f85-5f8a-bdced2221dc3");
        Set<String> supportedVersions = Set.of("2.1.0-dev.2");
        PrefixMatchImages prefixMatchImages = new PrefixMatchImages(vMImageUUIDs, defaultVMImageUUIDs, supportedVersions);
        when(prefixMatcherService.prefixMatchForCBVersion(any(), any())).thenReturn(prefixMatchImages);

        StatedImages images = underTest.getImages(
                new ImageFilter(imageCatalog, Collections.singleton("aws"), "2.6.0-rc.13", true, ImmutableSet.of("amazonlinux", "centos7"), null));

        boolean match = images.getImages().getBaseImages().stream()
                .anyMatch(img -> img.getUuid().equals("0f575e42-9d90-4f85-5f8a-bdced2221dc3"));
        assertTrue("Result doesn't contain the required base image with id.", match);
    }

    @Test
    public void testGetImagesWhenSimilarDevVersionDoesntExistInCatalogShouldReturnWithReleasedVersionIfExists() throws Exception {
        ImageCatalog imageCatalog = getImageCatalog();

        Set<String> vMImageUUIDs = Set.of("666aa8bf-bc1a-4cc6-43f1-427b4432c8c2");
        Set<String> defaultVMImageUUIDs = Set.of("666aa8bf-bc1a-4cc6-43f1-427b4432c8c2");
        Set<String> supportedVersions = Set.of("2.1.0-dev.2");
        PrefixMatchImages prefixMatchImages = new PrefixMatchImages(vMImageUUIDs, defaultVMImageUUIDs, supportedVersions);
        when(prefixMatcherService.prefixMatchForCBVersion(any(), any())).thenReturn(prefixMatchImages);

        StatedImages images = underTest.getImages(new ImageFilter(imageCatalog, Collections.singleton("azure"), "1.16.2-dev.132"));

        boolean match = images.getImages().getCdhImages().stream()
                .anyMatch(img -> "666aa8bf-bc1a-4cc6-43f1-427b4432c8c2".equals(img.getUuid()));
        assertTrue("Result doesn't contain the required image with id.", match);
    }

    @Test
    public void testGetImagesWhenSimilarRcVersionDoesntExistInCatalogShouldReturnWithReleasedVersionIfExists() throws Exception {
        ImageCatalog imageCatalog = getImageCatalog();

        Set<String> vMImageUUIDs = Set.of("666aa8bf-bc1a-4cc6-43f1-427b4432c8c2");
        Set<String> defaultVMImageUUIDs = Set.of("666aa8bf-bc1a-4cc6-43f1-427b4432c8c2");
        Set<String> supportedVersions = Set.of("2.1.0-dev.1", "2.0.0", "2.1.0-dev.100", "2.1.0-dev.2");
        PrefixMatchImages prefixMatchImages = new PrefixMatchImages(vMImageUUIDs, defaultVMImageUUIDs, supportedVersions);

        when(prefixMatcherService.prefixMatchForCBVersion(eq("1.16.2-rc.13"), any())).thenReturn(prefixMatchImages);

        StatedImages images = underTest.getImages(new ImageFilter(imageCatalog, Collections.singleton("azure"), "1.16.2-rc.13"));

        boolean match = images.getImages().getCdhImages().stream()
                .anyMatch(img -> "666aa8bf-bc1a-4cc6-43f1-427b4432c8c2".equals(img.getUuid()));
        assertTrue("Result doesn't contain the required image with id.", match);
    }

    @Test
    public void testGetImagesWhenSimilarDevVersionExistsInCatalog() throws Exception {
        ImageCatalog imageCatalog = getImageCatalog();
        Set<String> vMImageUUIDs = Set.of("f6e778fc-7f17-4535-9021-515351df3691");
        Set<String> defaultVMImageUUIDs = Set.of("f6e778fc-7f17-4535-9021-515351df3691", "7aca1fa6-980c-44e2-a75e-3144b18a5993");
        Set<String> supportedVersions = Set.of("2.1.0-dev.1", "2.0.0", "2.1.0-dev.100", "2.1.0-dev.2");
        PrefixMatchImages prefixMatchImages = new PrefixMatchImages(vMImageUUIDs, defaultVMImageUUIDs, supportedVersions);

        when(prefixMatcherService.prefixMatchForCBVersion(eq("2.1.0-dev.4000"), any())).thenReturn(prefixMatchImages);

        StatedImages images = underTest.getImages(new ImageFilter(imageCatalog, Collections.singleton("aws"), "2.1.0-dev.4000", true, null, null));

        boolean baseImgMatch = images.getImages().getBaseImages().stream()
                .anyMatch(img -> "f6e778fc-7f17-4535-9021-515351df3691".equals(img.getUuid()));
        assertTrue("Result doesn't contain the required image with id.", baseImgMatch);
    }

    @Test
    public void testGetImagesWhenSimilarRcVersionExistsInCatalog() throws Exception {
        ImageCatalog imageCatalog = getImageCatalog();

        Set<String> vMImageUUIDs = Set.of("666aa8bf-bc1a-4cc6-43f1-427b4432c8c2");
        Set<String> defaultVMImageUUIDs = Set.of("666aa8bf-bc1a-4cc6-43f1-427b4432c8c2");
        Set<String> supportedVersions = Set.of("2.1.0-dev.1", "2.0.0", "2.1.0-dev.100", "2.1.0-dev.2");
        PrefixMatchImages prefixMatchImages = new PrefixMatchImages(vMImageUUIDs, defaultVMImageUUIDs, supportedVersions);
        when(prefixMatcherService.prefixMatchForCBVersion(eq("2.0.0-rc.4"), any())).thenReturn(prefixMatchImages);

        StatedImages images = underTest.getImages(new ImageFilter(imageCatalog, Collections.singleton("azure"), "2.0.0-rc.4"));

        boolean match = images.getImages().getCdhImages().stream()
                .anyMatch(img -> "666aa8bf-bc1a-4cc6-43f1-427b4432c8c2".equals(img.getUuid()));
        assertTrue("Result doesn't contain the required image with id.", match);
    }

    @Test
    public void testGetImagesWhenExactVersionExistsInCatalogForPlatform() throws Exception {
        ImageCatalog imageCatalog = getImageCatalog();

        StatedImages images = underTest.getImages(new ImageFilter(imageCatalog, Collections.singleton("azure"), "1.16.2"));

        boolean exactImageIdMatch = images.getImages().getCdhImages().stream()
                .anyMatch(img -> "666aa8bf-bc1a-4cc6-43f1-427b4432c8c2".equals(img.getUuid()));
        assertTrue("Result doesn't contain the required image with id for the platform.", exactImageIdMatch);
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
        ret.setImageCatalogUrl(null);
        ret.setName(CUSTOM_CATALOG_NAME);
        when(imageCatalogRepository.findByNameAndWorkspaceId("name", ORG_ID)).thenReturn(Optional.of(ret));

        StatedImages actual = underTest.getImages(ORG_ID, "name", "aws");
        assertEquals(CUSTOM_CATALOG_NAME, actual.getImageCatalogName());
        assertNull(actual.getImageCatalogUrl());
    }

    @Test
    public void testGetImagesWhenCustomImageCatalogDoesNotExists() throws Exception {
        when(imageCatalogRepository.findByNameAndWorkspaceId(anyString(), anyLong())).thenThrow(new NotFoundException("no no"));

        thrown.expectMessage("The verycool catalog does not exist or does not belongs to your account.");
        thrown.expect(CloudbreakImageCatalogException.class);

        underTest.getImages(ORG_ID, "verycool", "aws").getImages();

        verify(entitlementService, times(1)).baseImageEnabled(Objects.requireNonNull(Crn.fromString(user.getUserCrn())).getAccountId());
        verify(entitlementService, never()).baseImageEnabled(user.getUserCrn());
        verify(imageCatalogProvider, times(0)).getImageCatalogV3("");
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
        String name = "cdp-default";

        thrown.expectMessage("cdp-default cannot be deleted because it is an environment default image catalog.");
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
        String name = "cdp-default";
        ImageCatalog actual = ThreadBasedUserCrnProvider.doAs(CrnTestUtil.getUserCrnBuilder()
                .setAccountId("ACCOUNT_ID")
                .setResource("USER")
                .build().toString(), () -> underTest.get(ORG_ID, name));

        verify(imageCatalogRepository, times(0)).findByNameAndWorkspace(eq(name), any(Workspace.class));

        assertEquals(actual.getName(), name);
        assertNull(actual.getId());
    }

    @Test
    public void testGetImagesFromDefaultWithEmptyInput() throws CloudbreakImageCatalogException {
        thrown.expect(BadRequestException.class);

        underTest.getImagesFromDefault(ORG_ID, null, null, Collections.emptySet());

        verify(entitlementService, times(1)).baseImageEnabled(Objects.requireNonNull(Crn.fromString(user.getUserCrn())).getAccountId());
        verify(entitlementService, never()).baseImageEnabled(user.getUserCrn());

        thrown.expectMessage("Either platform or stackName should be filled in request");
    }

    @Test
    public void testGetImagesFromDefaultGivenBothInput() throws CloudbreakImageCatalogException {
        thrown.expect(BadRequestException.class);

        underTest.getImagesFromDefault(ORG_ID, "stack", "AWS", Collections.emptySet());

        verify(entitlementService, times(1)).baseImageEnabled(Objects.requireNonNull(Crn.fromString(user.getUserCrn())).getAccountId());
        verify(entitlementService, never()).baseImageEnabled(user.getUserCrn());

        thrown.expectMessage("Platform or stackName cannot be filled in the same request");
    }

    @Test
    public void testGetImagesFromDefaultWithStackName() throws CloudbreakImageCatalogException {
        when(stackImageFilterService.getApplicableImages(anyLong(), anyString())).thenReturn(new Images(Lists.newArrayList(),
                Lists.newArrayList(), Lists.newArrayList(), Sets.newHashSet()));

        underTest.getImagesFromDefault(ORG_ID, "stack", null, Collections.emptySet());

        verify(stackImageFilterService, never()).getApplicableImages(anyLong(), anyString(), anyString());
        verify(stackImageFilterService, times(1)).getApplicableImages(anyLong(), anyString());
    }

    @Test
    public void testGetImagesFromDefaultWithPlatform() throws CloudbreakImageCatalogException, IOException {
        setupUserProfileService();
        setupImageCatalogProvider(DEFAULT_CATALOG_URL, V2_CB_CATALOG_FILE);

        underTest.getImagesFromDefault(ORG_ID, null, "AWS", Collections.emptySet());

        verify(entitlementService, times(1)).baseImageEnabled(Objects.requireNonNull(Crn.fromString(user.getUserCrn())).getAccountId());
        verify(entitlementService, never()).baseImageEnabled(user.getUserCrn());
        verify(stackImageFilterService, never()).getApplicableImages(anyLong(), anyString(), anyString());
        verify(stackImageFilterService, never()).getApplicableImages(anyLong(), anyString());
    }

    @Test
    public void testGetImagesWithEmptyInput() throws CloudbreakImageCatalogException {
        thrown.expect(BadRequestException.class);

        underTest.getImagesByCatalogName(ORG_ID, "catalog", null, null);

        verify(entitlementService, times(1)).baseImageEnabled(Objects.requireNonNull(Crn.fromString(user.getUserCrn())).getAccountId());
        verify(entitlementService, never()).baseImageEnabled(user.getUserCrn());

        thrown.expectMessage("Either platform or stackName should be filled in request");
    }

    @Test
    public void testGetImagesGivenBothInput() throws CloudbreakImageCatalogException {
        thrown.expect(BadRequestException.class);

        underTest.getImagesByCatalogName(ORG_ID, "catalog", "stack", "AWS");

        verify(entitlementService, times(1)).baseImageEnabled(Objects.requireNonNull(Crn.fromString(user.getUserCrn())).getAccountId());
        verify(entitlementService, never()).baseImageEnabled(user.getUserCrn());

        thrown.expectMessage("Platform or stackName cannot be filled in the same request");
    }

    @Test
    public void testGetImagesWithStackName() throws CloudbreakImageCatalogException {
        when(stackImageFilterService.getApplicableImages(anyLong(), anyString(), anyString())).thenReturn(new Images(Lists.newArrayList(),
                Lists.newArrayList(), Lists.newArrayList(), Sets.newHashSet()));

        underTest.getImagesByCatalogName(ORG_ID, "catalog", "stack", null);

        verify(stackImageFilterService, times(1)).getApplicableImages(anyLong(), anyString(), anyString());
        verify(stackImageFilterService, never()).getApplicableImages(anyLong(), anyString());
    }

    @Test
    public void testGetImagesWithPlatform() throws CloudbreakImageCatalogException, IOException {
        setupUserProfileService();
        setupImageCatalogProvider(DEFAULT_CATALOG_URL, V2_CB_CATALOG_FILE);
        ImageCatalog imageCatalog = new ImageCatalog();
        imageCatalog.setImageCatalogUrl(DEFAULT_CATALOG_URL);

        when(imageCatalogRepository.findByNameAndWorkspaceId(anyString(), anyLong())).thenReturn(Optional.of(imageCatalog));

        underTest.getImagesByCatalogName(ORG_ID, "catalog", null, "AWS");

        verify(entitlementService, times(1)).baseImageEnabled(Objects.requireNonNull(Crn.fromString(user.getUserCrn())).getAccountId());
        verify(entitlementService, never()).baseImageEnabled(user.getUserCrn());
        verify(stackImageFilterService, never()).getApplicableImages(anyLong(), anyString(), anyString());
        verify(stackImageFilterService, never()).getApplicableImages(anyLong(), anyString());
    }

    @Test
    public void testDeleteByWorkspaceWhenDtoNameFilledThenDeleteCalled() {
        when(legacyRestRequestThreadLocalService.getCloudbreakUser()).thenReturn(mock(CloudbreakUser.class));
        when(userService.getOrCreate(any(CloudbreakUser.class))).thenReturn(user);
        when(userProfileService.getOrCreate(user)).thenReturn(mock(UserProfile.class));

        ImageCatalog catalog = getImageCatalog();
        when(imageCatalogRepository.findByNameAndWorkspaceId(catalog.getName(), catalog.getWorkspace().getId())).thenReturn(Optional.of(catalog));

        ImageCatalog result = underTest.delete(NameOrCrn.ofName(catalog.getName()), catalog.getWorkspace().getId());

        assertEquals(catalog, result);
        verify(imageCatalogRepository, times(2)).findByNameAndWorkspaceId(anyString(), anyLong());
        verify(imageCatalogRepository, times(1)).save(any(ImageCatalog.class));
        verify(imageCatalogRepository, times(1)).save(catalog);
    }

    @Test
    public void testDeleteByWorkspaceWhenDtoCrnFilledThenDeleteCalled() {
        when(legacyRestRequestThreadLocalService.getCloudbreakUser()).thenReturn(mock(CloudbreakUser.class));
        when(userService.getOrCreate(any(CloudbreakUser.class))).thenReturn(user);
        when(userProfileService.getOrCreate(user)).thenReturn(mock(UserProfile.class));

        ImageCatalog catalog = getImageCatalog();
        when(imageCatalogRepository.findByResourceCrnAndArchivedFalseAndImageCatalogUrlIsNotNull(catalog.getResourceCrn())).thenReturn(Optional.of(catalog));
        when(imageCatalogRepository.findByNameAndWorkspaceId(catalog.getName(), catalog.getWorkspace().getId())).thenReturn(Optional.of(catalog));

        ImageCatalog result = underTest.delete(NameOrCrn.ofCrn(catalog.getResourceCrn()), catalog.getWorkspace().getId());

        assertEquals(catalog, result);
        verify(imageCatalogRepository, times(1)).findByResourceCrnAndArchivedFalseAndImageCatalogUrlIsNotNull(anyString());
        verify(imageCatalogRepository, times(1)).save(any(ImageCatalog.class));
        verify(imageCatalogRepository, times(1)).save(catalog);
    }

    @Test
    public void testGetByWorkspaceWhenDtoNameFilledThenProperGetCalled() {
        ImageCatalog catalog = getImageCatalog();
        when(imageCatalogRepository.findByNameAndWorkspaceId(catalog.getName(), catalog.getWorkspace().getId())).thenReturn(Optional.of(catalog));

        ImageCatalog result = underTest.get(NameOrCrn.ofName(catalog.getName()), catalog.getWorkspace().getId());

        assertEquals(catalog, result);
        verify(imageCatalogRepository, times(1)).findByNameAndWorkspaceId(anyString(), anyLong());
        verify(imageCatalogRepository, times(1)).findByNameAndWorkspaceId(catalog.getName(), catalog.getWorkspace().getId());
    }

    @Test
    public void testGetByWorkspaceWhenDtoCrnFilledThenProperGetCalled() {
        ImageCatalog catalog = getImageCatalog();
        when(imageCatalogRepository.findByResourceCrnAndArchivedFalseAndImageCatalogUrlIsNotNull(catalog.getResourceCrn())).thenReturn(Optional.of(catalog));

        ImageCatalog result = underTest.get(NameOrCrn.ofCrn(catalog.getResourceCrn()), catalog.getWorkspace().getId());

        assertEquals(catalog, result);
        verify(imageCatalogRepository, times(1)).findByResourceCrnAndArchivedFalseAndImageCatalogUrlIsNotNull(anyString());
        verify(imageCatalogRepository, times(1)).findByResourceCrnAndArchivedFalseAndImageCatalogUrlIsNotNull(catalog.getResourceCrn());
    }

    @Test
    public void testPopulateCrnCorrectly() throws TransactionExecutionException {
        ImageCatalog imageCatalog = getImageCatalog();

        when(workspaceService.get(WORKSPACE_ID, user)).thenReturn(imageCatalog.getWorkspace());
        when(workspaceService.retrieveForUser(user)).thenReturn(Set.of(imageCatalog.getWorkspace()));
        when(transactionService.required(isA(Supplier.class))).thenAnswer(invocation -> invocation.getArgument(0, Supplier.class).get());

        underTest.createForLoggedInUser(imageCatalog, WORKSPACE_ID, "account_id", "creator");

        assertThat(imageCatalog.getCreator(), is("creator"));
        String crnPattern = "crn:cdp:datahub:us-west-1:account_id:imageCatalog:.*";
        assertTrue(imageCatalog.getResourceCrn().matches(crnPattern));
        verify(ownerAssignmentService).assignResourceOwnerRoleIfEntitled(eq("creator"), matches(crnPattern), eq("account_id"));
    }

    @Test
    public void testGetImageByCatalogNameWithCustomCatalogNameAndExistingDatahubImage()
            throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException, IOException {

        setupCustomImageCatalog(ImageType.DATAHUB, "949bffa3-17d4-4076-9d5a-bf3d23c1086b", CUSTOM_BASE_PARCEL_URL);

        StatedImage image = underTest.getImageByCatalogName(WORKSPACE_ID, CUSTOM_IMAGE_ID, CUSTOM_CATALOG_NAME);
        assertEquals("Test uuid", image.getImage().getUuid());

        verify(customImageProvider).mergeSourceImageAndCustomImageProperties(sourceImageCaptor.capture(), any(), any(), any());

        Assertions.assertThat(sourceImageCaptor.getValue().getImage().getUuid()).isEqualTo("949bffa3-17d4-4076-9d5a-bf3d23c1086b");
    }

    @Test
    public void testGetImageByCatalogNameWithCustomCatalogNameAndExistingDatalakeImage()
            throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException, IOException {

        setupCustomImageCatalog(ImageType.DATALAKE, "232fe6b6-aec4-4fa9-bb02-2c295d319a36", CUSTOM_BASE_PARCEL_URL + "/");

        StatedImage image = underTest.getImageByCatalogName(WORKSPACE_ID, CUSTOM_IMAGE_ID, CUSTOM_CATALOG_NAME);
        assertEquals("Test uuid", image.getImage().getUuid());

        verify(customImageProvider).mergeSourceImageAndCustomImageProperties(sourceImageCaptor.capture(), any(), any(), any());

        Assertions.assertThat(sourceImageCaptor.getValue().getImage().getUuid()).isEqualTo("232fe6b6-aec4-4fa9-bb02-2c295d319a36");
    }

    @Test
    public void testGetImageByCatalogNameWithCustomCatalogNameAndExistingFreeipaImage()
            throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException, IOException {

        setupCustomImageCatalog(ImageType.FREEIPA, "3b6ae396-df40-4e2b-7c2b-54b15822614c", CUSTOM_BASE_PARCEL_URL);

        StatedImage image = underTest.getImageByCatalogName(WORKSPACE_ID, CUSTOM_IMAGE_ID, CUSTOM_CATALOG_NAME);
        assertEquals("Test uuid", image.getImage().getUuid());

        verify(customImageProvider).mergeSourceImageAndCustomImageProperties(sourceImageCaptor.capture(), any(), any(), any());

        Assertions.assertThat(sourceImageCaptor.getValue().getImage().getUuid()).isEqualTo("3b6ae396-df40-4e2b-7c2b-54b15822614c");
    }

    @Test
    public void testGetImageByCatalogNameWithCustomCatalogNameAndUnknownImageType()
            throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException, IOException {

        thrown.expect(CloudbreakImageCatalogException.class);

        setupCustomImageCatalog(ImageType.UNKNOWN, "3b6ae396-df40-4e2b-7c2b-54b15822614c", CUSTOM_BASE_PARCEL_URL);

        underTest.getImageByCatalogName(WORKSPACE_ID, CUSTOM_IMAGE_ID, CUSTOM_CATALOG_NAME);

        thrown.expectMessage("Image type is not supported.");
    }

    @Test
    public void testGetDatalakeImagesByFromCustomImageCatalogByImageCatalogName() throws CloudbreakImageCatalogException, IOException {
        ImageCatalog imageCatalog = new ImageCatalog();
        imageCatalog.setCustomImages(Set.of(getCustomImage(ImageType.DATALAKE, "5b60b723-4beb-40b0-5cba-47ea9c9b6e53", CUSTOM_BASE_PARCEL_URL)));
        StatedImage statedImage = StatedImage.statedImage(getImage(), CUSTOM_IMAGE_CATALOG_URL, CUSTOM_CATALOG_NAME);

        setupImageCatalogProvider(DEFAULT_CATALOG_URL, DEV_CATALOG_FILE);
        when(imageCatalogRepository.findByNameAndWorkspaceId(CUSTOM_CATALOG_NAME, WORKSPACE_ID)).thenReturn(Optional.of(imageCatalog));
        when(customImageProvider.mergeSourceImageAndCustomImageProperties(any(), any(), any(), any())).thenReturn(statedImage);

        StatedImages actual = underTest.getImages(WORKSPACE_ID, CUSTOM_CATALOG_NAME, Set.of("AWS"));
        assertEquals(statedImage.getImage(), actual.getImages().getCdhImages().stream().findFirst().get());

    }

    @Test
    public void testGetFreeipaImagesByFromCustomImageCatalogByImageCatalogName() throws CloudbreakImageCatalogException, IOException {
        ImageCatalog imageCatalog = new ImageCatalog();
        imageCatalog.setCustomImages(Set.of(getCustomImage(ImageType.FREEIPA, "cc7f487a-c992-400a-85ed-aae5f73ca4d2", CUSTOM_BASE_PARCEL_URL)));
        StatedImage statedImage = StatedImage.statedImage(getImage(), CUSTOM_IMAGE_CATALOG_URL, CUSTOM_CATALOG_NAME);

        setupImageCatalogProvider(DEFAULT_FREEIPA_CATALOG_URL, V3_FREEIPA_CATALOG_FILE);
        when(imageCatalogRepository.findByNameAndWorkspaceId(CUSTOM_CATALOG_NAME, WORKSPACE_ID)).thenReturn(Optional.of(imageCatalog));
        when(customImageProvider.mergeSourceImageAndCustomImageProperties(any(), any(), any(), any())).thenReturn(statedImage);

        StatedImages actual = underTest.getImages(WORKSPACE_ID, CUSTOM_CATALOG_NAME, Set.of("AWS"));
        assertEquals(statedImage.getImage(), actual.getImages().getFreeIpaImages().stream().findFirst().get());

    }

    @Test
    public void testGetImageShouldLookupCustomImageInCaseOfNullImageCatalogUrl()
            throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException, IOException {
        ImageCatalog imageCatalog = new ImageCatalog();
        CustomImage customImage = getCustomImage(ImageType.DATALAKE, "5b60b723-4beb-40b0-5cba-47ea9c9b6e53", CUSTOM_BASE_PARCEL_URL);
        imageCatalog.setCustomImages(Set.of(customImage));
        StatedImage statedImage = StatedImage.statedImage(getImage(), CUSTOM_IMAGE_CATALOG_URL, CUSTOM_CATALOG_NAME);

        setupImageCatalogProvider(DEFAULT_CATALOG_URL, DEV_CATALOG_FILE);
        when(restRequestThreadLocalService.getRequestedWorkspaceId()).thenReturn(WORKSPACE_ID);
        when(imageCatalogRepository.findByNameAndWorkspaceId(CUSTOM_CATALOG_NAME, WORKSPACE_ID)).thenReturn(Optional.of(imageCatalog));
        when(customImageProvider.mergeSourceImageAndCustomImageProperties(any(), any(), any(), any())).thenReturn(statedImage);

        StatedImage actual = underTest.getImage(null, CUSTOM_CATALOG_NAME, CUSTOM_IMAGE_ID);

        assertEquals(statedImage.getImage(), actual.getImage());
    }

    private void setupImageCatalogProvider(String catalogUrl, String catalogFile) throws IOException, CloudbreakImageCatalogException {
        String catalogJson = FileReaderUtils.readFileFromClasspath(catalogFile);
        CloudbreakImageCatalogV3 catalog = JsonUtil.readValue(catalogJson, CloudbreakImageCatalogV3.class);
        when(imageCatalog.getImageCatalogUrl()).thenReturn(catalogUrl);
        when(imageCatalogProvider.getImageCatalogV3(catalogUrl)).thenReturn(catalog);
        when(imageCatalogProvider.getImageCatalogV3(catalogUrl, true)).thenReturn(catalog);
        when(cloudbreakVersionListProvider.getVersions(any())).thenReturn(catalog.getVersions().getCloudbreakVersions());
    }

    private void setupImageCatalogProviderWithoutVersions(String catalogUrl, String catalogFile) throws IOException, CloudbreakImageCatalogException {
        String catalogJson = FileReaderUtils.readFileFromClasspath(catalogFile);
        CloudbreakImageCatalogV3 catalog = JsonUtil.readValue(catalogJson, CloudbreakImageCatalogV3.class);
        ReflectionTestUtils.setField(catalog, "versions", null);
        when(imageCatalog.getImageCatalogUrl()).thenReturn(catalogUrl);
        when(imageCatalogProvider.getImageCatalogV3(catalogUrl)).thenReturn(catalog);
    }

    private void setupUserProfileService() {
        UserProfile userProfile = new UserProfile();
        when(userProfileService.getOrCreate(any(User.class))).thenReturn(userProfile);
    }

    private void setupLatestDefaultImageUuidProvider(String uuid) {
        when(latestDefaultImageUuidProvider.getLatestDefaultImageUuids(any(), any())).thenReturn(List.of(uuid));
    }

    private void setupCustomImageCatalog(ImageType imageType, String customizedImageId, String baseParcelUrl)
            throws IOException, CloudbreakImageCatalogException {

        ImageCatalog imageCatalog = getImageCatalog();
        CustomImage customImage = getCustomImage(imageType, customizedImageId, baseParcelUrl);
        imageCatalog.setCustomImages(Set.of(customImage));

        setupImageCatalogProvider(CUSTOM_IMAGE_CATALOG_URL, imageType == ImageType.FREEIPA ? V3_FREEIPA_CATALOG_FILE : V3_CB_CATALOG_FILE);
        ReflectionTestUtils.setField(underTest,
                ImageCatalogService.class,
                imageType == ImageType.FREEIPA ? "defaultFreeIpaCatalogUrl" : "defaultCatalogUrl",
                CUSTOM_IMAGE_CATALOG_URL, null);
        when(imageCatalogRepository.findByNameAndWorkspaceId(anyString(), anyLong())).thenReturn(Optional.of(imageCatalog));

        StatedImage statedImage = StatedImage.statedImage(getImage(), CUSTOM_IMAGE_CATALOG_URL, CUSTOM_CATALOG_NAME);
        when(customImageProvider.mergeSourceImageAndCustomImageProperties(any(), any(), any(), any())).thenReturn(statedImage);
    }

    private ImageCatalog getImageCatalog() {
        ImageCatalog imageCatalog = new ImageCatalog();
        imageCatalog.setImageCatalogUrl(DEFAULT_CATALOG_URL);
        imageCatalog.setName("default");
        Workspace ws = new Workspace();
        ws.setId(ORG_ID);
        imageCatalog.setWorkspace(ws);
        imageCatalog.setCreator("someone");
        imageCatalog.setResourceCrn("someCrn");
        return imageCatalog;
    }

    private Image getImage() {
        return new Image(null, null, null, null, "Test uuid", null, null, null, null, null, null, null, null, null, true, null, null);
    }

    private CustomImage getCustomImage(ImageType imageType, String customizedImageId, String baseParcelUrl) {
        CustomImage customImage = new CustomImage();
        customImage.setId(0L);
        customImage.setName(CUSTOM_IMAGE_ID);
        customImage.setDescription("Test image");
        customImage.setImageType(imageType);
        customImage.setCustomizedImageId(customizedImageId);
        customImage.setBaseParcelUrl(baseParcelUrl);
        return customImage;
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
