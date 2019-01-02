package com.sequenceiq.cloudbreak.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.StackMatrixV4Response;
import com.sequenceiq.cloudbreak.cloud.model.component.AmbariInfo;
import com.sequenceiq.cloudbreak.cloud.model.component.AmbariRepoDetails;
import com.sequenceiq.cloudbreak.cloud.model.component.DefaultHDFEntries;
import com.sequenceiq.cloudbreak.cloud.model.component.DefaultHDFInfo;
import com.sequenceiq.cloudbreak.cloud.model.component.DefaultHDPEntries;
import com.sequenceiq.cloudbreak.cloud.model.component.DefaultHDPInfo;
import com.sequenceiq.cloudbreak.cloud.model.component.DefaultStackRepoDetails;

@RunWith(MockitoJUnitRunner.class)
public class StackMatrixServiceTest {

    @Mock
    private DefaultHDFEntries defaultHDFEntries;

    @Mock
    private DefaultHDPEntries defaultHDPEntries;

    @Mock
    private DefaultAmbariRepoService defaultAmbariRepoService;

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

        StackMatrixV4Response stackMatrixV4Response = stackMatrixService.getStackMatrix();

        assertEquals(2L, stackMatrixV4Response.getHdf().size());
        assertEquals(3L, stackMatrixV4Response.getHdp().size());

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

        DefaultHDFInfo threeOneHdfInfo = new DefaultHDFInfo();
        threeOneHdfInfo.setMinAmbari("2.6");
        threeOneHdfInfo.setVersion("3.1.2.0");
        threeOneHdfInfo.setRepo(getStackRepoDetails("3.1.2.0"));
        hdfEntries.put("3.1", threeOneHdfInfo);

        DefaultHDFInfo threeTwoHdfInfo = new DefaultHDFInfo();
        threeTwoHdfInfo.setMinAmbari("2.7");
        threeTwoHdfInfo.setVersion("3.2.4.1");
        threeTwoHdfInfo.setRepo(getStackRepoDetails("3.2.4.1"));
        hdfEntries.put("3.2", threeTwoHdfInfo);

        when(defaultHDFEntries.getEntries()).thenReturn(hdfEntries);

        Map<String, DefaultHDPInfo> hdpEntries = new HashMap<>();

        DefaultHDPInfo twoSixHdpInfo = new DefaultHDPInfo();
        twoSixHdpInfo.setMinAmbari("2.5");
        twoSixHdpInfo.setVersion("2.6.5.0");
        twoSixHdpInfo.setRepo(getStackRepoDetails("2.6.5.0"));
        hdpEntries.put("2.6", twoSixHdpInfo);

        DefaultHDPInfo threeZeroHdpInfo = new DefaultHDPInfo();
        threeZeroHdpInfo.setMinAmbari("2.7");
        threeZeroHdpInfo.setVersion("3.0.1.0");
        threeZeroHdpInfo.setRepo(getStackRepoDetails("3.0.1.0"));
        hdpEntries.put("3.0", threeZeroHdpInfo);

        DefaultHDPInfo threeOneHdpInfo = new DefaultHDPInfo();
        threeOneHdpInfo.setMinAmbari("2.7");
        threeOneHdpInfo.setVersion("3.1.8.0");
        threeOneHdpInfo.setRepo(getStackRepoDetails("3.1.8.0"));
        hdpEntries.put("3.1", threeOneHdpInfo);

        when(defaultHDPEntries.getEntries()).thenReturn(hdpEntries);
    }

    private void setupAmbariEntries() {
        Map<String, AmbariInfo> ambariEntries = new HashMap<>();

        AmbariInfo twoFiveAmbariInfo = new AmbariInfo();
        twoFiveAmbariInfo.setVersion("2.5.0.0");
        twoFiveAmbariInfo.setRepo(getAmbariRepo("2.5.0.0"));
        ambariEntries.put("2.5", twoFiveAmbariInfo);

        AmbariInfo twoSixAmbariInfo = new AmbariInfo();
        twoSixAmbariInfo.setVersion("2.6.1.0");
        twoSixAmbariInfo.setRepo(getAmbariRepo("2.6.1.0"));
        ambariEntries.put("2.6", twoSixAmbariInfo);

        AmbariInfo twoSevenAmbariInfo = new AmbariInfo();
        twoSevenAmbariInfo.setVersion("2.7.0.3");
        twoSevenAmbariInfo.setRepo(getAmbariRepo("2.7.0.3"));
        ambariEntries.put("2.7", twoSevenAmbariInfo);

        when(defaultAmbariRepoService.getEntries()).thenReturn(ambariEntries);
    }

    private Map<String, AmbariRepoDetails> getAmbariRepo(String version) {
        Map<String, AmbariRepoDetails> ambariRepo = new HashMap<>();

        AmbariRepoDetails redhat6RepoDetails = new AmbariRepoDetails();
        redhat6RepoDetails.setBaseurl("http://redhat6-base/" + version);
        redhat6RepoDetails.setGpgkey("http://redhat6-gpg/" + version);
        ambariRepo.put("redhat6", redhat6RepoDetails);

        AmbariRepoDetails redhat7RepoDetails = new AmbariRepoDetails();
        redhat7RepoDetails.setBaseurl("http://redhat7-base/" + version);
        redhat7RepoDetails.setGpgkey("http://redhat7-gpg/" + version);
        ambariRepo.put("redhat7", redhat7RepoDetails);

        return ambariRepo;
    }

    private DefaultStackRepoDetails getStackRepoDetails(String version) {
        DefaultStackRepoDetails stackRepoDetails = new DefaultStackRepoDetails();
        stackRepoDetails.setHdpVersion(version);
        Map<String, String> stackRepo = new HashMap<>();
        stackRepo.put("centos7", "http://centos7-repo/" + version);
        stackRepo.put("centos6", "http://centos6-repo/" + version);
        stackRepoDetails.setStack(stackRepo);
        Map<String, String> utilRepo = new HashMap<>();
        utilRepo.put("centos7", "http://centos7-util-repo/" + version);
        utilRepo.put("centos6", "http://centos6-util-repo/" + version);
        stackRepoDetails.setUtil(utilRepo);
        return stackRepoDetails;
    }
}