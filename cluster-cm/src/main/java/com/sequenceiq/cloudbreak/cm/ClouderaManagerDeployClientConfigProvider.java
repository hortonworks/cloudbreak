package com.sequenceiq.cloudbreak.cm;

import java.math.BigDecimal;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.cloudera.api.swagger.ClustersResourceApi;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiCommand;
import com.google.common.annotations.VisibleForTesting;
import com.sequenceiq.cloudbreak.cm.commands.DeployClientConfigCommandRetriever;
import com.sequenceiq.cloudbreak.cm.polling.ClouderaManagerPollingServiceProvider;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.CloudbreakException;

@Service
public class ClouderaManagerDeployClientConfigProvider {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(ClouderaManagerDeployClientConfigProvider.class);

    private final Integer interruptTimeoutSeconds;

    private final DeployClientConfigCommandRetriever deployClientConfigCommandRetriever;

    private final ClouderaManagerPollingServiceProvider clouderaManagerPollingServiceProvider;

    public ClouderaManagerDeployClientConfigProvider(
            DeployClientConfigCommandRetriever deployClientConfigCommandRetriever,
            ClouderaManagerPollingServiceProvider clouderaManagerPollingServiceProvider,
            @Value("${cb.cm.client.commands.deployClientConfig.interrupt.timeout.seconds:}")
                    Integer interruptTimeoutSeconds) {
        this.deployClientConfigCommandRetriever = deployClientConfigCommandRetriever;
        this.interruptTimeoutSeconds = interruptTimeoutSeconds;
        this.clouderaManagerPollingServiceProvider = clouderaManagerPollingServiceProvider;
    }

    /**
     * Obtain deploy cluster client config command ID with the following steps:
     * 1. check are there any existing successful deployClusterClientConfig commands
     * 2. execute deployClusterClientConfig command with a timeout
     * 3. if the command ties out, use 1. with polling
     * 4. if polling finished, do a last check, returns with the command id or throw an exception
     */
    public BigDecimal deployClientConfigAndGetCommandId(
            ClustersResourceApi api, Stack stack) throws CloudbreakException, ApiException {
        BigDecimal lastDeployClientConfigCommand = deployClientConfigCommandRetriever.getCommandId(api, stack);
        if (lastDeployClientConfigCommand != null) {
            LOGGER.debug("Found already existing DeployClusterClientConfig command with id: {}",
                    lastDeployClientConfigCommand);
        }
        ExecutorService executor = createExecutor();
        Future<BigDecimal> future = executor.submit(() -> {
            ApiCommand deployCommand = api.deployClientConfig(stack.getName());
            return deployCommand.getId();
        });
        try {
            return future.get(interruptTimeoutSeconds, TimeUnit.SECONDS);
        } catch (TimeoutException timeoutException) {
            LOGGER.debug("Deploy client config command took too much time. Start command ID "
                    + "query by listing active commands");
            clouderaManagerPollingServiceProvider.checkNewDeployClusterClientConfigCommand(
                    stack, api.getApiClient(), lastDeployClientConfigCommand,
                    deployClientConfigCommandRetriever);
            BigDecimal finalCommandId = deployClientConfigCommandRetriever.getCommandId(api, stack);
            if (finalCommandId == null) {
                throw new CloudbreakException(
                        String.format("Obtaining Cloudera Manager Deploy config command ID was not possible neither by"
                                + " listing Cloudera Manager commands nor using commandTable [stack: %s]", stack.getName()));
            }
            return finalCommandId;
        } catch (ExecutionException ee) {
            throw new ApiException(ee.getCause());
        } catch (InterruptedException e) {
            throw new CloudbreakException(
                    "Obtaining Cloudera Manager Deploy config command ID interrupted", e);
        } finally {
            executor.shutdown();
        }
    }

    @VisibleForTesting
    ExecutorService createExecutor() {
        return Executors.newSingleThreadExecutor();
    }
}
