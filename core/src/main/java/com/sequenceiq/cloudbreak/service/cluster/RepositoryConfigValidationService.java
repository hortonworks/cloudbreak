package com.sequenceiq.cloudbreak.service.cluster;

import static org.apache.commons.lang3.StringUtils.isNoneEmpty;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.model.repositoryconfig.RepoConfigValidationRequest;
import com.sequenceiq.cloudbreak.api.model.repositoryconfig.RepoConfigValidationResponse;
import com.sequenceiq.cloudbreak.common.service.url.UrlAccessValidationService;
import com.sequenceiq.cloudbreak.service.credential.PaywallCredentialService;

@Service
public class RepositoryConfigValidationService {

    @Inject
    private UrlAccessValidationService urlAccessValidationService;

    @Inject
    private PaywallCredentialService paywallCredentialService;

    public RepoConfigValidationResponse validate(RepoConfigValidationRequest request) {
        RepoConfigValidationResponse result = new RepoConfigValidationResponse();
        if (request != null) {
            String ambariBaseUrl = request.getAmbariBaseUrl();
            if (isNoneEmpty(ambariBaseUrl)) {
                result.setAmbariBaseUrl(repoUrlAvailable(ambariBaseUrl, "Ambari"));
            }

            String ambariGpgKeyUrl = request.getAmbariGpgKeyUrl();
            if (isNoneEmpty(ambariGpgKeyUrl)) {
                result.setAmbariGpgKeyUrl(isAccessible(ambariGpgKeyUrl, null));
            }

            String stackBaseURL = request.getStackBaseURL();
            if (isNoneEmpty(stackBaseURL)) {
                result.setStackBaseURL(repoUrlAvailable(stackBaseURL, "HDP"));
            }

            String utilsBaseURL = request.getUtilsBaseURL();
            if (isNoneEmpty(utilsBaseURL)) {
                result.setUtilsBaseURL(repoUrlAvailable(utilsBaseURL, "HDP-UTILS"));
            }

            String versionDefinitionFileUrl = request.getVersionDefinitionFileUrl();
            if (isNoneEmpty(versionDefinitionFileUrl)) {
                result.setVersionDefinitionFileUrl(isAccessible(versionDefinitionFileUrl, null));
            }

            String mpackUrl = request.getMpackUrl();
            if (isNoneEmpty(mpackUrl)) {
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
        if (isNoneEmpty(urlExtension)) {
            String ext = baseUrl.endsWith("/") ? urlExtension : '/' + urlExtension;
            url += ext;
        }
        Map<String, Object> headers = null;
        if (paywallCredentialService.paywallCredentialAvailable()) {
            headers = new HashMap<>();
            headers.put("Authorization", String.format("Basic %s", paywallCredentialService.getBasicAuthorizationEncoded()));
        }
        return urlAccessValidationService.isAccessible(url, headers);
    }
}
