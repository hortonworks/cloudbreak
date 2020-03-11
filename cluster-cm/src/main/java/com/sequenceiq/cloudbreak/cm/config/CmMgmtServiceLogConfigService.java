package com.sequenceiq.cloudbreak.cm.config;

import static com.sequenceiq.cloudbreak.cm.util.ConfigUtils.addConfig;

import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.cloudera.api.swagger.model.ApiConfig;
import com.cloudera.api.swagger.model.ApiRoleList;
import com.sequenceiq.cloudbreak.domain.stack.Stack;

@Service
class CmMgmtServiceLogConfigService implements CmConfigServiceDelegate {

    static final String MAX_LOG_BACKUP_INDEX = "max_log_backup_index";

    static final String VALUE = "1";

    @Override
    public void setConfigs(Stack stack, ApiRoleList apiRoleList) {
        if (Objects.nonNull(apiRoleList) && !CollectionUtils.isEmpty(apiRoleList.getItems())) {
            apiRoleList.getItems().forEach(apiRole -> addConfig(apiRole, createApiConfig()));
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
