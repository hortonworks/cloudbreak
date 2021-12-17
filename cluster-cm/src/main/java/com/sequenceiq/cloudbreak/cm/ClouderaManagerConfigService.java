package com.sequenceiq.cloudbreak.cm;

import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.isVersionNewerOrEqualThanLimited;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cloudera.api.swagger.ClouderaManagerResourceApi;
import com.cloudera.api.swagger.RoleConfigGroupsResourceApi;
import com.cloudera.api.swagger.ServicesResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiConfig;
import com.cloudera.api.swagger.model.ApiConfigList;
import com.cloudera.api.swagger.model.ApiRoleConfigGroupList;
import com.cloudera.api.swagger.model.ApiService;
import com.cloudera.api.swagger.model.ApiServiceConfig;
import com.cloudera.api.swagger.model.ApiServiceList;
import com.cloudera.api.swagger.model.ApiVersionInfo;
import com.sequenceiq.cloudbreak.cm.client.retry.ClouderaManagerApiFactory;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.common.type.Versioned;
import com.sequenceiq.cloudbreak.service.CloudbreakException;

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

    public void modifyServiceConfig(ApiClient client, String clusterName, String serviceType, Map<String, String> config) throws CloudbreakException {
        ServicesResourceApi servicesResourceApi = clouderaManagerApiFactory.getServicesResourceApi(client);
        LOGGER.info("Trying to modify config: {} for service {}", Arrays.asList(config), serviceType);
        getServiceName(clusterName, serviceType, servicesResourceApi)
                .ifPresentOrElse(
                        modifyServiceConfig(clusterName, servicesResourceApi, config),
                        () -> {
                            LOGGER.info("{} service name is missing, skip modification.", serviceType);
                            throw new ClouderaManagerOperationFailedException(String.format("Service of type: %s is not found", serviceType));
                        });
    }

    private Consumer<String> modifyServiceConfig(String clusterName, ServicesResourceApi servicesResourceApi, Map<String, String> config)
            throws CloudbreakException  {
        ApiServiceConfig apiServiceConfig = new ApiServiceConfig();
        return serviceName -> {
            config.forEach((key, value) -> {
                apiServiceConfig.addItemsItem(new ApiConfig().name(key).value(value));
            });
            try {
                servicesResourceApi.updateServiceConfig(clusterName, serviceName, "", apiServiceConfig);
            } catch (ApiException e) {
                LOGGER.error("Failed to set configs {} for service {}", Arrays.asList(config), serviceName, e);
                throw new ClouderaManagerOperationFailedException(e.getMessage(), e);
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

    public Optional<String> getRoleConfigValueByServiceType(ApiClient apiClient, String clusterName, String roleType, String serviceType, String configName) {
        LOGGER.debug("Looking for configuration: {} for cluster {}, roleType {}, and serviceType {}", configName, clusterName, roleType, serviceType);
        RoleConfigGroupsResourceApi roleConfigGroupsResourceApi = clouderaManagerApiFactory.getRoleConfigGroupsResourceApi(apiClient);
        ServicesResourceApi servicesResourceApi = clouderaManagerApiFactory.getServicesResourceApi(apiClient);
        try {
            String serviceName = getServiceNameValue(clusterName, serviceType, servicesResourceApi);
            String roleConfigGroupName = getRoleConfigGroupNameByTypeAndServiceName(roleType, clusterName, serviceName, roleConfigGroupsResourceApi);
            ApiConfigList roleConfig = roleConfigGroupsResourceApi.readConfig(clusterName, roleConfigGroupName, serviceName, "full");
            return roleConfig.getItems().stream()
                    .filter(apiConfig -> configName.equals(apiConfig.getName()))
                    .map(apiConfig -> Optional.ofNullable(apiConfig.getValue()).orElse(apiConfig.getDefault()))
                    .findFirst();
        } catch (ApiException | NotFoundException e) {
            LOGGER.debug("Failed to get configuration: {} for cluster {}, roleType {}, and serviceType {}", configName, clusterName, roleType, serviceType, e);
            return Optional.empty();
        }
    }

    private String getServiceNameValue(String clusterName, String serviceType, ServicesResourceApi servicesResourceApi) {
        return getServiceName(clusterName, serviceType, servicesResourceApi).orElseThrow(
                () -> new NotFoundException(String.format("No service found with %s service type", serviceType)));
    }

    private String getRoleConfigGroupNameByTypeAndServiceName(String roleType, String clusterName, String serviceName,
            RoleConfigGroupsResourceApi roleConfigGroupsResourceApi) throws ApiException {
        ApiRoleConfigGroupList roleConfigGroupList = roleConfigGroupsResourceApi.readRoleConfigGroups(clusterName, serviceName);
        return roleConfigGroupList.getItems()
                .stream()
                .filter(roleConfigGroup -> roleType.equals(roleConfigGroup.getRoleType()))
                .findFirst()
                .orElseThrow(() -> new NotFoundException(String.format("No role found with %s role type", roleType)))
                .getName();
    }
}