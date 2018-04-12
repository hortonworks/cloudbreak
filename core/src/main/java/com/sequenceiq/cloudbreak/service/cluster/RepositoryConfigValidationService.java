package com.sequenceiq.cloudbreak.service.cluster;

import com.sequenceiq.cloudbreak.api.model.repositoryconfig.RepoConfigValidationRequest;
import com.sequenceiq.cloudbreak.api.model.repositoryconfig.RepoConfigValidationResponse;
import com.sequenceiq.cloudbreak.common.service.url.UrlAccessValidationService;
import org.springframework.stereotype.Service;

import javax.inject.Inject;

import static org.apache.commons.lang3.StringUtils.isNoneEmpty;

@Service
public class RepositoryConfigValidationService {

    @Inject
    private UrlAccessValidationService urlAccessValidationService;

    public RepoConfigValidationResponse validate(RepoConfigValidationRequest request) {
        RepoConfigValidationResponse result = new RepoConfigValidationResponse();
        if (request != null) {
            String ambariBaseUrl = request.getAmbariBaseUrl();
            if (isNoneEmpty(ambariBaseUrl)) {
                result.setAmbariBaseUrl(isAccessible(ambariBaseUrl, "ambari.repo"));
            }

            String ambariGpgKeyUrl = request.getAmbariGpgKeyUrl();
            if (isNoneEmpty(ambariGpgKeyUrl)) {
                result.setAmbariGpgKeyUrl(isAccessible(ambariGpgKeyUrl, null));
            }

            String stackBaseURL = request.getStackBaseURL();
            if (isNoneEmpty(stackBaseURL)) {
                result.setStackBaseURL(isAccessible(stackBaseURL, "hdp.repo"));
            }

            String utilsBaseURL = request.getUtilsBaseURL();
            if (isNoneEmpty(utilsBaseURL)) {
                result.setUtilsBaseURL(isAccessible(utilsBaseURL, "hdp-utils.repo"));
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

    private Boolean isAccessible(String baseUrl, String urlExtension) {
        String url = baseUrl;
        if (isNoneEmpty(urlExtension)) {
            String ext = baseUrl.endsWith("/") ? urlExtension : '/' + urlExtension;
            url += ext;
        }
        return urlAccessValidationService.isAccessible(url);
    }
}
