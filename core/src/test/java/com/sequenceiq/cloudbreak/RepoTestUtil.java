package com.sequenceiq.cloudbreak;

import java.util.HashMap;
import java.util.Map;

import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.ClouderaManagerStackRepoDetailsV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.clouderamanager.ClouderaManagerRepositoryV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.ClouderaManagerInfoV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.ClouderaManagerStackDescriptorV4Response;
import com.sequenceiq.cloudbreak.cloud.model.catalog.StackRepoDetails;
import com.sequenceiq.cloudbreak.cloud.model.component.ClouderaManagerDefaultStackRepoDetails;
import com.sequenceiq.cloudbreak.cloud.model.component.DefaultCDHInfo;
import com.sequenceiq.cloudbreak.cloud.model.component.RepositoryDetails;
import com.sequenceiq.cloudbreak.cloud.model.component.RepositoryInfo;
import com.sequenceiq.cloudbreak.cloud.model.component.StackType;

public class RepoTestUtil {

    private RepoTestUtil() {
    }

    public static RepositoryInfo getClouderaManagerInfo(String version) {
        RepositoryInfo cmInfo = new RepositoryInfo();
        cmInfo.setVersion(version);
        cmInfo.setRepo(getCMRepo(version));
        return cmInfo;
    }

    public static Map<String, RepositoryDetails> getCMRepo(String version) {
        Map<String, RepositoryDetails> cmRepo = new HashMap<>();

        RepositoryDetails redhat7RepositoryDetails = new RepositoryDetails();
        redhat7RepositoryDetails.setBaseurl("http://redhat7-base/" + version);
        redhat7RepositoryDetails.setGpgkey("http://redhat7-gpg/" + version);
        cmRepo.put("redhat7", redhat7RepositoryDetails);

        return cmRepo;
    }

    public static ClouderaManagerInfoV4Response getClouderaManagerInfoResponse(String version) {
        ClouderaManagerInfoV4Response cmResponse = new ClouderaManagerInfoV4Response();
        cmResponse.setVersion(version);
        cmResponse.setRepository(getClouderaManagerRepositoryResponse(version));
        return cmResponse;
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

    public static ClouderaManagerDefaultStackRepoDetails getCMStackRepoDetails(String version) {
        ClouderaManagerDefaultStackRepoDetails stackRepoDetails = new ClouderaManagerDefaultStackRepoDetails();
        stackRepoDetails.setCdhVersion(version);
        stackRepoDetails.setStack(getStackRepo(version, StackType.CDH));
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

    public static ClouderaManagerStackDescriptorV4Response getCMStackDescriptorResponse(String minCM, String version) {
        ClouderaManagerStackDescriptorV4Response stackDescriptorV4Response = new ClouderaManagerStackDescriptorV4Response();
        stackDescriptorV4Response.setMinCM(minCM);
        stackDescriptorV4Response.setVersion(version);
        stackDescriptorV4Response.setRepository(getCMStackRepoDetailsResponse(version));
        return stackDescriptorV4Response;
    }

    public static ClouderaManagerStackRepoDetailsV4Response getCMStackRepoDetailsResponse(String version) {
        ClouderaManagerStackRepoDetailsV4Response cmStackRepoDetailsV4Response = new ClouderaManagerStackRepoDetailsV4Response();
        cmStackRepoDetailsV4Response.setStack(getStackRepo(version, StackType.CDH));
        return cmStackRepoDetailsV4Response;
    }
}
