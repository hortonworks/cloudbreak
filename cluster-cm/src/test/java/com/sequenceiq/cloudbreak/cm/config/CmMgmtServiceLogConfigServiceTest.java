package com.sequenceiq.cloudbreak.cm.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.cloudera.api.swagger.model.ApiConfig;
import com.cloudera.api.swagger.model.ApiRole;
import com.cloudera.api.swagger.model.ApiRoleList;
import com.sequenceiq.cloudbreak.domain.stack.Stack;

class CmMgmtServiceLogConfigServiceTest {

    private CmMgmtServiceLogConfigService underTest = new CmMgmtServiceLogConfigService();

    @Test
    public void shouldAddConfigToRoles() {
        Stack stack = new Stack();
        ApiRoleList apiRoleList = new ApiRoleList();
        ApiRole apiRole = new ApiRole();
        apiRoleList.addItemsItem(apiRole);

        underTest.setConfigs(stack, apiRoleList);

        assertThat(apiRole.getConfig().getItems())
                .anySatisfy(apiConfig -> assertThat(apiConfig)
                        .returns(CmMgmtServiceLogConfigService.MAX_LOG_BACKUP_INDEX, ApiConfig::getName)
                        .returns(CmMgmtServiceLogConfigService.VALUE, ApiConfig::getValue));
    }

}