package com.sequenceiq.cloudbreak.cm.config;

import java.util.Arrays;
import java.util.Optional;

import javax.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import com.cloudera.api.swagger.model.ApiRole;
import com.cloudera.api.swagger.model.ApiRoleList;
import com.sequenceiq.cloudbreak.cluster.model.ServiceLocationMap;
import com.sequenceiq.cloudbreak.domain.stack.Stack;

@Service
class CmMgmtServiceConfigLocationService implements CmConfigServiceDelegate {

    @Inject
    private CmMgmtVolumePathBuilder volumePathBuilder;

    @Override
    public void setConfigs(Stack stack, ApiRoleList apiRoleList) {
        ServiceLocationMap serviceLocationMap = volumePathBuilder.buildServiceLocationMap();
        Arrays.stream(MgmtServices.values()).forEach(mgmtService -> addConfigIfRoleIsPresent(apiRoleList, serviceLocationMap, mgmtService));
    }

    private void addConfigIfRoleIsPresent(ApiRoleList apiRoleList, ServiceLocationMap serviceLocationMap, MgmtServices mgmtService) {
        Optional<ApiRole> role = getApiRole(mgmtService.name(), apiRoleList);
        role.ifPresent(apiRole -> addConfig(apiRole, createApiConfig(mgmtService.getConfigName(),
                serviceLocationMap.getVolumePathByService(mgmtService.name()))));
    }

    private Optional<ApiRole> getApiRole(String roleName, ApiRoleList apiRoleList) {
        if (CollectionUtils.isEmpty(apiRoleList.getItems())) {
            return Optional.empty();
        }
        return apiRoleList.getItems().stream()
                .filter(apiRole -> apiRole != null && apiRole.getName().equals(roleName))
                .findFirst();
    }
}
