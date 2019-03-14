package com.sequenceiq.cloudbreak;

import java.util.HashMap;
import java.util.Map;

import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.AmbariStackRepoDetailsV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.ClouderaManagerStackRepoDetailsV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.ambari.ambarirepository.AmbariRepositoryV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.clouderamanager.ClouderaManagerRepositoryV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.AmbariInfoV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.AmbariStackDescriptorV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.ClouderaManagerInfoV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.ClouderaManagerStackDescriptorV4Response;
import com.sequenceiq.cloudbreak.cloud.model.catalog.StackRepoDetails;
import com.sequenceiq.cloudbreak.cloud.model.component.AmbariDefaultStackRepoDetails;
import com.sequenceiq.cloudbreak.cloud.model.component.ClouderaManagerDefaultStackRepoDetails;
import com.sequenceiq.cloudbreak.cloud.model.component.DefaultCDHInfo;
import com.sequenceiq.cloudbreak.cloud.model.component.DefaultHDFInfo;
import com.sequenceiq.cloudbreak.cloud.model.component.DefaultHDPInfo;
import com.sequenceiq.cloudbreak.cloud.model.component.RepositoryDetails;
import com.sequenceiq.cloudbreak.cloud.model.component.RepositoryInfo;
import com.sequenceiq.cloudbreak.cloud.model.component.StackType;

public class RepoTestUtil {

    private RepoTestUtil() {
    }

    public static RepositoryInfo getAmbariInfo(String version) {
        RepositoryInfo twoFiveAmbariInfo = new RepositoryInfo();
        twoFiveAmbariInfo.setVersion(version);
        twoFiveAmbariInfo.setRepo(getAmbariRepo(version));
        return twoFiveAmbariInfo;
    }

    public static RepositoryInfo getClouderaManagerInfo(String version) {
        RepositoryInfo cmInfo = new RepositoryInfo();
        cmInfo.setVersion(version);
        cmInfo.setRepo(getCMRepo(version));
        return cmInfo;
    }

    public static Map<String, RepositoryDetails> getAmbariRepo(String version) {
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

    public static Map<String, RepositoryDetails> getCMRepo(String version) {
        Map<String, RepositoryDetails> cmRepo = new HashMap<>();

        RepositoryDetails redhat7RepositoryDetails = new RepositoryDetails();
        redhat7RepositoryDetails.setBaseurl("http://redhat7-base/" + version);
        redhat7RepositoryDetails.setGpgkey("http://redhat7-gpg/" + version);
        cmRepo.put("redhat7", redhat7RepositoryDetails);

        return cmRepo;
    }

    public static AmbariInfoV4Response getAmbariInfoResponse(String version) {
        AmbariInfoV4Response ambariInfoV4Response = new AmbariInfoV4Response();
        ambariInfoV4Response.setVersion(version);
        ambariInfoV4Response.setRepository(getAmbariRepositoryResponse(version));
        return ambariInfoV4Response;
    }

    public static ClouderaManagerInfoV4Response getClouderaManagerInfoResponse(String version) {
        ClouderaManagerInfoV4Response cmResponse = new ClouderaManagerInfoV4Response();
        cmResponse.setVersion(version);
        cmResponse.setRepository(getClouderaManagerRepositoryResponse(version));
        return cmResponse;
    }

    public static Map<String, AmbariRepositoryV4Response> getAmbariRepositoryResponse(String version) {
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

    public static Map<String, ClouderaManagerRepositoryV4Response> getClouderaManagerRepositoryResponse(String version) {
        Map<String, ClouderaManagerRepositoryV4Response> response = new HashMap<>();

        ClouderaManagerRepositoryV4Response redhat7RepoResponse = new ClouderaManagerRepositoryV4Response();
        redhat7RepoResponse.setVersion(version);
        redhat7RepoResponse.setBaseUrl("http://redhat7-base/" + version);
        redhat7RepoResponse.setGpgKeyUrl("http://redhat7-gpg/" + version);
        response.put("redhat7", redhat7RepoResponse);

        return response;
    }

    public static DefaultCDHInfo getDefaultCDHInfo(String minCM, String version) {
        DefaultCDHInfo defaultCDHInfo = new DefaultCDHInfo();
        defaultCDHInfo.setMinCM(minCM);
        defaultCDHInfo.setVersion(version);
        defaultCDHInfo.setRepo(getCMStackRepoDetails(version));
        return defaultCDHInfo;
    }

    public static DefaultHDFInfo getDefaultHDFInfo(String minAmbari, String version) {
        DefaultHDFInfo defaultHDFInfo = new DefaultHDFInfo();
        defaultHDFInfo.setMinAmbari(minAmbari);
        defaultHDFInfo.setVersion(version);
        defaultHDFInfo.setRepo(getStackRepoDetails(version));
        return defaultHDFInfo;
    }

    public static DefaultHDPInfo getDefaultHDPInfo(String minAmbari, String version) {
        DefaultHDPInfo defaultHDPInfo = new DefaultHDPInfo();
        defaultHDPInfo.setMinAmbari(minAmbari);
        defaultHDPInfo.setVersion(version);
        defaultHDPInfo.setRepo(getStackRepoDetails(version));
        return defaultHDPInfo;
    }

    public static ClouderaManagerDefaultStackRepoDetails getCMStackRepoDetails(String version) {
        ClouderaManagerDefaultStackRepoDetails stackRepoDetails = new ClouderaManagerDefaultStackRepoDetails();
        stackRepoDetails.setCdhVersion(version);
        stackRepoDetails.setStack(getStackRepo(version, StackType.CDH));
        return stackRepoDetails;
    }

    public static AmbariDefaultStackRepoDetails getStackRepoDetails(String version) {
        AmbariDefaultStackRepoDetails stackRepoDetails = new AmbariDefaultStackRepoDetails();
        stackRepoDetails.setHdpVersion(version);
        stackRepoDetails.setStack(getStackRepo(version, StackType.HDP));
        stackRepoDetails.setUtil(getUtilRepo(version));
        return stackRepoDetails;
    }

    public static Map<String, String> getStackRepo(String version, StackType stackType) {
        Map<String, String> stackRepo = new HashMap<>();
        stackRepo.put("redhat7", "http://redhat7-repo/" + version);
        stackRepo.put("centos7", "http://centos7-repo/" + version);
        stackRepo.put("centos6", "http://centos6-repo/" + version);
        stackRepo.put(StackRepoDetails.REPO_ID_TAG, stackType.name());
        return stackRepo;
    }

    public static Map<String, String> getUtilRepo(String version) {
        Map<String, String> utilRepo = new HashMap<>();
        utilRepo.put("redhat7", "http://redhat7-util-repo/" + version);
        utilRepo.put("centos7", "http://centos7-util-repo/" + version);
        utilRepo.put("centos6", "http://centos6-util-repo/" + version);
        return utilRepo;
    }

    public static AmbariStackDescriptorV4Response getStackDescriptorResponse(String minAmbari, String version) {
        AmbariStackDescriptorV4Response ambariStackDescriptorV4Response = new AmbariStackDescriptorV4Response();
        ambariStackDescriptorV4Response.setMinAmbari(minAmbari);
        ambariStackDescriptorV4Response.setVersion(version);
        ambariStackDescriptorV4Response.setRepository(getStackRepoDetailsResponse(version));
        return ambariStackDescriptorV4Response;
    }

    public static ClouderaManagerStackDescriptorV4Response getCMStackDescriptorResponse(String minCM, String version) {
        ClouderaManagerStackDescriptorV4Response stackDescriptorV4Response = new ClouderaManagerStackDescriptorV4Response();
        stackDescriptorV4Response.setMinCM(minCM);
        stackDescriptorV4Response.setVersion(version);
        stackDescriptorV4Response.setRepository(getCMStackRepoDetailsResponse(version));
        return stackDescriptorV4Response;
    }

    public static AmbariStackRepoDetailsV4Response getStackRepoDetailsResponse(String version) {
        AmbariStackRepoDetailsV4Response ambariStackRepoDetailsV4Response = new AmbariStackRepoDetailsV4Response();
        ambariStackRepoDetailsV4Response.setStack(getStackRepo(version, StackType.HDP));
        ambariStackRepoDetailsV4Response.setUtil(getUtilRepo(version));
        return ambariStackRepoDetailsV4Response;
    }

    public static ClouderaManagerStackRepoDetailsV4Response getCMStackRepoDetailsResponse(String version) {
        ClouderaManagerStackRepoDetailsV4Response cmStackRepoDetailsV4Response = new ClouderaManagerStackRepoDetailsV4Response();
        cmStackRepoDetailsV4Response.setStack(getStackRepo(version, StackType.CDH));
        return cmStackRepoDetailsV4Response;
    }
}
