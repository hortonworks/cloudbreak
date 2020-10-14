package com.sequenceiq.mock.legacy.clouderamanager.v31.controller;

import java.math.BigDecimal;
import java.util.List;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

import com.sequenceiq.mock.legacy.clouderamanager.ProfileAwareResponse;
import com.sequenceiq.mock.legacy.clouderamanager.ResponseUtil;
import com.sequenceiq.mock.legacy.clouderamanager.DataProviderService;
import com.sequenceiq.mock.legacy.clouderamanager.DefaultModelService;
import com.sequenceiq.mock.swagger.v31.api.ClustersResourceApi;
import com.sequenceiq.mock.swagger.model.ApiCdhUpgradeArgs;
import com.sequenceiq.mock.swagger.model.ApiCluster;
import com.sequenceiq.mock.swagger.model.ApiClusterList;
import com.sequenceiq.mock.swagger.model.ApiClusterPerfInspectorArgs;
import com.sequenceiq.mock.swagger.model.ApiClusterTemplate;
import com.sequenceiq.mock.swagger.model.ApiClusterUtilization;
import com.sequenceiq.mock.swagger.model.ApiCommand;
import com.sequenceiq.mock.swagger.model.ApiCommandList;
import com.sequenceiq.mock.swagger.model.ApiConfigureForKerberosArguments;
import com.sequenceiq.mock.swagger.model.ApiEntityTag;
import com.sequenceiq.mock.swagger.model.ApiHdfsUpgradeDomainList;
import com.sequenceiq.mock.swagger.model.ApiHostRef;
import com.sequenceiq.mock.swagger.model.ApiHostRefList;
import com.sequenceiq.mock.swagger.model.ApiKerberosInfo;
import com.sequenceiq.mock.swagger.model.ApiRestartClusterArgs;
import com.sequenceiq.mock.swagger.model.ApiRollingRestartClusterArgs;
import com.sequenceiq.mock.swagger.model.ApiRollingUpgradeServicesArgs;
import com.sequenceiq.mock.swagger.model.ApiServiceList;
import com.sequenceiq.mock.swagger.model.ApiServiceTypeList;

@Controller
public class ClusterResourceV31Controller implements ClustersResourceApi {

    @Inject
    private HttpServletRequest request;

    @Inject
    private DefaultModelService defaultModelService;

    @Inject
    private DataProviderService dataProviderService;

    @Override
    public ResponseEntity<ApiHostRefList> addHosts(String clusterName, @Valid ApiHostRefList body) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<List<ApiEntityTag>> addTags(String clusterName, @Valid List<ApiEntityTag> body) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<Void> autoAssignRoles(String clusterName) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<Void> autoConfigure(String clusterName) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiCommand> configureAutoTlsServicesCommand(String clusterName) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiCommand> configureForKerberos(String clusterName, @Valid ApiConfigureForKerberosArguments body) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiClusterList> createClusters(@Valid ApiClusterList body) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiCluster> deleteCluster(String clusterName) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiCommand> deleteClusterCredentialsCommand(String clusterName) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<List<ApiEntityTag>> deleteTags(String clusterName, @Valid List<ApiEntityTag> body) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiCommand> deployClientConfig(String clusterName) {
        return ProfileAwareResponse.exec(dataProviderService.getSuccessfulApiCommand(), defaultModelService);
    }

    @Override
    public ResponseEntity<ApiCommand> deployClientConfigsAndRefresh(String clusterName) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiCommand> deployClusterClientConfig(String clusterName, @Valid ApiHostRefList body) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiCommand> enterMaintenanceMode(String clusterName) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiCommand> exitMaintenanceMode(String clusterName) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiCommand> expireLogs(String clusterName, @Valid BigDecimal days) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiClusterTemplate> export(String clusterName, @Valid Boolean exportAutoConfig) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiCommand> firstRun(String clusterName) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<Resource> getClientConfig(String clusterName) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiKerberosInfo> getKerberosInfo(String clusterName) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiClusterUtilization> getUtilizationReport(String clusterName, @Valid List<String> daysOfWeek, @Valid Integer endHourOfDay,
            @Valid String from, @Valid Integer startHourOfDay, @Valid String tenantType, @Valid String to) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiCommand> inspectHostsCommand(String clusterName) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiCommandList> listActiveCommands(String clusterName, @Valid String view) {
        ApiCommandList something = new ApiCommandList().items(List.of(new ApiCommand().name("something")));
        return ProfileAwareResponse.exec(something, defaultModelService);
    }

    @Override
    public ResponseEntity<ApiServiceList> listDfsServices(String clusterName, @Valid String view) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiHostRefList> listHosts(String clusterName, @Valid String configName, @Valid String configValue) {
        return ProfileAwareResponse.get(dataProviderService.getHostRefList(), defaultModelService).handle();
    }

    @Override
    public ResponseEntity<ApiServiceTypeList> listServiceTypes(String clusterName) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiHdfsUpgradeDomainList> listUpgradeDomains(String clusterName) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiCommand> perfInspectorCommand(String clusterName, @Valid ApiClusterPerfInspectorArgs body) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiCommand> poolsRefresh(String clusterName) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiCommand> preUpgradeCheckCommand(String clusterName, @Valid ApiCdhUpgradeArgs body) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiCluster> readCluster(String clusterName) {
        return ProfileAwareResponse.exec(new ApiCluster(), defaultModelService);
    }

    @Override
    public ResponseEntity<ApiClusterList> readClusters(@Valid String clusterType, @Valid String view) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<List<ApiEntityTag>> readTags(String clusterName, @Valid Integer limit, @Valid Integer offset) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiCommand> refresh(String clusterName) {
        return ProfileAwareResponse.exec(dataProviderService.getSuccessfulApiCommand(), defaultModelService);
    }

    @Override
    public ResponseEntity<ApiHostRefList> removeAllHosts(String clusterName) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiHostRef> removeHost(String clusterName, String hostId) {
        return ProfileAwareResponse.get(dataProviderService.getApiHostRef(hostId), defaultModelService).handle();
    }

    @Override
    public ResponseEntity<ApiCommand> restartCommand(String clusterName, @Valid ApiRestartClusterArgs body) {
        return ProfileAwareResponse.exec(dataProviderService.getSuccessfulApiCommand(), defaultModelService);
    }

    @Override
    public ResponseEntity<ApiCommand> rollingRestart(String clusterName, @Valid ApiRollingRestartClusterArgs body) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiCommand> rollingUpgrade(String clusterName, @Valid ApiRollingUpgradeServicesArgs body) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiCommand> startCommand(String clusterName) {
        return ProfileAwareResponse.get(new ApiCommand().id(BigDecimal.ONE).active(Boolean.TRUE).name("Start"), defaultModelService).handle();
    }

    @Override
    public ResponseEntity<ApiCommand> stopCommand(String clusterName) {
        return ProfileAwareResponse.get(new ApiCommand().id(BigDecimal.ONE).active(Boolean.TRUE).name("Stop"), defaultModelService).handle();
    }

    @Override
    public ResponseEntity<ApiCluster> updateCluster(String clusterName, @Valid ApiCluster body) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiCommand> upgradeCdhCommand(String clusterName, @Valid ApiCdhUpgradeArgs body) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiCommand> upgradeServicesCommand(String clusterName) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }
}
