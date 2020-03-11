package com.sequenceiq.cloudbreak.cm.config;

import java.util.List;

import org.springframework.stereotype.Service;

import com.cloudera.api.swagger.model.ApiConfig;
import com.cloudera.api.swagger.model.ApiRole;
import com.cloudera.api.swagger.model.ApiRoleList;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.stack.Stack;

@Service
class CmMgmtYarnConfigService extends AbstractCmConfigService {

    static final List<String> SUPPRESSION_NAMES = List.of("process_swap_memory_thresholds");

    static final String SUPPRESSION_VALUE = "{\"critical\":\"never\",\"warning\":\"never\"}";

    @Override
    void setConfigs(Stack stack, ApiRoleList apiRoleList) {
        if (CloudPlatform.YARN.equalsIgnoreCase(stack.getCloudPlatform())) {
            apiRoleList.getItems().forEach(this::addSuppressionConfigs);
        }
    }

    private void addSuppressionConfigs(ApiRole hostMonitor) {
        SUPPRESSION_NAMES.forEach(suppression -> {
            ApiConfig apiConfig = new ApiConfig();
            apiConfig.setName(suppression);
            apiConfig.setValue(SUPPRESSION_VALUE);
            apiConfig.setSensitive(false);

            setConfig(hostMonitor, apiConfig);
        });
    }
}
