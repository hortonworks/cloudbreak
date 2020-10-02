package com.sequenceiq.cloudbreak.cm.config;

import static com.sequenceiq.cloudbreak.template.VolumeUtils.buildSingleVolumePath;

import java.util.Optional;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Service;

import com.cloudera.api.swagger.model.ApiRole;
import com.cloudera.api.swagger.model.ApiRoleList;
import com.sequenceiq.cloudbreak.domain.stack.Stack;

@Service
class CmMgmtServiceConfigLocationService implements CmConfigServiceDelegate {

    @Override
    public void setConfigs(Stack stack, ApiRoleList apiRoleList) {
        Optional<ApiRole> eventServer = getApiRole("EVENTSERVER", apiRoleList);
        eventServer.ifPresent(apiRole -> addConfig(apiRole, createApiConfig("eventserver_index_dir",
                buildSingleVolumePath(1, "cloudera-scm-eventserver"))));

        Optional<ApiRole> hostMonitor = getApiRole("HOSTMONITOR", apiRoleList);
        hostMonitor.ifPresent(apiRole -> addConfig(apiRole, createApiConfig("firehose_storage_dir",
                buildSingleVolumePath(1, "cloudera-host-monitor"))));

        Optional<ApiRole> reportsManager = getApiRole("REPORTSMANAGER", apiRoleList);
        reportsManager.ifPresent(apiRole -> addConfig(apiRole, createApiConfig("headlamp_scratch_dir",
                buildSingleVolumePath(1, "cloudera-scm-headlamp"))));

        Optional<ApiRole> serviceMonitor = getApiRole("SERVICEMONITOR", apiRoleList);
        serviceMonitor.ifPresent(apiRole -> addConfig(apiRole, createApiConfig("firehose_storage_dir",
                buildSingleVolumePath(1, "cloudera-service-monitor"))));
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
