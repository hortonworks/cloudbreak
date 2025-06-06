package com.sequenceiq.mock.clouderamanager.v40.controller;

import jakarta.inject.Inject;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

import com.sequenceiq.mock.clouderamanager.base.MgmtServiceResourceOperation;
import com.sequenceiq.mock.swagger.model.ApiCommand;
import com.sequenceiq.mock.swagger.model.ApiCommandList;
import com.sequenceiq.mock.swagger.model.ApiRoleTypeList;
import com.sequenceiq.mock.swagger.model.ApiService;
import com.sequenceiq.mock.swagger.v40.api.MgmtServiceResourceApi;

@Controller
public class MgmtServiceResourceV40Controller implements MgmtServiceResourceApi {

    @Inject
    private MgmtServiceResourceOperation mgmtServiceResourceOperation;

    @Override
    public ResponseEntity<Void> autoConfigure(String mockUuid) {
        return mgmtServiceResourceOperation.autoConfigure(mockUuid);
    }

    @Override
    public ResponseEntity<ApiCommandList> listActiveCommands(String mockUuid, String view) {
        return mgmtServiceResourceOperation.listActiveCommands(mockUuid, view);
    }

    @Override
    public ResponseEntity<ApiRoleTypeList> listRoleTypes(String mockUuid) {
        return mgmtServiceResourceOperation.listRoleTypes(mockUuid);
    }

    @Override
    public ResponseEntity<ApiService> readService(String mockUuid, String view) {
        return mgmtServiceResourceOperation.readService(mockUuid, view);
    }

    @Override
    public ResponseEntity<ApiCommand> restartCommand(String mockUuid) {
        return mgmtServiceResourceOperation.restartCommand(mockUuid);
    }

    @Override
    public ResponseEntity<ApiService> setupCMS(String mockUuid, ApiService body) {
        return mgmtServiceResourceOperation.setupCMS(mockUuid, body);
    }

    @Override
    public ResponseEntity<ApiCommand> startCommand(String mockUuid) {
        return mgmtServiceResourceOperation.startCommand(mockUuid);
    }

    @Override
    public ResponseEntity<ApiCommand> stopCommand(String mockUuid) {
        return mgmtServiceResourceOperation.stopCommand(mockUuid);
    }
}
