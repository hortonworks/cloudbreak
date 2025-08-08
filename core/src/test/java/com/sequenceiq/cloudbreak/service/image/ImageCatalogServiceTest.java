package com.sequenceiq.cloudbreak.service.image;

import static com.sequenceiq.common.model.ImageCatalogPlatform.imageCatalogPlatform;
import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.AdditionalAnswers.returnsSecondArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
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
import static org.mockito.Mockito.verifyNoInteractions;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.sequenceiq.authorization.service.OwnerAssignmentService;
import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.auth.crn.CrnTestUtil;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareCrnGenerator;
import com.sequenceiq.cloudbreak.cloud.CloudConstant;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.cloud.model.catalog.CloudbreakImageCatalogV3;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Images;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.common.provider.ProviderPreferencesService;
import com.sequenceiq.cloudbreak.common.service.PlatformStringTransformer;
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
import com.sequenceiq.cloudbreak.service.image.catalog.AdvertisedImageCatalogService;
import com.sequenceiq.cloudbreak.service.image.catalog.AdvertisedImageProvider;
import com.sequenceiq.cloudbreak.service.image.catalog.ImageCatalogServiceProxy;
import com.sequenceiq.cloudbreak.service.image.catalog.VersionBasedImageCatalogService;
import com.sequenceiq.cloudbreak.service.image.catalog.VersionBasedImageProvider;
import com.sequenceiq.cloudbreak.service.image.catalog.model.ImageCatalogMetaData;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.upgrade.image.ImageFilterResult;
import com.sequenceiq.cloudbreak.service.user.UserProfileHandler;
import com.sequenceiq.cloudbreak.service.user.UserProfileService;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;
import com.sequenceiq.cloudbreak.structuredevent.LegacyRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.cloudbreak.util.TestConstants;
import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.common.api.type.ImageType;
import com.sequenceiq.common.model.Architecture;
import com.sequenceiq.common.model.ImageCatalogPlatform;

@ExtendWith(MockitoExtension.class)
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

    private static final Long WORKSPACE_ID = 1L;

    private static final String CUSTOM_CATALOG_NAME = "custom-catalog";

    private static final String CUSTOM_BASE_PARCEL_URL = "https://myarchive.test.com";

    private static final String CUSTOM_IMAGE_ID = "customImageId";

    private static final String ACCOUNT_ID = "1111";

    private static final String USER_CRN = "crn:altus:iam:us-west-1:1111:user:1111";

    private static final ImageCatalogPlatform IMAGE_CATALOG_PLATFORM = imageCatalogPlatform("aws");

    private static final String CB_VERSION = "unspecified";

    @Mock
    private ImageCatalogProvider imageCatalogProvider;

    @Spy
    private ImageCatalogVersionFilter versionFilter;

    @Mock
    private UserProfileService userProfileService;

    @Mock
    private ImageCatalogRepository imageCatalogRepository;

    @Mock
    private PlatformStringTransformer platformStringTransformer;

    @Mock
    private ProviderPreferencesService preferencesService;

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
    private RegionAwareCrnGenerator regionAwareCrnGenerator;

    @Mock
    private ProviderSpecificImageFilter providerSpecificImageFilter;

    @Mock
    private ImageOsService imageOsService;

    @BeforeEach
    public void beforeTest() throws Exception {
        setupImageCatalogProvider(DEFAULT_CATALOG_URL, V2_CB_CATALOG_FILE);

        lenient().when(providerSpecificImageFilter.filterImages(any(), anyList())).then(returnsSecondArg());
        lenient().when(preferencesService.enabledPlatforms()).thenReturn(new HashSet<>(Arrays.asList("AZURE", "AWS", "GCP")));
        lenient().when(user.getUserCrn()).thenReturn(TestConstants.CRN);
        lenient().when(userService.getOrCreate(any())).thenReturn(user);
        lenient().when(entitlementService.baseImageEnabled(anyString())).thenReturn(true);
        lenient().when(imageOsService.isSupported(any())).thenReturn(true);

        constants.add(new AwsCloudConstant());

        ReflectionTestUtils.setField(underTest, ImageCatalogService.class, "defaultCatalogUrl", DEFAULT_CATALOG_URL, null);
        ReflectionTestUtils.setField(underTest, ImageCatalogService.class, "defaultFreeIpaCatalogUrl", DEFAULT_FREEIPA_CATALOG_URL, null);
        setMockedCbVersion("cbVersion", CB_VERSION);

        ReflectionTestUtils.setField(underTest, "imageCatalogServiceProxy", imageCatalogServiceProxy);

        ReflectionTestUtils.setField(imageCatalogServiceProxy, "advertisedImageCatalogService", advertisedImageCatalogService);
        ReflectionTestUtils.setField(imageCatalogServiceProxy, "versionBasedImageCatalogService", versionBasedImageCatalogService);

        ReflectionTestUtils.setField(versionBasedImageCatalogService, "versionBasedImageProvider", versionBasedImageProvider);

        lenient().when(imageOsService.getDefaultOs()).thenReturn("centos7");
        ImageComparator comparator = new ImageComparator();
        ReflectionTestUtils.setField(comparator, "imageOsService", imageOsService);
        ReflectionTestUtils.setField(underTest, "imageComparator", comparator);

        CrnTestUtil.mockCrnGenerator(regionAwareCrnGenerator);
    }

    private void setMockedCbVersion(String cbVersion, String versionValue) {
        ReflectionTestUtils.setField(underTest, ImageCatalogService.class, cbVersion, versionValue, String.class);
    }

    @Test
    public void testGetLatestBaseImageDefaultPreferredWithNoDefaultsLatest() throws Exception {
        setupUserProfileService();
        setupImageCatalogProvider(DEFAULT_CATALOG_URL, V2_CB_CATALOG_FILE);

        ImageFilter imageFilter = ImageFilter.builder()
                .withImageCatalog(imageCatalog)
                .withPlatforms(Set.of(imageCatalogPlatform("AWS")))
                .withBaseImageEnabled(true)
                .build();
        StatedImage image = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            try {
                return underTest.getLatestImageDefaultPreferred(imageFilter, true);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        assertEquals("7aca1fa6-980c-44e2-a75e-3144b18a5993", image.getImage().getUuid());
        assertFalse(image.getImage().isDefaultImage());
    }

    @Test
    public void shouldGetStatedImagesFromAdvertisedImageProvider() throws Exception {
        setupUserProfileService();
        setupImageCatalogProviderWithoutVersions(DEFAULT_CATALOG_URL, V2_CB_CATALOG_FILE);
        when(advertisedImageProvider.getImages(any(), any())).thenReturn(
                StatedImages.statedImages(
                        new Images(Collections.singletonList(ImageTestUtil.getImage(false, "uuid", "stack", null)),
                                null, null, null), null, null));

        ImageFilter imageFilter = ImageFilter.builder()
                .withImageCatalog(imageCatalog)
                .withPlatforms(Set.of(imageCatalogPlatform("AWS")))
                .withBaseImageEnabled(true)
                .build();
        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            try {
                return underTest.getLatestImageDefaultPreferred(imageFilter, true);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

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

        ImageFilter imageFilter = ImageFilter.builder()
                .withImageCatalog(imageCatalog)
                .withPlatforms(Set.of(imageCatalogPlatform("AWS")))
                .withBaseImageEnabled(true)
                .build();
        StatedImage image = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            try {
                return underTest.getLatestImageDefaultPreferred(imageFilter, true);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        assertEquals("7aca1fa6-980c-44e2-a75e-3144b18a5993", image.getImage().getUuid());
        assertTrue(image.getImage().isDefaultImage());
    }

    @Test
    public void testGetLatestBaseImageDefaultPreferredWithMultipleDefaults() throws Exception {
        setupUserProfileService();
        setupImageCatalogProvider(DEFAULT_CATALOG_URL, V2_CB_CATALOG_FILE);
        ReflectionTestUtils.setField(underTest, ImageCatalogService.class, "cbVersion", "2.1.0-dev.1", null);
        setupLatestDefaultImageUuidProvider("7aca1fa6-980c-44e2-a75e-3144b18a5993");

        ImageFilter imageFilter = ImageFilter.builder()
                .withImageCatalog(imageCatalog)
                .withPlatforms(Set.of(imageCatalogPlatform("AWS")))
                .withBaseImageEnabled(true)
                .build();
        StatedImage image = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            try {
                return underTest.getLatestImageDefaultPreferred(imageFilter, true);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        assertEquals("7aca1fa6-980c-44e2-a75e-3144b18a5993", image.getImage().getUuid());
        assertTrue(image.getImage().isDefaultImage());
    }

    @Test
    public void testGetLatestBaseImageDefaultPreferredWenNotLatestSelected() throws Exception {
        setupUserProfileService();
        setupImageCatalogProvider(DEFAULT_CATALOG_URL, V2_CB_CATALOG_FILE);
        ReflectionTestUtils.setField(underTest, ImageCatalogService.class, "cbVersion", "2.1.0-dev.2", null);
        setupLatestDefaultImageUuidProvider("f6e778fc-7f17-4535-9021-515351df3691");

        ImageFilter imageFilter = ImageFilter.builder()
                .withImageCatalog(imageCatalog)
                .withPlatforms(Set.of(imageCatalogPlatform("AWS")))
                .withBaseImageEnabled(true)
                .build();
        StatedImage image = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            try {
                return underTest.getLatestImageDefaultPreferred(imageFilter, true);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        assertEquals("f6e778fc-7f17-4535-9021-515351df3691", image.getImage().getUuid());
        assertTrue(image.getImage().isDefaultImage());
    }

    @Test
    public void testGetStatedImagesFilteredByOperatingSystems() throws CloudbreakImageCatalogException, IOException {
        setupUserProfileService();
        setupImageCatalogProvider(DEFAULT_CATALOG_URL, DEV_CATALOG_FILE);
        Set<String> operatingSystems = new HashSet<>(Arrays.asList("redhat7", "redhat6", "amazonlinux2"));

        ImageFilter imageFilter = ImageFilter.builder()
                .withImageCatalog(imageCatalog)
                .withPlatforms(Set.of(imageCatalogPlatform("AWS")))
                .withOperatingSystems(operatingSystems)
                .withCbVersion(CB_VERSION)
                .build();
        StatedImages images = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            try {
                return underTest.getImages(imageFilter);
            } catch (CloudbreakImageCatalogException e) {
                throw new RuntimeException(e);
            }
        });

        boolean allMatch = images.getImages().getBaseImages().stream().allMatch(image -> operatingSystems.contains(image.getOsType()));
        assertTrue(allMatch, "All images should be based on supported OS");
    }

    @Test
    public void testGetImagesWhenExactVersionExistsInCatalog() throws Exception {
        String cbVersion = "1.16.2";
        ImageCatalog imageCatalog = getImageCatalog();

        ImageFilter imageFilter = ImageFilter.builder()
                .withImageCatalog(imageCatalog)
                .withPlatforms(Set.of(imageCatalogPlatform("azure")))
                .withCbVersion(cbVersion)
                .build();
        StatedImages images = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            try {
                return underTest.getImages(imageFilter);
            } catch (CloudbreakImageCatalogException e) {
                throw new RuntimeException(e);
            }
        });

        boolean exactImageIdMatch = images.getImages().getCdhImages().stream()
                .anyMatch(img -> "666aa8bf-bc1a-4cc6-43f1-427b4432c8c2".equals(img.getUuid()));
        assertTrue(exactImageIdMatch, "Result doesn't contain the required image with id.");
    }

    @Test
    public void testGetImagesWhenExactVersionExistsInCatalogAndMorePlatformRequested() throws Exception {
        String cbVersion = "2.0.0";
        ImageCatalog imageCatalog = getImageCatalog();
        ImageFilter imageFilter = ImageFilter.builder()
                .withImageCatalog(imageCatalog)
                .withPlatforms(ImmutableSet.of(IMAGE_CATALOG_PLATFORM, imageCatalogPlatform("azure")))
                .withCbVersion(cbVersion)
                .withOperatingSystems(ImmutableSet.of("amazonlinux"))
                .build();
        StatedImages images = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            try {
                return underTest.getImages(imageFilter);
            } catch (CloudbreakImageCatalogException e) {
                throw new RuntimeException(e);
            }
        });
        for (Image image : images.getImages().getBaseImages()) {
            boolean containsAws = image.getImageSetsByProvider().entrySet().stream().anyMatch(platformImages -> "aws".equals(platformImages.getKey()));
            boolean containsAzure = image.getImageSetsByProvider().entrySet().stream().anyMatch(platformImages -> "azure_rm".equals(platformImages.getKey()));
            assertTrue(containsAws || containsAzure, "Result doesn't contain the required image with id.");
        }
    }

    @Test
    public void testGetImagesWhenLatestVersionDoesntExistInCatalogShouldReturnWithReleasedVersionIfExists() throws Exception {
        setupImageCatalogProvider(DEFAULT_CATALOG_URL, PROD_CATALOG_FILE);
        ImageCatalog imageCatalog = getImageCatalog();

        ImageFilter imageFilter = ImageFilter.builder()
                .withImageCatalog(imageCatalog)
                .withPlatforms(Collections.singleton(IMAGE_CATALOG_PLATFORM))
                .withCbVersion("2.6.0")
                .withOperatingSystems(ImmutableSet.of("amazonlinux"))
                .withBaseImageEnabled(true)
                .build();
        StatedImages images = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            try {
                return underTest.getImages(imageFilter);
            } catch (CloudbreakImageCatalogException e) {
                throw new RuntimeException(e);
            }
        });

        boolean match = images.getImages().getBaseImages().stream()
                .anyMatch(img -> "0f575e42-9d90-4f85-5f8a-bdced2221dc3".equals(img.getUuid()));
        assertTrue(match, "Result doesn't contain the required base image with id.");
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

        ImageFilter imageFilter = ImageFilter.builder()
                .withImageCatalog(imageCatalog)
                .withPlatforms(Collections.singleton(IMAGE_CATALOG_PLATFORM))
                .withCbVersion("2.6.0-dev.132")
                .withOperatingSystems(ImmutableSet.of("amazonlinux", "centos7"))
                .withBaseImageEnabled(true)
                .build();
        StatedImages images = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            try {
                return underTest.getImages(imageFilter);
            } catch (CloudbreakImageCatalogException e) {
                throw new RuntimeException(e);
            }
        });

        boolean match = images.getImages().getBaseImages().stream()
                .anyMatch(img -> "cab28152-f5e1-43e1-5107-9e7bbed33eef".equals(img.getUuid()));
        assertTrue(match, "Result doesn't contain the required base image with id.");
    }

    @Test
    public void testGetImagesWhenLatestRcVersionDoesntExistInCatalogShouldReturnWithReleasedVersionIfExists() throws Exception {
        setupImageCatalogProvider(DEFAULT_CATALOG_URL, RC_CATALOG_FILE);
        ImageCatalog imageCatalog = getImageCatalog();

        ImageFilter imageFilter = ImageFilter.builder()
                .withImageCatalog(imageCatalog)
                .withPlatforms(Collections.singleton(IMAGE_CATALOG_PLATFORM))
                .withCbVersion("2.6.0-rc.13")
                .withOperatingSystems(ImmutableSet.of("amazonlinux", "centos7"))
                .withBaseImageEnabled(true)
                .build();
        StatedImages images = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            try {
                return underTest.getImages(imageFilter);
            } catch (CloudbreakImageCatalogException e) {
                throw new RuntimeException(e);
            }
        });

        boolean match = images.getImages().getBaseImages().stream()
                .anyMatch(img -> "0f575e42-9d90-4f85-5f8a-bdced2221dc3".equals(img.getUuid()));
        assertTrue(match, "Result doesn't contain the required base image with id.");
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

        ImageFilter imageFilter = ImageFilter.builder()
                .withImageCatalog(imageCatalog)
                .withPlatforms(Collections.singleton(IMAGE_CATALOG_PLATFORM))
                .withCbVersion("2.6.0-rc.13")
                .withOperatingSystems(ImmutableSet.of("amazonlinux", "centos7"))
                .withBaseImageEnabled(true)
                .build();
        StatedImages images = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            try {
                return underTest.getImages(imageFilter);
            } catch (CloudbreakImageCatalogException e) {
                throw new RuntimeException(e);
            }
        });

        boolean match = images.getImages().getBaseImages().stream()
                .anyMatch(img -> img.getUuid().equals("0f575e42-9d90-4f85-5f8a-bdced2221dc3"));
        assertTrue(match, "Result doesn't contain the required base image with id.");
    }

    @Test
    public void testGetImagesWhenSimilarDevVersionDoesntExistInCatalogShouldReturnWithReleasedVersionIfExists() throws Exception {
        ImageCatalog imageCatalog = getImageCatalog();

        Set<String> vMImageUUIDs = Set.of("666aa8bf-bc1a-4cc6-43f1-427b4432c8c2");
        Set<String> defaultVMImageUUIDs = Set.of("666aa8bf-bc1a-4cc6-43f1-427b4432c8c2");
        Set<String> supportedVersions = Set.of("2.1.0-dev.2");
        PrefixMatchImages prefixMatchImages = new PrefixMatchImages(vMImageUUIDs, defaultVMImageUUIDs, supportedVersions);
        when(prefixMatcherService.prefixMatchForCBVersion(any(), any())).thenReturn(prefixMatchImages);

        ImageFilter imageFilter = ImageFilter.builder()
                .withImageCatalog(imageCatalog)
                .withPlatforms(Collections.singleton(imageCatalogPlatform("azure")))
                .withCbVersion("1.16.2-dev.132")
                .withOperatingSystems(ImmutableSet.of("amazonlinux", "centos7"))
                .build();
        StatedImages images = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            try {
                return underTest.getImages(imageFilter);
            } catch (CloudbreakImageCatalogException e) {
                throw new RuntimeException(e);
            }
        });

        boolean match = images.getImages().getCdhImages().stream()
                .anyMatch(img -> "666aa8bf-bc1a-4cc6-43f1-427b4432c8c2".equals(img.getUuid()));
        assertTrue(match, "Result doesn't contain the required image with id.");
    }

    @Test
    public void testGetImagesWhenSimilarRcVersionDoesntExistInCatalogShouldReturnWithReleasedVersionIfExists() throws Exception {
        ImageCatalog imageCatalog = getImageCatalog();

        Set<String> vMImageUUIDs = Set.of("666aa8bf-bc1a-4cc6-43f1-427b4432c8c2");
        Set<String> defaultVMImageUUIDs = Set.of("666aa8bf-bc1a-4cc6-43f1-427b4432c8c2");
        Set<String> supportedVersions = Set.of("2.1.0-dev.1", "2.0.0", "2.1.0-dev.100", "2.1.0-dev.2");
        PrefixMatchImages prefixMatchImages = new PrefixMatchImages(vMImageUUIDs, defaultVMImageUUIDs, supportedVersions);

        when(prefixMatcherService.prefixMatchForCBVersion(eq("1.16.2-rc.13"), any())).thenReturn(prefixMatchImages);

        ImageFilter imageFilter = ImageFilter.builder()
                .withImageCatalog(imageCatalog)
                .withPlatforms(Collections.singleton(imageCatalogPlatform("azure")))
                .withCbVersion("1.16.2-rc.13")
                .build();
        StatedImages images = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            try {
                return underTest.getImages(imageFilter);
            } catch (CloudbreakImageCatalogException e) {
                throw new RuntimeException(e);
            }
        });

        boolean match = images.getImages().getCdhImages().stream()
                .anyMatch(img -> "666aa8bf-bc1a-4cc6-43f1-427b4432c8c2".equals(img.getUuid()));
        assertTrue(match, "Result doesn't contain the required image with id.");
    }

    @Test
    public void testGetImagesWhenSimilarDevVersionExistsInCatalog() throws Exception {
        ImageCatalog imageCatalog = getImageCatalog();
        Set<String> vMImageUUIDs = Set.of("f6e778fc-7f17-4535-9021-515351df3691");
        Set<String> defaultVMImageUUIDs = Set.of("f6e778fc-7f17-4535-9021-515351df3691", "7aca1fa6-980c-44e2-a75e-3144b18a5993");
        Set<String> supportedVersions = Set.of("2.1.0-dev.1", "2.0.0", "2.1.0-dev.100", "2.1.0-dev.2");
        PrefixMatchImages prefixMatchImages = new PrefixMatchImages(vMImageUUIDs, defaultVMImageUUIDs, supportedVersions);

        when(prefixMatcherService.prefixMatchForCBVersion(eq("2.1.0-dev.4000"), any())).thenReturn(prefixMatchImages);

        ImageFilter imageFilter = ImageFilter.builder()
                .withImageCatalog(imageCatalog)
                .withPlatforms(Collections.singleton(IMAGE_CATALOG_PLATFORM))
                .withCbVersion("2.1.0-dev.4000")
                .withBaseImageEnabled(true)
                .build();
        StatedImages images = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            try {
                return underTest.getImages(imageFilter);
            } catch (CloudbreakImageCatalogException e) {
                throw new RuntimeException(e);
            }
        });

        boolean baseImgMatch = images.getImages().getBaseImages().stream()
                .anyMatch(img -> "f6e778fc-7f17-4535-9021-515351df3691".equals(img.getUuid()));
        assertTrue(baseImgMatch, "Result doesn't contain the required image with id.");
    }

    @Test
    public void testGetImagesWhenSimilarRcVersionExistsInCatalog() throws Exception {
        ImageCatalog imageCatalog = getImageCatalog();

        Set<String> vMImageUUIDs = Set.of("666aa8bf-bc1a-4cc6-43f1-427b4432c8c2");
        Set<String> defaultVMImageUUIDs = Set.of("666aa8bf-bc1a-4cc6-43f1-427b4432c8c2");
        Set<String> supportedVersions = Set.of("2.1.0-dev.1", "2.0.0", "2.1.0-dev.100", "2.1.0-dev.2");
        PrefixMatchImages prefixMatchImages = new PrefixMatchImages(vMImageUUIDs, defaultVMImageUUIDs, supportedVersions);
        when(prefixMatcherService.prefixMatchForCBVersion(eq("2.0.0-rc.4"), any())).thenReturn(prefixMatchImages);

        ImageFilter imageFilter = ImageFilter.builder()
                .withImageCatalog(imageCatalog)
                .withPlatforms(Collections.singleton(imageCatalogPlatform("azure")))
                .withCbVersion("2.0.0-rc.4")
                .build();
        StatedImages images = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            try {
                return underTest.getImages(imageFilter);
            } catch (CloudbreakImageCatalogException e) {
                throw new RuntimeException(e);
            }
        });

        boolean match = images.getImages().getCdhImages().stream()
                .anyMatch(img -> "666aa8bf-bc1a-4cc6-43f1-427b4432c8c2".equals(img.getUuid()));
        assertTrue(match, "Result doesn't contain the required image with id.");
    }

    @Test
    public void testGetImagesWhenExactVersionExistsInCatalogForPlatform() throws Exception {
        ImageCatalog imageCatalog = getImageCatalog();

        ImageFilter imageFilter = ImageFilter.builder()
                .withImageCatalog(imageCatalog)
                .withPlatforms(Collections.singleton(imageCatalogPlatform("azure")))
                .withCbVersion("1.16.2")
                .build();
        StatedImages images = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            try {
                return underTest.getImages(imageFilter);
            } catch (CloudbreakImageCatalogException e) {
                throw new RuntimeException(e);
            }
        });

        boolean exactImageIdMatch = images.getImages().getCdhImages().stream()
                .anyMatch(img -> "666aa8bf-bc1a-4cc6-43f1-427b4432c8c2".equals(img.getUuid()));
        assertTrue(exactImageIdMatch, "Result doesn't contain the required image with id for the platform.");
    }

    @Test
    public void testGetImagesWhenArchitectureProvided() throws Exception {
        setupImageCatalogProvider(DEFAULT_CATALOG_URL, V3_CB_CATALOG_FILE);

        Set<String> vMImageUUIDs = Set.of("f3071603-8ab3-4214-b78d-94131c588e83", "0898c324-c1f0-4055-98e1-ddc565473878");
        Set<String> defaultVMImageUUIDs = Set.of("f3071603-8ab3-4214-b78d-94131c588e83", "0898c324-c1f0-4055-98e1-ddc565473878");
        Set<String> supportedVersions = Set.of("2.41.0-dev.1");
        PrefixMatchImages prefixMatchImages = new PrefixMatchImages(vMImageUUIDs, defaultVMImageUUIDs, supportedVersions);
        when(prefixMatcherService.prefixMatchForCBVersion(eq("2.41.0"), any())).thenReturn(prefixMatchImages);

        ImageFilter imageFilter = ImageFilter.builder()
                .withImageCatalog(imageCatalog)
                .withPlatforms(Collections.singleton(imageCatalogPlatform("aws")))
                .withArchitecture(Architecture.ARM64)
                .withCbVersion("2.41.0")
                .build();
        StatedImages images = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            try {
                return underTest.getImages(imageFilter);
            } catch (CloudbreakImageCatalogException e) {
                throw new RuntimeException(e);
            }
        });

        assertEquals(1, images.getImages().getCdhImages().size());
        boolean exactImageIdMatch = images.getImages().getCdhImages().stream()
                .anyMatch(img -> "f3071603-8ab3-4214-b78d-94131c588e83".equals(img.getUuid()));
        assertTrue(exactImageIdMatch, "Result doesn't contain the only required image with id for the architecture.");
    }

    @Test
    public void testGetImagesWhenArchitectureNotProvided() throws Exception {
        setupImageCatalogProvider(DEFAULT_CATALOG_URL, V3_CB_CATALOG_FILE);

        Set<String> vMImageUUIDs = Set.of("f3071603-8ab3-4214-b78d-94131c588e83", "0898c324-c1f0-4055-98e1-ddc565473878");
        Set<String> defaultVMImageUUIDs = Set.of("f3071603-8ab3-4214-b78d-94131c588e83", "0898c324-c1f0-4055-98e1-ddc565473878");
        Set<String> supportedVersions = Set.of("2.41.0-dev.1");
        PrefixMatchImages prefixMatchImages = new PrefixMatchImages(vMImageUUIDs, defaultVMImageUUIDs, supportedVersions);
        when(prefixMatcherService.prefixMatchForCBVersion(eq("2.41.0"), any())).thenReturn(prefixMatchImages);

        ImageFilter imageFilter = ImageFilter.builder()
                .withImageCatalog(imageCatalog)
                .withPlatforms(Collections.singleton(imageCatalogPlatform("aws")))
                .withCbVersion("2.41.0")
                .build();
        StatedImages images = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            try {
                return underTest.getImages(imageFilter);
            } catch (CloudbreakImageCatalogException e) {
                throw new RuntimeException(e);
            }
        });

        List<String> imageUuids = images.getImages().getCdhImages().stream().map(Image::getUuid).toList();
        assertTrue(imageUuids.contains("f3071603-8ab3-4214-b78d-94131c588e83"), "Result must contain the only image with arm64 architecture.");
        assertTrue(imageUuids.contains("0898c324-c1f0-4055-98e1-ddc565473878"), "Result must contain the only image with x86_64 architecture.");
    }

    @Test
    public void testGetImagesWhenExactVersionDoesnotExistInCatalogForPlatform() throws Exception {
        ImageCatalog imageCatalog = getImageCatalog();

        ImageFilter imageFilter = ImageFilter.builder()
                .withImageCatalog(imageCatalog)
                .withPlatforms(Collections.singleton(imageCatalogPlatform("owncloud")))
                .withCbVersion("1.16.4")
                .build();
        assertThatThrownBy(() -> underTest.getImages(imageFilter))
                .isInstanceOf(CloudbreakImageCatalogException.class)
                .hasMessage("Platform(s) owncloud are not supported by the current catalog");
    }

    @Test
    void testGetImagesWhenDefaultOnlyTrue() throws Exception {
        setupImageCatalogProvider(DEFAULT_CATALOG_URL, V3_CB_CATALOG_FILE);
        setupLatestDefaultImageUuidProvider("3cba3cd0-a169-4d62-8bc5-709df5f73b50");

        ImageFilter imageFilter = ImageFilter.builder()
                .withImageCatalog(imageCatalog)
                .withPlatforms(Collections.singleton(imageCatalogPlatform("azure")))
                .withCbVersion("2.41.0-b115")
                .withDefaultOnly(true)
                .build();
        StatedImages images = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            try {
                return underTest.getImages(imageFilter);
            } catch (CloudbreakImageCatalogException e) {
                throw new RuntimeException(e);
            }
        });

        assertEquals(1, images.getImages().getCdhImages().size());
        boolean exactImageIdMatch = images.getImages().getCdhImages().stream()
                .anyMatch(img -> "3cba3cd0-a169-4d62-8bc5-709df5f73b50".equals(img.getUuid()));
        assertTrue(exactImageIdMatch, "Result doesn't contain the required image with id for the platform.");
    }

    @Test
    void testGetImagesWhenDefaultOnlyFalse() throws Exception {
        setupImageCatalogProvider(DEFAULT_CATALOG_URL, V3_CB_CATALOG_FILE);
        setupLatestDefaultImageUuidProvider("3cba3cd0-a169-4d62-8bc5-709df5f73b50");

        ImageFilter imageFilter = ImageFilter.builder()
                .withImageCatalog(imageCatalog)
                .withPlatforms(Collections.singleton(imageCatalogPlatform("azure")))
                .withCbVersion("2.41.0-b115")
                .withDefaultOnly(false)
                .build();
        StatedImages images = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            try {
                return underTest.getImages(imageFilter);
            } catch (CloudbreakImageCatalogException e) {
                throw new RuntimeException(e);
            }
        });

        assertEquals(3, images.getImages().getCdhImages().size());
    }

    @Test
    public void testGetImagesWhenCustomImageCatalogExists() throws Exception {
        ImageCatalog ret = new ImageCatalog();
        ret.setImageCatalogUrl(null);
        ret.setName(CUSTOM_CATALOG_NAME);
        when(imageCatalogRepository.findByNameAndWorkspaceId("name", WORKSPACE_ID)).thenReturn(Optional.of(ret));

        StatedImages actual = underTest.getImages(WORKSPACE_ID, "name", null, IMAGE_CATALOG_PLATFORM, false, null);
        assertEquals(CUSTOM_CATALOG_NAME, actual.getImageCatalogName());
        assertNull(actual.getImageCatalogUrl());
    }

    @Test
    public void testGetImagesWhenCustomImageCatalogDoesNotExists() throws Exception {
        when(imageCatalogRepository.findByNameAndWorkspaceId(anyString(), anyLong())).thenThrow(new NotFoundException("no no"));

        assertThatThrownBy(() -> underTest.getImages(WORKSPACE_ID, "verycool", null, IMAGE_CATALOG_PLATFORM, false, null))
                .isInstanceOf(CloudbreakImageCatalogException.class)
                .hasMessage("The verycool catalog does not exist or does not belongs to your account.");

        verify(entitlementService, never()).baseImageEnabled(any());
        verify(imageCatalogProvider, times(0)).getImageCatalogV3("");
    }

    @Test
    public void testDeleteImageCatalog() {
        String name = "img-name";
        ImageCatalog imageCatalog = new ImageCatalog();
        imageCatalog.setName(name);
        imageCatalog.setArchived(false);
        doNothing().when(userProfileHandler).destroyProfileImageCatalogPreparation(any(ImageCatalog.class));
        when(imageCatalogRepository.findByNameAndWorkspaceId(name, WORKSPACE_ID)).thenReturn(Optional.of(imageCatalog));
        setupUserProfileService();

        underTest.delete(WORKSPACE_ID, name);

        verify(imageCatalogRepository, times(1)).save(imageCatalog);

        assertTrue(imageCatalog.isArchived());
        assertTrue(imageCatalog.getName().startsWith(name) && imageCatalog.getName().indexOf('_') == name.length());
    }

    @Test
    public void testDeleteImageCatalogWhenEnvDefault() {
        String name = "cdp-default";

        assertThatThrownBy(() -> underTest.delete(WORKSPACE_ID, name))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("cdp-default cannot be deleted because it is an environment default image catalog.");
    }

    @Test
    public void testGet() {
        String name = "img-name";
        ImageCatalog imageCatalog = new ImageCatalog();
        when(imageCatalogRepository.findByNameAndWorkspaceId(name, WORKSPACE_ID)).thenReturn(Optional.of(imageCatalog));
        ImageCatalog actual = underTest.getImageCatalogByName(WORKSPACE_ID, name);

        assertEquals(actual, imageCatalog);
    }

    @Test
    public void testGetWhenEnvDefault() {
        String name = "cdp-default";
        ImageCatalog actual = ThreadBasedUserCrnProvider.doAs(CrnTestUtil.getUserCrnBuilder()
                .setAccountId("ACCOUNT_ID")
                .setResource("USER")
                .build().toString(), () -> underTest.getImageCatalogByName(WORKSPACE_ID, name));

        verify(imageCatalogRepository, times(0)).findByNameAndWorkspace(eq(name), any(Workspace.class));

        assertEquals(actual.getName(), name);
        assertNull(actual.getId());
    }

    @Test
    public void testGetImagesFromDefaultWithEmptyInput() throws CloudbreakImageCatalogException {
        setupUserProfileService();

        assertThatThrownBy(() -> underTest.getImagesFromDefault(WORKSPACE_ID, null, null, emptySet(), null, false, false, null))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Either platform or stackName should be filled in request.");

        verify(entitlementService, never()).baseImageEnabled(any());
    }

    @Test
    public void testGetImagesFromDefaultGivenBothInput() throws CloudbreakImageCatalogException {
        assertThatThrownBy(() -> underTest.getImagesFromDefault(WORKSPACE_ID, "stack", imageCatalogPlatform("AWS"), emptySet(), null, false, false,
                null))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Platform or stackName cannot be filled in the same request.");

        verify(entitlementService, never()).baseImageEnabled(any());
    }

    @Test
    public void testGetImagesFromDefaultWithStackName() throws CloudbreakImageCatalogException {
        when(stackImageFilterService.getApplicableImages(anyLong(), anyString(), anyBoolean())).thenReturn(new Images(Lists.newArrayList(),
                Lists.newArrayList(), Lists.newArrayList(), Sets.newHashSet()));

        underTest.getImagesFromDefault(WORKSPACE_ID, "stack", null, emptySet(), null, false, false, null);

        verify(stackImageFilterService, never()).getApplicableImages(anyLong(), anyString(), anyString(), anyBoolean());
        verify(stackImageFilterService, times(1)).getApplicableImages(anyLong(), anyString(), anyBoolean());
    }

    @Test
    public void testGetImagesFromDefaultWithPlatform() throws CloudbreakImageCatalogException, IOException {
        setupUserProfileService();
        setupImageCatalogProvider(DEFAULT_CATALOG_URL, V2_CB_CATALOG_FILE);
        ImageCatalogPlatform imageCatalogPlatform = imageCatalogPlatform("AWS");
        when(platformStringTransformer.getPlatformStringForImageCatalog(any(ImageCatalogPlatform.class), anyBoolean())).thenReturn(imageCatalogPlatform);
        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            try {
                return underTest.getImagesFromDefault(WORKSPACE_ID, null, imageCatalogPlatform, emptySet(), null, false, false, null);
            } catch (CloudbreakImageCatalogException e) {
                throw new RuntimeException(e);
            }
        });

        verify(entitlementService, times(1))
                .baseImageEnabled(Objects.requireNonNull(Crn.fromString(user.getUserCrn())).getAccountId());
        verify(entitlementService, never()).baseImageEnabled(user.getUserCrn());
        verify(stackImageFilterService, never()).getApplicableImages(anyLong(), anyString(), anyString(), anyBoolean());
        verify(stackImageFilterService, never()).getApplicableImages(anyLong(), anyString(), anyBoolean());
    }

    @Test
    public void testGetImagesWithEmptyInput() throws CloudbreakImageCatalogException {
        assertThatThrownBy(() -> underTest.getImagesByCatalogName(WORKSPACE_ID, "catalog", null, null, null, false, false, null))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Either platform or stack name must be present in the request.");

        verify(entitlementService, never()).baseImageEnabled(any());
    }

    @Test
    public void testGetImagesGivenBothInput() throws CloudbreakImageCatalogException {
        assertThatThrownBy(() -> underTest.getImagesByCatalogName(WORKSPACE_ID, "catalog", "stack", imageCatalogPlatform("AWS"), null, false, false, null))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Both platform and existing stack name could not be present in the request.");

        verify(entitlementService, never()).baseImageEnabled(any());
    }

    @Test
    public void testGetImagesWithStackName() throws CloudbreakImageCatalogException {
        when(stackImageFilterService.getApplicableImages(anyLong(), anyString(), anyString(), anyBoolean())).thenReturn(new Images(Lists.newArrayList(),
                Lists.newArrayList(), Lists.newArrayList(), Sets.newHashSet()));

        underTest.getImagesByCatalogName(WORKSPACE_ID, "catalog", "stack", null, null, false, false, null);

        verify(stackImageFilterService, times(1)).getApplicableImages(anyLong(), anyString(), anyString(), anyBoolean());
        verify(stackImageFilterService, never()).getApplicableImages(anyLong(), anyString(), anyBoolean());
    }

    @Test
    public void testGetImagesWithPlatform() throws CloudbreakImageCatalogException, IOException {
        setupUserProfileService();
        setupImageCatalogProvider(DEFAULT_CATALOG_URL, V2_CB_CATALOG_FILE);
        ImageCatalog imageCatalog = new ImageCatalog();
        imageCatalog.setImageCatalogUrl(DEFAULT_CATALOG_URL);
        ImageCatalogPlatform imageCatalogPlatform = imageCatalogPlatform("AWS");
        when(platformStringTransformer.getPlatformStringForImageCatalog(any(String.class), anyBoolean())).thenReturn(imageCatalogPlatform);
        when(imageCatalogRepository.findByNameAndWorkspaceId(anyString(), anyLong())).thenReturn(Optional.of(imageCatalog));

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            try {
                return underTest.getImagesByCatalogName(WORKSPACE_ID, "catalog", null, imageCatalogPlatform, null, false, false, null);
            } catch (CloudbreakImageCatalogException e) {
                throw new RuntimeException(e);
            }
        });

        verify(entitlementService, times(1))
                .baseImageEnabled(Objects.requireNonNull(Crn.fromString(user.getUserCrn())).getAccountId());
        verify(entitlementService, never()).baseImageEnabled(user.getUserCrn());
        verify(stackImageFilterService, never()).getApplicableImages(anyLong(), anyString(), anyString(), anyBoolean());
        verify(stackImageFilterService, never()).getApplicableImages(anyLong(), anyString(), anyBoolean());
    }

    @Test
    public void testGetImagesWithArchitectureWhenArmIsSupported() throws CloudbreakImageCatalogException, IOException {
        setupUserProfileService();
        setupImageCatalogProvider(DEFAULT_CATALOG_URL, V2_CB_CATALOG_FILE);
        ImageCatalog imageCatalog = new ImageCatalog();
        imageCatalog.setImageCatalogUrl(DEFAULT_CATALOG_URL);
        ImageCatalogPlatform imageCatalogPlatform = imageCatalogPlatform("AWS");
        when(platformStringTransformer.getPlatformStringForImageCatalog(any(String.class), anyBoolean())).thenReturn(imageCatalogPlatform);
        when(imageCatalogRepository.findByNameAndWorkspaceId(anyString(), anyLong())).thenReturn(Optional.of(imageCatalog));

        Images images = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            try {
                return underTest.getImagesByCatalogName(WORKSPACE_ID, "catalog", null, imageCatalogPlatform, null, false, false, "arm64");
            } catch (CloudbreakImageCatalogException e) {
                throw new RuntimeException(e);
            }
        });

        assertThat(images.getCdhImages(), hasSize(1));
        assertEquals("996aa8bf-bc1a-4cc6-43f1-427b4432c8c2", images.getCdhImages().getFirst().getUuid());
    }

    @Test
    public void testGetImagesWithArchitectureWhenX86IsRequested() throws CloudbreakImageCatalogException, IOException {
        setupUserProfileService();
        setupImageCatalogProvider(DEFAULT_CATALOG_URL, V2_CB_CATALOG_FILE);
        ImageCatalog imageCatalog = new ImageCatalog();
        imageCatalog.setImageCatalogUrl(DEFAULT_CATALOG_URL);
        ImageCatalogPlatform imageCatalogPlatform = imageCatalogPlatform("AWS");
        when(platformStringTransformer.getPlatformStringForImageCatalog(any(String.class), anyBoolean())).thenReturn(imageCatalogPlatform);
        when(imageCatalogRepository.findByNameAndWorkspaceId(anyString(), anyLong())).thenReturn(Optional.of(imageCatalog));

        Images images = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            try {
                return underTest.getImagesByCatalogName(WORKSPACE_ID, "catalog", null, imageCatalogPlatform, null, false, false, "x86_64");
            } catch (CloudbreakImageCatalogException e) {
                throw new RuntimeException(e);
            }
        });

        assertThat(images.getCdhImages(), hasSize(1));
        assertEquals("886aa8bf-bc1a-4cc6-43f1-427b4432c8c2", images.getCdhImages().getFirst().getUuid());
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

        ImageCatalog result = underTest.getImageCatalogByName(NameOrCrn.ofName(catalog.getName()), catalog.getWorkspace().getId());

        assertEquals(catalog, result);
        verify(imageCatalogRepository, times(1)).findByNameAndWorkspaceId(anyString(), anyLong());
        verify(imageCatalogRepository, times(1)).findByNameAndWorkspaceId(catalog.getName(), catalog.getWorkspace().getId());
    }

    @Test
    public void testGetByWorkspaceWhenDtoCrnFilledThenProperGetCalled() {
        ImageCatalog catalog = getImageCatalog();
        when(imageCatalogRepository.findByResourceCrnAndArchivedFalseAndImageCatalogUrlIsNotNull(catalog.getResourceCrn())).thenReturn(Optional.of(catalog));

        ImageCatalog result = underTest.getImageCatalogByName(NameOrCrn.ofCrn(catalog.getResourceCrn()), catalog.getWorkspace().getId());

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
        verify(ownerAssignmentService).assignResourceOwnerRoleIfEntitled(eq("creator"), matches(crnPattern));
    }

    @Test
    public void testGetImageByCatalogNameWithCustomCatalogNameAndExistingRuntimeImage()
            throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException, IOException {

        setupCustomImageCatalog(ImageType.RUNTIME, "232fe6b6-aec4-4fa9-bb02-2c295d319a36", CUSTOM_BASE_PARCEL_URL + "/");

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

        setupCustomImageCatalog(ImageType.UNKNOWN, "3b6ae396-df40-4e2b-7c2b-54b15822614c", CUSTOM_BASE_PARCEL_URL);

        assertThatThrownBy(() -> underTest.getImageByCatalogName(WORKSPACE_ID, CUSTOM_IMAGE_ID, CUSTOM_CATALOG_NAME))
                .isInstanceOf(CloudbreakImageCatalogException.class)
                .hasMessage("Image type is not supported.");
    }

    @Test
    public void testGetRuntimeImagesFromCustomImageCatalogByImageCatalogName() throws CloudbreakImageCatalogException, IOException {
        ImageCatalog imageCatalog = new ImageCatalog();
        imageCatalog.setCustomImages(Set.of(getCustomImage(ImageType.RUNTIME, "5b60b723-4beb-40b0-5cba-47ea9c9b6e53", CUSTOM_BASE_PARCEL_URL)));
        StatedImage statedImage = StatedImage.statedImage(getImage(), CUSTOM_IMAGE_CATALOG_URL, CUSTOM_CATALOG_NAME);

        setupImageCatalogProvider(DEFAULT_CATALOG_URL, DEV_CATALOG_FILE);
        when(imageCatalogRepository.findByNameAndWorkspaceId(CUSTOM_CATALOG_NAME, WORKSPACE_ID)).thenReturn(Optional.of(imageCatalog));
        when(customImageProvider.mergeSourceImageAndCustomImageProperties(any(), any(), any(), any())).thenReturn(statedImage);

        StatedImages actual = underTest.getImages(ACCOUNT_ID, WORKSPACE_ID, CUSTOM_CATALOG_NAME, Set.of(imageCatalogPlatform("AWS")), null, true, false, null);
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

        StatedImages actual = underTest.getImages(ACCOUNT_ID, WORKSPACE_ID, CUSTOM_CATALOG_NAME, Set.of(imageCatalogPlatform("AWS")), null, true, false, null);

        assertEquals(statedImage.getImage(), actual.getImages().getFreeIpaImages().stream().findFirst().get());
    }

    @Test
    public void testGetImageShouldLookupCustomImageInCaseOfNullImageCatalogUrl()
            throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException, IOException {
        ImageCatalog imageCatalog = new ImageCatalog();
        CustomImage customImage = getCustomImage(ImageType.RUNTIME, "5b60b723-4beb-40b0-5cba-47ea9c9b6e53", CUSTOM_BASE_PARCEL_URL);
        imageCatalog.setCustomImages(Set.of(customImage));
        StatedImage statedImage = StatedImage.statedImage(getImage(), CUSTOM_IMAGE_CATALOG_URL, CUSTOM_CATALOG_NAME);

        setupImageCatalogProvider(DEFAULT_CATALOG_URL, DEV_CATALOG_FILE);
        when(imageCatalogRepository.findByNameAndWorkspaceId(CUSTOM_CATALOG_NAME, WORKSPACE_ID)).thenReturn(Optional.of(imageCatalog));
        when(customImageProvider.mergeSourceImageAndCustomImageProperties(any(), any(), any(), any())).thenReturn(statedImage);

        StatedImage actual = underTest.getImage(WORKSPACE_ID, null, CUSTOM_CATALOG_NAME, CUSTOM_IMAGE_ID);

        assertEquals(statedImage.getImage(), actual.getImage());
    }

    @Test
    public void testGetCustomStatedImageShouldThrowImageNotFoundExcceptionInCaseOfMissingCustomImage() {
        ImageCatalog imageCatalog = new ImageCatalog();
        imageCatalog.setName(CUSTOM_CATALOG_NAME);
        imageCatalog.setCustomImages(emptySet());

        when(imageCatalogRepository.findByNameAndWorkspaceId(CUSTOM_CATALOG_NAME, WORKSPACE_ID)).thenReturn(Optional.of(imageCatalog));

        assertThrows(CloudbreakImageNotFoundException.class, () -> underTest.getCustomStatedImage(WORKSPACE_ID, CUSTOM_CATALOG_NAME, CUSTOM_IMAGE_ID));
    }

    @Test
    public void testFindAllByIdsWithDefaultsInCaseOfCustomCatalogsOnly() {

        Set<ImageCatalog> imageCatalogs = getImageCatalogs();
        when(imageCatalogRepository.findAllByIdNotArchived(any())).thenReturn(imageCatalogs);

        Set<ImageCatalog> actual = underTest.findAllByIdsWithDefaults(null, true);

        assertEquals(1, actual.size());
        assertTrue(actual.contains(imageCatalogs.stream().filter(catalog -> catalog.getName().equals(CUSTOM_CATALOG_NAME)).findFirst().get()));
    }

    @Test
    public void testFindAllByIdsWithDefaultsInCaseOfAllCatalogsWithoutLegacyCatalog() {

        Set<ImageCatalog> imageCatalogs = getImageCatalogs();
        when(imageCatalogRepository.findAllByIdNotArchived(any())).thenReturn(imageCatalogs);

        Set<ImageCatalog> actual = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.findAllByIdsWithDefaults(null, false));

        assertEquals(3, actual.size());
        assertTrue(actual.contains(imageCatalogs.stream().filter(catalog -> catalog.getName().equals("default")).findFirst().get()));
        assertTrue(actual.contains(imageCatalogs.stream().filter(catalog -> catalog.getName().equals(CUSTOM_CATALOG_NAME)).findFirst().get()));
    }

    @Test
    public void testFindAllByIdsWithDefaultsInCaseOfAllCatalogsWithLegacyCatalog() {

        Set<ImageCatalog> imageCatalogs = getImageCatalogs();
        when(imageCatalogRepository.findAllByIdNotArchived(any())).thenReturn(imageCatalogs);
        setMockedLegacyCatalogEnabled(true);

        Set<ImageCatalog> actual = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.findAllByIdsWithDefaults(null, false));

        assertEquals(4, actual.size());
        assertTrue(actual.contains(imageCatalogs.stream().filter(catalog -> catalog.getName().equals("default")).findFirst().get()));
        assertTrue(actual.contains(imageCatalogs.stream().filter(catalog -> catalog.getName().equals(CUSTOM_CATALOG_NAME)).findFirst().get()));
    }

    @Test
    public void testGetRuntimeVersionsFromDefault() throws CloudbreakImageCatalogException {
        List<String> expected = List.of("7.2.1");
        ImageCatalogMetaData imageCatalogMetaData = mock(ImageCatalogMetaData.class);

        when(imageCatalogProvider.getImageCatalogMetaData(DEFAULT_CATALOG_URL)).thenReturn(imageCatalogMetaData);
        when(imageCatalogMetaData.getRuntimeVersions()).thenReturn(expected);

        List<String> actual = underTest.getRuntimeVersionsFromDefault();

        assertEquals(expected, actual);
    }

    @Test
    public void testCustomImageCatalogImageFilterResult() throws CloudbreakImageCatalogException, IOException {
        ImageCatalog imageCatalog = new ImageCatalog();
        String imageId = "5b60b723-4beb-40b0-5cba-47ea9c9b6e53";
        CustomImage customImage = getCustomImage(ImageType.RUNTIME, imageId, CUSTOM_BASE_PARCEL_URL);
        Image image = getImage();
        imageCatalog.setCustomImages(Set.of(customImage));
        StatedImage statedImage = StatedImage.statedImage(image, null, CUSTOM_CATALOG_NAME);

        setupImageCatalogProvider(DEFAULT_CATALOG_URL, DEV_CATALOG_FILE);
        when(imageCatalogRepository.findByNameAndWorkspaceId(CUSTOM_CATALOG_NAME, WORKSPACE_ID)).thenReturn(Optional.of(imageCatalog));
        when(customImageProvider.mergeSourceImageAndCustomImageProperties(any(), any(), any(), any())).thenReturn(statedImage);

        ImageFilterResult actual = underTest.getImageFilterResult(WORKSPACE_ID, CUSTOM_CATALOG_NAME, IMAGE_CATALOG_PLATFORM, false, imageId);

        assertEquals(1, actual.getImages().size());
        assertEquals(image, actual.getImages().getFirst());
        assertEquals(ImageFilterResult.EMPTY_REASON, actual.getReason());
    }

    @Test
    public void testImageCatalogImageFilterResult() throws CloudbreakImageCatalogException, IOException {
        ImageCatalog imageCatalog = new ImageCatalog();
        imageCatalog.setImageCatalogUrl(DEFAULT_CATALOG_URL);

        setupImageCatalogProvider(DEFAULT_CATALOG_URL, V3_CB_CATALOG_FILE);
        when(imageCatalogRepository.findByNameAndWorkspaceId("catalog", WORKSPACE_ID)).thenReturn(Optional.of(imageCatalog));

        String currentImageId = "232fe6b6-aec4-4fa9-bb02-2c295d319a36";
        ImageFilterResult actual = underTest.getImageFilterResult(WORKSPACE_ID, "catalog", IMAGE_CATALOG_PLATFORM, false, currentImageId);

        assertEquals(5, actual.getImages().size());
        assertTrue(actual.getImages().stream().anyMatch(image -> image.getUuid().equals(currentImageId)));
    }

    @Test
    public void testImageCatalogImageFilterResultWithGetAllImages() throws CloudbreakImageCatalogException, IOException {
        ImageCatalog imageCatalog = new ImageCatalog();
        imageCatalog.setImageCatalogUrl(DEFAULT_CATALOG_URL);

        setupImageCatalogProvider(DEFAULT_CATALOG_URL, V3_CB_CATALOG_FILE);
        when(imageCatalogRepository.findByNameAndWorkspaceId("catalog", WORKSPACE_ID)).thenReturn(Optional.of(imageCatalog));

        ImageFilterResult actual = underTest.getImageFilterResult(WORKSPACE_ID, "catalog", IMAGE_CATALOG_PLATFORM, true, "current-image-id");

        assertEquals(9, actual.getImages().size());
        verifyNoInteractions(prefixMatcherService);
    }

    private void setupImageCatalogProvider(String catalogUrl, String catalogFile) throws IOException, CloudbreakImageCatalogException {
        String catalogJson = FileReaderUtils.readFileFromClasspath(catalogFile);
        CloudbreakImageCatalogV3 catalog = JsonUtil.readValue(catalogJson, CloudbreakImageCatalogV3.class);
        lenient().when(imageCatalog.getImageCatalogUrl()).thenReturn(catalogUrl);
        lenient().when(imageCatalogProvider.getImageCatalogV3(catalogUrl)).thenReturn(catalog);
        lenient().when(imageCatalogProvider.getImageCatalogV3(catalogUrl, true)).thenReturn(catalog);
        lenient().when(cloudbreakVersionListProvider.getVersions(any())).thenReturn(catalog.getVersions().getCloudbreakVersions());
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
        lenient().when(userProfileService.getOrCreate(any(User.class))).thenReturn(userProfile);
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
        lenient().when(customImageProvider.mergeSourceImageAndCustomImageProperties(any(), any(), any(), any())).thenReturn(statedImage);
    }

    private ImageCatalog getImageCatalog() {
        ImageCatalog imageCatalog = new ImageCatalog();
        imageCatalog.setImageCatalogUrl(DEFAULT_CATALOG_URL);
        imageCatalog.setName("default");
        Workspace ws = new Workspace();
        ws.setId(WORKSPACE_ID);
        imageCatalog.setWorkspace(ws);
        imageCatalog.setCreator("someone");
        imageCatalog.setResourceCrn("someCrn");
        return imageCatalog;
    }

    private Image getImage() {
        return Image.builder()
                .withUuid("Test uuid")
                .build();
    }

    private CustomImage getCustomImage(ImageType imageType, String customizedImageId, String baseParcelUrl) {
        CustomImage customImage = new CustomImage();
        customImage.setId(0L);
        customImage.setName(CUSTOM_IMAGE_ID);
        customImage.setDescription("Test image");
        customImage.setImageType(imageType);
        customImage.setCustomizedImageId(customizedImageId);
        customImage.setBaseParcelUrl(baseParcelUrl);
        customImage.setImageCatalog(getImageCatalog());
        return customImage;
    }

    private Set<ImageCatalog> getImageCatalogs() {
        ImageCatalog customImageCatalog = new ImageCatalog();
        customImageCatalog.setName(CUSTOM_CATALOG_NAME);
        return Set.of(getImageCatalog(), customImageCatalog);
    }

    private void setMockedLegacyCatalogEnabled(boolean value) {
        ReflectionTestUtils.setField(underTest, ImageCatalogService.class, "legacyCatalogEnabled", value, boolean.class);
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
