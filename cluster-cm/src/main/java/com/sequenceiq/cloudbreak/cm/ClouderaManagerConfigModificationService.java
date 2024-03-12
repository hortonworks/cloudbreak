package com.sequenceiq.cloudbreak.cm;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

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
import com.google.common.collect.Table;
import com.google.common.collect.Table.Cell;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;

@Service
public class ClouderaManagerConfigModificationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerConfigModificationService.class);

    @Inject
    private ClouderaManagerConfigService configService;

    @Inject
    private ClouderaManagerServiceManagementService clouderaManagerServiceManagementService;

    public List<String> serviceNames(Table<String, String, String> newConfigs, ApiClient client, StackDtoDelegate stack) {
        return clouderaManagerServiceManagementService.readServices(client, stack.getName()).getItems()
                .stream()
                .filter(apiService -> newConfigs.rowKeySet().contains(apiService.getType()))
                .map(ApiService::getName)
                .toList();
    }

    public void updateConfig(Table<String, String, String> newConfigs, ApiClient client, StackDtoDelegate stack) throws Exception {
        Map<String, String> serviceMapFromCM = clouderaManagerServiceManagementService.readServices(client, stack.getName()).getItems()
                .stream()
                .collect(Collectors.toMap(ApiService::getType, ApiService::getName));
        Map<String, ApiServiceConfig> serviceConfigsFromCM = collectServiceConfigs(newConfigs, serviceMapFromCM, client, stack);
        LOGGER.debug("Collect config groups from CM if collected service configs do not contain all of the necessary configs.");
        Map<String, ApiRoleConfigGroupList> roleConfigGroupListsFromCM =
                collectRoleConfigGroupConfigsIfNeeded(newConfigs, serviceMapFromCM, serviceConfigsFromCM, client, stack);
        validateConfigKeyBeforeModification(newConfigs, serviceConfigsFromCM, roleConfigGroupListsFromCM);
        for (Cell<String, String, String> cell : newConfigs.cellSet()) {
            updateConfigBasedOnCell(client, stack, serviceMapFromCM, serviceConfigsFromCM, roleConfigGroupListsFromCM, cell);
        }
    }

    private void updateConfigBasedOnCell(ApiClient client, StackDtoDelegate stack, Map<String, String> serviceMapFromCM,
            Map<String, ApiServiceConfig> serviceConfigsFromCM, Map<String, ApiRoleConfigGroupList> roleConfigGroupListsFromCM,
            Cell<String, String, String> newConfigCell) {
        String newConfigServiceType = newConfigCell.getRowKey();
        String newConfigKey = newConfigCell.getColumnKey();
        String newConfigValue = newConfigCell.getValue();
        if (serviceMapFromCM.containsKey(newConfigServiceType)) {
            Map<String, String> newConfigMap = Map.of(newConfigKey, newConfigValue);
            String serviceName = serviceMapFromCM.get(newConfigServiceType);
            if (roleConfigGroupListsFromCM.containsKey(newConfigServiceType)) {
                updateConfigGroup(roleConfigGroupListsFromCM.get(newConfigServiceType), newConfigKey, newConfigMap, serviceName, client, stack);
            }
            if (configExistInConfigList(serviceConfigsFromCM.get(newConfigServiceType).getItems(), newConfigKey)) {
                configService.modifyServiceConfigs(client, stack.getName(), newConfigMap, serviceName);
            }
        } else {
            LOGGER.info("Service {} for config {} does not exists in CM.", newConfigServiceType, newConfigKey);
        }
    }

    private void validateConfigKeyBeforeModification(Table<String, String, String> newConfigs, Map<String, ApiServiceConfig> serviceConfigsFromCM,
            Map<String, ApiRoleConfigGroupList> roleConfigGroupListsFromCM) {
        LOGGER.info("Validating configs' existence before modification in CM.");
        for (Cell<String, String, String> newConfigCell : newConfigs.cellSet()) {
            String newConfigServiceType = newConfigCell.getRowKey();
            String newConfigKey = newConfigCell.getColumnKey();
            if (serviceConfigsFromCM.containsKey(newConfigServiceType) &&
                    !configPresentAsServiceConfig(serviceConfigsFromCM, newConfigServiceType, newConfigKey) &&
                    roleConfigGroupListsFromCM.get(newConfigServiceType).getItems()
                    .stream()
                    .noneMatch(apiRoleConfigGroupFromCM -> apiRoleConfigGroupFromCM.getConfig().getItems()
                            .stream()
                            .anyMatch(apiConfigFromCM -> StringUtils.equals(apiConfigFromCM.getName(), newConfigKey)))) {
                throw new CloudbreakServiceException(String.format("Config %s is not present in CM for service %s", newConfigKey, newConfigServiceType));
            }
        }
    }

    private void updateConfigGroup(ApiRoleConfigGroupList apiRoleConfigGroupListFromCM, String newConfigKey,
            Map<String, String> newConfigMap, String serviceName, ApiClient client, StackDtoDelegate stack) {
        Optional<ApiRoleConfigGroup> roleConfigGroupFromCM = apiRoleConfigGroupListFromCM.getItems().stream()
                .filter(apiRoleConfigGroup -> configExistInConfigList(apiRoleConfigGroup.getConfig().getItems(), newConfigKey))
                .findFirst();
        roleConfigGroupFromCM.ifPresent(apiRoleConfigGroupFromCM ->
                configService.modifyRoleConfigGroups(client, stack.getName(), serviceName, apiRoleConfigGroupFromCM.getName(), newConfigMap));
    }

    private Map<String, ApiRoleConfigGroupList> collectRoleConfigGroupConfigsIfNeeded(Table<String, String, String> newConfigs,
            Map<String, String> serviceMapFromCM, Map<String, ApiServiceConfig> serviceConfigsFromCM, ApiClient client, StackDtoDelegate stack) {
        return serviceMapFromCM.entrySet().stream()
                .filter(serviceEntry -> newConfigs.rowKeySet().contains(serviceEntry.getKey()))
                .filter(serviceEntry -> {
                    String serviceType = serviceEntry.getKey();
                    Set<String> affectedConfigByService = newConfigs.row(serviceType).keySet();
                    return !affectedConfigByService.stream().allMatch(config -> configPresentAsServiceConfig(serviceConfigsFromCM, serviceType, config));
                })
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> configService.readRoleConfigGroupConfigs(client, stack.getName(), entry.getValue())));
    }

    private boolean configPresentAsServiceConfig(Map<String, ApiServiceConfig> serviceConfigsFromCM, String newConfigServiceType, String newConfigKey) {
        return serviceConfigsFromCM.get(newConfigServiceType).getItems().stream()
                .anyMatch(apiConfig -> StringUtils.equals(apiConfig.getName(), newConfigKey));
    }

    private Map<String, ApiServiceConfig> collectServiceConfigs(Table<String, String, String> newConfigs, Map<String, String> serviceMapFromCM,
            ApiClient client, StackDtoDelegate stack) {
        return serviceMapFromCM.entrySet().stream()
                .filter(serviceEntry -> newConfigs.rowKeySet().contains(serviceEntry.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> configService.readServiceConfig(client, stack.getName(), entry.getValue())));
    }

    private boolean configExistInConfigList(List<ApiConfig> apiConfigListFromCM, String newConfigKey) {
        Predicate<ApiConfig> cmApiConfigPredicate = cmApiConfig -> StringUtils.equals(cmApiConfig.getName(), newConfigKey);
        return apiConfigListFromCM.stream().anyMatch(cmApiConfigPredicate);
    }

}
