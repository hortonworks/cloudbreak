package com.sequenceiq.cloudbreak.cm.config;

import java.util.Arrays;
import java.util.Optional;

import jakarta.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cloudera.api.swagger.model.ApiRole;
import com.cloudera.api.swagger.model.ApiRoleList;
import com.sequenceiq.cloudbreak.cluster.model.ServiceLocationMap;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessorFactory;
import com.sequenceiq.cloudbreak.cmtemplate.utils.BlueprintUtils;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.common.api.type.InstanceGroupName;

@Service
class CmMgmtServiceConfigLocationService implements CmConfigServiceDelegate {

    private static final Logger LOGGER = LoggerFactory.getLogger(CmMgmtServiceConfigLocationService.class);

    // In Enterprise Data Lakes without master host group CM MGMT shares the node with ZooKeeper and KRaft (fs1),
    // so its data shares the Kafka volume (fs2) to keep the quorum disk free for low latency fsyncs
    private static final int MGMT_VOLUME_INDEX_SHARED_WITH_KAFKA = 2;

    @Inject
    private CmMgmtVolumePathBuilder volumePathBuilder;

    @Inject
    private BlueprintUtils blueprintUtils;

    @Inject
    private CmTemplateProcessorFactory cmTemplateProcessorFactory;

    @Override
    public void setConfigs(StackDtoDelegate stack, ApiRoleList apiRoleList) {
        ServiceLocationMap serviceLocationMap = buildServiceLocationMap(stack);
        Arrays.stream(MgmtServices.values()).forEach(mgmtService -> addConfigIfRoleIsPresent(apiRoleList, serviceLocationMap, mgmtService));
    }

    private ServiceLocationMap buildServiceLocationMap(StackDtoDelegate stack) {
        if (isEnterpriseDatalakeWithoutMasterHostGroup(stack)) {
            LOGGER.info("CM MGMT service directories are placed on volume {} shared with Kafka", MGMT_VOLUME_INDEX_SHARED_WITH_KAFKA);
            return volumePathBuilder.buildServiceLocationMap(MGMT_VOLUME_INDEX_SHARED_WITH_KAFKA);
        }
        return volumePathBuilder.buildServiceLocationMap();
    }

    private boolean isEnterpriseDatalakeWithoutMasterHostGroup(StackDtoDelegate stack) {
        return blueprintUtils.isEnterpriseDatalake(stack)
                && !cmTemplateProcessorFactory.get(stack.getBlueprintJsonText()).getHostTemplateNames().contains(InstanceGroupName.MASTER.getName());
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
