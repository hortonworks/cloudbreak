package com.sequenceiq.mock.clouderamanager.v45.controller;

import javax.inject.Inject;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

import com.sequenceiq.mock.clouderamanager.base.CommandsResourceOperation;
import com.sequenceiq.mock.swagger.model.ApiCommand;
import com.sequenceiq.mock.swagger.v45.api.CommandsResourceApi;

@Controller
public class CommandsResourceV45Controller implements CommandsResourceApi {

    @Inject
    private CommandsResourceOperation commandsResourceOperation;

    @Override
    public ResponseEntity<ApiCommand> readCommand(String mockUuid, Integer commandId) {
        return commandsResourceOperation.readCommand(mockUuid, commandId);
    }
}
