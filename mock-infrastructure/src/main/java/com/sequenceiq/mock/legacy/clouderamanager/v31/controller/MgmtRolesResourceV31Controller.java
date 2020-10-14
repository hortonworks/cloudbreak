package com.sequenceiq.mock.legacy.clouderamanager.v31.controller;

import java.util.ArrayList;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

import com.sequenceiq.mock.legacy.clouderamanager.ProfileAwareResponse;
import com.sequenceiq.mock.legacy.clouderamanager.ResponseUtil;
import com.sequenceiq.mock.legacy.clouderamanager.DataProviderService;
import com.sequenceiq.mock.legacy.clouderamanager.DefaultModelService;
import com.sequenceiq.mock.swagger.v31.api.MgmtRolesResourceApi;
import com.sequenceiq.mock.swagger.model.ApiCommand;
import com.sequenceiq.mock.swagger.model.ApiCommandList;
import com.sequenceiq.mock.swagger.model.ApiConfigList;
import com.sequenceiq.mock.swagger.model.ApiRole;
import com.sequenceiq.mock.swagger.model.ApiRoleList;

@Controller
public class MgmtRolesResourceV31Controller implements MgmtRolesResourceApi {

    @Inject
    private HttpServletRequest request;

    @Inject
    private DefaultModelService defaultModelService;

    @Inject
    private DataProviderService dataProviderService;

    @Override
    public ResponseEntity<ApiRoleList> createRoles(@Valid ApiRoleList body) {
        return ProfileAwareResponse.exec(new ApiRoleList().items(new ArrayList<>()), defaultModelService);
    }

    @Override
    public ResponseEntity<ApiRole> deleteRole(String roleName) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiCommand> enterMaintenanceMode(String roleName) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiCommand> exitMaintenanceMode(String roleName) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<String> getFullLog(String roleName) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<String> getStacksLog(String roleName) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<Void> getStacksLogsBundle(String roleName) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<String> getStandardError(String roleName) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<String> getStandardOutput(String roleName) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiCommandList> listActiveCommands(String roleName, @Valid String view) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiRole> readRole(String roleName) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiConfigList> readRoleConfig(String roleName, @Valid String view) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiRoleList> readRoles() {
        return ProfileAwareResponse.exec(new ApiRoleList().items(new ArrayList<>()), defaultModelService);
    }

    @Override
    public ResponseEntity<ApiConfigList> updateRoleConfig(String roleName, @Valid String message, @Valid ApiConfigList body) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }
}
