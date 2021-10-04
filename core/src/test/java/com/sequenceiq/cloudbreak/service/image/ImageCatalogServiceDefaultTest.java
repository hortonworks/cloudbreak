package com.sequenceiq.cloudbreak.service.image;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.model.catalog.CloudbreakImageCatalogV3;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.domain.ImageCatalog;
import com.sequenceiq.cloudbreak.domain.UserProfile;
import com.sequenceiq.cloudbreak.repository.ImageCatalogRepository;
import com.sequenceiq.cloudbreak.structuredevent.LegacyRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.account.PreferencesService;
import com.sequenceiq.cloudbreak.service.image.catalog.AdvertisedImageCatalogService;
import com.sequenceiq.cloudbreak.service.image.catalog.AdvertisedImageProvider;
import com.sequenceiq.cloudbreak.service.image.catalog.ImageCatalogServiceProxy;
import com.sequenceiq.cloudbreak.service.image.catalog.VersionBasedImageCatalogService;
import com.sequenceiq.cloudbreak.service.image.catalog.VersionBasedImageProvider;
import com.sequenceiq.cloudbreak.service.user.UserProfileService;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.cloudbreak.util.TestConstants;
import com.sequenceiq.cloudbreak.workspace.model.User;

@RunWith(Parameterized.class)
public class ImageCatalogServiceDefaultTest {

    private static final String[] PROVIDERS = { "aws", "azure", "gcp" };

    private static final String DEFAULT_CDH_IMAGE_CATALOG = "com/sequenceiq/cloudbreak/service/image/default-cdh-imagecatalog.json";

    private final String catalogFile;

    private final String provider;

    private final String expectedImageId;

    private final String cbVersion;

    private final String clusterVersion;

    private final String os;

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

    @InjectMocks
    private VersionBasedImageProvider versionBasedImageProvider;

    @Mock
    private AdvertisedImageProvider advertisedImageProvider;

    @Mock
    private CloudbreakVersionListProvider cloudbreakVersionListProvider;

    @InjectMocks
    private ImageCatalogServiceProxy imageCatalogServiceProxy;

    @InjectMocks
    private AdvertisedImageCatalogService advertisedImageCatalogService;

    @InjectMocks
    private VersionBasedImageCatalogService versionBasedImageCatalogService;

    @InjectMocks
    private ImageCatalogService underTest;

    public ImageCatalogServiceDefaultTest(String catalogFile, String provider,
            String clusterVersion, String expectedImageId, String cbVersion, String os) {
        this.catalogFile = catalogFile;
        this.provider = provider;
        this.expectedImageId = expectedImageId;
        this.cbVersion = cbVersion;
        this.clusterVersion = clusterVersion;
        this.os = os;
    }

    @Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                {DEFAULT_CDH_IMAGE_CATALOG, "aws", "2.6", "latest-hdp", "5.0.0", "" },
                {DEFAULT_CDH_IMAGE_CATALOG, "aws", "2.6", "latest-hdp", "5.0.0", "centos7" },
                {DEFAULT_CDH_IMAGE_CATALOG, "aws", "2.6", "latest-amazonlinux-hdp", "5.0.0", "amazonlinux2" },
                {DEFAULT_CDH_IMAGE_CATALOG, "aws", "2.6", "second-latest-hdp", "6.0.0", "" },
                {DEFAULT_CDH_IMAGE_CATALOG, "aws", "2.6", "second-latest-hdp", "6.1.0", "" },
                {DEFAULT_CDH_IMAGE_CATALOG, "aws", "2.6", "latest-hdp", "9.0.0", "" }
        });
    }

    @Before
    public void beforeTest() throws Exception {
        MockitoAnnotations.initMocks(this);
        String catalogJson = FileReaderUtils.readFileFromClasspath(catalogFile);
        CloudbreakImageCatalogV3 catalog = JsonUtil.readValue(catalogJson, CloudbreakImageCatalogV3.class);
        when(imageCatalogProvider.getImageCatalogV3(catalogFile)).thenReturn(catalog);
        when(preferencesService.enabledPlatforms()).thenReturn(new HashSet<>(Arrays.asList(PROVIDERS)));

        when(userProfileService.getOrCreate(user)).thenReturn(new UserProfile());
        when(userProfileService.getOrCreate(user)).thenReturn(new UserProfile());
        when(imageCatalog.getImageCatalogUrl()).thenReturn(catalogFile);
        lenient().when(user.getUserCrn()).thenReturn(TestConstants.CRN);
        when(userService.getOrCreate(any())).thenReturn(user);
        when(entitlementService.baseImageEnabled(anyString())).thenReturn(true);

        ReflectionTestUtils.setField(underTest, "imageCatalogServiceProxy", imageCatalogServiceProxy);
        ReflectionTestUtils.setField(imageCatalogServiceProxy, "advertisedImageCatalogService", advertisedImageCatalogService);
        ReflectionTestUtils.setField(imageCatalogServiceProxy, "versionBasedImageCatalogService", versionBasedImageCatalogService);
        ReflectionTestUtils.setField(versionBasedImageCatalogService, "versionBasedImageProvider", versionBasedImageProvider);
    }

    @Test
    public void testGetDefaultImageShouldReturnProperDefaultImage() throws Exception {
        // GIVEN
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
        ImageFilter imageFilter = new ImageFilter(imageCatalog, Set.of(provider), cbVersion, true, operatingSystems, clusterVersion);
        StatedImage statedImage = underTest.getImagePrewarmedDefaultPreferred(imageFilter, image -> true);
        // THEN
        Assert.assertEquals("Wrong default image has been selected", expectedImageId, statedImage.getImage().getUuid());
    }

    private void setupLatestDefaultImageUuidProvider(String uuid) {
        when(latestDefaultImageUuidProvider.getLatestDefaultImageUuids(any(), any())).thenReturn(List.of(uuid));
    }
}
