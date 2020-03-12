package com.sequenceiq.cloudbreak.cm.config;

import com.cloudera.api.swagger.model.ApiConfig;
import com.cloudera.api.swagger.model.ApiConfigList;
import com.cloudera.api.swagger.model.ApiRole;
import com.cloudera.api.swagger.model.ApiRoleList;
import com.sequenceiq.cloudbreak.domain.stack.Stack;

interface CmConfigServiceDelegate {

    void setConfigs(Stack stack, ApiRoleList apiRoleList);

    default void addConfig(ApiRole apiRole, ApiConfig apiConfig) {
        if (apiRole.getConfig() == null || apiRole.getConfig().getItems() == null) {
            ApiConfigList apiConfigList = new ApiConfigList();
            apiConfigList.addItemsItem(apiConfig);
            apiRole.setConfig(apiConfigList);
        } else {
            apiRole.getConfig().getItems().add(apiConfig);
        }
    }
}
