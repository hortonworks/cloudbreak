package com.sequenceiq.cloudbreak.service.cluster;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import jakarta.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.util.requests.RepoConfigValidationV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.RepoConfigValidationV4Response;
import com.sequenceiq.cloudbreak.auth.service.url.UrlAccessValidationService;

@Service
public class RepositoryConfigValidationService {

    @Inject
    private UrlAccessValidationService urlAccessValidationService;

    public RepoConfigValidationV4Response validate(RepoConfigValidationV4Request request) {
        RepoConfigValidationV4Response result = new RepoConfigValidationV4Response();
        if (request != null) {
            String ambariBaseUrl = request.getAmbariBaseUrl();
            if (isNotEmpty(ambariBaseUrl)) {
                result.setAmbariBaseUrl(repoUrlAvailable(ambariBaseUrl, "Ambari"));
            }

            String ambariGpgKeyUrl = request.getAmbariGpgKeyUrl();
            if (isNotEmpty(ambariGpgKeyUrl)) {
                result.setAmbariGpgKeyUrl(isAccessible(ambariGpgKeyUrl, null));
            }

            String stackBaseURL = request.getStackBaseURL();
            if (isNotEmpty(stackBaseURL)) {
                result.setStackBaseURL(repoUrlAvailable(stackBaseURL, "HDP"));
            }

            String utilsBaseURL = request.getUtilsBaseURL();
            if (isNotEmpty(utilsBaseURL)) {
                result.setUtilsBaseURL(repoUrlAvailable(utilsBaseURL, "HDP-UTILS"));
            }

            String versionDefinitionFileUrl = request.getVersionDefinitionFileUrl();
            if (isNotEmpty(versionDefinitionFileUrl)) {
                result.setVersionDefinitionFileUrl(isAccessible(versionDefinitionFileUrl, null));
            }

            String mpackUrl = request.getMpackUrl();
            if (isNotEmpty(mpackUrl)) {
                result.setMpackUrl(isAccessible(mpackUrl, null));
            }
        }
        return result;
    }

    private boolean repoUrlAvailable(String ambariBaseUrl, String service) {
        return rpmRepoAvailable(ambariBaseUrl) || debRepoAvailable(ambariBaseUrl, service);
    }

    private Boolean debRepoAvailable(String stackBaseURL, String serviceName) {
        String urlExtension = String.format("dists/%s/InRelease", serviceName);
        return isAccessible(stackBaseURL, urlExtension);
    }

    private Boolean rpmRepoAvailable(String stackBaseURL) {
        return isAccessible(stackBaseURL, "repodata/repomd.xml");
    }

    private Boolean isAccessible(String baseUrl, String urlExtension) {
        String url = baseUrl;
        if (isNotEmpty(urlExtension)) {
            String ext = baseUrl.endsWith("/") ? urlExtension : '/' + urlExtension;
            url += ext;
        }
        return urlAccessValidationService.isAccessible(url);
    }
}
