package com.sequenceiq.cloudbreak.cm;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiCommand;
import com.sequenceiq.cloudbreak.util.CheckedFunction;

@Component
public class ClouderaManagerCommonCommandService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerCommonCommandService.class);

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
}
