package com.sequenceiq.mock.legacy.clouderamanager.v40.controller;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

import com.sequenceiq.mock.legacy.clouderamanager.ResponseUtil;
import com.sequenceiq.mock.legacy.clouderamanager.DataProviderService;
import com.sequenceiq.mock.legacy.clouderamanager.DefaultModelService;
import com.sequenceiq.mock.legacy.clouderamanager.ProfileAwareResponse;
import com.sequenceiq.mock.swagger.v40.api.MgmtServiceResourceApi;
import com.sequenceiq.mock.swagger.model.ApiCommand;
import com.sequenceiq.mock.swagger.model.ApiCommandList;
import com.sequenceiq.mock.swagger.model.ApiRoleTypeList;
import com.sequenceiq.mock.swagger.model.ApiService;
import com.sequenceiq.mock.swagger.model.ApiServiceConfig;
import com.sequenceiq.mock.swagger.model.ApiServiceState;

@Controller
public class MgmtServiceResourceV40Controller implements MgmtServiceResourceApi {

    @Inject
    private HttpServletRequest request;

    @Inject
    private DefaultModelService defaultModelService;

    @Inject
    private DataProviderService dataProviderService;

    @Override
    public ResponseEntity<Void> autoAssignRoles() {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<Void> autoConfigure() {
        return ProfileAwareResponse.exec(defaultModelService);
    }

    @Override
    public ResponseEntity<ApiService> deleteCMS() {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiCommand> enterMaintenanceMode() {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiCommand> exitMaintenanceMode() {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiCommandList> listActiveCommands(@Valid String view) {
        ApiCommandList response = new ApiCommandList().items(List.of(new ApiCommand().id(new BigDecimal(1)).active(Boolean.FALSE).success(Boolean.TRUE)));
        return ProfileAwareResponse.exec(response, defaultModelService);
    }

    @Override
    public ResponseEntity<ApiRoleTypeList> listRoleTypes() {
        return ProfileAwareResponse.exec(new ApiRoleTypeList().items(new ArrayList<>()), defaultModelService);
    }

    @Override
    public ResponseEntity<ApiService> readService(@Valid String view) {
        ApiService apiService = new ApiService().serviceState(ApiServiceState.STARTED);
        return ProfileAwareResponse.exec(apiService, defaultModelService);
    }

    @Override
    public ResponseEntity<ApiServiceConfig> readServiceConfig(@Valid String view) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiCommand> restartCommand() {
        return ProfileAwareResponse.exec(dataProviderService.getSuccessfulApiCommand(), defaultModelService);
    }

    @Override
    public ResponseEntity<ApiService> setupCMS(@Valid ApiService body) {
        return ProfileAwareResponse.exec(new ApiService(), defaultModelService);
    }

    @Override
    public ResponseEntity<ApiCommand> startCommand() {
        return ProfileAwareResponse.exec(new ApiCommand(), defaultModelService);
    }

    @Override
    public ResponseEntity<ApiCommand> stopCommand() {
        return ProfileAwareResponse.exec(new ApiCommand(), defaultModelService);
    }

    @Override
    public ResponseEntity<ApiServiceConfig> updateServiceConfig(@Valid String message, @Valid ApiServiceConfig body) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }
}
