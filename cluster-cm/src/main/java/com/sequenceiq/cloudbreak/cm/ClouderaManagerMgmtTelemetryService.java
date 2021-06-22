package com.sequenceiq.cloudbreak.cm;

import static com.sequenceiq.cloudbreak.cm.util.ConfigUtils.makeApiConfigList;
import static java.util.stream.Collectors.joining;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cloudera.api.swagger.ClouderaManagerResourceApi;
import com.cloudera.api.swagger.MgmtRoleConfigGroupsResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiConfigList;
import com.cloudera.api.swagger.model.ApiHostRef;
import com.cloudera.api.swagger.model.ApiRole;
import com.cloudera.api.swagger.model.ApiRoleList;
import com.google.common.annotations.VisibleForTesting;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.altus.model.AltusCredential;
import com.sequenceiq.cloudbreak.cm.client.retry.ClouderaManagerApiFactory;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.dto.ProxyConfig;
import com.sequenceiq.cloudbreak.telemetry.DataBusEndpointProvider;
import com.sequenceiq.common.api.telemetry.model.Telemetry;
import com.sequenceiq.common.api.telemetry.model.WorkloadAnalytics;

@Service
public class ClouderaManagerMgmtTelemetryService {

    static final String TELEMETRYPUBLISHER = "TELEMETRYPUBLISHER";

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

    private static final String TELEMETRY_PROXY_ENABLED = "telemetrypublisher_proxy_enabled";

    private static final String TELEMETRY_PROXY_SERVER = "telemetrypublisher_proxy_server";

    private static final String TELEMETRY_PROXY_PORT = "telemetrypublisher_proxy_port";

    private static final String TELEMETRY_PROXY_USER = "telemetrypublisher_proxy_user";

    private static final String TELEMETRY_PROXY_PASSWORD = "telemetrypublisher_proxy_password";

    // Telemetry publisher - Safety valve settings
    private static final String DATABUS_HEADER_SDX_ID = "databus.header.sdx.id";

    private static final String DATABUS_HEADER_SDX_NAME = "databus.header.sdx.name";

    private static final String TELEMETRY_WA_CLUSTER_TYPE_HEADER = "cluster.type";

    private static final String TELEMETRY_UPLOAD_LOGS = "telemetry.upload.job.logs";

    private static final String TELEMETRY_WA_DEFAULT_CLUSTER_TYPE = "DATALAKE";

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

    public void setupTelemetryRole(final Stack stack, final ApiClient client, final ApiHostRef cmHostRef,
            final ApiRoleList mgmtRoles, final Telemetry telemetry) throws ApiException {
        if (isWorkflowAnalyticsEnabled(stack, telemetry)) {
            WorkloadAnalytics workloadAnalytics = telemetry.getWorkloadAnalytics();
            Crn userCrn = Crn.fromString(stack.getCreator().getUserCrn());
            boolean useDbusCnameEndpoint = entitlementService.useDataBusCNameEndpointEnabled(userCrn.getAccountId());
            String databusEndpoint = dataBusEndpointProvider.getDataBusEndpoint(workloadAnalytics.getDatabusEndpoint(), useDbusCnameEndpoint);

            ClouderaManagerResourceApi cmResourceApi = clouderaManagerApiFactory.getClouderaManagerResourceApi(client);
            ApiConfigList apiConfigList = buildTelemetryCMConfigList(workloadAnalytics, databusEndpoint);
            cmResourceApi.updateConfig("Adding telemetry settings.", apiConfigList);

            AltusCredential credentials = clouderaManagerDatabusService.getAltusCredential(stack);
            Map<String, String> accountConfigs = new HashMap<>();
            accountConfigs.put(ALTUS_CREDENTIAL_ACCESS_KEY_NAME, credentials.getAccessKey());
            accountConfigs.put(ALTUS_CREDENTIAL_PRIVATE_KEY_NAME, new String(credentials.getPrivateKey()));

            externalAccountService.createExternalAccount(ALTUS_CREDENTIAL_NAME, ALTUS_CREDENTIAL_NAME,
                    ALTUS_CREDENTIAL_TYPE, accountConfigs, client);

            final ApiRole telemetryPublisher = new ApiRole();
            telemetryPublisher.setName(TELEMETRYPUBLISHER);
            telemetryPublisher.setType(TELEMETRYPUBLISHER);
            telemetryPublisher.setHostRef(cmHostRef);
            mgmtRoles.addItemsItem(telemetryPublisher);
        } else {
            LOGGER.info("Telemetry WA is disabled");
        }
    }

    public void updateTelemetryConfigs(final Stack stack, final ApiClient client,
            final Telemetry telemetry, final String sdxContextName,
            final String sdxStackCrn, final ProxyConfig proxyConfig) throws ApiException {
        if (isWorkflowAnalyticsEnabled(stack, telemetry)) {
            MgmtRoleConfigGroupsResourceApi mgmtRoleConfigGroupsResourceApi = clouderaManagerApiFactory.getMgmtRoleConfigGroupsResourceApi(client);
            ApiConfigList configList = buildTelemetryConfigList(stack, telemetry.getWorkloadAnalytics(),
                    sdxContextName, sdxStackCrn, proxyConfig);
            mgmtRoleConfigGroupsResourceApi.updateConfig(String.format(MGMT_CONFIG_GROUP_NAME_PATTERN, TELEMETRYPUBLISHER),
                    "Set configs for Telemetry publisher by CB", configList);
        }
    }

    @VisibleForTesting
    ApiConfigList buildTelemetryConfigList(Stack stack, WorkloadAnalytics wa, String sdxContextName,
            String sdxCrn, ProxyConfig proxyConfig) {
        final Map<String, String> configsToUpdate = new HashMap<>();
        Map<String, String> telemetrySafetyValveMap = new HashMap<>();
        telemetrySafetyValveMap.put(TELEMETRY_WA_CLUSTER_TYPE_HEADER,
                TELEMETRY_WA_DEFAULT_CLUSTER_TYPE);
        enrichWithSdxData(sdxContextName, sdxCrn, stack, wa, telemetrySafetyValveMap);
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

    @VisibleForTesting
    ApiConfigList buildTelemetryCMConfigList(WorkloadAnalytics workloadAnalytics, String databusUrl) {
        final Map<String, String> configsToUpdate = new HashMap<>();
        configsToUpdate.put(TELEMETRY_MASTER, "true");
        configsToUpdate.put(TELEMETRY_WA, "true");
        configsToUpdate.put(TELEMETRY_COLLECT_JOB_LOGS, "true");
        configsToUpdate.put(TELEMETRY_ALTUS_ACCOUNT, ALTUS_CREDENTIAL_NAME);
        if (StringUtils.isNotEmpty(databusUrl)) {
            configsToUpdate.put(TELEMETRY_ALTUS_URL, databusUrl);
        }
        return makeApiConfigList(configsToUpdate);
    }

    @VisibleForTesting
    void enrichWithSdxData(String sdxContextName, String sdxCrn, Stack stack, WorkloadAnalytics workloadAnalytics,
            Map<String, String> telemetrySafetyValveMap) {
        sdxContextName = StringUtils.isNotEmpty(sdxContextName)
                ? sdxContextName : String.format("%s-%s", stack.getCluster().getName(), stack.getCluster().getId().toString());
        String sdxId = sdxCrn != null && Crn.fromString(sdxCrn) != null
                ? Crn.fromString(sdxCrn).getResource() : UUID.nameUUIDFromBytes(sdxContextName.getBytes()).toString();
        telemetrySafetyValveMap.put(DATABUS_HEADER_SDX_ID, sdxId);
        telemetrySafetyValveMap.put(DATABUS_HEADER_SDX_NAME, sdxContextName);
        if (workloadAnalytics.getAttributes() != null) {
            for (Map.Entry<String, Object> entry : workloadAnalytics.getAttributes().entrySet()) {
                telemetrySafetyValveMap.put(entry.getKey(), ObjectUtils.defaultIfNull(entry.getValue().toString(), ""));
            }
        }
    }

    private String createStringFromSafetyValveMap(Map<String, String> telemetrySafetyValveMap) {
        return telemetrySafetyValveMap.entrySet()
                .stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(joining("\n"));
    }

    private boolean isWorkflowAnalyticsEnabled(Stack stack, Telemetry telemetry) {
        return telemetry != null
                && telemetry.getWorkloadAnalytics() != null
                && !StackType.DATALAKE.equals(stack.getType());
    }
}
