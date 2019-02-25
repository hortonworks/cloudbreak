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

import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.AmbariStackRepoDetailsV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.ClouderaManagerStackRepoDetailsV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.ambari.ambarirepository.AmbariRepositoryV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.clouderamanager.ClouderaManagerRepositoryV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.AmbariInfoV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.AmbariStackDescriptorV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.ClouderaManagerInfoV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.ClouderaManagerStackDescriptorV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.StackMatrixV4Response;
import com.sequenceiq.cloudbreak.api.util.ConverterUtil;
import com.sequenceiq.cloudbreak.cloud.model.component.AmbariDefaultStackRepoDetails;
import com.sequenceiq.cloudbreak.cloud.model.component.ClouderaManagerDefaultStackRepoDetails;
import com.sequenceiq.cloudbreak.cloud.model.component.DefaultCDHEntries;
import com.sequenceiq.cloudbreak.cloud.model.component.DefaultCDHInfo;
import com.sequenceiq.cloudbreak.cloud.model.component.DefaultHDFEntries;
import com.sequenceiq.cloudbreak.cloud.model.component.DefaultHDFInfo;
import com.sequenceiq.cloudbreak.cloud.model.component.DefaultHDPEntries;
import com.sequenceiq.cloudbreak.cloud.model.component.DefaultHDPInfo;
import com.sequenceiq.cloudbreak.cloud.model.component.RepositoryDetails;
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

    private RepositoryInfo getAmbariInfo(String version) {
        RepositoryInfo twoFiveAmbariInfo = new RepositoryInfo();
        twoFiveAmbariInfo.setVersion(version);
        twoFiveAmbariInfo.setRepo(getAmbariRepo(version));
        return twoFiveAmbariInfo;
    }

    private RepositoryInfo getClouderaManagerInfo(String version) {
        RepositoryInfo cmInfo = new RepositoryInfo();
        cmInfo.setVersion(version);
        cmInfo.setRepo(getCMRepo(version));
        return cmInfo;
    }

    private Map<String, RepositoryDetails> getAmbariRepo(String version) {
        Map<String, RepositoryDetails> ambariRepo = new HashMap<>();

        RepositoryDetails redhat6RepositoryDetails = new RepositoryDetails();
        redhat6RepositoryDetails.setBaseurl("http://redhat6-base/" + version);
        redhat6RepositoryDetails.setGpgkey("http://redhat6-gpg/" + version);
        ambariRepo.put("redhat6", redhat6RepositoryDetails);

        RepositoryDetails redhat7RepositoryDetails = new RepositoryDetails();
        redhat7RepositoryDetails.setBaseurl("http://redhat7-base/" + version);
        redhat7RepositoryDetails.setGpgkey("http://redhat7-gpg/" + version);
        ambariRepo.put("redhat7", redhat7RepositoryDetails);

        return ambariRepo;
    }

    private Map<String, RepositoryDetails> getCMRepo(String version) {
        Map<String, RepositoryDetails> cmRepo = new HashMap<>();

        RepositoryDetails redhat7RepositoryDetails = new RepositoryDetails();
        redhat7RepositoryDetails.setBaseurl("http://redhat7-base/" + version);
        redhat7RepositoryDetails.setGpgkey("http://redhat7-gpg/" + version);
        cmRepo.put("redhat7", redhat7RepositoryDetails);

        return cmRepo;
    }

    private AmbariInfoV4Response getAmbariInfoResponse(String version) {
        AmbariInfoV4Response ambariInfoV4Response = new AmbariInfoV4Response();
        ambariInfoV4Response.setVersion(version);
        ambariInfoV4Response.setRepository(getAmbariRepositoryResponse(version));
        return ambariInfoV4Response;
    }

    private ClouderaManagerInfoV4Response getClouderaManagerInfoResponse(String version) {
        ClouderaManagerInfoV4Response cmResponse = new ClouderaManagerInfoV4Response();
        cmResponse.setVersion(version);
        cmResponse.setRepository(getClouderaManagerRepositoryResponse(version));
        return cmResponse;
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

    private Map<String, ClouderaManagerRepositoryV4Response> getClouderaManagerRepositoryResponse(String version) {
        Map<String, ClouderaManagerRepositoryV4Response> response = new HashMap<>();

        ClouderaManagerRepositoryV4Response redhat7RepoResponse = new ClouderaManagerRepositoryV4Response();
        redhat7RepoResponse.setVersion(version);
        redhat7RepoResponse.setBaseUrl("http://redhat7-base/" + version);
        redhat7RepoResponse.setGpgKeyUrl("http://redhat7-gpg/" + version);
        response.put("redhat7", redhat7RepoResponse);

        return response;
    }

    private DefaultCDHInfo getDefaultCDHInfo(String minCM, String version) {
        DefaultCDHInfo defaultCDHInfo = new DefaultCDHInfo();
        defaultCDHInfo.setMinCM(minCM);
        defaultCDHInfo.setVersion(version);
        defaultCDHInfo.setRepo(getCMStackRepoDetails(version));
        return defaultCDHInfo;
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

    private ClouderaManagerDefaultStackRepoDetails getCMStackRepoDetails(String version) {
        ClouderaManagerDefaultStackRepoDetails stackRepoDetails = new ClouderaManagerDefaultStackRepoDetails();
        stackRepoDetails.setCdhVersion(version);
        stackRepoDetails.setStack(getStackRepo(version));
        return stackRepoDetails;
    }

    private AmbariDefaultStackRepoDetails getStackRepoDetails(String version) {
        AmbariDefaultStackRepoDetails stackRepoDetails = new AmbariDefaultStackRepoDetails();
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

    private AmbariStackDescriptorV4Response getStackDescriptorResponse(String minAmbari, String version) {
        AmbariStackDescriptorV4Response ambariStackDescriptorV4Response = new AmbariStackDescriptorV4Response();
        ambariStackDescriptorV4Response.setMinAmbari(minAmbari);
        ambariStackDescriptorV4Response.setVersion(version);
        ambariStackDescriptorV4Response.setRepository(getStackRepoDetailsResponse(version));
        return ambariStackDescriptorV4Response;
    }

    private ClouderaManagerStackDescriptorV4Response getCMStackDescriptorResponse(String minCM, String version) {
        ClouderaManagerStackDescriptorV4Response stackDescriptorV4Response = new ClouderaManagerStackDescriptorV4Response();
        stackDescriptorV4Response.setMinCM(minCM);
        stackDescriptorV4Response.setVersion(version);
        stackDescriptorV4Response.setRepository(getCMStackRepoDetailsResponse(version));
        return stackDescriptorV4Response;
    }

    private AmbariStackRepoDetailsV4Response getStackRepoDetailsResponse(String version) {
        AmbariStackRepoDetailsV4Response ambariStackRepoDetailsV4Response = new AmbariStackRepoDetailsV4Response();
        ambariStackRepoDetailsV4Response.setStack(getStackRepo(version));
        ambariStackRepoDetailsV4Response.setUtil(getUtilRepo(version));
        return ambariStackRepoDetailsV4Response;
    }

    private ClouderaManagerStackRepoDetailsV4Response getCMStackRepoDetailsResponse(String version) {
        ClouderaManagerStackRepoDetailsV4Response cmStackRepoDetailsV4Response = new ClouderaManagerStackRepoDetailsV4Response();
        cmStackRepoDetailsV4Response.setStack(getStackRepo(version));
        return cmStackRepoDetailsV4Response;
    }

}