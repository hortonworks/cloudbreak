package com.sequenceiq.cloudbreak.cm;

import static com.sequenceiq.cloudbreak.polling.PollingResult.isExited;
import static com.sequenceiq.cloudbreak.polling.PollingResult.isTimeout;

import java.math.BigDecimal;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.commons.collections.CollectionUtils;
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
import com.sequenceiq.cloudbreak.client.HttpClientConfig;
import com.sequenceiq.cloudbreak.cloud.scheduler.CancellationException;
import com.sequenceiq.cloudbreak.cluster.api.ClusterDiagnosticsService;
import com.sequenceiq.cloudbreak.cluster.service.ClusterClientInitException;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerApiClientProvider;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerClientInitException;
import com.sequenceiq.cloudbreak.cm.client.retry.ClouderaManagerApiFactory;
import com.sequenceiq.cloudbreak.cm.polling.ClouderaManagerPollingServiceProvider;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.polling.PollingResult;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
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

    private final Stack stack;

    private final HttpClientConfig clientConfig;

    private ApiClient client;

    public ClouderaManagerDiagnosticsService(Stack stack, HttpClientConfig clientConfig) {
        this.stack = stack;
        this.clientConfig = clientConfig;
    }

    @PostConstruct
    public void initApiClient() throws ClusterClientInitException {
        Cluster cluster = stack.getCluster();
        String cloudbreakAmbariUser = cluster.getCloudbreakAmbariUser();
        String cloudbreakAmbariPassword = cluster.getCloudbreakAmbariPassword();
        try {
            client = clouderaManagerApiClientProvider
                    .getClient(stack.getGatewayPort(), cloudbreakAmbariUser, cloudbreakAmbariPassword, clientConfig);
        } catch (ClouderaManagerClientInitException e) {
            throw new ClusterClientInitException(e);
        }
    }

    @Override
    public void collectDiagnostics(CmDiagnosticsParameters parameters) throws CloudbreakException {
        ClouderaManagerResourceApi resourceApi = clouderaManagerApiFactory.getClouderaManagerResourceApi(client);
        try {
            ApiCommand collectDiagnostics = resourceApi.collectDiagnosticDataCommand(convertToCollectDiagnosticDataArguments(parameters));
            PollingResult pollingResult = clouderaManagerPollingServiceProvider.startPollingCollectDiagnostics(stack, client, collectDiagnostics.getId());
            if (isExited(pollingResult)) {
                throw new CancellationException("Cluster was terminated while waiting for command API to be available for diagnostics collections");
            } else if (isTimeout(pollingResult)) {
                throw new CloudbreakException("Timeout during waiting for command API to be available (diagnostics collections).");
            }
        } catch (ApiException e) {
            LOGGER.error("Error during CM based diagnostics collection", e);
            throw new ClouderaManagerOperationFailedException("Collect diagnostics failed", e);
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
        args.setIncludeInfoLog(parameters.getIncludeInfoLog());
        args.setEnableMonitorMetricsCollection(parameters.getEnableMonitorMetricsCollection());
        if (CollectionUtils.isNotEmpty(parameters.getRoles())) {
            args.setRoles(parameters.getRoles());
        }
        return args;
    }
}
