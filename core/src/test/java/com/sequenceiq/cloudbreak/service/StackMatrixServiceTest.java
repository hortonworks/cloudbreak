package com.sequenceiq.cloudbreak.service;

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

import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.StackRepoDetailsV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.ambari.ambarirepository.AmbariRepositoryV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.AmbariInfoV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.StackDescriptorV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.StackMatrixV4Response;
import com.sequenceiq.cloudbreak.api.util.ConverterUtil;
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
        when(converterUtil.convert(any(AmbariInfo.class), eq(AmbariInfoV4Response.class))).thenReturn(new AmbariInfoV4Response());

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

        DefaultHDFInfo threeOneHdfInfo = getDefaultHDFInfo("2.6", "3.1.2.0");
        hdfEntries.put("3.1", threeOneHdfInfo);
        when(converterUtil.convert(threeOneHdfInfo, StackDescriptorV4Response.class)).thenReturn(getStackDescriptorResponse("2.6", "3.1.2.0"));

        DefaultHDFInfo threeTwoHdfInfo = getDefaultHDFInfo("2.7", "3.2.4.1");
        hdfEntries.put("3.2", threeTwoHdfInfo);
        when(converterUtil.convert(threeTwoHdfInfo, StackDescriptorV4Response.class)).thenReturn(getStackDescriptorResponse("2.7", "3.2.4.1"));

        when(defaultHDFEntries.getEntries()).thenReturn(hdfEntries);

        Map<String, DefaultHDPInfo> hdpEntries = new HashMap<>();

        DefaultHDPInfo twoSixHdpInfo = getDefaultHDPInfo("2.5", "2.6.5.0");
        hdpEntries.put("2.6", twoSixHdpInfo);
        when(converterUtil.convert(twoSixHdpInfo, StackDescriptorV4Response.class)).thenReturn(getStackDescriptorResponse("2.5", "2.6.5.0"));

        DefaultHDPInfo threeZeroHdpInfo = getDefaultHDPInfo("2.7", "3.0.1.0");
        hdpEntries.put("3.0", threeZeroHdpInfo);
        when(converterUtil.convert(threeZeroHdpInfo, StackDescriptorV4Response.class)).thenReturn(getStackDescriptorResponse("2.7", "3.0.1.0"));

        DefaultHDPInfo threeOneHdpInfo = getDefaultHDPInfo("2.7", "3.1.8.0");
        hdpEntries.put("3.1", threeOneHdpInfo);
        when(converterUtil.convert(threeOneHdpInfo, StackDescriptorV4Response.class)).thenReturn(getStackDescriptorResponse("2.7", "3.1.8.0"));

        when(defaultHDPEntries.getEntries()).thenReturn(hdpEntries);
    }

    private void setupAmbariEntries() {
        Map<String, AmbariInfo> ambariEntries = new HashMap<>();

        AmbariInfo twoFiveAmbariInfo = getAmbariInfo("2.5.0.0");
        ambariEntries.put("2.5", twoFiveAmbariInfo);
        when(converterUtil.convert(twoFiveAmbariInfo, AmbariInfoV4Response.class)).thenReturn(getAmbariInfoResponse("2.5.0.0"));

        AmbariInfo twoSixAmbariInfo = getAmbariInfo("2.6.1.0");
        ambariEntries.put("2.6", twoSixAmbariInfo);
        when(converterUtil.convert(twoSixAmbariInfo, AmbariInfoV4Response.class)).thenReturn(getAmbariInfoResponse("2.6.1.0"));

        AmbariInfo twoSevenAmbariInfo = getAmbariInfo("2.7.0.3");
        ambariEntries.put("2.7", twoSevenAmbariInfo);
        when(converterUtil.convert(twoSevenAmbariInfo, AmbariInfoV4Response.class)).thenReturn(getAmbariInfoResponse("2.7.0.3"));

        when(defaultAmbariRepoService.getEntries()).thenReturn(ambariEntries);
    }

    private AmbariInfo getAmbariInfo(String version) {
        AmbariInfo twoFiveAmbariInfo = new AmbariInfo();
        twoFiveAmbariInfo.setVersion(version);
        twoFiveAmbariInfo.setRepo(getAmbariRepo(version));
        return twoFiveAmbariInfo;
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

    private AmbariInfoV4Response getAmbariInfoResponse(String version) {
        AmbariInfoV4Response ambariInfoV4Response = new AmbariInfoV4Response();
        ambariInfoV4Response.setVersion(version);
        ambariInfoV4Response.setRepository(getAmbariRepositoryResponse(version));
        return ambariInfoV4Response;
    }

    private Map<String, AmbariRepositoryV4Response> getAmbariRepositoryResponse(String version) {
        Map<String, AmbariRepositoryV4Response> ambariRepoResponse = new HashMap<>();

        AmbariRepositoryV4Response redhat6RepoResponse = new AmbariRepositoryV4Response();
        redhat6RepoResponse.setVersion(version);
        redhat6RepoResponse.setBaseUrl("http://redhat6-base/" + version);
        redhat6RepoResponse.setGpgKeyUrl("http://redhat6-gpg/" + version);
        ambariRepoResponse.put("redhat6", redhat6RepoResponse);

        AmbariRepositoryV4Response redhat7RepoResponse = new AmbariRepositoryV4Response();
        redhat7RepoResponse.setVersion(version);
        redhat7RepoResponse.setBaseUrl("http://redhat7-base/" + version);
        redhat7RepoResponse.setGpgKeyUrl("http://redhat7-gpg/" + version);
        ambariRepoResponse.put("redhat7", redhat7RepoResponse);

        return ambariRepoResponse;
    }

    private DefaultHDFInfo getDefaultHDFInfo(String minAmbari, String version) {
        DefaultHDFInfo defaultHDFInfo = new DefaultHDFInfo();
        defaultHDFInfo.setMinAmbari(minAmbari);
        defaultHDFInfo.setVersion(version);
        defaultHDFInfo.setRepo(getStackRepoDetails(version));
        return defaultHDFInfo;
    }

    private DefaultHDPInfo getDefaultHDPInfo(String minAmbari, String version) {
        DefaultHDPInfo defaultHDPInfo = new DefaultHDPInfo();
        defaultHDPInfo.setMinAmbari(minAmbari);
        defaultHDPInfo.setVersion(version);
        defaultHDPInfo.setRepo(getStackRepoDetails(version));
        return defaultHDPInfo;
    }

    private DefaultStackRepoDetails getStackRepoDetails(String version) {
        DefaultStackRepoDetails stackRepoDetails = new DefaultStackRepoDetails();
        stackRepoDetails.setHdpVersion(version);
        stackRepoDetails.setStack(getStackRepo(version));
        stackRepoDetails.setUtil(getUtilRepo(version));
        return stackRepoDetails;
    }

    private Map<String, String> getStackRepo(String version) {
        Map<String, String> stackRepo = new HashMap<>();
        stackRepo.put("centos7", "http://centos7-repo/" + version);
        stackRepo.put("centos6", "http://centos6-repo/" + version);
        return stackRepo;
    }

    private Map<String, String> getUtilRepo(String version) {
        Map<String, String> utilRepo = new HashMap<>();
        utilRepo.put("centos7", "http://centos7-util-repo/" + version);
        utilRepo.put("centos6", "http://centos6-util-repo/" + version);
        return utilRepo;
    }

    private StackDescriptorV4Response getStackDescriptorResponse(String minAmbari, String version) {
        StackDescriptorV4Response stackDescriptorV4Response = new StackDescriptorV4Response();
        stackDescriptorV4Response.setMinAmbari(minAmbari);
        stackDescriptorV4Response.setVersion(version);
        stackDescriptorV4Response.setRepo(getStackRepoDetailsResponse(version));
        return stackDescriptorV4Response;
    }

    private StackRepoDetailsV4Response getStackRepoDetailsResponse(String version) {
        StackRepoDetailsV4Response stackRepoDetailsV4Response = new StackRepoDetailsV4Response();
        stackRepoDetailsV4Response.setStack(getStackRepo(version));
        stackRepoDetailsV4Response.setUtil(getUtilRepo(version));
        return stackRepoDetailsV4Response;
    }

}