package com.sequenceiq.cloudbreak.cm;

import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.isVersionNewerOrEqualThanLimited;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.cloudera.api.swagger.ClouderaManagerResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiConfig;
import com.cloudera.api.swagger.model.ApiConfigList;
import com.cloudera.api.swagger.model.ApiVersionInfo;
import com.sequenceiq.cloudbreak.client.HttpClientConfig;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerApiClientProvider;
import com.sequenceiq.cloudbreak.cm.client.retry.ClouderaManagerApiFactory;
import com.sequenceiq.cloudbreak.common.type.Versioned;

@Service
@Scope("prototype")
public class ClouderaManagerConfigService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerConfigService.class);

    @Inject
    private ClouderaManagerApiClientProvider clouderaManagerApiClientProvider;

    @Inject
    private ClouderaManagerApiFactory clouderaManagerApiFactory;

    public void setCdpEnvironmentIfCmVersionAtLeast(Versioned versionAtLeast, ApiClient apiClient, HttpClientConfig httpClientConfig) {

        try {
            ClouderaManagerResourceApi resourceApiInstance = clouderaManagerApiFactory.getClouderaManagerResourceApi(apiClient);
            if (isVersionAtLeast(versionAtLeast, resourceApiInstance)) {
                setCdpEnvironment(resourceApiInstance);
            }
        } catch (ApiException e) {
            LOGGER.debug("Failed to set cdp_environment on CM", e);
            throw new ClouderaManagerOperationFailedException("Failed to set cdp_environment to PUBLIC_CLOUD on CM", e);
        }
    }

    private void setCdpEnvironment(ClouderaManagerResourceApi resourceApiInstance) throws ApiException {
        ApiConfigList apiConfigListResponse = resourceApiInstance.updateConfig("",
                new ApiConfigList().addItemsItem(
                        new ApiConfig()
                                .name("cdp_environment")
                                .value("PUBLIC_CLOUD")
                ));
        LOGGER.debug("Response of setting cdp_environment: {}", apiConfigListResponse);
    }

    private boolean isVersionAtLeast(Versioned requiredVersion, ClouderaManagerResourceApi resourceApiInstance) throws ApiException {
        ApiVersionInfo versionInfo = resourceApiInstance.getVersion();
        LOGGER.debug("CM version is {}, used version string {}", versionInfo, versionInfo.getVersion());

        if (isVersionNewerOrEqualThanLimited(versionInfo.getVersion(), requiredVersion)) {
            return true;
        }
        LOGGER.debug("Version is smaller than {}, not setting cdp_environment", requiredVersion.getVersion());
        return false;
    }
}