package com.sequenceiq.cloudbreak.cm.config;

import com.cloudera.api.swagger.model.ApiConfig;
import com.cloudera.api.swagger.model.ApiConfigList;
import com.cloudera.api.swagger.model.ApiRole;
import com.cloudera.api.swagger.model.ApiRoleList;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;

interface CmConfigServiceDelegate {

    void setConfigs(StackDtoDelegate stack, ApiRoleList apiRoleList);

    default void addConfig(ApiRole apiRole, ApiConfig apiConfig) {
        if (apiRole.getConfig() == null || apiRole.getConfig().getItems() == null) {
            ApiConfigList apiConfigList = new ApiConfigList();
            apiConfigList.addItemsItem(apiConfig);
            apiRole.setConfig(apiConfigList);
        } else {
            apiRole.getConfig().getItems().add(apiConfig);
        }
    }

    default ApiConfig createApiConfig(String name, String value) {
        ApiConfig apiConfig = new ApiConfig();
        apiConfig.setName(name);
        apiConfig.setValue(value);
        apiConfig.setSensitive(false);
        return apiConfig;
    }
}
