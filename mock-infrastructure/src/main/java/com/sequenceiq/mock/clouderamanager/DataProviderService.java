package com.sequenceiq.mock.clouderamanager;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.mock.service.HostNameService;
import com.sequenceiq.mock.spi.SpiService;
import com.sequenceiq.mock.spi.SpiStoreService;
import com.sequenceiq.mock.swagger.model.ApiCluster;
import com.sequenceiq.mock.swagger.model.ApiClusterTemplateHostTemplate;
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
import com.sequenceiq.mock.swagger.model.ApiService;
import com.sequenceiq.mock.swagger.model.ApiServiceList;

@Component
public class DataProviderService {

    public static final long SECONDS_TO_ADD = 60000L;

    @Inject
    private SpiStoreService spiStoreService;

    @Inject
    private SpiService spiService;

    @Inject
    private ClouderaManagerStoreService clouderaManagerStoreService;

    @Inject
    private HostNameService hostNameService;

    public ApiCommand getSuccessfulApiCommand(Integer id) {
        ApiCommand result = new ApiCommand();
        result.setId(id);
        result.setActive(false);
        result.setSuccess(true);
        return result;
    }

    public ApiHostRefList getHostRefList(String mockUuid) {
        List<CloudVmMetaDataStatus> metadata = spiStoreService.getMetadata(mockUuid);
        ApiHostRefList apiHostList = new ApiHostRefList();
        for (CloudVmMetaDataStatus cloudVmMetaDataStatus : metadata) {
            ApiHostRef apiHost = getApiHostRef(mockUuid, cloudVmMetaDataStatus);
            apiHostList.addItemsItem(apiHost);
        }
        return apiHostList;
    }

    public ApiHostList getHostList(String mockUuid) {
        List<CloudVmMetaDataStatus> metadata = spiStoreService.getMetadata(mockUuid);
        ApiHostList apiHostList = new ApiHostList();
        for (CloudVmMetaDataStatus cloudVmMetaDataStatus : metadata) {
            ApiHost apiHost = getApiHost(mockUuid, cloudVmMetaDataStatus);
            apiHostList.addItemsItem(apiHost);
        }
        return apiHostList;
    }

    public ApiHostRef getApiHostRef(String mockUuid, String hostId) {
        CloudVmMetaDataStatus cloudVmMetaDataStatus = spiService.getByInstanceId(mockUuid, hostId);
        return getApiHostRef(mockUuid, cloudVmMetaDataStatus);
    }

    public ApiHostRef getApiHostRef(String mockUuid, CloudVmMetaDataStatus cloudVmMetaDataStatus) {
        String privateIp = cloudVmMetaDataStatus.getMetaData().getPrivateIp();
        String hostname = hostNameService.getHostName(mockUuid, privateIp);
        return new ApiHostRef()
                .hostId(cloudVmMetaDataStatus.getCloudVmInstanceStatus().getCloudInstance().getInstanceId())
                .hostname(hostname);
    }

    public ApiHealthSummary instanceStatusToApiHealthSummary(CloudVmMetaDataStatus cloudVmMetaDataStatus) {
        if (cloudVmMetaDataStatus.getCloudVmInstanceStatus().getStatus() == InstanceStatus.STARTED) {
            return ApiHealthSummary.GOOD;
        }
        return ApiHealthSummary.BAD;
    }

    public ApiHost getApiHost(String mockUuid, String hostId) {
        CloudVmMetaDataStatus cloudVmMetaDataStatus = spiService.getByInstanceId(mockUuid, hostId);
        return getApiHost(mockUuid, cloudVmMetaDataStatus);
    }

    public ApiHostList readHosts(String mockUuid) {
        List<CloudVmMetaDataStatus> metadata = spiStoreService.getMetadata(mockUuid);
        ApiHostList apiHostList = new ApiHostList();
        for (CloudVmMetaDataStatus cloudVmMetaDataStatus : metadata) {
            ApiHost apiHost = getApiHost(mockUuid, cloudVmMetaDataStatus);
            apiHostList.addItemsItem(apiHost);
        }
        return apiHostList;
    }

    public ApiHost getApiHost(String mockUuid, CloudVmMetaDataStatus cloudVmMetaDataStatus) {
        ApiHealthSummary healthSummary = instanceStatusToApiHealthSummary(cloudVmMetaDataStatus);
        String privateIp = cloudVmMetaDataStatus.getMetaData().getPrivateIp();
        String hostname = hostNameService.getHostName(mockUuid, privateIp);
        return new ApiHost()
                .hostId(cloudVmMetaDataStatus.getCloudVmInstanceStatus().getCloudInstance().getInstanceId())
                .hostname(hostname)
                .ipAddress(privateIp)
                .lastHeartbeat(Instant.now().plusSeconds(SECONDS_TO_ADD).toString())
                .healthSummary(healthSummary)
                .healthChecks(List.of(new ApiHealthCheck()
                        .name("HOST_SCM_HEALTH")
                        .summary(healthSummary)));
    }

    public ApiHostTemplateList hostTemplates(String mockUuid) {
        List<ApiClusterTemplateHostTemplate> hostTemplates = clouderaManagerStoreService.read(mockUuid).getClusterTemplate().getHostTemplates();
        List<ApiHostTemplate> templates = hostTemplates.stream().map(this::getApiHostTemplate).collect(Collectors.toList());
        return new ApiHostTemplateList().items(templates);
    }

    private ApiHostTemplate getApiHostTemplate(ApiClusterTemplateHostTemplate templateHostTemplate) {
        List<ApiRoleConfigGroupRef> list = templateHostTemplate.getRoleConfigGroupsRefNames()
                .stream().map(s -> new ApiRoleConfigGroupRef().roleConfigGroupName(s))
                .collect(Collectors.toList());
        return new ApiHostTemplate()
                .name(templateHostTemplate.getRefName())
                .roleConfigGroupRefs(list);
    }

    public ApiHostTemplate getApiHostTemplate(String name, String group) {
        return new ApiHostTemplate()
                .name(name)
                .roleConfigGroupRefs(
                        List.of(new ApiRoleConfigGroupRef().roleConfigGroupName(group)
                        ));
    }

    public ApiCluster readCluster(String mockUuid, String clusterName) {
        ClouderaManagerDto cmDto = clouderaManagerStoreService.read(mockUuid);
        if (cmDto.getClusterTemplate() != null) {
            return new ApiCluster();
        }
        return null;
    }

    public ApiServiceList readServices(String mockUuid, String clusterName, String view) {
        ClouderaManagerDto read = clouderaManagerStoreService.read(mockUuid);
        List<ApiService> services = read.getClusterTemplate().getServices().stream()
                .map(s -> new ApiService()
                        .name(s.getRefName())
                        .serviceState(read.getServiceStates().get(s.getRefName()))
                        .displayName(s.getDisplayName())).collect(Collectors.toList());
        return new ApiServiceList().items(services);
    }
}
