package com.sequenceiq.mock.legacy.clouderamanager.v31.controller;

import java.util.List;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

import com.sequenceiq.mock.legacy.clouderamanager.ProfileAwareResponse;
import com.sequenceiq.mock.legacy.clouderamanager.ResponseUtil;
import com.sequenceiq.mock.legacy.clouderamanager.DataProviderService;
import com.sequenceiq.mock.legacy.clouderamanager.DefaultModelService;
import com.sequenceiq.mock.swagger.v31.api.RolesResourceApi;
import com.sequenceiq.mock.swagger.model.ApiCommand;
import com.sequenceiq.mock.swagger.model.ApiCommandList;
import com.sequenceiq.mock.swagger.model.ApiCommandMetadataList;
import com.sequenceiq.mock.swagger.model.ApiConfigList;
import com.sequenceiq.mock.swagger.model.ApiEntityTag;
import com.sequenceiq.mock.swagger.model.ApiImpalaRoleDiagnosticsArgs;
import com.sequenceiq.mock.swagger.model.ApiMetricList;
import com.sequenceiq.mock.swagger.model.ApiRole;
import com.sequenceiq.mock.swagger.model.ApiRoleList;
import com.sequenceiq.mock.swagger.model.ApiRoleNameList;
import com.sequenceiq.mock.swagger.model.ApiServiceRef;

@Controller
public class RolesResourceV31Controller implements RolesResourceApi {

    @Inject
    private HttpServletRequest request;

    @Inject
    private DefaultModelService defaultModelService;

    @Inject
    private DataProviderService dataProviderService;

    @Override
    public ResponseEntity<List<ApiEntityTag>> addTags(String clusterName, String roleName, String serviceName, @Valid List<ApiEntityTag> body) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiRoleList> bulkDeleteRoles(String clusterName, String serviceName, @Valid ApiRoleNameList body) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiRoleList> createRoles(String clusterName, String serviceName, @Valid ApiRoleList body) {
        ApiRoleList roleList = new ApiRoleList().items(List.of(new ApiRole().name("role1").serviceRef(new ApiServiceRef().serviceName("service1"))));
        return ProfileAwareResponse.exec(roleList, defaultModelService);
    }

    @Override
    public ResponseEntity<ApiRole> deleteRole(String clusterName, String roleName, String serviceName) {
        return ProfileAwareResponse.exec(new ApiRole().name("role1"), defaultModelService);
    }

    @Override
    public ResponseEntity<List<ApiEntityTag>> deleteTags(String clusterName, String roleName, String serviceName, @Valid List<ApiEntityTag> body) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiCommand> enterMaintenanceMode(String clusterName, String roleName, String serviceName) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiCommand> exitMaintenanceMode(String clusterName, String roleName, String serviceName) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<String> getFullLog(String clusterName, String roleName, String serviceName) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiMetricList> getMetrics(String clusterName, String roleName, String serviceName, @Valid String from, @Valid List<String> metrics,
            @Valid String to, @Valid String view) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<String> getStacksLog(String clusterName, String roleName, String serviceName) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<Void> getStacksLogsBundle(String clusterName, String roleName, String serviceName) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<String> getStandardError(String clusterName, String roleName, String serviceName) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<String> getStandardOutput(String clusterName, String roleName, String serviceName) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiCommand> impalaDiagnostics(String clusterName, String roleName, String serviceName, @Valid ApiImpalaRoleDiagnosticsArgs body) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiCommandList> listActiveCommands(String clusterName, String roleName, String serviceName, @Valid String view) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiCommandMetadataList> listCommands(String clusterName, String roleName, String serviceName) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiRole> readRole(String clusterName, String roleName, String serviceName, @Valid String view) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiConfigList> readRoleConfig(String clusterName, String roleName, String serviceName, @Valid String view) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiRoleList> readRoles(String clusterName, String serviceName, @Valid String filter, @Valid String view) {
        ApiRoleList roleList = new ApiRoleList().items(List.of(new ApiRole().name("role1").serviceRef(new ApiServiceRef().serviceName(serviceName))));
        return ProfileAwareResponse.exec(roleList, defaultModelService);
    }

    @Override
    public ResponseEntity<List<ApiEntityTag>> readTags(String clusterName, String roleName, String serviceName, @Valid Integer limit, @Valid Integer offset) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiConfigList> updateRoleConfig(String clusterName, String roleName, String serviceName, @Valid String message,
            @Valid ApiConfigList body) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }
}
