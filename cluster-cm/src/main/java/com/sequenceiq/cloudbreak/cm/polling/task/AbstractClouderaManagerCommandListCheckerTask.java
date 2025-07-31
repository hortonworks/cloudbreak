package com.sequenceiq.cloudbreak.cm.polling.task;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloudera.api.swagger.CommandsResourceApi;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiCommand;
import com.sequenceiq.cloudbreak.cluster.service.ClusterEventService;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerApiPojoFactory;
import com.sequenceiq.cloudbreak.cm.exception.ClouderaManagerOperationFailedException;
import com.sequenceiq.cloudbreak.cm.exception.CommandDetails;
import com.sequenceiq.cloudbreak.cm.exception.CommandDetailsFormatter;
import com.sequenceiq.cloudbreak.cm.polling.ClouderaManagerCommandListPollerObject;
import com.sequenceiq.cloudbreak.cm.util.ClouderaManagerCommandUtil;

public abstract class AbstractClouderaManagerCommandListCheckerTask<T extends ClouderaManagerCommandListPollerObject>
        extends AbstractClouderaManagerApiCheckerTask<T> {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractClouderaManagerCommandListCheckerTask.class);

    protected AbstractClouderaManagerCommandListCheckerTask(ClouderaManagerApiPojoFactory clouderaManagerApiPojoFactory,
            ClusterEventService clusterEventService) {
        super(clouderaManagerApiPojoFactory, clusterEventService);
    }

    protected boolean doStatusCheck(T pollerObject) throws ApiException {
        CommandsResourceApi commandsResourceApi = clouderaManagerApiPojoFactory.getCommandsResourceApi(pollerObject.getApiClient());
        List<ApiCommand> apiCommands = collectApiCommands(pollerObject, commandsResourceApi);
        boolean allCommandsFinished = apiCommands.stream().noneMatch(ApiCommand::getActive);
        if (allCommandsFinished) {
            validateApiCommandResults(apiCommands, commandsResourceApi);
            return true;
        } else {
            return false;
        }
    }

    private List<ApiCommand> collectApiCommands(T pollerObject, CommandsResourceApi commandsResourceApi) throws ApiException {
        List<ApiCommand> apiCommands = new ArrayList<>();
        for (BigDecimal commandId : pollerObject.getIdList()) {
            ApiCommand apiCommand = commandsResourceApi.readCommand(commandId);
            apiCommands.add(apiCommand);
            if (apiCommand.getActive()) {
                LOGGER.debug("Command [" + getCommandName() + "] with id [" + commandId + "] is active, so it hasn't finished yet");
                break;
            }
        }
        return apiCommands;
    }

    private void validateApiCommandResults(List<ApiCommand> apiCommands, CommandsResourceApi commandsResourceApi) {
        List<ApiCommand> failedCommands = apiCommands.stream().filter(cmd -> !cmd.getSuccess()).collect(Collectors.toList());
        if (!failedCommands.isEmpty()) {
            List<CommandDetails> failedCommandDetails = new LinkedList<>();
            failedCommands.forEach(cmd -> failedCommandDetails.addAll(ClouderaManagerCommandUtil.getFailedOrActiveCommands(cmd, commandsResourceApi)));
            String message = CommandDetailsFormatter.formatFailedCommands(failedCommandDetails);
            LOGGER.debug("Top level commands {}. Failed or active commands: {}",
                    failedCommands.stream().map(CommandDetails::fromApiCommand).collect(Collectors.toList()), failedCommandDetails);
            throw new ClouderaManagerOperationFailedException(message);
        }
    }

    protected String getOperationIdentifier(T pollerObject) {
        return StringUtils.join(pollerObject.getIdList(), ",");
    }

    protected abstract String getCommandName();

    @Override
    protected String getPollingName() {
        return getCommandName();
    }

    @Override
    protected String getToleratedErrorMessage(T pollerObject, ApiException e) {
        return String.format("Commands [%s] with ids [%s] failed with a tolerated error '%s' for the %s. time(s). ",
                getCommandName(), getOperationIdentifier(pollerObject), e.getMessage(), toleratedErrorCounter);
    }

    @Override
    protected String getErrorMessage(T pollerObject, ApiException e) {
        return String.format("Commands [%s] with ids [%s] failed with a %s.",
                getCommandName(), getOperationIdentifier(pollerObject), e.getClass().getSimpleName());
    }
}
