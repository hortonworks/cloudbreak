package com.sequenceiq.mock.legacy.clouderamanager.v31.controller;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;
import com.sequenceiq.mock.legacy.clouderamanager.ProfileAwareResponse;
import com.sequenceiq.mock.legacy.clouderamanager.ResponseUtil;
import com.sequenceiq.mock.legacy.clouderamanager.DataProviderService;
import com.sequenceiq.mock.legacy.clouderamanager.DefaultModelService;
import com.sequenceiq.mock.legacy.service.HostNameUtil;
import com.sequenceiq.mock.swagger.v31.api.HostsResourceApi;
import com.sequenceiq.mock.swagger.model.ApiCommand;
import com.sequenceiq.mock.swagger.model.ApiConfigList;
import com.sequenceiq.mock.swagger.model.ApiEntityTag;
import com.sequenceiq.mock.swagger.model.ApiGenerateHostCertsArguments;
import com.sequenceiq.mock.swagger.model.ApiHealthCheck;
import com.sequenceiq.mock.swagger.model.ApiHost;
import com.sequenceiq.mock.swagger.model.ApiHostList;
import com.sequenceiq.mock.swagger.model.ApiMetricList;
import com.sequenceiq.mock.swagger.model.ApiMigrateRolesArguments;

@Controller
public class HostsResourceV31Controller implements HostsResourceApi {

    @Inject
    private HttpServletRequest request;

    @Inject
    private DefaultModelService defaultModelService;

    @Inject
    private DataProviderService dataProviderService;

    @Override
    public ResponseEntity<List<ApiEntityTag>> addTags(String hostname, @Valid List<ApiEntityTag> body) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiHostList> createHosts(@Valid ApiHostList body) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiHostList> deleteAllHosts() {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiHost> deleteHost(String hostId) {
        ApiHost apiHost = dataProviderService.getApiHost(hostId);
        return ProfileAwareResponse.exec(apiHost, defaultModelService);
    }

    @Override
    public ResponseEntity<List<ApiEntityTag>> deleteTags(String hostname, @Valid List<ApiEntityTag> body) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiCommand> enterMaintenanceMode(String hostId) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiCommand> exitMaintenanceMode(String hostId) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiCommand> generateHostCerts(String hostId, @Valid ApiGenerateHostCertsArguments body) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    //CHECKSTYLE:OFF
    public ResponseEntity<ApiMetricList> getMetrics(String hostId, @Valid String from, @Valid List<String> ifs, @Valid List<String> metrics,
            @Valid Boolean queryNw, @Valid Boolean queryStorage, @Valid List<String> storageIds, @Valid String to, @Valid String view) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }
    //CHECKSTYLE:ON

    @Override
    public ResponseEntity<ApiCommand> migrateRoles(String hostId, @Valid ApiMigrateRolesArguments body) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiHost> readHost(String hostId, @Valid String view) {
        ApiHost apiHost = dataProviderService.getApiHost(hostId);
        return ProfileAwareResponse.exec(apiHost, defaultModelService);
    }

    @Override
    public ResponseEntity<ApiConfigList> readHostConfig(String hostId, @Valid String view) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiHostList> readHosts(@Valid String configName, @Valid String configValue, @Valid String view) {
        Map<String, CloudVmMetaDataStatus> instanceMap = defaultModelService.getInstanceMap();
        ApiHostList apiHostList = new ApiHostList();
        for (Map.Entry<String, CloudVmMetaDataStatus> entry : instanceMap.entrySet()) {
            ApiHost apiHost = new ApiHost()
                    .hostId(entry.getValue().getCloudVmInstanceStatus().getCloudInstance().getInstanceId())
                    .hostname(HostNameUtil.generateHostNameByIp(entry.getValue().getMetaData().getPrivateIp()))
                    .ipAddress(entry.getValue().getMetaData().getPrivateIp())
                    .healthChecks(List.of(new ApiHealthCheck()
                            .name("HOST_SCM_HEALTH")
                            .summary(dataProviderService.instanceStatusToApiHealthSummary(entry.getValue()))))
                    .lastHeartbeat(Instant.now().toString());
            apiHostList.addItemsItem(apiHost);
        }
        return ProfileAwareResponse.exec(apiHostList, defaultModelService);
    }

    @Override
    public ResponseEntity<List<ApiEntityTag>> readTags(String hostname, @Valid Integer limit, @Valid Integer offset) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiCommand> resetHostId(String hostId, @Valid String newHostId) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiHost> updateHost(String hostId, @Valid ApiHost body) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiConfigList> updateHostConfig(String hostId, @Valid String message, @Valid ApiConfigList body) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }
}
