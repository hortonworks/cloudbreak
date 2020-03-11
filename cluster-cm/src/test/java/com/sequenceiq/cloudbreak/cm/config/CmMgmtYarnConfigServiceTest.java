package com.sequenceiq.cloudbreak.cm.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.cloudera.api.swagger.model.ApiRole;
import com.cloudera.api.swagger.model.ApiRoleList;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.stack.Stack;

class CmMgmtYarnConfigServiceTest {

    private CmMgmtYarnConfigService underTest = new CmMgmtYarnConfigService();

    @Test
    public void shouldNotDoAnythingOnNonYcloud() {
        Stack stack = new Stack();
        stack.setCloudPlatform(CloudPlatform.AWS.name());
        ApiRoleList apiRoleList = new ApiRoleList();

        underTest.setConfigs(stack, apiRoleList);

        assertThat(apiRoleList.getItems()).isNullOrEmpty();
    }

    @Test
    public void shouldAddApiRolesOnYarn() {
        Stack stack = new Stack();
        stack.setCloudPlatform(CloudPlatform.YARN.name());
        ApiRoleList apiRoleList = new ApiRoleList();
        ApiRole apiRole = new ApiRole();
        apiRoleList.addItemsItem(apiRole);

        underTest.setConfigs(stack, apiRoleList);

        assertThat(apiRole.getConfig()).isNotNull();
        assertThat(apiRole.getConfig().getItems()).allSatisfy(apiConfig -> {
            assertThat(apiConfig.getName()).isIn(CmMgmtYarnConfigService.SUPPRESSION_NAMES);
            assertThat(apiConfig.getValue()).isEqualTo(CmMgmtYarnConfigService.SUPPRESSION_VALUE);
        });
    }
}