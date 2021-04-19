package com.sequenceiq.cloudbreak.cm;

import static com.sequenceiq.cloudbreak.util.Benchmark.checkedMeasure;
import static com.sequenceiq.cloudbreak.util.Benchmark.multiCheckedMeasure;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.ClustersResourceApi;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiCommand;
import com.sequenceiq.cloudbreak.cm.commands.SyncApiCommandPollerConfig;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.util.Benchmark;
import com.sequenceiq.cloudbreak.util.CheckedFunction;

@Component
public class ClouderaManagerCommonCommandService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerCommonCommandService.class);

    private final SyncApiCommandPollerConfig syncApiCommandPollerConfig;

    private final ClouderaManagerSyncApiCommandIdProvider clouderaManagerSyncApiCommandIdProvider;

    public ClouderaManagerCommonCommandService(SyncApiCommandPollerConfig syncApiCommandPollerConfig,
            ClouderaManagerSyncApiCommandIdProvider clouderaManagerSyncApiCommandIdProvider) {
        this.syncApiCommandPollerConfig = syncApiCommandPollerConfig;
        this.clouderaManagerSyncApiCommandIdProvider = clouderaManagerSyncApiCommandIdProvider;

    }

    protected Integer getDeployClientConfigCommandId(Stack stack, ClustersResourceApi clustersResourceApi, List<ApiCommand> commands)
            throws ApiException, CloudbreakException {
        Integer deployClientConfigCommandId;
        if (syncApiCommandPollerConfig.isSyncApiCommandPollingEnaabled(stack.getResourceCrn())) {
            LOGGER.debug("Execute DeployClusterClientConfig command with sync poller.");
            deployClientConfigCommandId = multiCheckedMeasure(
                    (Benchmark.MultiCheckedSupplier<Integer, ApiException, CloudbreakException>)
                            () -> getSyncDeployClientConfigCommandId(stack, clustersResourceApi, commands),
                    LOGGER, "The DeployClusterClientConfig command (with sync poller) registration to CM took {} ms");
        } else {
            LOGGER.debug("Execute DeployClusterClientConfig command without sync poller.");
            ApiCommand deployClientConfigCmd = checkedMeasure(
                    () -> getApiCommand(commands, "DeployClusterClientConfig", stack.getName(),
                            clustersResourceApi::deployClientConfig),
                    LOGGER,
                    "The DeployClusterClientConfig command registration to CM took {} ms");
            deployClientConfigCommandId = deployClientConfigCmd.getId();
        }
        return deployClientConfigCommandId;
    }

    protected ApiCommand getApiCommand(List<ApiCommand> commands, String commandString, String clusterName, CheckedFunction<String, ApiCommand, ApiException> fn)
            throws ApiException {
        Optional<ApiCommand> optionalCommand = commands.stream().filter(cmd -> commandString.equals(cmd.getName())).findFirst();
        ApiCommand command;
        if (optionalCommand.isPresent()) {
            command = optionalCommand.get();
            LOGGER.debug("{} is already running with id: [{}]", commandString, command.getId());
        } else {
            command = fn.apply(clusterName);
        }
        return command;
    }

    private Integer getSyncDeployClientConfigCommandId(Stack stack, ClustersResourceApi clustersResourceApi, List<ApiCommand> commands)
            throws CloudbreakException, ApiException {
        return clouderaManagerSyncApiCommandIdProvider.executeSyncApiCommandAndGetCommandId(
                syncApiCommandPollerConfig.getDeployClusterClientConfigCommandName(), clustersResourceApi, stack, commands,
                deployClientConfigCall(stack, clustersResourceApi));
    }

    private Callable<ApiCommand> deployClientConfigCall(Stack stack, ClustersResourceApi clustersResourceApi) {
        return () -> clustersResourceApi.deployClientConfig(stack.getName());
    }
}
