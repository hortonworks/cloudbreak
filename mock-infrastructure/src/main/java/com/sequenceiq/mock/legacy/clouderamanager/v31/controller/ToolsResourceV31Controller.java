package com.sequenceiq.mock.legacy.clouderamanager.v31.controller;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

import com.sequenceiq.mock.legacy.clouderamanager.ResponseUtil;
import com.sequenceiq.mock.swagger.v31.api.ToolsResourceApi;
import com.sequenceiq.mock.swagger.model.ApiEcho;

@Controller
public class ToolsResourceV31Controller implements ToolsResourceApi {

    @Inject
    private HttpServletRequest request;

    @Override
    public ResponseEntity<ApiEcho> echo(@Valid String message) {
        message = message == null ? "Hello World!" : message;
        return ResponseEntity.ok(new ApiEcho().message(message));
    }

    @Override
    public ResponseEntity<ApiEcho> echoError(@Valid String message) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }
}
