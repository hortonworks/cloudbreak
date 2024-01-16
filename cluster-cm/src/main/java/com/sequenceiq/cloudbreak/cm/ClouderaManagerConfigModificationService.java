package com.sequenceiq.cloudbreak.cm;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.inject.Inject;

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

    public List<String> serviceNames(Table<String, String, String> configTable, ApiClient client, StackDtoDelegate stack) {
        return clouderaManagerServiceManagementService.readServices(client, stack.getName()).getItems()
                .stream()
                .filter(apiService -> configTable.rowKeySet().contains(apiService.getType()))
                .map(ApiService::getName)
                .toList();
    }

    public void updateConfig(Table<String, String, String> configTable, ApiClient client, StackDtoDelegate stack) throws Exception {
        Map<String, String> serviceMap = clouderaManagerServiceManagementService.readServices(client, stack.getName()).getItems()
                .stream()
                .collect(Collectors.toMap(ApiService::getType, ApiService::getName));
        Map<String, ApiServiceConfig> serviceConfigs = collectServiceConfigs(configTable, serviceMap, client, stack);
        LOGGER.debug("Collect config groups from CM if collected service configs do not contain all of the necessary configs.");
        Map<String, ApiRoleConfigGroupList> roleConfigGroupLists =
                collectRoleConfigGroupConfigsIfNeeded(configTable, serviceMap, serviceConfigs, client, stack);
        validateConfigKeyBeforeModification(configTable, serviceConfigs, roleConfigGroupLists);
        for (Cell<String, String, String> cell : configTable.cellSet()) {
            updateConfigBasedOnCell(client, stack, serviceMap, serviceConfigs, roleConfigGroupLists, cell);
        }
    }

    private void updateConfigBasedOnCell(ApiClient client, StackDtoDelegate stack, Map<String, String> serviceMap,
            Map<String, ApiServiceConfig> serviceConfigs, Map<String, ApiRoleConfigGroupList> roleConfigGroupLists, Cell<String, String, String> cell) {
        String serviceType = cell.getRowKey();
        String configKey = cell.getColumnKey();
        String newValue = cell.getValue();
        Map<String, String> configMap = Map.of(configKey, newValue);
        String serviceName = serviceMap.get(serviceType);
        if (roleConfigGroupLists.containsKey(serviceType)) {
            updateConfigGroup(roleConfigGroupLists.get(serviceType), configKey, configMap, serviceName, client, stack);
        }
        if (configExistInConfigList(serviceConfigs.get(serviceType).getItems(), configKey)) {
            configService.modifyServiceConfigs(client, stack.getName(), configMap, serviceName);
        }
    }

    private void validateConfigKeyBeforeModification(Table<String, String, String> configTable, Map<String, ApiServiceConfig> serviceConfigs,
            Map<String, ApiRoleConfigGroupList> roleConfigGroupLists) {
        LOGGER.info("Validating configs' existence before modification in CM.");
        for (Cell<String, String, String> cell : configTable.cellSet()) {
            String serviceType = cell.getRowKey();
            String configKey = cell.getColumnKey();
            if (!configPresentAsServiceConfig(serviceConfigs, serviceType, configKey) && roleConfigGroupLists.get(serviceType).getItems()
                    .stream()
                    .noneMatch(apiRoleConfigGroup -> apiRoleConfigGroup.getConfig().getItems()
                            .stream()
                            .anyMatch(apiConfig -> StringUtils.equals(apiConfig.getName(), configKey)))) {
                throw new CloudbreakServiceException(String.format("Config %s is not present in CM for service %s", configKey, serviceType));
            }
        }
    }

    private void updateConfigGroup(ApiRoleConfigGroupList apiRoleConfigGroupList, String configKey,
            Map<String, String> configMap, String serviceName, ApiClient client, StackDtoDelegate stack) {
        Optional<ApiRoleConfigGroup> roleConfigGroup = apiRoleConfigGroupList.getItems().stream()
                .filter(apiRoleConfigGroup -> configExistInConfigList(apiRoleConfigGroup.getConfig().getItems(), configKey))
                .findFirst();
        roleConfigGroup.ifPresent(apiRoleConfigGroup ->
                configService.modifyRoleConfigGroups(client, stack.getName(), serviceName, apiRoleConfigGroup.getName(), configMap));
    }

    private Map<String, ApiRoleConfigGroupList> collectRoleConfigGroupConfigsIfNeeded(Table<String, String, String> configTable, Map<String, String> serviceMap,
            Map<String, ApiServiceConfig> serviceConfigs, ApiClient client, StackDtoDelegate stack) {
        return serviceMap.entrySet().stream()
                .filter(serviceEntry -> configTable.rowKeySet().contains(serviceEntry.getKey()))
                .filter(serviceEntry -> {
                    String serviceType = serviceEntry.getKey();
                    Set<String> affectedConfigByService = configTable.row(serviceType).keySet();
                    return !affectedConfigByService.stream().allMatch(config -> configPresentAsServiceConfig(serviceConfigs, serviceType, config));
                })
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> configService.readRoleConfigGroupConfigs(client, stack.getName(), entry.getValue())));
    }

    private boolean configPresentAsServiceConfig(Map<String, ApiServiceConfig> serviceConfigs, String serviceType, String config) {
        return serviceConfigs.get(serviceType).getItems().stream()
                .anyMatch(apiConfig -> org.apache.commons.lang3.StringUtils.equals(apiConfig.getName(), config));
    }

    private Map<String, ApiServiceConfig> collectServiceConfigs(Table<String, String, String> configTable, Map<String, String> serviceMap,
            ApiClient client, StackDtoDelegate stack) {
        return serviceMap.entrySet().stream()
                .filter(serviceEntry -> configTable.rowKeySet().contains(serviceEntry.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> configService.readServiceConfig(client, stack.getName(), entry.getValue())));
    }

    private boolean configExistInConfigList(List<ApiConfig> apiConfigList, String configKey) {
        Predicate<ApiConfig> apiConfigPredicate = apiConfig -> org.apache.commons.lang3.StringUtils.equals(apiConfig.getName(), configKey);
        return apiConfigList.stream().anyMatch(apiConfigPredicate);
    }

}
