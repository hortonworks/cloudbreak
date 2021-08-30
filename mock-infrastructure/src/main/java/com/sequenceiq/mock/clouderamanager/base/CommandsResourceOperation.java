package com.sequenceiq.mock.clouderamanager.base;

import javax.inject.Inject;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

import com.sequenceiq.mock.clouderamanager.ResponseCreatorComponent;
import com.sequenceiq.mock.swagger.model.ApiCommand;

@Controller
public class CommandsResourceOperation {

    @Inject
    private ResponseCreatorComponent responseCreatorComponent;

    public ResponseEntity<ApiCommand> readCommand(String mockUuid, Integer commandId) {
        ApiCommand apiCommand = new ApiCommand().id(commandId).active(Boolean.FALSE).success(Boolean.TRUE);
        return responseCreatorComponent.exec(apiCommand);
    }
}
