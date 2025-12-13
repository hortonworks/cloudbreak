package com.sequenceiq.cloudbreak.service.image;

import static com.sequenceiq.common.model.ImageCatalogPlatform.imageCatalogPlatform;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.AdditionalAnswers.returnsSecondArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.model.catalog.CloudbreakImageCatalogV3;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.common.provider.ProviderPreferencesService;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.domain.ImageCatalog;
import com.sequenceiq.cloudbreak.domain.UserProfile;
import com.sequenceiq.cloudbreak.repository.ImageCatalogRepository;
import com.sequenceiq.cloudbreak.service.image.catalog.AdvertisedImageCatalogService;
import com.sequenceiq.cloudbreak.service.image.catalog.AdvertisedImageProvider;
import com.sequenceiq.cloudbreak.service.image.catalog.ImageCatalogServiceProxy;
import com.sequenceiq.cloudbreak.service.image.catalog.VersionBasedImageCatalogService;
import com.sequenceiq.cloudbreak.service.image.catalog.VersionBasedImageProvider;
import com.sequenceiq.cloudbreak.service.user.UserProfileService;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.structuredevent.LegacyRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.cloudbreak.util.TestConstants;
import com.sequenceiq.cloudbreak.workspace.model.User;

public class ImageCatalogServiceDefaultTest {

    private static final String TEST_USER_CRN = "crn:cdp:iam:us-west-1:1234:user:1";

    private static final String[] PROVIDERS = {"aws", "azure", "gcp"};

    private static final String DEFAULT_CDH_IMAGE_CATALOG = "com/sequenceiq/cloudbreak/service/image/default-cdh-imagecatalog.json";

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:acc1:user:user1";

    @Mock
    private ImageCatalogProvider imageCatalogProvider;

    @Spy
    private ImageCatalogVersionFilter versionFilter;

    @Mock
    private UserProfileService userProfileService;

    @Mock
    private ImageCatalogRepository imageCatalogRepository;

    @Mock
    private ProviderPreferencesService preferencesService;

    @Mock
    private User user;

    @Mock
    private ImageCatalog imageCatalog;

    @Mock
    private UserService userService;

    @Mock
    private LegacyRestRequestThreadLocalService legacyRestRequestThreadLocalService;

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private PrefixMatcherService prefixMatcherService;

    @Mock
    private LatestDefaultImageUuidProvider latestDefaultImageUuidProvider;

    @Mock
    private ProviderSpecificImageFilter providerSpecificImageFilter;

    @InjectMocks
    private VersionBasedImageProvider versionBasedImageProvider;

    @Mock
    private AdvertisedImageProvider advertisedImageProvider;

    @Mock
    private CloudbreakVersionListProvider cloudbreakVersionListProvider;

    @Mock
    private ImageComparator imageComparator;

    @Mock
    private ImageOsService imageOsService;

    @InjectMocks
    private ImageCatalogServiceProxy imageCatalogServiceProxy;

    @InjectMocks
    private AdvertisedImageCatalogService advertisedImageCatalogService;

    @InjectMocks
    private VersionBasedImageCatalogService versionBasedImageCatalogService;

    @InjectMocks
    private ImageCatalogService underTest;

    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                // catalog                  //provider  //clusterVersion    // expected image           //cbversion     //os
                {"Testing catalog for filterin 2.6 runtimes  for 5.0.0 CB  without os limit",
                        DEFAULT_CDH_IMAGE_CATALOG, "aws", "2.6", "latest-hdp", "5.0.0", ""},
                {"Testing catalog for filterin 2.6 runtimes for 5.0.0 CB with centos7 limit",
                        DEFAULT_CDH_IMAGE_CATALOG, "aws", "2.6", "latest-hdp", "5.0.0", "centos7"},
                {"Testing catalog for filterin 2.6 runtimes for 5.0.0 CB with amazonlinux2 limit",
                        DEFAULT_CDH_IMAGE_CATALOG, "aws", "2.6", "latest-amazonlinux-hdp", "5.0.0", "amazonlinux2"},
                {"Testing catalog for filterin 2.6 runtimes for 6.0.0 CB without os limit",
                        DEFAULT_CDH_IMAGE_CATALOG, "aws", "2.6", "second-latest-hdp", "6.0.0", ""},
                {"Testing catalog for filterin 2.6 runtimes for 6.1.0 CB without os limit",
                        DEFAULT_CDH_IMAGE_CATALOG, "aws", "2.6", "second-latest-hdp", "6.1.0", ""},
                {"Testing catalog for filterin 2.6 runtimes for 9.0.0 CB without os limit",
                        DEFAULT_CDH_IMAGE_CATALOG, "aws", "2.6", "latest-hdp", "9.0.0", ""}
        });
    }

    @BeforeEach
    public void beforeTest() {
        MockitoAnnotations.initMocks(this);
        when(preferencesService.enabledPlatforms()).thenReturn(new HashSet<>(Arrays.asList(PROVIDERS)));

        when(userProfileService.getOrCreate(user)).thenReturn(new UserProfile());
        when(userProfileService.getOrCreate(user)).thenReturn(new UserProfile());
        lenient().when(user.getUserCrn()).thenReturn(TestConstants.CRN);
        when(userService.getOrCreate(any())).thenReturn(user);
        when(entitlementService.baseImageEnabled(anyString())).thenReturn(true);
        when(providerSpecificImageFilter.filterImages(any(), anyList())).then(returnsSecondArg());

        lenient().when(imageComparator.compare(any(), any())).thenReturn(1);
        lenient().when(imageOsService.isSupported(any())).thenReturn(true);

        ReflectionTestUtils.setField(underTest, "imageCatalogServiceProxy", imageCatalogServiceProxy);
        ReflectionTestUtils.setField(imageCatalogServiceProxy, "advertisedImageCatalogService", advertisedImageCatalogService);
        ReflectionTestUtils.setField(imageCatalogServiceProxy, "versionBasedImageCatalogService", versionBasedImageCatalogService);
        ReflectionTestUtils.setField(versionBasedImageCatalogService, "versionBasedImageProvider", versionBasedImageProvider);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    public void testGetDefaultImageShouldReturnProperDefaultImage(String name, String catalogFile, String provider,
            String clusterVersion, String expectedImageId, String cbVersion, String os) throws Exception {
        // GIVEN
        String catalogJson = FileReaderUtils.readFileFromClasspath(catalogFile);
        CloudbreakImageCatalogV3 catalog = JsonUtil.readValue(catalogJson, CloudbreakImageCatalogV3.class);
        when(imageCatalogProvider.getImageCatalogV3(catalogFile)).thenReturn(catalog);
        when(imageCatalog.getImageCatalogUrl()).thenReturn(catalogFile);
        ReflectionTestUtils.setField(underTest, "cbVersion", cbVersion);
        ReflectionTestUtils.setField(underTest, "defaultCatalogUrl", "");

        // WHEN
        when(prefixMatcherService.prefixMatchForCBVersion(eq(cbVersion), any()))
                .thenReturn(new PrefixMatchImages(Set.of(expectedImageId), Collections.emptySet(), Set.of(cbVersion)));
        Set<String> operatingSystems = null;
        if (StringUtils.isNotEmpty(os)) {
            operatingSystems = Collections.singleton(os);
        }
        setupLatestDefaultImageUuidProvider(expectedImageId);
        ImageFilter imageFilter = ImageFilter.builder()
                .withImageCatalog(imageCatalog)
                .withPlatforms(Set.of(imageCatalogPlatform(provider)))
                .withCbVersion(cbVersion)
                .withOperatingSystems(operatingSystems)
                .withClusterVersion(clusterVersion)
                .build();
        StatedImage statedImage = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            try {
                return underTest.getImagePrewarmedDefaultPreferred(imageFilter);
            } catch (CloudbreakImageNotFoundException | CloudbreakImageCatalogException e) {
                throw new RuntimeException(e);
            }
        });
        // THEN
        assertEquals(expectedImageId, statedImage.getImage().getUuid(), "Wrong default image has been selected");

        verify(providerSpecificImageFilter, times(3)).filterImages(any(), anyList());
    }

    private void setupLatestDefaultImageUuidProvider(String uuid) {
        when(latestDefaultImageUuidProvider.getLatestDefaultImageUuids(any(), any())).thenReturn(List.of(uuid));
    }
}
