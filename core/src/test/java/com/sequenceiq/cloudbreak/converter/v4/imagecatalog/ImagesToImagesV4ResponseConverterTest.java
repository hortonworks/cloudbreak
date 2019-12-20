package com.sequenceiq.cloudbreak.converter.v4.imagecatalog;

import static com.sequenceiq.cloudbreak.RepoTestUtil.getDefaultCDHInfo;
import static org.junit.Assert.assertEquals;
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
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.BaseImageV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.ImagesV4Response;
import com.sequenceiq.cloudbreak.api.util.ConverterUtil;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Images;
import com.sequenceiq.cloudbreak.cloud.model.catalog.StackDetails;
import com.sequenceiq.cloudbreak.cloud.model.catalog.StackRepoDetails;
import com.sequenceiq.cloudbreak.cloud.model.component.DefaultCDHEntries;
import com.sequenceiq.cloudbreak.cloud.model.component.DefaultCDHInfo;
import com.sequenceiq.cloudbreak.cloud.model.component.StackType;
import com.sequenceiq.cloudbreak.converter.AbstractEntityConverterTest;
import com.sequenceiq.cloudbreak.service.DefaultClouderaManagerRepoService;

@RunWith(MockitoJUnitRunner.class)
public class ImagesToImagesV4ResponseConverterTest extends AbstractEntityConverterTest<Images> {

    public static final String REDHAT_7 = "redhat7";

    @Mock
    private DefaultCDHEntries defaultCDHEntries;

    @Mock
    private DefaultClouderaManagerRepoService defaultClouderaManagerRepoService;

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
        setupClouderaManagerEntries();
        setupStackEntries();
        ImagesV4Response result = underTest.convert(createSource());
        assertEquals(1, result.getBaseImages().size());
        assertEquals(1, result.getHdfImages().size());
        assertEquals(1, result.getCdhImages().size());
        assertEquals(1, result.getHdpImages().size());
        BaseImageV4Response baseImageV4Response = result.getBaseImages().get(0);
        assertEquals(1, baseImageV4Response.getCdhStacks().size());
        assertEquals("6.1.0", baseImageV4Response.getClouderaManagerRepo().getVersion());
    }

    private Images getImages() {
        Images images = new Images(
                Collections.singletonList(getImage(REDHAT_7, null)),
                Collections.singletonList(getImage(REDHAT_7, StackType.HDP)),
                Collections.singletonList(getImage(REDHAT_7, StackType.HDF)),
                Collections.singletonList(getImage(REDHAT_7, StackType.CDH)),
                new HashSet<>()
        );
        return images;
    }

    private Image getImage(String osType, StackType type) {
        Map<String, Map<String, String>> imageSetsByProvider = new HashMap<>();
        imageSetsByProvider.put("AWS", null);
        StackRepoDetails repoDetails = new StackRepoDetails(Collections.emptyMap(), Collections.emptyMap());
        StackDetails stackDetails = new StackDetails("3.1", repoDetails, "1");

        return new Image("", System.currentTimeMillis(), "", osType, UUID.randomUUID().toString(), "",
                Collections.emptyMap(), imageSetsByProvider, stackDetails, osType, Collections.emptyMap(),
                Collections.emptyList(), Collections.emptyList(), "1");
    }

    private void setupStackEntries() {

        Map<String, DefaultCDHInfo> cdhEntries = new HashMap<>();

        DefaultCDHInfo cdhInfo = getDefaultCDHInfo("6.1", "6.1.0-1.cdh6.1.0.p0.770702");
        cdhEntries.put("6.1.0", cdhInfo);

        when(defaultCDHEntries.getEntries()).thenReturn(cdhEntries);
    }

    private void setupClouderaManagerEntries() {

        ClouderaManagerRepo clouderaManagerRepo = new ClouderaManagerRepo();
        clouderaManagerRepo.setBaseUrl("http://public-repo-1.hortonworks.com/cm/centos7/6.1.0/updates/6.1.0");
        clouderaManagerRepo.setVersion("6.1.0");
        when(defaultClouderaManagerRepoService.getDefault(REDHAT_7)).thenReturn(clouderaManagerRepo);
    }
}
