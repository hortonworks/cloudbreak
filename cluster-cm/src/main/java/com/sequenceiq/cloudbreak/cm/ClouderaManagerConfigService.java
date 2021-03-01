package com.sequenceiq.cloudbreak.cm;

import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.isVersionNewerOrEqualThanLimited;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cloudera.api.swagger.ClouderaManagerResourceApi;
import com.cloudera.api.swagger.ServicesResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiConfig;
import com.cloudera.api.swagger.model.ApiService;
import com.cloudera.api.swagger.model.ApiServiceConfig;
import com.cloudera.api.swagger.model.ApiServiceList;
import com.cloudera.api.swagger.model.ApiVersionInfo;
import com.sequenceiq.cloudbreak.cm.client.retry.ClouderaManagerApiFactory;
import com.sequenceiq.cloudbreak.common.type.Versioned;

@Service
public class ClouderaManagerConfigService {

    static final String KNOX_AUTORESTART_ON_STOP = "autorestart_on_stop";

    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerConfigService.class);

    private static final String KNOX_SERVICE = "KNOX";

    @Inject
    private ClouderaManagerApiFactory clouderaManagerApiFactory;

    private boolean isVersionAtLeast(Versioned requiredVersion, ClouderaManagerResourceApi resourceApiInstance) throws ApiException {
        ApiVersionInfo versionInfo = resourceApiInstance.getVersion();
        LOGGER.debug("CM version is {}, used version string {}", versionInfo, versionInfo.getVersion());

        if (isVersionNewerOrEqualThanLimited(versionInfo.getVersion(), requiredVersion)) {
            return true;
        }
        LOGGER.debug("Version is smaller than {}, not setting cdp_environment", requiredVersion.getVersion());
        return false;
    }

    public void disableKnoxAutorestartIfCmVersionAtLeast(Versioned versionAtLeast, ApiClient client, String clusterName) {
        try {
            ClouderaManagerResourceApi resourceApiInstance = clouderaManagerApiFactory.getClouderaManagerResourceApi(client);
            if (isVersionAtLeast(versionAtLeast, resourceApiInstance)) {
                modifyKnoxAutorestart(client, clusterName, false);
            }
        } catch (ApiException e) {
            LOGGER.debug("Failed to initialize CM client.", e);
        }
    }

    public void enableKnoxAutorestartIfCmVersionAtLeast(Versioned versionAtLeast, ApiClient client, String clusterName) {
        try {
            ClouderaManagerResourceApi resourceApiInstance = clouderaManagerApiFactory.getClouderaManagerResourceApi(client);
            if (isVersionAtLeast(versionAtLeast, resourceApiInstance)) {
                modifyKnoxAutorestart(client, clusterName, true);
            }
        } catch (ApiException e) {
            LOGGER.debug("Failed to initialize CM client.", e);
        }
    }

    private void modifyKnoxAutorestart(ApiClient client, String clusterName, boolean autorestart) {
        ServicesResourceApi servicesResourceApi = clouderaManagerApiFactory.getServicesResourceApi(client);
        LOGGER.info("Try to modify Knox auto restart to {}", autorestart);
        getKnoxServiceName(clusterName, servicesResourceApi)
                .ifPresentOrElse(
                        modifyKnoxAutorestart(clusterName, servicesResourceApi, autorestart),
                        () -> LOGGER.info("KNOX service name is missing, skip modifying the autorestart property."));
    }

    private Consumer<String> modifyKnoxAutorestart(String clusterName, ServicesResourceApi servicesResourceApi, boolean autorestart) {
        return knoxServiceName -> {
            ApiConfig autorestartConfig = new ApiConfig().name(KNOX_AUTORESTART_ON_STOP).value(Boolean.valueOf(autorestart).toString());
            ApiServiceConfig serviceConfig = new ApiServiceConfig().addItemsItem(autorestartConfig);
            try {
                servicesResourceApi.updateServiceConfig(clusterName, knoxServiceName, "", serviceConfig);
            } catch (ApiException e) {
                LOGGER.debug("Failed to set autorestart_on_stop to KNOX in Cloudera Manager.", e);
            }
        };
    }

    private Optional<String> getKnoxServiceName(String clusterName, ServicesResourceApi servicesResourceApi) {
        try {
            ApiServiceList serviceList = servicesResourceApi.readServices(clusterName, DataView.SUMMARY.name());
            return serviceList.getItems().stream()
                    .filter(service -> KNOX_SERVICE.equals(service.getType()))
                    .map(ApiService::getName)
                    .findFirst();
        } catch (ApiException e) {
            LOGGER.debug("Failed to get KNOX service name from Cloudera Manager.", e);
            return Optional.empty();
        }
    }

    public void modifyServiceConfigValue(ApiClient client, String clusterName, String serviceType, String configName, String newConfigValue) {
        ServicesResourceApi servicesResourceApi = clouderaManagerApiFactory.getServicesResourceApi(client);
        LOGGER.info("Trying to modify {} {} to {}", serviceType, configName, newConfigValue);
        getServiceName(clusterName, serviceType, servicesResourceApi)
            .ifPresentOrElse(
                modifyServiceConfigValue(clusterName, servicesResourceApi, configName, newConfigValue),
                () -> LOGGER.info("{} service name is missing, skip modifying the {} property.", serviceType, configName));
    }

    private Consumer<String> modifyServiceConfigValue(String clusterName, ServicesResourceApi servicesResourceApi, String configName,
            String newConfigValue) {
        return serviceName -> {
            ApiConfig autorestartConfig = new ApiConfig().name(configName).value(newConfigValue);
            ApiServiceConfig serviceConfig = new ApiServiceConfig().addItemsItem(autorestartConfig);
            try {
                servicesResourceApi.updateServiceConfig(clusterName, serviceName, "", serviceConfig);
            } catch (ApiException e) {
                LOGGER.debug(String.format("Failed to set config %s to %s in service %s", configName, newConfigValue, serviceName), e);
            }
        };
    }

    private Optional<String> getServiceName(String clusterName, String serviceType, ServicesResourceApi servicesResourceApi) {
        Objects.requireNonNull(serviceType);
        try {
            LOGGER.debug("Looking for service of name {} in cluster {}", serviceType, clusterName);
            ApiServiceList serviceList = servicesResourceApi.readServices(clusterName, DataView.SUMMARY.name());
            return serviceList.getItems().stream()
                .filter(service -> serviceType.equals(service.getType()))
                .map(ApiService::getName)
                .findFirst();
        } catch (ApiException e) {
            LOGGER.debug(String.format("Failed to get %s service name from Cloudera Manager.", serviceType), e);
            return Optional.empty();
        }
    }
}