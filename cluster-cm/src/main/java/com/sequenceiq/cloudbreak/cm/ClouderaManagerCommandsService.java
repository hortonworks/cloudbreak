package com.sequenceiq.cloudbreak.cm;

import java.math.BigDecimal;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cloudera.api.swagger.CommandsResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiCommand;
import com.sequenceiq.cloudbreak.cm.client.retry.ClouderaManagerApiFactory;

@Service
public class ClouderaManagerCommandsService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerCommandsService.class);

    @Inject
    private ClouderaManagerApiFactory clouderaManagerApiFactory;

    public ApiCommand getApiCommand(ApiClient client, BigDecimal commandId) throws ApiException {
        CommandsResourceApi commandsResourceApi = clouderaManagerApiFactory.getCommandsResourceApi(client);
        ApiCommand apiCommand = commandsResourceApi.readCommand(commandId);
        LOGGER.debug("Get Api command by id {} result is {}", commandId, apiCommand);
        return apiCommand;
    }

    public ApiCommand retryApiCommand(ApiClient client, BigDecimal commandId) throws ApiException {
        CommandsResourceApi commandsResourceApi = clouderaManagerApiFactory.getCommandsResourceApi(client);
        ApiCommand retryApiCommand = commandsResourceApi.retry(commandId);
        LOGGER.debug("Retry Api command by id {} result is {}", commandId, retryApiCommand);
        return retryApiCommand;
    }
}