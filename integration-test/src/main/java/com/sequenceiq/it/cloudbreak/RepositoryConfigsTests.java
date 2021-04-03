package com.sequenceiq.it.cloudbreak;

import org.testng.Assert;
import org.testng.annotations.Ignore;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.model.repositoryconfig.RepoConfigValidationRequest;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakTest;
import com.sequenceiq.it.cloudbreak.newway.RepositoryConfigs;

public class RepositoryConfigsTests extends CloudbreakTest {
    private static final String VALID_AMBARI_BASE_URL = "http://public-repo-1.hortonworks.com/ambari/centos7/2.x/updates/2.6.1.3";

    private static final String VALID_AMBARI_GPGKEY_URL = "http://public-repo-1.hortonworks.com/ambari/centos7/RPM-GPG-KEY/RPM-GPG-KEY-Jenkins";

    private static final String VALID_MPACK_URL = "http://private-repo-1.hortonworks.com/HDP/ubuntu16/3.x/updates/3.1.6.0-77/HDP-3.1.6.0-77.xml";

    private static final String VALID_STACK_BASE_URL = "http://public-repo-1.hortonworks.com/HDP/centos6/2.x/updates/2.5.5.0/";

    private static final String VALID_UTIL_BASE_URL = "http://public-repo-1.hortonworks.com/HDP-UTILS-1.1.0.22/repos/centos7";

    private static final String VALID_VERSION_DEF_FILE_URL = "http://public-repo-1.hortonworks.com/HDP/centos7/2.x/updates/2.6.4.0/HDP-2.6.4.0-91.xml";

    private static final String INVALID_URL = "www.google.com";

    @Test
    @Ignore
    public void testValidAmbariBaseUrl() throws Exception {
        given(CloudbreakClient.created());
        given(RepositoryConfigs.request(createRepoConfigValidationReqest(VALID_AMBARI_BASE_URL, "", "", "", "",
                "")));
        when(RepositoryConfigs.post());
        then(RepositoryConfigs.assertThis(
                (repositoryConfigs, t) -> Assert.assertTrue(repositoryConfigs.getRepoConfigsResponse().getAmbariBaseUrl()))
        );
    }

    @Test
    @Ignore
    public void testValidAmbariGpgKeyUrl() throws Exception {
        given(CloudbreakClient.created());
        given(RepositoryConfigs.request(createRepoConfigValidationReqest("", VALID_AMBARI_GPGKEY_URL, "", "", "",
                "")));
        when(RepositoryConfigs.post());
        then(RepositoryConfigs.assertThis(
                (repositoryConfigs, t) -> Assert.assertTrue(repositoryConfigs.getRepoConfigsResponse().getAmbariGpgKeyUrl()))
        );
    }

    @Test()
    @Ignore
    public void testValidMpackUrl() throws Exception {
        given(CloudbreakClient.created());
        given(RepositoryConfigs.request(createRepoConfigValidationReqest("", "", VALID_MPACK_URL, "", "", "")));
        when(RepositoryConfigs.post());
        then(RepositoryConfigs.assertThis(
                (repositoryConfigs, t) -> Assert.assertTrue(repositoryConfigs.getRepoConfigsResponse().getMpackUrl()))
        );
    }

    @Test
    @Ignore
    public void testValidStackBaseUrl() throws Exception {
        given(CloudbreakClient.created());
        given(RepositoryConfigs.request(createRepoConfigValidationReqest("", "", "", VALID_STACK_BASE_URL, "",
                "")));
        when(RepositoryConfigs.post());
        then(RepositoryConfigs.assertThis(
                (repositoryConfigs, t) -> Assert.assertTrue(repositoryConfigs.getRepoConfigsResponse().getStackBaseURL()))
        );
    }

    @Test
    @Ignore
    public void testValidUtilBaseUrl() throws Exception {
        given(CloudbreakClient.created());
        given(RepositoryConfigs.request(createRepoConfigValidationReqest("", "", "", "", VALID_UTIL_BASE_URL,
                "")));
        when(RepositoryConfigs.post());
        then(RepositoryConfigs.assertThis(
                (repositoryConfigs, t) -> Assert.assertTrue(repositoryConfigs.getRepoConfigsResponse().getUtilsBaseURL()))
        );
    }

    @Test
    @Ignore
    public void testValidVersionDefFileUrl() throws Exception {
        given(CloudbreakClient.created());
        given(RepositoryConfigs.request(createRepoConfigValidationReqest("", "", "", "", "",
                VALID_VERSION_DEF_FILE_URL)));
        when(RepositoryConfigs.post());
        then(RepositoryConfigs.assertThis(
                (repositoryConfigs, t) -> Assert.assertTrue(repositoryConfigs.getRepoConfigsResponse().getVersionDefinitionFileUrl()))
        );
    }

    @Test
    @Ignore
    public void testValidRepoConfigsAll() throws Exception {
        given(CloudbreakClient.created());
        given(RepositoryConfigs.request(createRepoConfigValidationReqest(VALID_AMBARI_BASE_URL, VALID_AMBARI_GPGKEY_URL, VALID_MPACK_URL, VALID_STACK_BASE_URL,
                VALID_UTIL_BASE_URL, VALID_VERSION_DEF_FILE_URL)));
        when(RepositoryConfigs.post());
        then(RepositoryConfigs.assertAllTrue());
    }

    @Test
    @Ignore
    public void testInvalidRepoConfigsAllEmpty() throws Exception {
        given(CloudbreakClient.created());
        given(RepositoryConfigs.request(createRepoConfigValidationReqest("", "", "", "", "",
                "")));
        when(RepositoryConfigs.post());
        then(RepositoryConfigs.assertAllNull());
    }

    @Test
    @Ignore
    public void testInvalidRepoConfigsAllInvalid() throws Exception {
        given(CloudbreakClient.created());
        given(RepositoryConfigs.request(createRepoConfigValidationReqest("a", "a", "a", "a", "a",
                "a")));
        when(RepositoryConfigs.post());
        then(RepositoryConfigs.assertAllFalse());
    }

    @Test
    @Ignore
    public void testInvalidRepoConfigsAllNotAvailable() throws Exception {
        given(CloudbreakClient.created());
        given(RepositoryConfigs.request(createRepoConfigValidationReqest(INVALID_URL, INVALID_URL, INVALID_URL, INVALID_URL, INVALID_URL, INVALID_URL)));
        when(RepositoryConfigs.post());
        then(RepositoryConfigs.assertAllFalse());
    }

    private RepoConfigValidationRequest createRepoConfigValidationReqest(String ambariBaseUrl, String ambariGpgKeyUrl, String mPackUrl,
            String stackBaseUrl, String utilsBaseUrl, String versionDefinitionFileUrl) {
        RepoConfigValidationRequest repoConfigValidationRequest = new RepoConfigValidationRequest();
        repoConfigValidationRequest.setAmbariBaseUrl(ambariBaseUrl);
        repoConfigValidationRequest.setAmbariGpgKeyUrl(ambariGpgKeyUrl);
        repoConfigValidationRequest.setMpackUrl(mPackUrl);
        repoConfigValidationRequest.setStackBaseURL(stackBaseUrl);
        repoConfigValidationRequest.setUtilsBaseURL(utilsBaseUrl);
        repoConfigValidationRequest.setVersionDefinitionFileUrl(versionDefinitionFileUrl);
        return repoConfigValidationRequest;
    }
}