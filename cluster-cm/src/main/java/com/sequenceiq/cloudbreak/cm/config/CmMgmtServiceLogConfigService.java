package com.sequenceiq.cloudbreak.cm.config;

import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.cloudera.api.swagger.model.ApiRoleList;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;

@Service
class CmMgmtServiceLogConfigService implements CmConfigServiceDelegate {

    static final String MAX_LOG_BACKUP_INDEX = "max_log_backup_index";

    static final String VALUE = "1";

    @Override
    public void setConfigs(StackDtoDelegate stack, ApiRoleList apiRoleList) {
        if (Objects.nonNull(apiRoleList) && !CollectionUtils.isEmpty(apiRoleList.getItems())) {
            apiRoleList.getItems().forEach(apiRole -> addConfig(apiRole,
                    createApiConfig(CmMgmtServiceLogConfigService.MAX_LOG_BACKUP_INDEX, CmMgmtServiceLogConfigService.VALUE)));
        }
    }
}
