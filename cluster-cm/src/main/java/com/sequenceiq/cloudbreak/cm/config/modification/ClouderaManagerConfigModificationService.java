package com.sequenceiq.cloudbreak.cm.config.modification;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.model.ApiConfig;
import com.cloudera.api.swagger.model.ApiRoleConfigGroup;
import com.cloudera.api.swagger.model.ApiRoleConfigGroupList;
import com.cloudera.api.swagger.model.ApiService;
import com.cloudera.api.swagger.model.ApiServiceConfig;
import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.cm.ClouderaManagerConfigService;
import com.sequenceiq.cloudbreak.cm.ClouderaManagerServiceManagementService;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;

@Service
public class ClouderaManagerConfigModificationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerConfigModificationService.class);

    @Inject
    private ClouderaManagerConfigService configService;

    @Inject
    private ClouderaManagerServiceManagementService clouderaManagerServiceManagementService;

    public List<String> getServiceNames(List<CmConfig> newConfigs, ApiClient client, StackDtoDelegate stack) {
        return clouderaManagerServiceManagementService.readServices(client, stack.getName()).getItems()
                .stream()
                .filter(apiService -> newConfigs.stream().map(cmConfig -> cmConfig.serviceType().type()).toList().contains(apiService.getType()))
                .map(ApiService::getName)
                .toList();
    }

    public void updateConfigs(List<CmConfig> newConfigs, ApiClient client, StackDtoDelegate stack) {
        List<CmServiceMetadata> cmServiceMetadata = clouderaManagerServiceManagementService.readServices(client, stack.getName()).getItems()
                .stream()
                .map(apiService -> new CmServiceMetadata(apiService.getName(), new CmServiceType(apiService.getType())))
                .toList();
        LOGGER.info("Attempting to update CM with following configs: {}", newConfigs);
        Map<String, ApiServiceConfig> serviceConfigCache = Maps.newHashMap();
        Map<String, ApiRoleConfigGroupList> roleConfigGroupCache = Maps.newHashMap();
        newConfigs.forEach(newConfig -> {
            updateConfig(client, stack, newConfig, cmServiceMetadata, serviceConfigCache, roleConfigGroupCache);
        });
    }

    private void updateConfig(ApiClient client, StackDtoDelegate stack, CmConfig newConfig, List<CmServiceMetadata> cmServiceMetadata,
            Map<String, ApiServiceConfig> serviceConfigCache, Map<String, ApiRoleConfigGroupList> roleConfigGroupCache) {
        List<String> serviceNames = getServiceNames(newConfig, cmServiceMetadata);
        Predicate<ApiConfig> cmApiConfigPredicate = cmApiConfig -> StringUtils.equals(cmApiConfig.getName(), newConfig.key());
        Map<String, String> newConfigMap = Map.of(newConfig.key(), newConfig.value());
        if (!serviceNames.isEmpty()) {
            serviceNames.forEach(serviceName -> {
                ApiServiceConfig apiServiceConfig =
                        serviceConfigCache.computeIfAbsent(serviceName, sn -> configService.readServiceConfig(client, stack.getName(), sn));
                if (apiServiceConfig.getItems().stream().anyMatch(cmApiConfigPredicate)) {
                    configService.modifyServiceConfigs(client, stack.getName(), newConfigMap, serviceName);
                } else {
                    LOGGER.info("Config key {} cannot be found in service configs for service {}, searching for it in role group configs.",
                            newConfig.key(), serviceName);
                    updateRoleConfigGroupIfConfigPresent(roleConfigGroupCache, client, stack, newConfig,
                            serviceName, cmApiConfigPredicate, newConfigMap);
                }
            });
        } else {
            LOGGER.warn("Provided service name by service type {} cannot be found in CM.", newConfig.serviceType().type());
        }
    }

    private List<String> getServiceNames(CmConfig newConfig, List<CmServiceMetadata> cmServiceMetadata) {
        return cmServiceMetadata.stream()
                .filter(cmMd -> StringUtils.equals(cmMd.type().type(), newConfig.serviceType().type()))
                .map(CmServiceMetadata::name)
                .toList();
    }

    private void updateRoleConfigGroupIfConfigPresent(Map<String, ApiRoleConfigGroupList> roleConfigGroupCache,
            ApiClient client, StackDtoDelegate stack, CmConfig newConfig, String serviceName,
            Predicate<ApiConfig> cmApiConfigPredicate, Map<String, String> newConfigMap) {
        ApiRoleConfigGroupList apiRoleConfigGroupList =
                roleConfigGroupCache.computeIfAbsent(serviceName, sn -> configService.readRoleConfigGroupConfigs(client, stack.getName(), sn));
        List<String> roleGroupConfigGroupNames = getRoleGroupConfigGroupNames(apiRoleConfigGroupList, cmApiConfigPredicate);
        if (!roleGroupConfigGroupNames.isEmpty()) {
            roleGroupConfigGroupNames.forEach(roleGroupConfigGroupName ->
                    configService.modifyRoleConfigGroup(client, stack.getName(), serviceName, roleGroupConfigGroupName, newConfigMap));
        } else {
            LOGGER.warn("Config key {} cannot be found for any role group config for service {}, skipping update.", newConfig.key(), serviceName);
        }
    }

    private List<String> getRoleGroupConfigGroupNames(ApiRoleConfigGroupList apiRoleConfigGroupList, Predicate<ApiConfig> cmApiConfigPredicate) {
        return apiRoleConfigGroupList
                .getItems().stream()
                .filter(apiRoleConfigGroup -> apiRoleConfigGroup.getConfig().getItems().stream().anyMatch(cmApiConfigPredicate))
                .map(ApiRoleConfigGroup::getName)
                .toList();
    }

}
