package com.sequenceiq.cloudbreak.service.cluster;

import static org.apache.commons.lang3.StringUtils.isNoneEmpty;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.model.repositoryconfig.RepoConfigValidationRequest;
import com.sequenceiq.cloudbreak.api.model.repositoryconfig.RepoConfigValidationResponse;
import com.sequenceiq.cloudbreak.client.RestClientUtil;

@Service
public class RepositoryConfigValidationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RepositoryConfigValidationService.class);

    public RepoConfigValidationResponse validate(RepoConfigValidationRequest request) {
        RepoConfigValidationResponse result = new RepoConfigValidationResponse();
        if (request != null) {
            Client client = createRestClient();

            String ambariBaseUrl = request.getAmbariBaseUrl();
            if (isNoneEmpty(ambariBaseUrl)) {
                result.setAmbariBaseUrl(repoUrlAvailable(client, ambariBaseUrl, "Ambari"));
            }

            String ambariGpgKeyUrl = request.getAmbariGpgKeyUrl();
            if (isNoneEmpty(ambariGpgKeyUrl)) {
                result.setAmbariGpgKeyUrl(validateUrl(ambariGpgKeyUrl, null, client));
            }

            String stackBaseURL = request.getStackBaseURL();
            if (isNoneEmpty(stackBaseURL)) {
                result.setStackBaseURL(repoUrlAvailable(client, stackBaseURL, "HDP"));
            }

            String utilsBaseURL = request.getUtilsBaseURL();
            if (isNoneEmpty(utilsBaseURL)) {
                result.setUtilsBaseURL(repoUrlAvailable(client, utilsBaseURL, "HDP-UTILS"));
            }

            String versionDefinitionFileUrl = request.getVersionDefinitionFileUrl();
            if (isNoneEmpty(versionDefinitionFileUrl)) {
                result.setVersionDefinitionFileUrl(validateUrl(versionDefinitionFileUrl, null, client));
            }

            String mpackUrl = request.getMpackUrl();
            if (isNoneEmpty(mpackUrl)) {
                result.setMpackUrl(validateUrl(mpackUrl, null, client));
            }
        }
        return result;
    }

    private boolean repoUrlAvailable(Client client, String ambariBaseUrl, String service) {
        return rpmRepoAvailable(client, ambariBaseUrl) || debRepoAvailable(client, ambariBaseUrl, service);
    }

    private Boolean debRepoAvailable(Client client, String stackBaseURL, String serviceName) {
        String urlExtension = String.format("dists/%s/InRelease", serviceName);
        return validateUrl(stackBaseURL, urlExtension, client);
    }

    private Boolean rpmRepoAvailable(Client client, String stackBaseURL) {
        return validateUrl(stackBaseURL, "repodata/repomd.xml", client);
    }

    Client createRestClient() {
        return RestClientUtil.get();
    }

    private Boolean validateUrl(String url, String urlExtension, Client client) {
        boolean result = false;

        if (isNoneEmpty(urlExtension)) {
            String ext = url.endsWith("/") ? urlExtension : "/" + urlExtension;
            url = url + ext;
        }

        try {
            WebTarget target = client.target(url);
            Response response = target.request().get();
            if (HttpStatus.OK.value() == response.getStatus()) {
                result = true;
            }
        } catch (Exception ex) {
            LOGGER.info("The following URL is not reachable by Cloudbreak: '{}'", url);
        }
        return result;
    }
}
