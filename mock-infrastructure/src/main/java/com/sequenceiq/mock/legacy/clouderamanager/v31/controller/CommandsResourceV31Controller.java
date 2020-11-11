package com.sequenceiq.mock.legacy.clouderamanager.v31.controller;

import java.math.BigDecimal;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;

import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

import com.sequenceiq.mock.legacy.clouderamanager.DefaultModelService;
import com.sequenceiq.mock.legacy.clouderamanager.ProfileAwareResponse;
import com.sequenceiq.mock.legacy.clouderamanager.ResponseUtil;
import com.sequenceiq.mock.swagger.model.ApiCommand;
import com.sequenceiq.mock.swagger.v31.api.CommandsResourceApi;

@Controller
public class CommandsResourceV31Controller implements CommandsResourceApi {

    @Inject
    private HttpServletRequest request;

    @Inject
    private DefaultModelService defaultModelService;

    @Override
    public ResponseEntity<ApiCommand> abortCommand(BigDecimal commandId) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<Resource> getStandardError(BigDecimal commandId) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<Resource> getStandardOutput(BigDecimal commandId) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiCommand> readCommand(BigDecimal commandId) {
        ApiCommand apiCommand = new ApiCommand().id(commandId).active(Boolean.FALSE).success(Boolean.TRUE);
        return ProfileAwareResponse.exec(apiCommand, defaultModelService);
    }

    @Override
    public ResponseEntity<ApiCommand> retry(BigDecimal commandId) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }
}
