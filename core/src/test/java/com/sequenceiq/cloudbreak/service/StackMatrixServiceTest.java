package com.sequenceiq.cloudbreak.service;

import static com.sequenceiq.cloudbreak.RepoTestUtil.getAmbariInfo;
import static com.sequenceiq.cloudbreak.RepoTestUtil.getAmbariInfoResponse;
import static com.sequenceiq.cloudbreak.RepoTestUtil.getCMStackDescriptorResponse;
import static com.sequenceiq.cloudbreak.RepoTestUtil.getDefaultCDHInfo;
import static com.sequenceiq.cloudbreak.RepoTestUtil.getDefaultHDFInfo;
import static com.sequenceiq.cloudbreak.RepoTestUtil.getDefaultHDPInfo;
import static com.sequenceiq.cloudbreak.RepoTestUtil.getStackDescriptorResponse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.AmbariInfoV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.AmbariStackDescriptorV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.ClouderaManagerInfoV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.ClouderaManagerStackDescriptorV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.StackMatrixV4Response;
import com.sequenceiq.cloudbreak.api.util.ConverterUtil;
import com.sequenceiq.cloudbreak.cloud.model.component.DefaultCDHEntries;
import com.sequenceiq.cloudbreak.cloud.model.component.DefaultCDHInfo;
import com.sequenceiq.cloudbreak.cloud.model.component.DefaultHDFEntries;
import com.sequenceiq.cloudbreak.cloud.model.component.DefaultHDFInfo;
import com.sequenceiq.cloudbreak.cloud.model.component.DefaultHDPEntries;
import com.sequenceiq.cloudbreak.cloud.model.component.DefaultHDPInfo;
import com.sequenceiq.cloudbreak.cloud.model.component.RepositoryInfo;

@RunWith(MockitoJUnitRunner.class)
public class StackMatrixServiceTest {

    @Mock
    private DefaultHDFEntries defaultHDFEntries;

    @Mock
    private DefaultHDPEntries defaultHDPEntries;

    @Mock
    private DefaultCDHEntries defaultCDHEntries;

    @Mock
    private DefaultAmbariRepoService defaultAmbariRepoService;

    @Mock
    private DefaultClouderaManagerRepoService defaultClouderaManagerRepoService;

    @Mock
    private ConverterUtil converterUtil;

    @InjectMocks
    private StackMatrixService stackMatrixService;

    @Test
    public void getStackMatrix() {
        setupStackEntries();
        setupAmbariEntries();

        StackMatrixV4Response stackMatrixV4Response = stackMatrixService.getStackMatrix();

        assertEquals(2L, stackMatrixV4Response.getHdf().size());
        assertEquals(3L, stackMatrixV4Response.getHdp().size());

        assertEquals("2.6", stackMatrixV4Response.getHdf().get("3.1").getMinAmbari());
        assertEquals("3.1.2.0", stackMatrixV4Response.getHdf().get("3.1").getVersion());
        assertEquals("2.6.1.0", stackMatrixV4Response.getHdf().get("3.1").getAmbari().getVersion());
        assertEquals("http://redhat6-base/2.6.1.0", stackMatrixV4Response.getHdf().get("3.1").getAmbari().getRepository().get("redhat6").getBaseUrl());

        assertEquals("2.7", stackMatrixV4Response.getHdf().get("3.2").getMinAmbari());
        assertEquals("3.2.4.1", stackMatrixV4Response.getHdf().get("3.2").getVersion());
        assertEquals("2.7.0.3", stackMatrixV4Response.getHdf().get("3.2").getAmbari().getVersion());
        assertEquals("http://redhat7-base/2.7.0.3", stackMatrixV4Response.getHdf().get("3.2").getAmbari().getRepository().get("redhat7").getBaseUrl());

        assertEquals("2.5", stackMatrixV4Response.getHdp().get("2.6").getMinAmbari());
        assertEquals("2.6.5.0", stackMatrixV4Response.getHdp().get("2.6").getVersion());
        assertEquals("2.5.0.0", stackMatrixV4Response.getHdp().get("2.6").getAmbari().getVersion());
        assertEquals("http://redhat6-base/2.5.0.0", stackMatrixV4Response.getHdp().get("2.6").getAmbari().getRepository().get("redhat6").getBaseUrl());

        assertEquals("2.7", stackMatrixV4Response.getHdp().get("3.1").getMinAmbari());
        assertEquals("3.1.8.0", stackMatrixV4Response.getHdp().get("3.1").getVersion());
        assertEquals("2.7.0.3", stackMatrixV4Response.getHdp().get("3.1").getAmbari().getVersion());
        assertEquals("http://redhat7-base/2.7.0.3", stackMatrixV4Response.getHdp().get("3.1").getAmbari().getRepository().get("redhat7").getBaseUrl());
    }

    @Test
    public void getStackMatrixWithoutAmbari() {
        setupStackEntries();

        when(converterUtil.convert(any(RepositoryInfo.class), eq(AmbariInfoV4Response.class))).thenReturn(new AmbariInfoV4Response());
        when(converterUtil.convert(any(RepositoryInfo.class), eq(ClouderaManagerInfoV4Response.class))).thenReturn(new ClouderaManagerInfoV4Response());

        StackMatrixV4Response stackMatrixV4Response = stackMatrixService.getStackMatrix();

        assertEquals(2L, stackMatrixV4Response.getHdf().size());
        assertEquals(3L, stackMatrixV4Response.getHdp().size());
        assertEquals(1L, stackMatrixV4Response.getCdh().size());

        assertEquals("6.1", stackMatrixV4Response.getCdh().get("6.1.0").getMinCM());
        assertEquals("6.1.0-1.cdh6.1.0.p0.770702", stackMatrixV4Response.getCdh().get("6.1.0").getVersion());
        assertNull(stackMatrixV4Response.getCdh().get("6.1.0").getClouderaManager().getRepository());
        assertNull(stackMatrixV4Response.getCdh().get("6.1.0").getClouderaManager().getVersion());

        assertEquals("2.6", stackMatrixV4Response.getHdf().get("3.1").getMinAmbari());
        assertEquals("3.1.2.0", stackMatrixV4Response.getHdf().get("3.1").getVersion());
        assertNull(stackMatrixV4Response.getHdf().get("3.1").getAmbari().getRepository());
        assertNull(stackMatrixV4Response.getHdf().get("3.1").getAmbari().getVersion());

        assertEquals("2.7", stackMatrixV4Response.getHdf().get("3.2").getMinAmbari());
        assertEquals("3.2.4.1", stackMatrixV4Response.getHdf().get("3.2").getVersion());
        assertNull(stackMatrixV4Response.getHdf().get("3.2").getAmbari().getRepository());
        assertNull(stackMatrixV4Response.getHdf().get("3.2").getAmbari().getVersion());

        assertEquals("2.5", stackMatrixV4Response.getHdp().get("2.6").getMinAmbari());
        assertEquals("2.6.5.0", stackMatrixV4Response.getHdp().get("2.6").getVersion());
        assertNull(stackMatrixV4Response.getHdp().get("2.6").getAmbari().getRepository());
        assertNull(stackMatrixV4Response.getHdp().get("2.6").getAmbari().getVersion());

        assertEquals("2.7", stackMatrixV4Response.getHdp().get("3.1").getMinAmbari());
        assertEquals("3.1.8.0", stackMatrixV4Response.getHdp().get("3.1").getVersion());
        assertNull(stackMatrixV4Response.getHdp().get("3.1").getAmbari().getRepository());
        assertNull(stackMatrixV4Response.getHdp().get("3.1").getAmbari().getVersion());
    }

    private void setupStackEntries() {
        Map<String, DefaultHDFInfo> hdfEntries = new HashMap<>();

        DefaultHDFInfo threeOneHdfInfo = getDefaultHDFInfo("2.6", "3.1.2.0");
        hdfEntries.put("3.1", threeOneHdfInfo);
        when(converterUtil.convert(threeOneHdfInfo, AmbariStackDescriptorV4Response.class)).thenReturn(getStackDescriptorResponse("2.6", "3.1.2.0"));

        DefaultHDFInfo threeTwoHdfInfo = getDefaultHDFInfo("2.7", "3.2.4.1");
        hdfEntries.put("3.2", threeTwoHdfInfo);
        when(converterUtil.convert(threeTwoHdfInfo, AmbariStackDescriptorV4Response.class)).thenReturn(getStackDescriptorResponse("2.7", "3.2.4.1"));

        when(defaultHDFEntries.getEntries()).thenReturn(hdfEntries);

        Map<String, DefaultHDPInfo> hdpEntries = new HashMap<>();

        DefaultHDPInfo twoSixHdpInfo = getDefaultHDPInfo("2.5", "2.6.5.0");
        hdpEntries.put("2.6", twoSixHdpInfo);
        when(converterUtil.convert(twoSixHdpInfo, AmbariStackDescriptorV4Response.class)).thenReturn(getStackDescriptorResponse("2.5", "2.6.5.0"));

        DefaultHDPInfo threeZeroHdpInfo = getDefaultHDPInfo("2.7", "3.0.1.0");
        hdpEntries.put("3.0", threeZeroHdpInfo);
        when(converterUtil.convert(threeZeroHdpInfo, AmbariStackDescriptorV4Response.class)).thenReturn(getStackDescriptorResponse("2.7", "3.0.1.0"));

        DefaultHDPInfo threeOneHdpInfo = getDefaultHDPInfo("2.7", "3.1.8.0");
        hdpEntries.put("3.1", threeOneHdpInfo);
        when(converterUtil.convert(threeOneHdpInfo, AmbariStackDescriptorV4Response.class)).thenReturn(getStackDescriptorResponse("2.7", "3.1.8.0"));

        when(defaultHDPEntries.getEntries()).thenReturn(hdpEntries);

        Map<String, DefaultCDHInfo> cdhEntries = new HashMap<>();

        DefaultCDHInfo cdhInfo = getDefaultCDHInfo("6.1", "6.1.0-1.cdh6.1.0.p0.770702");
        cdhEntries.put("6.1.0", cdhInfo);
        when(converterUtil.convert(cdhInfo, ClouderaManagerStackDescriptorV4Response.class))
                .thenReturn(getCMStackDescriptorResponse("6.1", "6.1.0-1.cdh6.1.0.p0.770702"));

        when(defaultCDHEntries.getEntries()).thenReturn(cdhEntries);
    }

    private void setupAmbariEntries() {
        Map<String, RepositoryInfo> ambariEntries = new HashMap<>();

        RepositoryInfo twoFiveAmbariInfo = getAmbariInfo("2.5.0.0");
        ambariEntries.put("2.5", twoFiveAmbariInfo);
        when(converterUtil.convert(twoFiveAmbariInfo, AmbariInfoV4Response.class)).thenReturn(getAmbariInfoResponse("2.5.0.0"));

        RepositoryInfo twoSixAmbariInfo = getAmbariInfo("2.6.1.0");
        ambariEntries.put("2.6", twoSixAmbariInfo);
        when(converterUtil.convert(twoSixAmbariInfo, AmbariInfoV4Response.class)).thenReturn(getAmbariInfoResponse("2.6.1.0"));

        RepositoryInfo twoSevenAmbariInfo = getAmbariInfo("2.7.0.3");
        ambariEntries.put("2.7", twoSevenAmbariInfo);
        when(converterUtil.convert(twoSevenAmbariInfo, AmbariInfoV4Response.class)).thenReturn(getAmbariInfoResponse("2.7.0.3"));

        when(defaultAmbariRepoService.getEntries()).thenReturn(ambariEntries);
    }
}