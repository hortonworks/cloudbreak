package com.sequenceiq.cloudbreak.cm;

import static com.sequenceiq.cloudbreak.cm.util.ClouderaManagerConstants.DEPLOY_CLUSTER_CLIENT_CONFIG_COMMAND_NAME;
import static com.sequenceiq.cloudbreak.cm.util.ClouderaManagerConstants.SUMMARY;
import static com.sequenceiq.cloudbreak.util.Benchmark.checkedMeasure;
import static com.sequenceiq.cloudbreak.util.Benchmark.multiCheckedMeasure;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.retry.support.RetrySynchronizationManager;
import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.ClustersResourceApi;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiCommand;
import com.sequenceiq.cloudbreak.cm.commands.SyncApiCommandPollerConfig;
import com.sequenceiq.cloudbreak.cm.model.ClouderaManagerClientConfigDeployRequest;
import com.sequenceiq.cloudbreak.cm.polling.ClouderaManagerPollingServiceProvider;
import com.sequenceiq.cloudbreak.cm.polling.PollingResultErrorHandler;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.util.Benchmark;

@Component
public class ClouderaManagerClientConfigDeployService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerClientConfigDeployService.class);

    private static final int CLIENT_CONFIG_MAX_ATTEMPTS = 5;

    private static final int CLIENT_CONFIG_BACKOFF = 5000;

    private final SyncApiCommandPollerConfig syncApiCommandPollerConfig;

    private final ClouderaManagerSyncApiCommandIdProvider clouderaManagerSyncApiCommandIdProvider;

    private final ClouderaManagerPollingServiceProvider clouderaManagerPollingServiceProvider;

    private final PollingResultErrorHandler pollingResultErrorHandler;

    private final ClouderaManagerCommonCommandService clouderaManagerCommonCommandService;

    public ClouderaManagerClientConfigDeployService(
            SyncApiCommandPollerConfig syncApiCommandPollerConfig,
            ClouderaManagerSyncApiCommandIdProvider clouderaManagerSyncApiCommandIdProvider,
            ClouderaManagerPollingServiceProvider clouderaManagerPollingServiceProvider,
            PollingResultErrorHandler pollingResultErrorHandler,
            ClouderaManagerCommonCommandService clouderaManagerCommonCommandService) {
        this.syncApiCommandPollerConfig = syncApiCommandPollerConfig;
        this.clouderaManagerSyncApiCommandIdProvider = clouderaManagerSyncApiCommandIdProvider;
        this.clouderaManagerPollingServiceProvider = clouderaManagerPollingServiceProvider;
        this.pollingResultErrorHandler = pollingResultErrorHandler;
        this.clouderaManagerCommonCommandService = clouderaManagerCommonCommandService;
    }

    @Retryable(
            value = { Exception.class },
            maxAttempts = CLIENT_CONFIG_MAX_ATTEMPTS,
            backoff = @Backoff(delay = CLIENT_CONFIG_BACKOFF)
    )
    public void deployAndPollClientConfig(ClouderaManagerClientConfigDeployRequest request)
            throws ApiException, CloudbreakException {
        LOGGER.info("deployAndPollClientConfig retry number: {}", RetrySynchronizationManager.getContext().getRetryCount());
        pollingResultErrorHandler.handlePollingResult(
                clouderaManagerPollingServiceProvider
                        .startDefaultPolling(
                                request.stack(),
                                request.client(),
                                deployClientConfigWithoutPollingResult(request),
                                request.message()
                        ),
                "Cluster was terminated while waiting for config deploy",
                "Timeout while Cloudera Manager was config deploying services.");
    }

    @Retryable(
            value = { Exception.class },
            maxAttempts = CLIENT_CONFIG_MAX_ATTEMPTS,
            backoff = @Backoff(delay = CLIENT_CONFIG_BACKOFF)
    )
    public BigDecimal deployClientConfig(ClouderaManagerClientConfigDeployRequest request)
            throws ApiException, CloudbreakException {
        LOGGER.info("deployClientConfig retry number: {}", RetrySynchronizationManager.getContext().getRetryCount());
        return deployClientConfigWithoutPollingResult(request);
    }

    private BigDecimal deployClientConfigWithoutPollingResult(ClouderaManagerClientConfigDeployRequest request)
            throws ApiException, CloudbreakException {
        List<ApiCommand> commands = request.api().listActiveCommands(request.stack().getName(), SUMMARY, null).getItems();
        return deployClientConfig(request.api(), request.stack(), commands);
    }

    private BigDecimal deployClientConfig(ClustersResourceApi clustersResourceApi, StackDtoDelegate stack, List<ApiCommand> commands)
            throws ApiException, CloudbreakException {
        return getDeployClientConfigCommandId(stack, clustersResourceApi, commands);
    }

    private BigDecimal getDeployClientConfigCommandId(StackDtoDelegate stack, ClustersResourceApi clustersResourceApi, List<ApiCommand> commands)
            throws ApiException, CloudbreakException {
        BigDecimal deployClientConfigCommandId;
        if (syncApiCommandPollerConfig.isSyncApiCommandPollingEnabled(stack.getResourceCrn())) {
            LOGGER.debug("Execute DeployClusterClientConfig command with sync poller.");
            deployClientConfigCommandId = multiCheckedMeasure(
                    (Benchmark.MultiCheckedSupplier<BigDecimal, ApiException, CloudbreakException>)
                            () -> getSyncDeployClientConfigCommandId(stack, clustersResourceApi, commands),
                    LOGGER, "The DeployClusterClientConfig command (with sync poller) registration to CM took {} ms");
        } else {
            LOGGER.debug("Execute DeployClusterClientConfig command without sync poller.");
            ApiCommand deployClientConfigCmd = checkedMeasure(
                    () -> clouderaManagerCommonCommandService.getApiCommand(
                            commands, DEPLOY_CLUSTER_CLIENT_CONFIG_COMMAND_NAME, stack.getName(), clustersResourceApi::deployClientConfig),
                    LOGGER,
                    "The DeployClusterClientConfig command registration to CM took {} ms");
            deployClientConfigCommandId = deployClientConfigCmd.getId();
        }
        return deployClientConfigCommandId;
    }

    private BigDecimal getSyncDeployClientConfigCommandId(StackDtoDelegate stack, ClustersResourceApi clustersResourceApi, List<ApiCommand> commands)
            throws CloudbreakException, ApiException {
        return clouderaManagerSyncApiCommandIdProvider.executeSyncApiCommandAndGetCommandId(
                syncApiCommandPollerConfig.getDeployClusterClientConfigCommandName(),
                clustersResourceApi,
                stack,
                commands,
                deployClientConfigCall(stack, clustersResourceApi)
        );
    }

    private Callable<ApiCommand> deployClientConfigCall(StackDtoDelegate stack, ClustersResourceApi clustersResourceApi) {
        return () -> clustersResourceApi.deployClientConfig(stack.getName());
    }
}
