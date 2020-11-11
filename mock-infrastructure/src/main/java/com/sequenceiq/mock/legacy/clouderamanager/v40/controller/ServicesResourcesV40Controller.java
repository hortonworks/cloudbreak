package com.sequenceiq.mock.legacy.clouderamanager.v40.controller;

import java.math.BigDecimal;
import java.util.List;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

import com.sequenceiq.mock.legacy.clouderamanager.ResponseUtil;
import com.sequenceiq.mock.legacy.clouderamanager.DefaultModelService;
import com.sequenceiq.mock.legacy.clouderamanager.ProfileAwareResponse;
import com.sequenceiq.mock.swagger.v40.api.ServicesResourceApi;
import com.sequenceiq.mock.swagger.model.ApiCommand;
import com.sequenceiq.mock.swagger.model.ApiCommandList;
import com.sequenceiq.mock.swagger.model.ApiCommandMetadataList;
import com.sequenceiq.mock.swagger.model.ApiDisableJtHaArguments;
import com.sequenceiq.mock.swagger.model.ApiDisableLlamaHaArguments;
import com.sequenceiq.mock.swagger.model.ApiDisableNnHaArguments;
import com.sequenceiq.mock.swagger.model.ApiDisableOozieHaArguments;
import com.sequenceiq.mock.swagger.model.ApiDisableRmHaArguments;
import com.sequenceiq.mock.swagger.model.ApiDisableSentryHaArgs;
import com.sequenceiq.mock.swagger.model.ApiEnableJtHaArguments;
import com.sequenceiq.mock.swagger.model.ApiEnableLlamaHaArguments;
import com.sequenceiq.mock.swagger.model.ApiEnableLlamaRmArguments;
import com.sequenceiq.mock.swagger.model.ApiEnableNnHaArguments;
import com.sequenceiq.mock.swagger.model.ApiEnableOozieHaArguments;
import com.sequenceiq.mock.swagger.model.ApiEnableRmHaArguments;
import com.sequenceiq.mock.swagger.model.ApiEnableSentryHaArgs;
import com.sequenceiq.mock.swagger.model.ApiEntityTag;
import com.sequenceiq.mock.swagger.model.ApiHdfsDisableHaArguments;
import com.sequenceiq.mock.swagger.model.ApiHdfsFailoverArguments;
import com.sequenceiq.mock.swagger.model.ApiHdfsHaArguments;
import com.sequenceiq.mock.swagger.model.ApiHdfsUsageReport;
import com.sequenceiq.mock.swagger.model.ApiImpalaUtilization;
import com.sequenceiq.mock.swagger.model.ApiMetricList;
import com.sequenceiq.mock.swagger.model.ApiMrUsageReport;
import com.sequenceiq.mock.swagger.model.ApiRoleNameList;
import com.sequenceiq.mock.swagger.model.ApiRoleTypeList;
import com.sequenceiq.mock.swagger.model.ApiRollEditsArgs;
import com.sequenceiq.mock.swagger.model.ApiRollingRestartArgs;
import com.sequenceiq.mock.swagger.model.ApiService;
import com.sequenceiq.mock.swagger.model.ApiServiceConfig;
import com.sequenceiq.mock.swagger.model.ApiServiceList;
import com.sequenceiq.mock.swagger.model.ApiServiceState;
import com.sequenceiq.mock.swagger.model.ApiYarnApplicationDiagnosticsCollectionArgs;
import com.sequenceiq.mock.swagger.model.ApiYarnUtilization;

@Controller
public class ServicesResourcesV40Controller implements ServicesResourceApi {

    @Inject
    private HttpServletRequest request;

    @Inject
    private DefaultModelService defaultModelService;

    @Override
    public ResponseEntity<List<ApiEntityTag>> addTags(String clusterName, String serviceName, @Valid List<ApiEntityTag> body) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiCommand> collectYarnApplicationDiagnostics(String clusterName, String serviceName,
            @Valid ApiYarnApplicationDiagnosticsCollectionArgs body) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiCommand> createBeeswaxWarehouseCommand(String clusterName, String serviceName) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiCommand> createHBaseRootCommand(String clusterName, String serviceName) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiCommand> createHiveUserDirCommand(String clusterName, String serviceName) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiCommand> createHiveWarehouseCommand(String clusterName, String serviceName) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiCommand> createImpalaUserDirCommand(String clusterName, String serviceName) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiCommand> createOozieDb(String clusterName, String serviceName) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiServiceList> createServices(String clusterName, @Valid ApiServiceList body) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiCommand> createSolrHdfsHomeDirCommand(String clusterName, String serviceName) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiCommand> createSqoopUserDirCommand(String clusterName, String serviceName) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiCommand> createYarnCmContainerUsageInputDirCommand(String clusterName, String serviceName) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiCommand> createYarnJobHistoryDirCommand(String clusterName, String serviceName) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiCommand> createYarnNodeManagerRemoteAppLogDirCommand(String clusterName, String serviceName) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiCommand> decommissionCommand(String clusterName, String serviceName, @Valid ApiRoleNameList body) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiService> deleteService(String clusterName, String serviceName) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<List<ApiEntityTag>> deleteTags(String clusterName, String serviceName, @Valid List<ApiEntityTag> body) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiCommand> deployClientConfigCommand(String clusterName, String serviceName, @Valid ApiRoleNameList body) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiCommand> disableJtHaCommand(String clusterName, String serviceName, @Valid ApiDisableJtHaArguments body) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiCommand> disableLlamaHaCommand(String clusterName, String serviceName, @Valid ApiDisableLlamaHaArguments body) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiCommand> disableLlamaRmCommand(String clusterName, String serviceName) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiCommand> disableOozieHaCommand(String clusterName, String serviceName, @Valid ApiDisableOozieHaArguments body) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiCommand> disableRmHaCommand(String clusterName, String serviceName, @Valid ApiDisableRmHaArguments body) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiCommand> disableSentryHaCommand(String clusterName, String serviceName, @Valid ApiDisableSentryHaArgs body) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiCommand> enableJtHaCommand(String clusterName, String serviceName, @Valid ApiEnableJtHaArguments body) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiCommand> enableLlamaHaCommand(String clusterName, String serviceName, @Valid ApiEnableLlamaHaArguments body) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiCommand> enableLlamaRmCommand(String clusterName, String serviceName, @Valid ApiEnableLlamaRmArguments body) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiCommand> enableOozieHaCommand(String clusterName, String serviceName, @Valid ApiEnableOozieHaArguments body) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiCommand> enableRmHaCommand(String clusterName, String serviceName, @Valid ApiEnableRmHaArguments body) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiCommand> enableSentryHaCommand(String clusterName, String serviceName, @Valid ApiEnableSentryHaArgs body) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiCommand> enterMaintenanceMode(String clusterName, String serviceName) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiCommand> exitMaintenanceMode(String clusterName, String serviceName) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiCommand> firstRun(String clusterName, String serviceName) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<Resource> getClientConfig(String clusterName, String serviceName) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiHdfsUsageReport> getHdfsUsageReport(String clusterName, String serviceName, @Valid String aggregation, @Valid String from,
            @Valid String nameservice, @Valid String to) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    //CHECKSTYLE:OFF
    public ResponseEntity<ApiImpalaUtilization> getImpalaUtilization(String clusterName, String serviceName, @Valid List<String> daysOfWeek,
            @Valid Integer endHourOfDay, @Valid String from, @Valid Integer startHourOfDay, @Valid String tenantType, @Valid String to) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }
    //CHECKSTYLE:ON

    @Override
    public ResponseEntity<ApiMetricList> getMetrics(String clusterName, String serviceName, @Valid String from, @Valid List<String> metrics, @Valid String to,
            @Valid String view) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiMrUsageReport> getMrUsageReport(String clusterName, String serviceName, @Valid String aggregation, @Valid String from,
            @Valid String to) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    //CHECKSTYLE:OFF
    public ResponseEntity<ApiYarnUtilization> getYarnUtilization(String clusterName, String serviceName, @Valid List<String> daysOfWeek,
            @Valid Integer endHourOfDay, @Valid String from, @Valid Integer startHourOfDay, @Valid String tenantType, @Valid String to) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }
    //CHECKSTYLE:ON

    @Override
    public ResponseEntity<ApiCommand> hbaseUpgradeCommand(String clusterName, String serviceName) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiCommand> hdfsCreateTmpDir(String clusterName, String serviceName) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiCommand> hdfsDisableAutoFailoverCommand(String clusterName, String serviceName, @Valid String body) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiCommand> hdfsDisableHaCommand(String clusterName, String serviceName, @Valid ApiHdfsDisableHaArguments body) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiCommand> hdfsDisableNnHaCommand(String clusterName, String serviceName, @Valid ApiDisableNnHaArguments body) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiCommand> hdfsEnableAutoFailoverCommand(String clusterName, String serviceName, @Valid ApiHdfsFailoverArguments body) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiCommand> hdfsEnableHaCommand(String clusterName, String serviceName, @Valid ApiHdfsHaArguments body) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiCommand> hdfsEnableNnHaCommand(String clusterName, String serviceName, @Valid ApiEnableNnHaArguments body) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiCommand> hdfsFailoverCommand(String clusterName, String serviceName, @Valid Boolean force, @Valid ApiRoleNameList body) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiCommand> hdfsFinalizeRollingUpgrade(String clusterName, String serviceName) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiCommand> hdfsRollEditsCommand(String clusterName, String serviceName, @Valid ApiRollEditsArgs body) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiCommand> hdfsUpgradeMetadataCommand(String clusterName, String serviceName) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiCommand> hiveCreateMetastoreDatabaseCommand(String clusterName, String serviceName) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiCommand> hiveCreateMetastoreDatabaseTablesCommand(String clusterName, String serviceName) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiCommand> hiveUpdateMetastoreNamenodesCommand(String clusterName, String serviceName) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiCommand> hiveUpgradeMetastoreCommand(String clusterName, String serviceName) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiCommand> hiveValidateMetastoreSchemaCommand(String clusterName, String serviceName) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiCommand> hueDumpDbCommand(String clusterName, String serviceName) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiCommand> hueLoadDbCommand(String clusterName, String serviceName) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiCommand> hueSyncDbCommand(String clusterName, String serviceName) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiCommand> impalaCreateCatalogDatabaseCommand(String clusterName, String serviceName) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiCommand> impalaCreateCatalogDatabaseTablesCommand(String clusterName, String serviceName) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiCommand> importMrConfigsIntoYarn(String clusterName, String serviceName) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiCommand> initSolrCommand(String clusterName, String serviceName) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiCommand> installMrFrameworkJars(String clusterName, String serviceName) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiCommand> installOozieShareLib(String clusterName, String serviceName) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiCommand> ksMigrateToSentry(String clusterName, String serviceName) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiCommandList> listActiveCommands(String clusterName, String serviceName, @Valid String view) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiRoleTypeList> listRoleTypes(String clusterName, String serviceName) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiCommandMetadataList> listServiceCommands(String clusterName, String serviceName) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiCommand> offlineCommand(String clusterName, String serviceName, @Valid BigDecimal timeout, @Valid ApiRoleNameList body) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiCommand> oozieCreateEmbeddedDatabaseCommand(String clusterName, String serviceName) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiCommand> oozieDumpDatabaseCommand(String clusterName, String serviceName) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiCommand> oozieLoadDatabaseCommand(String clusterName, String serviceName) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiCommand> oozieUpgradeDbCommand(String clusterName, String serviceName) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiService> readService(String clusterName, String serviceName, @Valid String view) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiServiceConfig> readServiceConfig(String clusterName, String serviceName, @Valid String view) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiServiceList> readServices(String clusterName, @Valid String view) {
        ApiServiceList response = new ApiServiceList().items(List.of(new ApiService().name("service1").serviceState(ApiServiceState.STARTED)));
        return ProfileAwareResponse.exec(response, defaultModelService);
    }

    @Override
    public ResponseEntity<List<ApiEntityTag>> readTags(String clusterName, String serviceName, @Valid Integer limit, @Valid Integer offset) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiCommand> recommissionCommand(String clusterName, String serviceName, @Valid ApiRoleNameList body) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiCommand> recommissionWithStartCommand(String clusterName, String serviceName, @Valid ApiRoleNameList body) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiCommand> restartCommand(String clusterName, String serviceName) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiCommand> rollingRestart(String clusterName, String serviceName, @Valid ApiRollingRestartArgs body) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiCommand> sentryCreateDatabaseCommand(String clusterName, String serviceName) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiCommand> sentryCreateDatabaseTablesCommand(String clusterName, String serviceName) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiCommand> sentryUpgradeDatabaseTablesCommand(String clusterName, String serviceName) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiCommand> serviceCommandByName(String clusterName, String commandName, String serviceName) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiCommand> solrBootstrapCollectionsCommand(String clusterName, String serviceName) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiCommand> solrBootstrapConfigCommand(String clusterName, String serviceName) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiCommand> solrConfigBackupCommand(String clusterName, String serviceName) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiCommand> solrMigrateSentryPrivilegesCommand(String clusterName, String serviceName) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiCommand> solrReinitializeStateForUpgradeCommand(String clusterName, String serviceName) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiCommand> solrValidateMetadataCommand(String clusterName, String serviceName) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiCommand> sqoopCreateDatabaseTablesCommand(String clusterName, String serviceName) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiCommand> sqoopUpgradeDbCommand(String clusterName, String serviceName) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiCommand> startCommand(String clusterName, String serviceName) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiCommand> stopCommand(String clusterName, String serviceName) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiCommand> switchToMr2(String clusterName, String serviceName) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiService> updateService(String clusterName, String serviceName, @Valid ApiService body) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiServiceConfig> updateServiceConfig(String clusterName, String serviceName, @Valid String message, @Valid ApiServiceConfig body) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiCommand> yarnFormatStateStore(String clusterName, String serviceName) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiCommand> zooKeeperCleanupCommand(String clusterName, String serviceName) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiCommand> zooKeeperInitCommand(String clusterName, String serviceName) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }
}
