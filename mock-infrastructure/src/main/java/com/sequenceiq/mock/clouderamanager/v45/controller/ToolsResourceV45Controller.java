package com.sequenceiq.mock.clouderamanager.v45.controller;

import jakarta.inject.Inject;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

import com.sequenceiq.mock.clouderamanager.base.ToolsResourceOperation;
import com.sequenceiq.mock.swagger.model.ApiEcho;
import com.sequenceiq.mock.swagger.v45.api.ToolsResourceApi;

@Controller
public class ToolsResourceV45Controller implements ToolsResourceApi {

    @Inject
    private ToolsResourceOperation toolsResourceOperation;

    @Override
    public ResponseEntity<ApiEcho> echo(String mockUuid, String message) {
        return toolsResourceOperation.echo(mockUuid, message);
    }

}
