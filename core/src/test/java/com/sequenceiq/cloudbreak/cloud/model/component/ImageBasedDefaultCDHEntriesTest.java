package com.sequenceiq.cloudbreak.cloud.model.component;

import static com.sequenceiq.cloudbreak.service.image.ImageCatalogService.CDP_DEFAULT_CATALOG_NAME;
import static com.sequenceiq.common.model.ImageCatalogPlatform.imageCatalogPlatform;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.ImageStackDetails;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Images;
import com.sequenceiq.cloudbreak.cloud.model.catalog.StackRepoDetails;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogService;
import com.sequenceiq.cloudbreak.service.image.ImageOsService;
import com.sequenceiq.cloudbreak.service.image.PreWarmParcelParser;
import com.sequenceiq.cloudbreak.service.image.StatedImages;
import com.sequenceiq.common.model.Architecture;
import com.sequenceiq.common.model.ImageCatalogPlatform;

@ExtendWith(MockitoExtension.class)
class ImageBasedDefaultCDHEntriesTest {

    private static final String IMAGE_VERSION = "7.2.0";

    private static final String REPO_VERSION = "repo version";

    private static final String OS = "redhat7";

    private static final String OS_URL = "http://cloudera-build-us-west-1.vpc.cloudera.com/s3/build/2365123/cm7/7.1.0/redhat7/yum/";

    private static final List<String> PRE_WARM_CSD = asList("csd");

    private static final String PLATFORM = CloudPlatform.AWS.name();

    private static final String IMAGE_CATALOG_NAME = "imageCatalogName";

    @Mock
    private Images images;

    @Mock
    private Images emptyImages;

    @Mock
    private ImageCatalogService imageCatalogService;

    @Mock
    private PreWarmParcelParser preWarmParcelParser;

    @Mock
    private ImageOsService imageOsService;

    @InjectMocks
    private ImageBasedDefaultCDHEntries victim;

    @BeforeEach
    void initTests() {
        when(imageOsService.isSupported(any())).thenReturn(true);
        lenient().when(imageOsService.getDefaultOs()).thenReturn(OS);
        lenient().when(imageOsService.getPreferredOs()).thenReturn(OS);
    }

    @Test
    void shouldReturnImageBasedDefaultCDHInfoMapByDefaultCdhImages() {
        List<Image> imageList = getImages(OS, Architecture.X86_64);
        when(images.getCdhImages()).thenReturn(imageList);

        Map<String, ImageBasedDefaultCDHInfo> actual = victim.getEntries(images);
        Image image = imageList.stream().filter(Image::isDefaultImage).findFirst().get();

        verify(image, actual.get(IMAGE_VERSION));
    }

    @Test
    void shouldFilterByOs() {
        String os = "redhat8";
        when(imageOsService.isSupported(os)).thenReturn(false);
        List<Image> imageList = getImages(os, Architecture.X86_64);
        when(images.getCdhImages()).thenReturn(imageList);

        Map<String, ImageBasedDefaultCDHInfo> actual = victim.getEntries(images);

        assertEquals(0, actual.size());
    }

    @Test
    void shouldPreferOs() {
        List<Image> imageList = new ArrayList<>(getImages("redhat8", Architecture.X86_64));
        List<Image> defaultOsImages = getImages(OS, Architecture.X86_64);
        imageList.addAll(defaultOsImages);
        when(images.getCdhImages()).thenReturn(imageList);

        Map<String, ImageBasedDefaultCDHInfo> actual = victim.getEntries(images);
        Image image = defaultOsImages.stream().filter(Image::isDefaultImage).findFirst().get();

        verify(image, actual.get(IMAGE_VERSION));
    }

    @Test
    void shouldReturnImageBasedDefaultCDHInfoMapByPlatformAndOsAndArchitectureAndImageCatalog() throws CloudbreakImageCatalogException {
        List<Image> x86Images = getImages(OS, Architecture.X86_64);
        List<Image> x86Images2 = getImages("centos7", Architecture.X86_64);
        List<Image> armImages = getImages(OS, Architecture.ARM64);
        List<Image> armImages2 = getImages("centos7", Architecture.ARM64);
        List<Image> imageList = new ArrayList<>();
        imageList.addAll(x86Images);
        imageList.addAll(x86Images2);
        imageList.addAll(armImages);
        imageList.addAll(armImages2);
        when(images.getCdhImages()).thenReturn(imageList);
        ImageCatalogPlatform imageCatalogPlatform = imageCatalogPlatform(PLATFORM);

        StatedImages statedImages = StatedImages.statedImages(images, null, null);
        when(imageCatalogService.getImages(0L, IMAGE_CATALOG_NAME, null, imageCatalogPlatform, true, null)).thenReturn(statedImages);

        Map<String, ImageBasedDefaultCDHInfo> x86Actual = victim.getEntries(0L, imageCatalogPlatform, null, Architecture.X86_64, IMAGE_CATALOG_NAME);
        Image x86Image = x86Images.stream().filter(Image::isDefaultImage).findFirst().get();
        verify(x86Image, x86Actual.get(IMAGE_VERSION));

        Map<String, ImageBasedDefaultCDHInfo> armActual = victim.getEntries(0L, imageCatalogPlatform, null, Architecture.ARM64, IMAGE_CATALOG_NAME);
        Image armImage = armImages.stream().filter(Image::isDefaultImage).findFirst().get();
        verify(armImage, armActual.get(IMAGE_VERSION));
    }

    @Test
    void shouldFallBackToAwsInCaseOfMissingCdhImages() throws CloudbreakImageCatalogException {
        ImageCatalogPlatform imageCatalogPlatform = imageCatalogPlatform(CloudPlatform.YARN.name());
        List<Image> imageList = getImages(OS, Architecture.X86_64);
        when(images.getCdhImages()).thenReturn(imageList);

        StatedImages statedImages = StatedImages.statedImages(images, null, null);
        StatedImages emptyStatedImages = StatedImages.statedImages(emptyImages, null, null);

        when(imageCatalogService.getImages(0L, CDP_DEFAULT_CATALOG_NAME, null, imageCatalogPlatform, true, null)).thenReturn(emptyStatedImages);
        when(imageCatalogService.getImages(0L, CDP_DEFAULT_CATALOG_NAME, null, imageCatalogPlatform, true, null)).thenReturn(statedImages);

        Map<String, ImageBasedDefaultCDHInfo> actual = victim.getEntries(0L, imageCatalogPlatform, null, Architecture.X86_64, "");

        Image image = imageList.stream().filter(Image::isDefaultImage).findFirst().get();

        verify(image, actual.get(IMAGE_VERSION));
    }

    @Test
    void testgetEntriesWhenNullStackDetails() {
        Image image = mock();
        when(image.isDefaultImage()).thenReturn(true);
        when(image.getVersion()).thenReturn(IMAGE_VERSION);
        when(images.getCdhImages()).thenReturn(List.of(image));

        Map<String, ImageBasedDefaultCDHInfo> result = victim.getEntries(images, null, null);

        assertThat(result).hasSize(1);
        assertNull(result.get(IMAGE_VERSION).getImage().getStackDetails());
    }

    private List<Image> getImages(String os, Architecture architecture) {
        StackRepoDetails stackRepoDetails = new StackRepoDetails(getRepo(), null);
        ImageStackDetails stackDetails = new ImageStackDetails(null, stackRepoDetails, null);
        List<List<String>> parcels = getParcels();
        Image defaultImage = mock(Image.class);
        when(defaultImage.isDefaultImage()).thenReturn(true);
        lenient().when(defaultImage.getVersion()).thenReturn(IMAGE_VERSION);
        lenient().when(defaultImage.getStackDetails()).thenReturn(stackDetails);
        lenient().when(defaultImage.getPreWarmParcels()).thenReturn(parcels);
        lenient().when(defaultImage.getPreWarmCsd()).thenReturn(PRE_WARM_CSD);
        when(defaultImage.getOs()).thenReturn(os);
        lenient().when(defaultImage.getArchitecture()).thenReturn(architecture.getName());

        Image nonDefaultImage = mock(Image.class);
        when(nonDefaultImage.isDefaultImage()).thenReturn(false);
        lenient().when(nonDefaultImage.getOs()).thenReturn(os);
        lenient().when(nonDefaultImage.getArchitecture()).thenReturn(architecture.getName());

        //Default image added double times to test
        //the algorithm is not failing on multiple default images for the same version
        return asList(defaultImage, defaultImage, nonDefaultImage);
    }

    private Map<String, String> getRepo() {
        Map<String, String> repo = new HashMap<>();
        repo.put(com.sequenceiq.cloudbreak.cloud.model.component.StackRepoDetails.REPOSITORY_VERSION, REPO_VERSION);
        repo.put(OS, OS_URL);

        return repo;
    }

    private List<List<String>> getParcels() {
        List<String> parcel = asList("parcel");
        lenient().when(preWarmParcelParser.parseProductFromParcel(parcel, PRE_WARM_CSD)).thenReturn(Optional.of(mock(ClouderaManagerProduct.class)));

        return asList(parcel);
    }

    private void verify(Image image, ImageBasedDefaultCDHInfo imageBasedDefaultCDHInfo) {
        assertNotNull(imageBasedDefaultCDHInfo);
        assertEquals(image, imageBasedDefaultCDHInfo.getImage());
        assertEquals(REPO_VERSION, imageBasedDefaultCDHInfo.getDefaultCDHInfo().getVersion());
        assertEquals(REPO_VERSION, imageBasedDefaultCDHInfo.getDefaultCDHInfo().getVersion());
        assertEquals(OS_URL, imageBasedDefaultCDHInfo.getDefaultCDHInfo().getRepo().getStack().get(OS));
        assertEquals(1, imageBasedDefaultCDHInfo.getDefaultCDHInfo().getParcels().size());
        assertEquals(PRE_WARM_CSD, imageBasedDefaultCDHInfo.getDefaultCDHInfo().getCsd());
    }
}