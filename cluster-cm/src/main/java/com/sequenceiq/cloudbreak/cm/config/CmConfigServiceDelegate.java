package com.sequenceiq.cloudbreak.cm.config;

import com.cloudera.api.swagger.model.ApiRoleList;
import com.sequenceiq.cloudbreak.domain.stack.Stack;

interface CmConfigServiceDelegate {

    void setConfigs(Stack stack, ApiRoleList apiRoleList);
}
