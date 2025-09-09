package com.sequenceiq.cloudbreak.cm;

import java.math.BigDecimal;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.cloudera.api.swagger.ClouderaManagerResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiCollectDiagnosticDataArguments;
import com.cloudera.api.swagger.model.ApiCommand;
import com.cloudera.api.swagger.model.ApiConfig;
import com.cloudera.api.swagger.model.ApiConfigList;
import com.sequenceiq.cloudbreak.client.HttpClientConfig;
import com.sequenceiq.cloudbreak.cloud.scheduler.CancellationException;
import com.sequenceiq.cloudbreak.cluster.api.ClusterDiagnosticsService;
import com.sequenceiq.cloudbreak.cluster.service.ClusterClientInitException;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerApiClientProvider;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerClientInitException;
import com.sequenceiq.cloudbreak.cm.client.retry.ClouderaManagerApiFactory;
import com.sequenceiq.cloudbreak.cm.exception.ClouderaManagerOperationFailedException;
import com.sequenceiq.cloudbreak.cm.polling.ClouderaManagerPollingServiceProvider;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.cloudbreak.polling.ExtendedPollingResult;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.common.api.telemetry.model.DiagnosticsDestination;
import com.sequenceiq.common.model.diagnostics.CmDiagnosticsParameters;

@Service
@Scope("prototype")
public class ClouderaManagerDiagnosticsService implements ClusterDiagnosticsService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerDiagnosticsService.class);

    @Inject
    private ClouderaManagerApiClientProvider clouderaManagerApiClientProvider;

    @Inject
    private ClouderaManagerApiFactory clouderaManagerApiFactory;

    @Inject
    private ClouderaManagerPollingServiceProvider clouderaManagerPollingServiceProvider;

    private final StackDtoDelegate stack;

    private final HttpClientConfig clientConfig;

    private ApiClient client;

    public ClouderaManagerDiagnosticsService(StackDtoDelegate stack, HttpClientConfig clientConfig) {
        this.stack = stack;
        this.clientConfig = clientConfig;
    }

    @PostConstruct
    public void initApiClient() throws ClusterClientInitException {
        ClusterView cluster = stack.getCluster();
        String cloudbreakClusterManagerUser = cluster.getCloudbreakClusterManagerUser();
        String cloudbreakClusterManagerPassword = cluster.getCloudbreakClusterManagerPassword();
        try {
            client = clouderaManagerApiClientProvider
                    .getV31Client(stack.getGatewayPort(), cloudbreakClusterManagerUser, cloudbreakClusterManagerPassword, clientConfig);
        } catch (ClouderaManagerClientInitException e) {
            throw new ClusterClientInitException(e);
        }
    }

    @Override
    public void collectDiagnostics(CmDiagnosticsParameters parameters) throws CloudbreakException {
        ClouderaManagerResourceApi resourceApi = clouderaManagerApiFactory.getClouderaManagerResourceApi(client);
        try {
            ApiConfigList configList = resourceApi.getConfig(null);
            Boolean globalPhoneHomeConfig = getPhoneHomeConfig(configList);
            preUpdatePhoneHomeConfig(parameters.getDestination(), resourceApi, globalPhoneHomeConfig);
            ApiCommand collectDiagnostics = resourceApi.collectDiagnosticDataCommand(convertToCollectDiagnosticDataArguments(parameters));
            ExtendedPollingResult pollingResult = clouderaManagerPollingServiceProvider
                    .startPollingCollectDiagnostics(stack, client, collectDiagnostics.getId());
            if (pollingResult.isExited()) {
                throw new CancellationException("Cluster was terminated while waiting for command API to be available for diagnostics collections");
            } else if (pollingResult.isTimeout()) {
                throw new CloudbreakException("Timeout during waiting for command API to be available (diagnostics collections).");
            }
            postUpdatePhoneHomeConfig(parameters.getDestination(), resourceApi, globalPhoneHomeConfig);
        } catch (ApiException e) {
            LOGGER.error("Error during CM based diagnostics collection", e);
            throw new ClouderaManagerOperationFailedException("Collect diagnostics failed", e);
        }
    }

    private void preUpdatePhoneHomeConfig(DiagnosticsDestination destination, ClouderaManagerResourceApi resourceApi,
            Boolean globalPhoneHomeConfig) throws ApiException {
        if (DiagnosticsDestination.SUPPORT.equals(destination)) {
            if (globalPhoneHomeConfig) {
                LOGGER.debug("PHONE_HOME is set properly for sending data to Cloudera Support, value: {}", globalPhoneHomeConfig);
            } else {
                LOGGER.debug("PHONE_HOME is not set properly for sending data to Cloudera Support, value: {}, updating it ...",
                        globalPhoneHomeConfig);
                resourceApi.updateConfig(createPhoneHomeConfig(true), "Update phone home setting for diagnostics (support destination)");
            }
        } else {
            if (globalPhoneHomeConfig) {
                LOGGER.debug("PHONE_HOME is not set properly for NOT sending data to Cloudera Support (only to cloud storage), " +
                                "value: {}, updating it ...", globalPhoneHomeConfig);
                resourceApi.updateConfig(createPhoneHomeConfig(false), "Update phone home setting for diagnostics (support destination)");
            } else {
                LOGGER.debug("PHONE_HOME is set properly for sending data to cloud storage only, value: {}", globalPhoneHomeConfig);
            }
        }
    }

    private void postUpdatePhoneHomeConfig(DiagnosticsDestination destination, ClouderaManagerResourceApi resourceApi,
            Boolean globalPhoneHomeConfig) throws ApiException {
        if (DiagnosticsDestination.SUPPORT.equals(destination)) {
            if (!globalPhoneHomeConfig) {
                LOGGER.debug("As PHONE_HOME value was different before diagnostics, setting its value back to false.");
                resourceApi.updateConfig(createPhoneHomeConfig(globalPhoneHomeConfig),
                        "Update phone home setting for diagnostics (support destination) - post update");
            }
        } else {
            if (globalPhoneHomeConfig) {
                LOGGER.debug("As PHONE_HOME value was different before diagnostics, setting its value back to true.");
                resourceApi.updateConfig(createPhoneHomeConfig(globalPhoneHomeConfig),
                        "Update phone home setting for diagnostics (support destination) - post update");
            }
        }
    }

    private ApiCollectDiagnosticDataArguments convertToCollectDiagnosticDataArguments(CmDiagnosticsParameters parameters) {
        ApiCollectDiagnosticDataArguments args = new ApiCollectDiagnosticDataArguments();
        args.setClusterName(parameters.getClusterName());
        args.setEndTime(parameters.getStartTime() == null ? new DateTime().toString() : new DateTime(parameters.getStartTime()).toString());
        args.setEndTime(parameters.getEndTime() == null ? new DateTime().toString() : new DateTime(parameters.getEndTime()).toString());
        args.setComments(parameters.getComments());
        args.setTicketNumber(parameters.getTicketNumber());
        args.setBundleSizeBytes(parameters.getBundleSizeBytes() == null ? new BigDecimal(Long.MAX_VALUE) : parameters.getBundleSizeBytes());
        args.setEnableMonitorMetricsCollection(parameters.getEnableMonitorMetricsCollection());
        if (CollectionUtils.isNotEmpty(parameters.getRoles())) {
            args.setRoles(parameters.getRoles());
        }
        return args;
    }

    private ApiConfigList createPhoneHomeConfig(Boolean value) {
        return new ApiConfigList().addItemsItem(new ApiConfig().name("PHONE_HOME").value(value.toString()));
    }

    private Boolean getPhoneHomeConfig(ApiConfigList configs) {
        Boolean result = true;
        if (configs != null && CollectionUtils.isNotEmpty(configs.getItems())) {
            for (ApiConfig config :configs.getItems()) {
                if ("phone_home".equalsIgnoreCase(config.getName())) {
                    result = Boolean.valueOf(config.getValue());
                }
            }
        }
        return result;
    }
}
