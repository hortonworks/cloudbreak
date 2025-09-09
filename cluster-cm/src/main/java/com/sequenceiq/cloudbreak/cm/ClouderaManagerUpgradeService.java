package com.sequenceiq.cloudbreak.cm;

import java.math.BigDecimal;
import java.util.Optional;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.cloudera.api.swagger.ClustersResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiCdhUpgradeArgs;
import com.cloudera.api.swagger.model.ApiCommand;
import com.cloudera.api.swagger.model.ApiRollingUpgradeClusterArgs;
import com.sequenceiq.cloudbreak.cm.commands.SyncApiCommandRetriever;
import com.sequenceiq.cloudbreak.cm.polling.ClouderaManagerPollingServiceProvider;
import com.sequenceiq.cloudbreak.cm.polling.PollingResultErrorHandler;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.cloudbreak.polling.ExtendedPollingResult;
import com.sequenceiq.cloudbreak.service.CloudbreakException;

@Service
class ClouderaManagerUpgradeService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerUpgradeService.class);

    private static final String RUNTIME_UPGRADE_COMMAND = "UpgradeCluster";

    private static final String POST_RUNTIME_UPGRADE_COMMAND = "PostClouderaRuntimeUpgradeCommand";

    @Inject
    private ClouderaManagerPollingServiceProvider clouderaManagerPollingServiceProvider;

    @Inject
    private PollingResultErrorHandler pollingResultErrorHandler;

    @Inject
    private SyncApiCommandRetriever syncApiCommandRetriever;

    @Inject
    private ClouderaManagerCommandsService clouderaManagerCommandsService;

    void callUpgradeCdhCommand(String stackProductVersion, ClustersResourceApi clustersResourceApi, StackDtoDelegate stack, ApiClient apiClient,
            boolean rollingUpgradeEnabled) throws ApiException, CloudbreakException {
        LOGGER.info("Upgrading the CDP Runtime...");
        try {
            BigDecimal upgradeCommandId = determineUpgradeLogic(stackProductVersion, clustersResourceApi, stack, apiClient, false, rollingUpgradeEnabled);
            ExtendedPollingResult pollingResult = startPollingCdpRuntimeUpgrade(stack, apiClient, rollingUpgradeEnabled, upgradeCommandId);
            pollingResultErrorHandler.handlePollingResult(pollingResult,
                    "Cluster was terminated while waiting for CDP Runtime to be upgraded",
                    "Timeout during CDP Runtime upgrade.");
        } catch (ApiException ex) {
            String responseBody = ex.getResponseBody();
            if (StringUtils.hasText(responseBody) && responseBody.contains("Cannot upgrade because the version is already CDH")) {
                LOGGER.info("The Runtime has already been upgraded to {}", stackProductVersion);
            } else {
                throw ex;
            }
        }
        LOGGER.info("Runtime is successfully upgraded!");
    }

    void callPostRuntimeUpgradeCommand(ClustersResourceApi clustersResourceApi, StackDtoDelegate stack, ApiClient apiClient)
            throws ApiException, CloudbreakException {
        LOGGER.info("Call post runtime upgrade command after maintenance upgrade");
        BigDecimal upgradeCommandId = determineUpgradeLogic("", clustersResourceApi, stack, apiClient, true, false);
        ExtendedPollingResult pollingResult = startPollingCdpRuntimeUpgrade(stack, apiClient, false, upgradeCommandId);
        pollingResultErrorHandler.handlePollingResult(pollingResult.getPollingResult(), "Cluster was terminated while waiting for CDP Runtime to be upgraded",
                "Timeout during CDP Runtime upgrade.");
        LOGGER.info("Runtime is successfully upgraded!");
    }

    private BigDecimal determineUpgradeLogic(String stackProductVersion, ClustersResourceApi clustersResourceApi, StackDtoDelegate stack,
            ApiClient apiClient, boolean postRuntimeUpgrade, boolean rollingUpgradeEnabled) throws ApiException {
        String command = postRuntimeUpgrade ? POST_RUNTIME_UPGRADE_COMMAND : RUNTIME_UPGRADE_COMMAND;
        LOGGER.debug("Upgrade command to execute: {}", command);
        Optional<BigDecimal> optionalUpgradeCommand = findUpgradeApiCommandId(clustersResourceApi, stack, command);
        BigDecimal upgradeCommandId;
        if (optionalUpgradeCommand.isPresent()) {
            upgradeCommandId = optionalUpgradeCommand.get();
            ApiCommand upgradeCommand = clouderaManagerCommandsService.getApiCommand(apiClient, upgradeCommandId);
            Boolean commandActive = upgradeCommand.isActive();
            Boolean commandSuccess = upgradeCommand.isSuccess();
            Boolean commandCanRetry = upgradeCommand.isCanRetry();
            if (commandActive) {
                LOGGER.debug("Upgrade of CDP Runtime is already running with id: [{}]", upgradeCommandId);
            } else {
                if (!commandSuccess && commandCanRetry) {
                    LOGGER.debug("Retrying previous failed upgrade with command id {}", upgradeCommandId);
                    upgradeCommandId = clouderaManagerCommandsService.retryApiCommand(apiClient, upgradeCommandId).getId();
                } else {
                    LOGGER.debug("Last upgrade command ({}) is not active, it was {} successful and {} retryable, submitting it now", upgradeCommandId,
                            commandSuccess ? "" : "not",
                            commandCanRetry ? "" : "not");
                    upgradeCommandId = executeUpgrade(stackProductVersion, clustersResourceApi, stack, postRuntimeUpgrade, rollingUpgradeEnabled);
                }
            }
        } else {
            LOGGER.debug("There is no upgrade command submitted yet, submitting it now");
            upgradeCommandId = executeUpgrade(stackProductVersion, clustersResourceApi, stack, postRuntimeUpgrade, rollingUpgradeEnabled);
        }
        return upgradeCommandId;
    }

    private BigDecimal executeUpgrade(String stackProductVersion, ClustersResourceApi clustersResourceApi, StackDtoDelegate stack,
            boolean postRuntimeUpgrade, boolean rollingUpgradeEnabled) throws ApiException {
        BigDecimal upgradeCommandId;
        if (postRuntimeUpgrade) {
            LOGGER.debug("Calling post upgrade with command {}", POST_RUNTIME_UPGRADE_COMMAND);
            upgradeCommandId = callPostUpgrade(clustersResourceApi, stack);
        } else {
            LOGGER.debug("Calling upgrade with command {}", RUNTIME_UPGRADE_COMMAND);
            upgradeCommandId = callUpgrade(stackProductVersion, clustersResourceApi, stack, rollingUpgradeEnabled);
        }
        return upgradeCommandId;
    }

    private BigDecimal callUpgrade(String stackProductVersion, ClustersResourceApi clustersResourceApi, StackDtoDelegate stack, boolean rollingUpgradeEnabled)
            throws ApiException {
        ApiCdhUpgradeArgs upgradeArgs = new ApiCdhUpgradeArgs();
        upgradeArgs.setCdhParcelVersion(stackProductVersion);
        if (rollingUpgradeEnabled) {
            LOGGER.debug("Rolling upgrade is enabled for CDH upgrade command.");
            upgradeArgs.setRollingRestartArgs(new ApiRollingUpgradeClusterArgs());
        }
        return clustersResourceApi.upgradeCdhCommand(stack.getName(), upgradeArgs).getId();
    }

    private BigDecimal callPostUpgrade(ClustersResourceApi clustersResourceApi, StackDtoDelegate stack) throws ApiException {
        return clustersResourceApi.postClouderaRuntimeUpgrade(stack.getName()).getId();
    }

    private ExtendedPollingResult startPollingCdpRuntimeUpgrade(StackDtoDelegate stack, ApiClient apiClient, boolean rollingUpgradeEnabled,
            BigDecimal upgradeCommandId) {
        return clouderaManagerPollingServiceProvider.startPollingCdpRuntimeUpgrade(stack, apiClient, upgradeCommandId, rollingUpgradeEnabled);
    }

    private Optional<BigDecimal> findUpgradeApiCommandId(ClustersResourceApi clustersResourceApi, StackDtoDelegate stack, String command) {
        try {
            return syncApiCommandRetriever.getCommandId(command, clustersResourceApi, stack.getStack());
        } catch (CloudbreakException | ApiException e) {
            LOGGER.warn("Unexpected error during CM command table fetching, assuming no such command exists", e);
            return Optional.empty();
        }
    }

}
