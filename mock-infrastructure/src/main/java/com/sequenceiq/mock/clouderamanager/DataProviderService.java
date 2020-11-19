package com.sequenceiq.mock.clouderamanager;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.mock.HostNameUtil;
import com.sequenceiq.mock.spi.SpiService;
import com.sequenceiq.mock.spi.SpiStoreService;
import com.sequenceiq.mock.swagger.model.ApiAuthRoleRef;
import com.sequenceiq.mock.swagger.model.ApiCommand;
import com.sequenceiq.mock.swagger.model.ApiHealthCheck;
import com.sequenceiq.mock.swagger.model.ApiHealthSummary;
import com.sequenceiq.mock.swagger.model.ApiHost;
import com.sequenceiq.mock.swagger.model.ApiHostList;
import com.sequenceiq.mock.swagger.model.ApiHostRef;
import com.sequenceiq.mock.swagger.model.ApiHostRefList;
import com.sequenceiq.mock.swagger.model.ApiHostTemplate;
import com.sequenceiq.mock.swagger.model.ApiHostTemplateList;
import com.sequenceiq.mock.swagger.model.ApiRoleConfigGroupRef;
import com.sequenceiq.mock.swagger.model.ApiUser2;
import com.sequenceiq.mock.swagger.model.ApiUser2List;

@Component
public class DataProviderService {

    public static final long SECONDS_TO_ADD = 60000L;

    @Inject
    private SpiStoreService spiStoreService;

    @Inject
    private SpiService spiService;

    public ApiUser2List getUserList() {
        ApiUser2List apiUser2List = new ApiUser2List();
        ApiAuthRoleRef authRoleRef = new ApiAuthRoleRef().displayName("Full Administrator").uuid(UUID.randomUUID().toString());
        apiUser2List.addItemsItem(new ApiUser2().name("admin").addAuthRolesItem(authRoleRef));
        apiUser2List.addItemsItem(new ApiUser2().name("cloudbreak").addAuthRolesItem(authRoleRef));
        return apiUser2List;
    }

    public ApiCommand getSuccessfulApiCommand() {
        ApiCommand result = new ApiCommand();
        result.setId(BigDecimal.valueOf(1L));
        result.setActive(true);
        result.setSuccess(true);
        return result;
    }

    public ApiHostRefList getHostRefList(String mockUuid) {
        List<CloudVmMetaDataStatus> metadata = spiStoreService.getMetadata(mockUuid);
        ApiHostRefList apiHostList = new ApiHostRefList();
        for (CloudVmMetaDataStatus cloudVmMetaDataStatus : metadata) {
            ApiHostRef apiHost = getApiHostRef(cloudVmMetaDataStatus);
            apiHostList.addItemsItem(apiHost);
        }
        return apiHostList;
    }

    public ApiHostRef getApiHostRef(String mockUuid, String hostId) {
        CloudVmMetaDataStatus cloudVmMetaDataStatus = spiService.getByInstanceId(mockUuid, hostId);
        return getApiHostRef(cloudVmMetaDataStatus);
    }

    public ApiHostRef getApiHostRef(CloudVmMetaDataStatus cloudVmMetaDataStatus) {
        return new ApiHostRef()
                .hostId(cloudVmMetaDataStatus.getCloudVmInstanceStatus().getCloudInstance().getInstanceId())
                .hostname(HostNameUtil.generateHostNameByIp(cloudVmMetaDataStatus.getMetaData().getPrivateIp()));
    }

    public ApiHealthSummary instanceStatusToApiHealthSummary(CloudVmMetaDataStatus cloudVmMetaDataStatus) {
        if (cloudVmMetaDataStatus.getCloudVmInstanceStatus().getStatus() == InstanceStatus.STARTED) {
            return ApiHealthSummary.GOOD;
        }
        return ApiHealthSummary.BAD;
    }

    public ApiHost getApiHost(String mockUuid, String hostId) {
        CloudVmMetaDataStatus cloudVmMetaDataStatus = spiService.getByInstanceId(mockUuid, hostId);
        return getApiHost(cloudVmMetaDataStatus);
    }

    public ApiHostList readHosts(String mockUuid) {
        List<CloudVmMetaDataStatus> metadata = spiStoreService.getMetadata(mockUuid);
        ApiHostList apiHostList = new ApiHostList();
        for (CloudVmMetaDataStatus cloudVmMetaDataStatus : metadata) {
            ApiHost apiHost = new ApiHost()
                    .hostId(cloudVmMetaDataStatus.getCloudVmInstanceStatus().getCloudInstance().getInstanceId())
                    .hostname(HostNameUtil.generateHostNameByIp(cloudVmMetaDataStatus.getMetaData().getPrivateIp()))
                    .ipAddress(cloudVmMetaDataStatus.getMetaData().getPrivateIp())
                    .healthChecks(List.of(new ApiHealthCheck()
                            .name("HOST_SCM_HEALTH")
                            .summary(instanceStatusToApiHealthSummary(cloudVmMetaDataStatus))))
                    .lastHeartbeat(Instant.now().toString());
            apiHostList.addItemsItem(apiHost);
        }
        return apiHostList;
    }

    public ApiHost getApiHost(CloudVmMetaDataStatus cloudVmMetaDataStatus) {
        ApiHealthSummary healthSummary = instanceStatusToApiHealthSummary(cloudVmMetaDataStatus);
        return new ApiHost()
                .hostId(cloudVmMetaDataStatus.getCloudVmInstanceStatus().getCloudInstance().getInstanceId())
                .hostname(HostNameUtil.generateHostNameByIp(cloudVmMetaDataStatus.getMetaData().getPrivateIp()))
                .ipAddress(cloudVmMetaDataStatus.getMetaData().getPrivateIp())
                .lastHeartbeat(Instant.now().plusSeconds(SECONDS_TO_ADD).toString())
                .healthSummary(healthSummary)
                .healthChecks(List.of(new ApiHealthCheck()
                        .name("HOST_SCM_HEALTH")
                        .summary(healthSummary)));
    }

    public ApiHostTemplateList hostTemplates() {
        ApiHostTemplate hostTemplateWorker = getApiHostTemplate("worker", "WORKER");
        ApiHostTemplate hostTemplateCompute = getApiHostTemplate("compute", "DATANODE");
        return new ApiHostTemplateList().items(List.of(hostTemplateWorker, hostTemplateCompute));
    }

    public ApiHostTemplate getApiHostTemplate(String name, String group) {
        return new ApiHostTemplate()
                .name(name)
                .roleConfigGroupRefs(
                        List.of(new ApiRoleConfigGroupRef().roleConfigGroupName(group)
                        ));
    }
}
