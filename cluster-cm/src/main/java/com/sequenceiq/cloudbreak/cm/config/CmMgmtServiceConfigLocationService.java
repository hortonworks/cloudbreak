package com.sequenceiq.cloudbreak.cm.config;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.cloudera.api.swagger.model.ApiConfig;
import com.cloudera.api.swagger.model.ApiRole;
import com.cloudera.api.swagger.model.ApiRoleList;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.template.VolumeUtils;

@Service
class CmMgmtServiceConfigLocationService extends AbstractCmConfigService {

    @Override
    void setConfigs(Stack stack, ApiRoleList apiRoleList) {
        List<Resource> attachedDisks = stack.getDiskResources();
        if (!attachedDisks.isEmpty()) {
            Optional<ApiRole> eventServer = getApiRole("EVENTSERVER", apiRoleList);
            eventServer.ifPresent(apiRole -> setConfig(apiRole, createApiConfig("eventserver_index_dir", "cloudera-scm-eventserver", attachedDisks.size())));

            Optional<ApiRole> hostMonitor = getApiRole("HOSTMONITOR", apiRoleList);
            hostMonitor.ifPresent(apiRole -> setConfig(apiRole, createApiConfig("firehose_storage_dir", "cloudera-host-monitor", attachedDisks.size())));

            Optional<ApiRole> reportsManager = getApiRole("REPORTSMANAGER", apiRoleList);
            reportsManager.ifPresent(apiRole -> setConfig(apiRole, createApiConfig("headlamp_scratch_dir", "cloudera-scm-headlamp", attachedDisks.size())));

            Optional<ApiRole> serviceMonitor = getApiRole("SERVICEMONITOR", apiRoleList);
            serviceMonitor.ifPresent(apiRole -> setConfig(apiRole, createApiConfig("firehose_storage_dir", "cloudera-service-monitor", attachedDisks.size())));
        }
    }

    private Optional<ApiRole> getApiRole(String roleName, ApiRoleList apiRoleList) {
        return apiRoleList.getItems().stream()
                .filter(apiRole -> apiRole.getName().equals(roleName))
                .findFirst();
    }

    private ApiConfig createApiConfig(String name, String configDir, int numberOfVolumes) {
        ApiConfig apiConfig = new ApiConfig();
        apiConfig.setName(name);
        apiConfig.setValue(VolumeUtils.buildSingleVolumePath(numberOfVolumes, configDir));
        apiConfig.setSensitive(false);
        return apiConfig;
    }
}
