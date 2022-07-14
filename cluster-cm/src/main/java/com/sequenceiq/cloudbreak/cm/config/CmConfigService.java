package com.sequenceiq.cloudbreak.cm.config;

import java.util.List;

import org.springframework.stereotype.Service;

import com.cloudera.api.swagger.model.ApiRoleList;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;

@Service
public class CmConfigService {

    private List<CmConfigServiceDelegate> delegates;

    public CmConfigService(List<CmConfigServiceDelegate> delegates) {
        this.delegates = delegates;
    }

    public void setConfigs(StackDtoDelegate stack, ApiRoleList apiRoleList) {
        delegates.forEach(configService -> configService.setConfigs(stack, apiRoleList));
    }
}
