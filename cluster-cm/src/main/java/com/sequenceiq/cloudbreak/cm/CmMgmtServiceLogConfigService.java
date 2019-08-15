package com.sequenceiq.cloudbreak.cm;

import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.cloudera.api.swagger.model.ApiConfig;
import com.cloudera.api.swagger.model.ApiConfigList;
import com.cloudera.api.swagger.model.ApiRole;
import com.cloudera.api.swagger.model.ApiRoleList;

@Service
class CmMgmtServiceLogConfigService {

    private static final String MAX_LOG_BACKUP_INDEX = "max_log_backup_index";

    private static final String VALUE = "1";

    void setLogConfig(ApiRoleList apiRoleList) {
        if (Objects.nonNull(apiRoleList) && !CollectionUtils.isEmpty(apiRoleList.getItems())) {
            apiRoleList.getItems().forEach(apiRole -> {
                setConfig(apiRole, createApiConfig());
            });
        }
    }

    private void setConfig(ApiRole apiRole, ApiConfig apiConfig) {
        if (apiRole.getConfig() == null || apiRole.getConfig().getItems() == null) {
            ApiConfigList apiConfigList = new ApiConfigList();
            apiConfigList.addItemsItem(apiConfig);
            apiRole.setConfig(apiConfigList);
        } else {
            apiRole.getConfig().getItems().add(apiConfig);
        }
    }

    private ApiConfig createApiConfig() {
        ApiConfig apiConfig = new ApiConfig();
        apiConfig.setName(CmMgmtServiceLogConfigService.MAX_LOG_BACKUP_INDEX);
        apiConfig.setValue(CmMgmtServiceLogConfigService.VALUE);
        apiConfig.setSensitive(false);
        return apiConfig;
    }
}
