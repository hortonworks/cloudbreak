package com.sequenceiq.mock.clouderamanager.v40.controller;

import java.time.Instant;
import java.util.List;

import javax.inject.Inject;
import javax.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;
import com.sequenceiq.mock.HostNameUtil;
import com.sequenceiq.mock.clouderamanager.DataProviderService;
import com.sequenceiq.mock.clouderamanager.ProfileAwareComponent;
import com.sequenceiq.mock.spi.SpiStoreService;
import com.sequenceiq.mock.swagger.model.ApiHealthCheck;
import com.sequenceiq.mock.swagger.model.ApiHost;
import com.sequenceiq.mock.swagger.model.ApiHostList;
import com.sequenceiq.mock.swagger.v40.api.HostsResourceApi;

@Controller
public class HostsResourceV40Controller implements HostsResourceApi {

    @Inject
    private SpiStoreService spiStoreService;

    @Inject
    private DataProviderService dataProviderService;

    @Inject
    private ProfileAwareComponent profileAwareComponent;

    @Override
    public ResponseEntity<ApiHost> deleteHost(String mockUuid, String hostId) {
        ApiHost apiHost = dataProviderService.getApiHost(mockUuid, hostId);
        return profileAwareComponent.exec(apiHost);
    }

    @Override
    public ResponseEntity<ApiHost> readHost(String mockUuid, String hostId, @Valid String view) {
        ApiHost apiHost = dataProviderService.getApiHost(mockUuid, hostId);
        return profileAwareComponent.exec(apiHost);
    }

    @Override
    public ResponseEntity<ApiHostList> readHosts(String mockUuid, @Valid String configName, @Valid String configValue, @Valid String view) {
        List<CloudVmMetaDataStatus> instanceMap = spiStoreService.getMetadata(mockUuid);
        ApiHostList apiHostList = new ApiHostList();
        for (CloudVmMetaDataStatus metaDataStatus : instanceMap) {
            ApiHost apiHost = new ApiHost()
                    .hostId(metaDataStatus.getCloudVmInstanceStatus().getCloudInstance().getInstanceId())
                    .hostname(HostNameUtil.generateHostNameByIp(metaDataStatus.getMetaData().getPrivateIp()))
                    .ipAddress(metaDataStatus.getMetaData().getPrivateIp())
                    .healthChecks(List.of(new ApiHealthCheck()
                            .name("HOST_SCM_HEALTH")
                            .summary(dataProviderService.instanceStatusToApiHealthSummary(metaDataStatus))))
                    .lastHeartbeat(Instant.now().toString());
            apiHostList.addItemsItem(apiHost);
        }
        return profileAwareComponent.exec(apiHostList);
    }
}
