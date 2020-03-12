package com.sequenceiq.cloudbreak.cm.config;

import java.util.List;

import org.springframework.stereotype.Service;

import com.cloudera.api.swagger.model.ApiRoleList;
import com.sequenceiq.cloudbreak.domain.stack.Stack;

@Service
public class CmConfigService {

    private List<CmConfigServiceDelegate> delegates;

    public CmConfigService(List<CmConfigServiceDelegate> delegates) {
        this.delegates = delegates;
    }

    public void setConfigs(Stack stack, ApiRoleList apiRoleList) {
        delegates.forEach(configService -> configService.setConfigs(stack, apiRoleList));
    }
}
