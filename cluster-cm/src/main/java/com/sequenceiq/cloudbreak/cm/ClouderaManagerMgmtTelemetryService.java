package com.sequenceiq.cloudbreak.cm;

import static com.sequenceiq.cloudbreak.cm.util.ConfigUtils.makeApiConfigList;
import static com.sequenceiq.cloudbreak.common.type.CloudConstants.AWS_NATIVE_GOV;
import static java.util.stream.Collectors.joining;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cloudera.api.swagger.ClouderaManagerResourceApi;
import com.cloudera.api.swagger.MgmtRoleConfigGroupsResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiConfig;
import com.cloudera.api.swagger.model.ApiConfigList;
import com.cloudera.api.swagger.model.ApiHostRef;
import com.cloudera.api.swagger.model.ApiRole;
import com.cloudera.api.swagger.model.ApiRoleList;
import com.google.common.annotations.VisibleForTesting;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.altus.model.AltusCredential;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.cluster.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.cm.client.retry.ClouderaManagerApiFactory;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessorFactory;
import com.sequenceiq.cloudbreak.cmtemplate.metering.MeteringServiceFieldResolver;
import com.sequenceiq.cloudbreak.dto.ProxyConfig;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.cloudbreak.telemetry.DataBusEndpointProvider;
import com.sequenceiq.cloudbreak.telemetry.monitoring.MonitoringConfiguration;
import com.sequenceiq.common.api.telemetry.model.Telemetry;
import com.sequenceiq.common.api.telemetry.model.WorkloadAnalytics;

@Service
public class ClouderaManagerMgmtTelemetryService {

    static final String TELEMETRYPUBLISHER = "TELEMETRYPUBLISHER";

    private static final String SERVICE_MONITOR = "SERVICEMONITOR";

    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerMgmtTelemetryService.class);

    private static final String ALTUS_CREDENTIAL_NAME = "cb-altus-access";

    private static final String ALTUS_CREDENTIAL_TYPE = "ALTUS_ACCESS_KEY_AUTH";

    private static final String ALTUS_CREDENTIAL_ACCESS_KEY_NAME = "access_key_id";

    private static final String ALTUS_CREDENTIAL_PRIVATE_KEY_NAME = "private_key";

    private static final String MGMT_CONFIG_GROUP_NAME_PATTERN = "MGMT-%s-BASE";

    private static final String TELEMETRY_MASTER = "telemetry_master";

    private static final String TELEMETRY_WA = "telemetry_wa";

    private static final String TELEMETRY_COLLECT_JOB_LOGS = "telemetry_collect_job_logs";

    private static final String TELEMETRY_ALTUS_ACCOUNT = "telemetry_altus_account";

    private static final String TELEMETRY_ALTUS_URL = "telemetry_altus_url";

    private static final String TELEMETRY_SAFETY_VALVE = "telemetrypublisher_safety_valve";

    private static final String CDP_METADATA_SAFETY_VALVE = "telemetrypublisher_cdp_metadata_safety_valve";

    private static final String TELEMETRY_PROXY_ENABLED = "telemetrypublisher_proxy_enabled";

    private static final String TELEMETRY_PROXY_SERVER = "telemetrypublisher_proxy_server";

    private static final String TELEMETRY_PROXY_PORT = "telemetrypublisher_proxy_port";

    private static final String TELEMETRY_PROXY_USER = "telemetrypublisher_proxy_user";

    private static final String TELEMETRY_PROXY_PASSWORD = "telemetrypublisher_proxy_password";

    // Telemetry publisher - Safety valve settings
    private static final String DATABUS_HEADER_ENVIRONMENT_CRN = "databus.header.environment.crn";

    private static final String DATABUS_HEADER_DATALAKE_CRN = "databus.header.datalake.crn";

    private static final String DATABUS_HEADER_DATAHUB_CRN = "databus.header.datahub.crn";

    private static final String DATABUS_HEADER_DATAHUB_NAME = "databus.header.datahub.name";

    private static final String DATABUS_HEADER_DATAHUB_TYPE = "databus.header.datahub.type";

    private static final String DATABUS_HEADER_CLOUDPROVIDER_NAME = "databus.header.cloudprovider.name";

    private static final String DATABUS_HEADER_CLOUDPROVIDER_REGION = "databus.header.cloudprovider.region";

    private static final String DATABUS_HEADER_SDX_ID = "databus.header.sdx.id";

    private static final String DATABUS_HEADER_SDX_NAME = "databus.header.sdx.name";

    private static final String TELEMETRY_CONFIG_EXTRACTOR_METRIC_ENABLED = "extractor.metric.enabled";

    private static final String TELEMETRY_CONFIG_EXTRACTOR_EVENT_ENABLED = "extractor.event.enabled";

    private static final String TELEMETRY_CONFIG_EXTRACTOR_HMS_METADATA_ENABLED = "extractor.hms.metadata.enabled";

    private static final String TELEMETRY_WA_CLUSTER_TYPE_HEADER = "cluster.type";

    private static final String TELEMETRY_UPLOAD_LOGS = "telemetry.upload.job.logs";

    private static final String TELEMETRY_WA_DEFAULT_CLUSTER_TYPE = "DATALAKE";

    private static final String SMON_PROMETHEUS_PORT = "prometheus_metrics_endpoint_port";

    private static final String SMON_PROMETHEUS_USERNAME = "prometheus_metrics_endpoint_username";

    private static final String SMON_PROMETHEUS_PASSWORD = "prometheus_metrics_endpoint_password";

    private static final String SMON_PROMETHEUS_ADAPTER_ENABLED = "prometheus_adapter_enabled";

    private static final String IGNORE_CM_EXCEPTION = "Could not find config to delete with template name";

    private static final String RUNTIME_SUFFIX = "_RUNTIME";

    //This buildNumber consists of changes where we introduce a new safety valve to hide critical dbus headers.
    private static final Long CDP_METADATA_SAFETY_VALVE_BUILD_GBN = 72192358L;

    @Inject
    private ClusterComponentConfigProvider clusterComponentConfigProvider;

    @Inject
    private ClouderaManagerExternalAccountService externalAccountService;

    @Inject
    private ClouderaManagerDatabusService clouderaManagerDatabusService;

    @Inject
    private ClouderaManagerApiFactory clouderaManagerApiFactory;

    @Inject
    private EntitlementService entitlementService;

    @Inject
    private DataBusEndpointProvider dataBusEndpointProvider;

    @Inject
    private MonitoringConfiguration monitoringConfiguration;

    @Inject
    private CmTemplateProcessorFactory cmTemplateProcessorFactory;

    @Inject
    private MeteringServiceFieldResolver meteringServiceFieldResolver;

    public void setupTelemetryRole(StackDtoDelegate stack, ApiClient client, ApiHostRef cmHostRef,
            ApiRoleList mgmtRoles, Telemetry telemetry, String sdxStackCrn) throws ApiException {
        if (isAnalyticsEnabled(stack, telemetry)) {
            WorkloadAnalytics workloadAnalytics = telemetry.getWorkloadAnalytics();
            String accountId = Crn.safeFromString(stack.getResourceCrn()).getAccountId();
            boolean useDbusCnameEndpoint = entitlementService.useDataBusCNameEndpointEnabled(accountId);
            String databusEndpoint = dataBusEndpointProvider.getDataBusEndpoint(workloadAnalytics.getDatabusEndpoint(), useDbusCnameEndpoint);

            ClouderaManagerResourceApi cmResourceApi = clouderaManagerApiFactory.getClouderaManagerResourceApi(client);
            ApiConfigList apiConfigList = buildTelemetryCMConfigList(workloadAnalytics, databusEndpoint);
            cmResourceApi.updateConfig(apiConfigList, "Adding telemetry settings.");

            AltusCredential credentials = clouderaManagerDatabusService.getAltusCredential(stack, sdxStackCrn);
            Map<String, String> accountConfigs = new HashMap<>();
            accountConfigs.put(ALTUS_CREDENTIAL_ACCESS_KEY_NAME, credentials.getAccessKey());
            accountConfigs.put(ALTUS_CREDENTIAL_PRIVATE_KEY_NAME, new String(credentials.getPrivateKey()));

            externalAccountService.createExternalAccount(ALTUS_CREDENTIAL_NAME, ALTUS_CREDENTIAL_NAME,
                    ALTUS_CREDENTIAL_TYPE, accountConfigs, client);

            ApiRole telemetryPublisher = new ApiRole();
            telemetryPublisher.setName(TELEMETRYPUBLISHER);
            telemetryPublisher.setType(TELEMETRYPUBLISHER);
            telemetryPublisher.setHostRef(cmHostRef);
            mgmtRoles.addItemsItem(telemetryPublisher);
            LOGGER.info("Added Telemetry management role for {}", stack.getResourceCrn());
        } else {
            LOGGER.info("Telemetry WA is disabled for {}", stack.getResourceCrn());
        }
    }

    public void updateTelemetryConfigs(StackDtoDelegate stack, ApiClient client, Telemetry telemetry,
            String sdxContextName, String sdxStackCrn, ProxyConfig proxyConfig) throws ApiException {
        if (isAnalyticsEnabled(stack, telemetry)) {
            MgmtRoleConfigGroupsResourceApi mgmtRoleConfigGroupsResourceApi = clouderaManagerApiFactory.getMgmtRoleConfigGroupsResourceApi(client);
            ApiConfigList configList = buildTelemetryConfigList(
                    stack, telemetry.getWorkloadAnalytics(), sdxContextName, sdxStackCrn, proxyConfig);
            mgmtRoleConfigGroupsResourceApi.updateConfig(String.format(MGMT_CONFIG_GROUP_NAME_PATTERN, TELEMETRYPUBLISHER),
                    configList, "Set configs for Telemetry publisher by CB");
        }
    }

    public void updateServiceMonitorConfigs(StackDtoDelegate stack, ApiClient client, Telemetry telemetry) throws ApiException {
        if (isMonitoringSupported(telemetry)) {
            MgmtRoleConfigGroupsResourceApi mgmtRoleConfigGroupsResourceApi = clouderaManagerApiFactory.getMgmtRoleConfigGroupsResourceApi(client);
            ApiConfigList serviecMonitorConfigList = mgmtRoleConfigGroupsResourceApi.readConfig(
                    String.format(MGMT_CONFIG_GROUP_NAME_PATTERN, SERVICE_MONITOR), "FULL");
            List<String> allConfigKeys = getKeysFromApiConfigList(serviecMonitorConfigList);
            if (allConfigKeys.containsAll(List.of(SMON_PROMETHEUS_USERNAME, SMON_PROMETHEUS_PASSWORD, SMON_PROMETHEUS_PORT))) {
                String monitoringUser = stack.getCluster().getCloudbreakClusterManagerMonitoringUser();
                String monitoringPassword = stack.getCluster().getCloudbreakClusterManagerMonitoringPassword();
                Integer exporterPort = monitoringConfiguration.getClouderaManagerExporter().getPort();
                Map<String, String> configsToUpdate = new HashMap<>();
                if (monitoringUser != null && monitoringPassword != null) {
                    configsToUpdate.put(SMON_PROMETHEUS_USERNAME, monitoringUser);
                    configsToUpdate.put(SMON_PROMETHEUS_PASSWORD, monitoringPassword);
                    configsToUpdate.put(SMON_PROMETHEUS_PORT, String.valueOf(exporterPort));
                    if (allConfigKeys.contains(SMON_PROMETHEUS_ADAPTER_ENABLED)) {
                        LOGGER.debug("Prometheus adapter is enabled for service monitor.");
                        configsToUpdate.put(SMON_PROMETHEUS_ADAPTER_ENABLED, "true");
                    }
                    ApiConfigList configList = makeApiConfigList(configsToUpdate);
                    try {
                        mgmtRoleConfigGroupsResourceApi.updateConfig(String.format(MGMT_CONFIG_GROUP_NAME_PATTERN, SERVICE_MONITOR),
                                configList, "Set service monitoring configs for CM metrics exporter by CB");
                    } catch (ApiException e) {
                        if (e.getMessage() != null && e.getMessage().contains(IGNORE_CM_EXCEPTION)) {
                            LOGGER.info("Could not configure smon telemetry, because: {}", e.getMessage());
                        } else {
                            throw e;
                        }
                    }
                }
            }
        }
    }

    private List<String> getKeysFromApiConfigList(ApiConfigList serviceMonitorConfigList) {
        return serviceMonitorConfigList != null && CollectionUtils.isNotEmpty(serviceMonitorConfigList.getItems())
                ? serviceMonitorConfigList.getItems().stream().map(ApiConfig::getName).collect(Collectors.toList())
                : new ArrayList<>();
    }

    private ApiConfigList buildTelemetryConfigList(StackDtoDelegate stack, WorkloadAnalytics wa, String sdxContextName,
            String sdxCrn, ProxyConfig proxyConfig) {
        Map<String, String> configsToUpdate = new HashMap<>();
        Map<String, String> telemetrySafetyValveMap = new HashMap<>();
        Map<String, String> cdpMetadataSafetyValveMap = new HashMap<>();
        if (stack == null || stack.getType() != StackType.DATALAKE) {
            // the HMS metadata extractor is true by default in the Observability, but this make impact on the DHs performance therefore we should disable it.
            // Enabling this temporarily, but will be reverted once DL telemetry issues are fixed.
            telemetrySafetyValveMap.put(TELEMETRY_CONFIG_EXTRACTOR_HMS_METADATA_ENABLED, "true");
        }

        if (stack != null && hasCdpMetadataKey(stack)) {
            populateClusterType(stack, cdpMetadataSafetyValveMap);
            enrichWithEnvironmentMetadata(sdxContextName, sdxCrn, stack, wa, cdpMetadataSafetyValveMap);
            configsToUpdate.put(CDP_METADATA_SAFETY_VALVE, createStringFromSafetyValveMap(cdpMetadataSafetyValveMap));
        } else {
            populateClusterType(stack, telemetrySafetyValveMap);
            enrichWithEnvironmentMetadata(sdxContextName, sdxCrn, stack, wa, telemetrySafetyValveMap);
        }
        telemetrySafetyValveMap.put(TELEMETRY_UPLOAD_LOGS, "true");
        configsToUpdate.put(TELEMETRY_SAFETY_VALVE, createStringFromSafetyValveMap(telemetrySafetyValveMap));
        if (proxyConfig != null) {
            configsToUpdate.put(TELEMETRY_PROXY_ENABLED, "true");
            configsToUpdate.put(TELEMETRY_PROXY_SERVER, proxyConfig.getServerHost());
            configsToUpdate.put(TELEMETRY_PROXY_PORT, String.valueOf(proxyConfig.getServerPort()));
            proxyConfig.getProxyAuthentication().ifPresent(auth -> {
                configsToUpdate.put(TELEMETRY_PROXY_USER, auth.getUserName());
                configsToUpdate.put(TELEMETRY_PROXY_PASSWORD, auth.getPassword());
            });
            // TODO: no_proxy config should be added
        }
        return makeApiConfigList(configsToUpdate);
    }

    // This check ensures backward compatibility with older safety valve configuration.
    @VisibleForTesting
    boolean hasCdpMetadataKey(StackDtoDelegate stack) {
        ClouderaManagerRepo clouderaManagerRepo = clusterComponentConfigProvider.getClouderaManagerRepoDetails(stack.getCluster().getId());
        String currentBuildGbn = clouderaManagerRepo.getBuildNumber();
        if (currentBuildGbn == null) {
            return false;
        }
        try {
            Long currentGbnLong = Long.parseLong(currentBuildGbn);
            return currentGbnLong.compareTo(CDP_METADATA_SAFETY_VALVE_BUILD_GBN) >= 0;
        } catch (Exception ex) {
            LOGGER.warn("Invalid Build GBN found {} with exception", currentBuildGbn, ex);
        }
        return false;
    }

    private void populateClusterType(StackDtoDelegate stack, Map<String, String> safetyValveMap) {
        if (stack != null && stack.getType() == StackType.DATALAKE) {
            safetyValveMap.put(TELEMETRY_WA_CLUSTER_TYPE_HEADER, TELEMETRY_WA_DEFAULT_CLUSTER_TYPE + RUNTIME_SUFFIX);
        } else {
            safetyValveMap.put(TELEMETRY_WA_CLUSTER_TYPE_HEADER, TELEMETRY_WA_DEFAULT_CLUSTER_TYPE);
        }
    }

    private ApiConfigList buildTelemetryCMConfigList(WorkloadAnalytics workloadAnalytics, String databusUrl) {
        Map<String, String> configsToUpdate = new HashMap<>();
        configsToUpdate.put(TELEMETRY_MASTER, "true");
        configsToUpdate.put(TELEMETRY_WA, "true");
        configsToUpdate.put(TELEMETRY_COLLECT_JOB_LOGS, "true");
        configsToUpdate.put(TELEMETRY_ALTUS_ACCOUNT, ALTUS_CREDENTIAL_NAME);
        if (StringUtils.isNotEmpty(databusUrl)) {
            configsToUpdate.put(TELEMETRY_ALTUS_URL, databusUrl);
        }
        return makeApiConfigList(configsToUpdate);
    }

    private void enrichWithEnvironmentMetadata(
            String sdxContextName,
            String sdxCrn,
            StackDtoDelegate stack,
            WorkloadAnalytics workloadAnalytics,
            Map<String, String> safetyValveMap) {

        String sdxName = getSdxName(stack, sdxContextName);
        String sdxId = getSdxId(sdxCrn, sdxName);

        safetyValveMap.put(DATABUS_HEADER_SDX_NAME, sdxName);
        safetyValveMap.put(DATABUS_HEADER_SDX_ID, sdxId);

        addDatalakeSpecificValues(stack, safetyValveMap);

        addStackMetadata(stack, safetyValveMap);
        addWorkloadAnalyticsAttributes(workloadAnalytics, safetyValveMap);
    }

    private String getSdxId(String sdxCrn, String sdxContextName) {
        if (StringUtils.isNotEmpty(sdxCrn) && Crn.fromString(sdxCrn) != null) {
            return Crn.fromString(sdxCrn).getResource();
        }
        return UUID.nameUUIDFromBytes(sdxContextName.getBytes()).toString();
    }

    private String getSdxName(StackDtoDelegate stack, String sdxContextName) {
        if (StringUtils.isNotEmpty(sdxContextName)) {
            return sdxContextName;
        }
        return String.format("%s-%s", stack.getCluster().getName(), stack.getCluster().getId().toString());
    }

    private void addStackMetadata(StackDtoDelegate stack, Map<String, String> safetyValveMap) {
        if (stack == null) {
            return;
        }
        addIfNotEmpty(safetyValveMap, DATABUS_HEADER_ENVIRONMENT_CRN, stack.getEnvironmentCrn());
        addIfNotEmpty(safetyValveMap, DATABUS_HEADER_CLOUDPROVIDER_NAME, stack.getCloudPlatform());
        addIfNotEmpty(safetyValveMap, DATABUS_HEADER_CLOUDPROVIDER_REGION, stack.getRegion());

        if (stack.getType() == StackType.WORKLOAD) {
            addIfNotEmpty(safetyValveMap, DATABUS_HEADER_DATAHUB_CRN, stack.getResourceCrn());
            addIfNotEmpty(safetyValveMap, DATABUS_HEADER_DATAHUB_NAME, stack.getName());
            addIfNotEmpty(safetyValveMap, DATABUS_HEADER_DATAHUB_TYPE, getDatahubType(stack).orElse("UNDEFINED"));
            addIfNotEmpty(safetyValveMap, DATABUS_HEADER_DATALAKE_CRN, stack.getDatalakeCrn());
        } else if (stack.getType() == StackType.DATALAKE) {
            addIfNotEmpty(safetyValveMap, DATABUS_HEADER_DATALAKE_CRN, stack.getResourceCrn());
        }
    }

    private void addWorkloadAnalyticsAttributes(WorkloadAnalytics workloadAnalytics, Map<String, String> safetyValveMap) {
        if (workloadAnalytics != null && workloadAnalytics.getAttributes() != null) {
            for (Map.Entry<String, Object> entry : workloadAnalytics.getAttributes().entrySet()) {
                safetyValveMap.put(entry.getKey(), ObjectUtils.defaultIfNull(entry.getValue().toString(), ""));
            }
        }
    }

    private void addIfNotEmpty(Map<String, String> map, String key, String value) {
        if (StringUtils.isNotEmpty(value)) {
            map.put(key, value);
        }
    }

    private String createStringFromSafetyValveMap(Map<String, String> safetyValveMap) {
        return safetyValveMap.entrySet()
                .stream()
                .map(e -> e.getKey() + '=' + e.getValue())
                .collect(joining("\n"));
    }

    private boolean isAnalyticsEnabled(StackDtoDelegate stack, Telemetry telemetry) {
        return telemetry != null
                && telemetry.getWorkloadAnalytics() != null
                && !isGovCloud(stack);
    }

    private boolean isMonitoringSupported(Telemetry telemetry) {
        return telemetry.isComputeMonitoringEnabled() && telemetry.isMonitoringFeatureEnabled();
    }

    private void addDatalakeSpecificValues(StackDtoDelegate stack, Map<String, String> safetyValveMap) {
        if (stack != null && stack.getType() == StackType.DATALAKE) {
            safetyValveMap.put(TELEMETRY_CONFIG_EXTRACTOR_METRIC_ENABLED, "false");
            safetyValveMap.put(TELEMETRY_CONFIG_EXTRACTOR_EVENT_ENABLED, "false");
        }
    }

    private boolean isGovCloud(StackDtoDelegate stack) {
        return stack.getPlatformVariant() != null
                && ("govCloud".equalsIgnoreCase(stack.getPlatformVariant())
                || AWS_NATIVE_GOV.equalsIgnoreCase(stack.getPlatformVariant()));
    }

    private Optional<String> getDatahubType(StackDtoDelegate stack) {
        String blueprintJsonText = stack.getBlueprintJsonText();
        CmTemplateProcessor cmTemplateProcessor = cmTemplateProcessorFactory.get(blueprintJsonText);

        return Optional.ofNullable(meteringServiceFieldResolver.resolveServiceFeature(cmTemplateProcessor));
    }

}
