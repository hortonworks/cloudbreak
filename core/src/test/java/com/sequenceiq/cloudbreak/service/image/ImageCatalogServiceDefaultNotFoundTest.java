package com.sequenceiq.cloudbreak.service.image;

import static com.sequenceiq.cloudbreak.service.image.catalog.model.ImageCatalogPlatform.imageCatalogPlatform;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.cloud.model.catalog.CloudbreakImageCatalogV3;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.common.provider.ProviderPreferencesService;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.domain.ImageCatalog;
import com.sequenceiq.cloudbreak.repository.ImageCatalogRepository;
import com.sequenceiq.cloudbreak.service.image.catalog.AdvertisedImageCatalogService;
import com.sequenceiq.cloudbreak.service.image.catalog.AdvertisedImageProvider;
import com.sequenceiq.cloudbreak.service.image.catalog.ImageCatalogServiceProxy;
import com.sequenceiq.cloudbreak.service.image.catalog.VersionBasedImageCatalogService;
import com.sequenceiq.cloudbreak.service.image.catalog.VersionBasedImageProvider;
import com.sequenceiq.cloudbreak.service.user.UserProfileService;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.cloudbreak.workspace.model.User;

@RunWith(MockitoJUnitRunner.class)
public class ImageCatalogServiceDefaultNotFoundTest {

    private static final String[] PROVIDERS = {"aws", "azure", "gcp"};

    private static final String DEFAULT_CDH_IMAGE_CATALOG = "com/sequenceiq/cloudbreak/service/image/default-cdh-imagecatalog.json";

    @Mock
    private ImageCatalogProvider imageCatalogProvider;

    @Mock
    private UserProfileService userProfileService;

    @Spy
    private ImageCatalogVersionFilter versionFilter;

    @Mock
    private ImageCatalogRepository imageCatalogRepository;

    @Mock
    private ProviderPreferencesService preferencesService;

    @Mock
    private User user;

    @Mock
    private ImageCatalog imageCatalog;

    @InjectMocks
    private ImageCatalogService underTest;

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

    @InjectMocks
    private ImageCatalogServiceProxy imageCatalogServiceProxy;

    @InjectMocks
    private AdvertisedImageCatalogService advertisedImageCatalogService;

    @InjectMocks
    private VersionBasedImageCatalogService versionBasedImageCatalogService;

    @Before
    public void beforeTest() {
        ReflectionTestUtils.setField(underTest, "cbVersion", "5.0.0");
        ReflectionTestUtils.setField(underTest, "defaultCatalogUrl", "");
        ReflectionTestUtils.setField(underTest, "imageCatalogServiceProxy", imageCatalogServiceProxy);
        ReflectionTestUtils.setField(imageCatalogServiceProxy, "advertisedImageCatalogService", advertisedImageCatalogService);
        ReflectionTestUtils.setField(imageCatalogServiceProxy, "versionBasedImageCatalogService", versionBasedImageCatalogService);
        ReflectionTestUtils.setField(versionBasedImageCatalogService, "versionBasedImageProvider", versionBasedImageProvider);

        when(preferencesService.enabledPlatforms()).thenReturn(new HashSet<>(Arrays.asList(PROVIDERS)));
    }

    @Test
    public void testGetDefaultImageShouldThrowNotFoundException() throws Exception {
        ImageFilter imageFilter = new ImageFilter(imageCatalog, Set.of(imageCatalogPlatform("gcp")), null, true, Set.of("notimportant"), null);
        try {
            underTest.getImagePrewarmedDefaultPreferred(imageFilter, image -> true);
        } catch (CloudbreakImageNotFoundException exception) {
            Assertions.assertEquals("Could not find any image for platform 'gcp', runtime 'null' and Cloudbreak version '5.0.0' in 'null' image catalog.",
                    exception.getMessage());
        }
        verify(providerSpecificImageFilter, never()).filterImages(any(), anyList());

    }

    @Test
    public void testGetDefaultImageShouldThrowNotFoundException2() throws Exception {
        String catalogJson = FileReaderUtils.readFileFromClasspath(DEFAULT_CDH_IMAGE_CATALOG);
        CloudbreakImageCatalogV3 catalog = JsonUtil.readValue(catalogJson, CloudbreakImageCatalogV3.class);
        when(imageCatalogProvider.getImageCatalogV3(DEFAULT_CDH_IMAGE_CATALOG)).thenReturn(catalog);
        when(cloudbreakVersionListProvider.getVersions(any())).thenReturn(catalog.getVersions().getCloudbreakVersions());
        when(imageCatalog.getImageCatalogUrl()).thenReturn(DEFAULT_CDH_IMAGE_CATALOG);

        ImageFilter imageFilter = new ImageFilter(imageCatalog, Set.of(imageCatalogPlatform("aws")), "2.6", true, Set.of("centos7", "amazonlinux2"), null);
        try {
            underTest.getImagePrewarmedDefaultPreferred(imageFilter, image -> true);
        } catch (CloudbreakImageNotFoundException exception) {
            Assertions.assertEquals("Could not find any image for platform 'aws', runtime 'null' and Cloudbreak version '5.0.0' in 'null' image catalog.",
                    exception.getMessage());
        }
        verify(providerSpecificImageFilter, times(3)).filterImages(eq(Set.of(imageCatalogPlatform(PROVIDERS[0]))), anyList());
        verify(providerSpecificImageFilter, never()).filterImages(eq(Set.of(imageCatalogPlatform(PROVIDERS[1]))), anyList());
        verify(providerSpecificImageFilter, never()).filterImages(eq(Set.of(imageCatalogPlatform(PROVIDERS[2]))), anyList());
    }
}
