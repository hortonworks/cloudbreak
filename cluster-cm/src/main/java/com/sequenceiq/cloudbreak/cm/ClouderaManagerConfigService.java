package com.sequenceiq.cloudbreak.cm;

import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.isVersionNewerOrEqualThanLimited;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import com.cloudera.api.swagger.ClouderaManagerResourceApi;
import com.cloudera.api.swagger.RoleConfigGroupsResourceApi;
import com.cloudera.api.swagger.ServicesResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiConfig;
import com.cloudera.api.swagger.model.ApiConfigList;
import com.cloudera.api.swagger.model.ApiRoleConfigGroup;
import com.cloudera.api.swagger.model.ApiRoleConfigGroupList;
import com.cloudera.api.swagger.model.ApiService;
import com.cloudera.api.swagger.model.ApiServiceConfig;
import com.cloudera.api.swagger.model.ApiServiceList;
import com.cloudera.api.swagger.model.ApiVersionInfo;
import com.google.common.base.Joiner;
import com.sequenceiq.cloudbreak.cm.client.retry.ClouderaManagerApiFactory;
import com.sequenceiq.cloudbreak.cm.exception.ClouderaManagerOperationFailedException;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.common.type.Versioned;
import com.sequenceiq.cloudbreak.service.CloudbreakException;

@Service
public class ClouderaManagerConfigService {

    static final String KNOX_AUTORESTART_ON_STOP = "autorestart_on_stop";

    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerConfigService.class);

    private static final String KNOX_SERVICE = "KNOX";

    private static final int BACKOFF = 5000;

    @Inject
    private ClouderaManagerApiFactory clouderaManagerApiFactory;

    public void modifyKnoxAutoRestartIfCmVersionAtLeast(Versioned versionAtLeast, ApiClient client, String clusterName, boolean enabled) {
        try {
            ClouderaManagerResourceApi resourceApiInstance = clouderaManagerApiFactory.getClouderaManagerResourceApi(client);
            ApiVersionInfo versionInfo = resourceApiInstance.getVersion();
            LOGGER.debug("CM version is {}, used version string {}", versionInfo, versionInfo.getVersion());
            if (isVersionNewerOrEqualThanLimited(versionInfo.getVersion(), versionAtLeast)) {
                modifyKnoxAutorestart(client, clusterName, enabled);
            }
        } catch (ApiException e) {
            LOGGER.debug("Failed to initialize CM client.", e);
        }
    }

    private void modifyKnoxAutorestart(ApiClient client, String clusterName, boolean autorestart) {
        ServicesResourceApi servicesResourceApi = clouderaManagerApiFactory.getServicesResourceApi(client);
        LOGGER.info("Try to modify Knox auto restart to {}", autorestart);
        getServiceName(clusterName, KNOX_SERVICE, servicesResourceApi)
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

    public void modifyServiceConfig(ApiClient client, String clusterName, String serviceType, Map<String, String> config) throws CloudbreakException {
        ServicesResourceApi servicesResourceApi = clouderaManagerApiFactory.getServicesResourceApi(client);
        LOGGER.info("Trying to modify config: {} for service {}", config, serviceType);
        getServiceName(clusterName, serviceType, servicesResourceApi)
                .ifPresentOrElse(
                        modifyServiceConfig(clusterName, servicesResourceApi, config),
                        () -> {
                            LOGGER.info("{} service name is missing, skip modification.", serviceType);
                            throw new ClouderaManagerOperationFailedException(String.format("Service of type: %s is not found", serviceType));
                        });
    }

    private Consumer<String> modifyServiceConfig(String clusterName, ServicesResourceApi servicesResourceApi, Map<String, String> config) {
        ApiServiceConfig apiServiceConfig = new ApiServiceConfig();
        return serviceName -> {
            config.forEach((key, value) -> {
                apiServiceConfig.addItemsItem(new ApiConfig().name(key).value(value));
            });
            try {
                servicesResourceApi.updateServiceConfig(clusterName, serviceName, "", apiServiceConfig);
            } catch (ApiException e) {
                LOGGER.error("Failed to set configs {} for service {}", config, serviceName, e);
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
                () -> new NotFoundException(String.format("No service name found with %s service type", serviceType)));
    }

    public boolean isRolePresent(ApiClient apiClient, String clusterName, String roleType, String serviceType) {
        LOGGER.debug("Looking for role name for cluster {}, roleType {}, and serviceType {}", clusterName, roleType, serviceType);
        RoleConfigGroupsResourceApi roleConfigGroupsResourceApi = clouderaManagerApiFactory.getRoleConfigGroupsResourceApi(apiClient);
        ServicesResourceApi servicesResourceApi = clouderaManagerApiFactory.getServicesResourceApi(apiClient);
        try {
            String serviceName = getServiceNameValue(clusterName, serviceType, servicesResourceApi);
            getRoleConfigGroupNameByTypeAndServiceName(roleType, clusterName, serviceName, roleConfigGroupsResourceApi);
            return true;
        } catch (NotFoundException e) {
            LOGGER.debug("Role not found for cluster {}, roleType {}, and serviceType {}", clusterName, roleType, serviceType, e);
            return false;
        } catch (ApiException e) {
            LOGGER.debug("Failed to get role name for cluster {}, roleType {}, and serviceType {}", clusterName, roleType, serviceType, e);
            throw new ClouderaManagerOperationFailedException(e.getMessage(), e);
        }
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

    @Retryable(value = ClouderaManagerOperationFailedException.class, backoff = @Backoff(delay = BACKOFF))
    public ApiServiceConfig readServiceConfig(ApiClient client, String clusterName, String serviceName) {
        try {
            LOGGER.debug("Reading {} service config from Cloudera Manager for cluster {}.", serviceName, clusterName);
            ServicesResourceApi servicesResourceApi = clouderaManagerApiFactory.getServicesResourceApi(client);
            return servicesResourceApi.readServiceConfig(clusterName, serviceName, DataView.SUMMARY.name());
        } catch (ApiException e) {
            LOGGER.error("Failed to get service config for service {} from Cloudera Manager.", serviceName, e);
            throw new ClouderaManagerOperationFailedException(e.getMessage(), e);
        }
    }

    @Retryable(value = ClouderaManagerOperationFailedException.class, backoff = @Backoff(delay = BACKOFF))
    public ApiRoleConfigGroupList readRoleConfigGroupConfigs(ApiClient client, String clusterName, String serviceName) {
        try {
            LOGGER.debug("Reading config groups of {} service in Cloudera Manager for cluster {}.", serviceName, clusterName);
            RoleConfigGroupsResourceApi roleConfigGroupsResourceApi = clouderaManagerApiFactory.getRoleConfigGroupsResourceApi(client);
            return roleConfigGroupsResourceApi.readRoleConfigGroups(clusterName, serviceName);
        } catch (ApiException e) {
            LOGGER.error("Failed to get role config groups for service {} from Cloudera Manager.", serviceName, e);
            throw new ClouderaManagerOperationFailedException(e.getMessage(), e);
        }
    }

    @Retryable(value = ClouderaManagerOperationFailedException.class, backoff = @Backoff(delay = BACKOFF))
    public void modifyServiceConfigs(ApiClient client, String clusterName, Map<String, String> config, String serviceName) {
        try {
            LOGGER.debug("Modifying {} service configs [{}] of Cloudera Manager for cluster {}.",
                    serviceName, Joiner.on(",").join(config.keySet()), clusterName);
            ServicesResourceApi servicesResourceApi = clouderaManagerApiFactory.getServicesResourceApi(client);
            ApiServiceConfig apiServiceConfig = new ApiServiceConfig();
            config.forEach((key, value) -> apiServiceConfig.addItemsItem(new ApiConfig().name(key).value(value)));
            servicesResourceApi.updateServiceConfig(clusterName, serviceName, "", apiServiceConfig);
        } catch (ApiException e) {
            LOGGER.error("Failed to set configs [{}] for service {}.", Joiner.on(",").withKeyValueSeparator("=").join(config), serviceName, e);
            throw new ClouderaManagerOperationFailedException(e.getMessage(), e);
        }
    }

    @Retryable(value = ClouderaManagerOperationFailedException.class, backoff = @Backoff(delay = BACKOFF))
    public void modifyRoleConfigGroup(ApiClient client, String clusterName, String serviceName, String roleConfigGroupName, Map<String, String> config) {
        try {
            LOGGER.debug("Modifying {} role config group of {} service regarding configs [{}] in Cloudera Manager for cluster {}.",
                    roleConfigGroupName, serviceName, Joiner.on(",").join(config.keySet()), clusterName);
            RoleConfigGroupsResourceApi roleConfigGroupsResourceApi = clouderaManagerApiFactory.getRoleConfigGroupsResourceApi(client);
            ApiRoleConfigGroup apiRoleConfigGroup = new ApiRoleConfigGroup();
            ApiConfigList apiConfigList = new ApiConfigList();
            config.forEach((key, value) -> apiConfigList.addItemsItem(new ApiConfig().name(key).value(value)));
            apiRoleConfigGroup.setConfig(apiConfigList);
            roleConfigGroupsResourceApi.updateRoleConfigGroup(clusterName, roleConfigGroupName, serviceName, "", apiRoleConfigGroup);
        } catch (ApiException e) {
            LOGGER.error("Failed to update role group config [{}] for role group {} of service {}",
                    Joiner.on(",").withKeyValueSeparator("=").join(config), roleConfigGroupName, serviceName, e);
            throw new ClouderaManagerOperationFailedException(e.getMessage(), e);
        }
    }

    public void modifyRoleBasedConfig(ApiClient client, String stackName, String serviceType, Map<String, String> config, List<String> roleConfigGroupName) {
        ServicesResourceApi servicesResourceApi = clouderaManagerApiFactory.getServicesResourceApi(client);
        RoleConfigGroupsResourceApi roleConfigGroupsResourceApi = clouderaManagerApiFactory.getRoleConfigGroupsResourceApi(client);
        LOGGER.info("Trying to modify config: {} for service {}", config, serviceType);
        getServiceName(stackName, serviceType, servicesResourceApi)
                .ifPresentOrElse(
                        modifyRoleBasedConfig(stackName, roleConfigGroupsResourceApi, config, roleConfigGroupName),
                        () -> {
                            LOGGER.error("{} service name is missing.", serviceType);
                            throw new ClouderaManagerOperationFailedException(String.format("Service of type: %s is not found", serviceType));
                        });
    }

    private Consumer<String> modifyRoleBasedConfig(String stackName, RoleConfigGroupsResourceApi roleConfigGroupsResourceApi,
            Map<String, String> config, List<String> roleConfigGroupNames) {
        ApiConfigList apiConfigList = createApiConfigList(config);
        return serviceName -> roleConfigGroupNames.forEach(role -> {
            try {
                roleConfigGroupsResourceApi.updateConfig(stackName, role, serviceName, "Modifying role based config for service " + serviceName,
                        apiConfigList);
            } catch (ApiException e) {
                LOGGER.error("Failed to set configs {} for service {}", config, serviceName, e);
                throw new ClouderaManagerOperationFailedException(e.getMessage(), e);
            }
        });
    }

    private ApiConfigList createApiConfigList(Map<String, String> config) {
        ApiConfigList apiConfigList = new ApiConfigList();
        config.forEach((key, value) -> {
            apiConfigList.addItemsItem(new ApiConfig().name(key).value(value));
        });
        return apiConfigList;
    }
}
