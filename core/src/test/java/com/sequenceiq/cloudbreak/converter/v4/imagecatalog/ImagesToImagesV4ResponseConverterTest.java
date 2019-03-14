package com.sequenceiq.cloudbreak.converter.v4.imagecatalog;

import static com.sequenceiq.cloudbreak.RepoTestUtil.getDefaultCDHInfo;
import static com.sequenceiq.cloudbreak.RepoTestUtil.getDefaultHDFInfo;
import static com.sequenceiq.cloudbreak.RepoTestUtil.getDefaultHDPInfo;
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
import com.sequenceiq.cloudbreak.cloud.model.AmbariRepo;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Images;
import com.sequenceiq.cloudbreak.cloud.model.catalog.StackDetails;
import com.sequenceiq.cloudbreak.cloud.model.catalog.StackRepoDetails;
import com.sequenceiq.cloudbreak.cloud.model.component.DefaultCDHEntries;
import com.sequenceiq.cloudbreak.cloud.model.component.DefaultCDHInfo;
import com.sequenceiq.cloudbreak.cloud.model.component.DefaultHDFEntries;
import com.sequenceiq.cloudbreak.cloud.model.component.DefaultHDFInfo;
import com.sequenceiq.cloudbreak.cloud.model.component.DefaultHDPEntries;
import com.sequenceiq.cloudbreak.cloud.model.component.DefaultHDPInfo;
import com.sequenceiq.cloudbreak.cloud.model.component.StackType;
import com.sequenceiq.cloudbreak.converter.AbstractEntityConverterTest;
import com.sequenceiq.cloudbreak.service.DefaultAmbariRepoService;
import com.sequenceiq.cloudbreak.service.DefaultClouderaManagerRepoService;

@RunWith(MockitoJUnitRunner.class)
public class ImagesToImagesV4ResponseConverterTest extends AbstractEntityConverterTest<Images> {

    public static final String REDHAT_7 = "redhat7";

    @Mock
    private DefaultHDPEntries defaultHDPEntries;

    @Mock
    private DefaultHDFEntries defaultHDFEntries;

    @Mock
    private DefaultCDHEntries defaultCDHEntries;

    @Mock
    private DefaultAmbariRepoService defaultAmbariRepoService;

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
        setupAmbariEntries();
        setupClouderaManagerEntries();
        setupStackEntries();
        ImagesV4Response result = underTest.convert(createSource());
        assertEquals(1, result.getBaseImages().size());
        assertEquals(1, result.getHdfImages().size());
        assertEquals(1, result.getCdhImages().size());
        assertEquals(1, result.getHdpImages().size());
        BaseImageV4Response baseImageV4Response = result.getBaseImages().get(0);
        assertEquals(1, baseImageV4Response.getCdhStacks().size());
        assertEquals(2, baseImageV4Response.getHdfStacks().size());
        assertEquals(3, baseImageV4Response.getHdpStacks().size());
        assertEquals("2.6.2.2", baseImageV4Response.getAmbariRepo().getVersion());
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
        StackDetails stackDetails = new StackDetails("3.1", repoDetails, Collections.emptyList());

        return new Image("", "", osType, UUID.randomUUID().toString(), "",
                Collections.emptyMap(), imageSetsByProvider, stackDetails, osType, Collections.emptyMap());
    }

    private void setupStackEntries() {
        Map<String, DefaultHDFInfo> hdfEntries = new HashMap<>();

        DefaultHDFInfo threeOneHdfInfo = getDefaultHDFInfo("2.6", "3.1.2.0");
        hdfEntries.put("3.1", threeOneHdfInfo);

        DefaultHDFInfo threeTwoHdfInfo = getDefaultHDFInfo("2.7", "3.2.4.1");
        hdfEntries.put("3.2", threeTwoHdfInfo);

        when(defaultHDFEntries.getEntries()).thenReturn(hdfEntries);

        Map<String, DefaultHDPInfo> hdpEntries = new HashMap<>();

        DefaultHDPInfo twoSixHdpInfo = getDefaultHDPInfo("2.5", "2.6.5.0");
        hdpEntries.put("2.6", twoSixHdpInfo);

        DefaultHDPInfo threeZeroHdpInfo = getDefaultHDPInfo("2.7", "3.0.1.0");
        hdpEntries.put("3.0", threeZeroHdpInfo);

        DefaultHDPInfo threeOneHdpInfo = getDefaultHDPInfo("2.7", "3.1.8.0");
        hdpEntries.put("3.1", threeOneHdpInfo);

        when(defaultHDPEntries.getEntries()).thenReturn(hdpEntries);

        Map<String, DefaultCDHInfo> cdhEntries = new HashMap<>();

        DefaultCDHInfo cdhInfo = getDefaultCDHInfo("6.1", "6.1.0-1.cdh6.1.0.p0.770702");
        cdhEntries.put("6.1.0", cdhInfo);

        when(defaultCDHEntries.getEntries()).thenReturn(cdhEntries);
    }

    private void setupAmbariEntries() {

        AmbariRepo ambariRepo = new AmbariRepo();
        ambariRepo.setBaseUrl("http://public-repo-1.hortonworks.com/ambari/centos7/2.x/updates/2.6.2.2");
        ambariRepo.setVersion("2.6.2.2");
        when(defaultAmbariRepoService.getDefault(REDHAT_7)).thenReturn(ambariRepo);
    }

    private void setupClouderaManagerEntries() {

        ClouderaManagerRepo clouderaManagerRepo = new ClouderaManagerRepo();
        clouderaManagerRepo.setBaseUrl("http://public-repo-1.hortonworks.com/cm/centos7/6.1.0/updates/6.1.0");
        clouderaManagerRepo.setVersion("6.1.0");
        when(defaultClouderaManagerRepoService.getDefault(REDHAT_7)).thenReturn(clouderaManagerRepo);
    }
}
