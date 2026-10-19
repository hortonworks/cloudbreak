package com.sequenceiq.cloudbreak.converter.v4.imagecatalog;

import static com.sequenceiq.cloudbreak.RepoTestUtil.getDefaultCDHInfo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.ImagesV4Response;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.ImageStackDetails;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Images;
import com.sequenceiq.cloudbreak.cloud.model.catalog.StackRepoDetails;
import com.sequenceiq.cloudbreak.cloud.model.component.DefaultCDHInfo;
import com.sequenceiq.cloudbreak.cloud.model.component.ImageBasedDefaultCDHEntries;
import com.sequenceiq.cloudbreak.cloud.model.component.ImageBasedDefaultCDHInfo;
import com.sequenceiq.cloudbreak.converter.AbstractEntityConverterTest;

@ExtendWith(MockitoExtension.class)
class ImagesToImagesV4ResponseConverterTest extends AbstractEntityConverterTest<Images> {

    public static final String REDHAT_7 = "redhat7";

    private static final String SOURCE_IMAGE_ID = "949bffa3-17d4-4076-9d5a-bf3d23c1086b";

    private static final String BASE_PARCEL_URL = "https://mirror.example.com/cloudera/p/";

    @Mock
    private ImageBasedDefaultCDHEntries imageBasedDefaultCDHEntries;

    @Mock
    private ImageToImageV4ResponseConverter imageToImageV4ResponseConverter;

    @InjectMocks
    private ImagesToImagesV4ResponseConverter underTest;

    @Override
    public Images createSource() {
        return getImages();
    }

    @Test
    void testConvert() {
        setupStackEntries();
        ImagesV4Response result = underTest.convert(createSource());
        assertEquals(1, result.getCdhImages().size());
    }

    @Test
    void testConvertKeepsSourceImageIdAndBaseParcelUrlOfBaseAndRuntimeImages() {
        setupStackEntries();
        Image customImage = Image.builder()
                .copy(getImage(REDHAT_7))
                .withSourceImageId(SOURCE_IMAGE_ID)
                .withBaseParcelUrl(BASE_PARCEL_URL)
                .build();

        ImagesV4Response result = underTest.convert(new Images(List.of(customImage), List.of(customImage), Collections.emptyList()));

        assertEquals(SOURCE_IMAGE_ID, result.getBaseImages().getFirst().getSourceImageId());
        assertEquals(BASE_PARCEL_URL, result.getBaseImages().getFirst().getBaseParcelUrl());
        assertEquals(SOURCE_IMAGE_ID, result.getCdhImages().getFirst().getSourceImageId());
        assertEquals(BASE_PARCEL_URL, result.getCdhImages().getFirst().getBaseParcelUrl());
    }

    private Images getImages() {
        Images images = new Images(
                Collections.singletonList(getImage(REDHAT_7)),
                Collections.singletonList(getImage(REDHAT_7)),
                Collections.emptyList()
        );
        return images;
    }

    private Image getImage(String osType) {
        Map<String, Map<String, String>> imageSetsByProvider = new HashMap<>();
        imageSetsByProvider.put("AWS", null);
        StackRepoDetails repoDetails = new StackRepoDetails(Collections.emptyMap(), Collections.emptyMap());
        ImageStackDetails stackDetails = new ImageStackDetails("3.1", repoDetails, "1");

        return Image.builder()
                .withUuid(UUID.randomUUID().toString())
                .withImageSetsByProvider(imageSetsByProvider)
                .withStackDetails(stackDetails)
                .withOsType(osType)
                .withAdvertised(true)
                .build();
    }

    private void setupStackEntries() {

        Map<String, ImageBasedDefaultCDHInfo> cdhEntries = new HashMap<>();

        DefaultCDHInfo cdhInfo = getDefaultCDHInfo("6.1.0-1.cdh6.1.0.p0.770702");
        Image image = mock(Image.class);
        cdhEntries.put("6.1.0", new ImageBasedDefaultCDHInfo(cdhInfo, image));
        when(imageBasedDefaultCDHEntries.getEntries(any(Images.class))).thenReturn(cdhEntries);
    }
}
