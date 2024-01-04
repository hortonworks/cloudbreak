package com.sequenceiq.mock.clouderamanager.base;

import jakarta.inject.Inject;
import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

import com.sequenceiq.mock.clouderamanager.ClouderaManagerStoreService;
import com.sequenceiq.mock.swagger.model.ApiEcho;

@Controller
public class ToolsResourceOperation {

    @Inject
    private ClouderaManagerStoreService clouderaManagerStoreService;

    public ResponseEntity<ApiEcho> echo(String mockUuid, @Valid String message) {
        clouderaManagerStoreService.start(mockUuid);
        message = message == null ? "Hello World!" : message;
        return ResponseEntity.ok(new ApiEcho().message(message));
    }

}
