package com.sequenceiq.cloudbreak.cm;

import java.math.BigDecimal;
import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.cloudera.api.swagger.ClustersResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiCdhUpgradeArgs;
import com.cloudera.api.swagger.model.ApiCommand;
import com.sequenceiq.cloudbreak.cm.commands.SyncApiCommandRetriever;
import com.sequenceiq.cloudbreak.cm.polling.ClouderaManagerPollingServiceProvider;
import com.sequenceiq.cloudbreak.cm.polling.PollingResultErrorHandler;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.polling.PollingResult;
import com.sequenceiq.cloudbreak.service.CloudbreakException;

@Service
class ClouderaManagerUpgradeService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerUpgradeService.class);

    @Inject
    private ClouderaManagerPollingServiceProvider clouderaManagerPollingServiceProvider;

    @Inject
    private PollingResultErrorHandler pollingResultErrorHandler;

    @Inject
    private SyncApiCommandRetriever syncApiCommandRetriever;

    @Inject
    private ClouderaManagerCommandsService clouderaManagerCommandsService;

    void callUpgradeCdhCommand(String stackProductVersion, ClustersResourceApi clustersResourceApi, Stack stack, ApiClient apiClient)
            throws ApiException, CloudbreakException {
        LOGGER.info("Upgrading the CDP Runtime...");
        try {
            BigDecimal upgradeCommandId = determineUpgradeLogic(stackProductVersion, clustersResourceApi, stack, apiClient);
            PollingResult pollingResult = clouderaManagerPollingServiceProvider.startPollingCdpRuntimeUpgrade(stack, apiClient, upgradeCommandId);
            pollingResultErrorHandler.handlePollingResult(pollingResult, "Cluster was terminated while waiting for CDP Runtime to be upgraded",
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

    private BigDecimal determineUpgradeLogic(String stackProductVersion, ClustersResourceApi clustersResourceApi, Stack stack, ApiClient apiClient)
            throws ApiException {
        Optional<BigDecimal> optionalUpgradeCommand = findUpgradeApiCommandId(clustersResourceApi, stack);
        BigDecimal upgradeCommandId;
        if (optionalUpgradeCommand.isPresent()) {
            upgradeCommandId = optionalUpgradeCommand.get();
            ApiCommand upgradeCommand = clouderaManagerCommandsService.getApiCommand(apiClient, upgradeCommandId);
            Boolean commandActive = upgradeCommand.getActive();
            Boolean commandSuccess = upgradeCommand.getSuccess();
            Boolean commandCanRetry = upgradeCommand.getCanRetry();
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
                    upgradeCommandId = callUpgrade(stackProductVersion, clustersResourceApi, stack);
                }
            }
        } else {
            LOGGER.debug("There is no upgrade command submitted yet, submitting it now");
            upgradeCommandId = callUpgrade(stackProductVersion, clustersResourceApi, stack);
        }
        return upgradeCommandId;
    }

    private BigDecimal callUpgrade(String stackProductVersion, ClustersResourceApi clustersResourceApi, Stack stack) throws ApiException {
        ApiCdhUpgradeArgs upgradeArgs = new ApiCdhUpgradeArgs();
        upgradeArgs.setCdhParcelVersion(stackProductVersion);
        return clustersResourceApi.upgradeCdhCommand(stack.getName(), upgradeArgs).getId();
    }

    private Optional<BigDecimal> findUpgradeApiCommandId(ClustersResourceApi clustersResourceApi, Stack stack) {
        try {
            return syncApiCommandRetriever.getCommandId("UpgradeCluster", clustersResourceApi, stack);
        } catch (CloudbreakException | ApiException e) {
            LOGGER.warn("Unexpected error during CM command table fetching, assuming no such command exists", e);
            return Optional.empty();
        }
    }

}
