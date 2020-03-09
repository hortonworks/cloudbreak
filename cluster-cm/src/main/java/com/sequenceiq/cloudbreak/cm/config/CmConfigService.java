package com.sequenceiq.cloudbreak.cm.config;

import java.util.List;

import org.springframework.stereotype.Service;

import com.cloudera.api.swagger.model.ApiRoleList;
import com.sequenceiq.cloudbreak.domain.stack.Stack;

@Service
public class CmConfigService {

    private List<AbstractCmConfigService> configServices;

    public CmConfigService(List<AbstractCmConfigService> configServices) {
        this.configServices = configServices;
    }

    public void setConfigs(Stack stack, ApiRoleList apiRoleList) {
        configServices.forEach(configService -> configService.setConfigs(stack, apiRoleList));
    }
}
