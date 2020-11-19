package com.sequenceiq.mock.clouderamanager.v40.controller;

import java.math.BigDecimal;

import javax.inject.Inject;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

import com.sequenceiq.mock.clouderamanager.ProfileAwareComponent;
import com.sequenceiq.mock.swagger.model.ApiCommand;
import com.sequenceiq.mock.swagger.v40.api.CommandsResourceApi;

@Controller
public class CommandsResourceV40Controller implements CommandsResourceApi {

    @Inject
    private ProfileAwareComponent profileAwareComponent;

    @Override
    public ResponseEntity<ApiCommand> readCommand(String mockUuid, BigDecimal commandId) {
        ApiCommand apiCommand = new ApiCommand().id(commandId).active(Boolean.FALSE).success(Boolean.TRUE);
        return profileAwareComponent.exec(apiCommand);
    }
}
