package com.sequenceiq.cloudbreak.cm.config;

import java.util.Optional;

import com.cloudera.api.swagger.model.ApiConfig;
import com.cloudera.api.swagger.model.ApiConfigList;
import com.cloudera.api.swagger.model.ApiRole;
import com.cloudera.api.swagger.model.ApiRoleList;
import com.sequenceiq.cloudbreak.domain.stack.Stack;

abstract class AbstractCmConfigService {

    abstract void setConfigs(Stack stack, ApiRoleList apiRoleList);

    protected Optional<ApiRole> getApiRole(String roleName, ApiRoleList apiRoleList) {
        return apiRoleList.getItems().stream()
                .filter(apiRole -> apiRole.getName().equals(roleName))
                .findFirst();
    }

    protected void setConfig(ApiRole apiRole, ApiConfig apiConfig) {
        if (apiRole.getConfig() == null || apiRole.getConfig().getItems() == null) {
            ApiConfigList apiConfigList = new ApiConfigList();
            apiConfigList.addItemsItem(apiConfig);
            apiRole.setConfig(apiConfigList);
        } else {
            apiRole.getConfig().getItems().add(apiConfig);
        }
    }
}
