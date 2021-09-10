package com.sequenceiq.cloudbreak.converter.v4.imagecatalog;

import static com.sequenceiq.cloudbreak.RepoTestUtil.getDefaultCDHInfo;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.ImagesV4Response;
import com.sequenceiq.cloudbreak.api.util.ConverterUtil;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Images;
import com.sequenceiq.cloudbreak.cloud.model.catalog.StackDetails;
import com.sequenceiq.cloudbreak.cloud.model.catalog.StackRepoDetails;
import com.sequenceiq.cloudbreak.cloud.model.component.DefaultCDHInfo;
import com.sequenceiq.cloudbreak.cloud.model.component.ImageBasedDefaultCDHEntries;
import com.sequenceiq.cloudbreak.cloud.model.component.ImageBasedDefaultCDHInfo;
import com.sequenceiq.cloudbreak.converter.AbstractEntityConverterTest;

@RunWith(MockitoJUnitRunner.class)
public class ImagesToImagesV4ResponseConverterTest extends AbstractEntityConverterTest<Images> {

    public static final String REDHAT_7 = "redhat7";

    @Mock
    private ImageBasedDefaultCDHEntries imageBasedDefaultCDHEntries;

    @Mock
    private ConverterUtil converterUtil;

    @InjectMocks
    private ImagesToImagesV4ResponseConverter underTest = new ImagesToImagesV4ResponseConverter();

    @Override
    public Images createSource() {
        return getImages();
    }

    @Test
    public void testConvert() {
        setupStackEntries();
        ImagesV4Response result = underTest.convert(createSource());
        assertEquals(1, result.getCdhImages().size());
    }

    private Images getImages() {
        Images images = new Images(
                Collections.singletonList(getImage(REDHAT_7)),
                Collections.singletonList(getImage(REDHAT_7)),
                Collections.emptyList(),
                new HashSet<>()
        );
        return images;
    }

    private Image getImage(String osType) {
        Map<String, Map<String, String>> imageSetsByProvider = new HashMap<>();
        imageSetsByProvider.put("AWS", null);
        StackRepoDetails repoDetails = new StackRepoDetails(Collections.emptyMap(), Collections.emptyMap());
        StackDetails stackDetails = new StackDetails("3.1", repoDetails, "1");

        return new Image("", System.currentTimeMillis(), "", osType, UUID.randomUUID().toString(), "",
                Collections.emptyMap(), imageSetsByProvider, stackDetails, osType, Collections.emptyMap(),
                Collections.emptyList(), Collections.emptyList(), "1", true, null, null);
    }

    private void setupStackEntries() {

        Map<String, ImageBasedDefaultCDHInfo> cdhEntries = new HashMap<>();

        DefaultCDHInfo cdhInfo = getDefaultCDHInfo("6.1.0-1.cdh6.1.0.p0.770702");
        Image image = mock(Image.class);
        cdhEntries.put("6.1.0", new ImageBasedDefaultCDHInfo(cdhInfo, image));
        when(imageBasedDefaultCDHEntries.getEntries(Mockito.any(Images.class))).thenReturn(cdhEntries);
    }
}
