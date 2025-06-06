package com.sequenceiq.mock.clouderamanager.v45.controller;

import jakarta.inject.Inject;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

import com.sequenceiq.mock.clouderamanager.base.ClouderaManagerResourceOperation;
import com.sequenceiq.mock.swagger.model.ApiClusterTemplate;
import com.sequenceiq.mock.swagger.model.ApiCommand;
import com.sequenceiq.mock.swagger.model.ApiCommandList;
import com.sequenceiq.mock.swagger.model.ApiConfigList;
import com.sequenceiq.mock.swagger.model.ApiHostNameList;
import com.sequenceiq.mock.swagger.model.ApiVersionInfo;
import com.sequenceiq.mock.swagger.v45.api.ClouderaManagerResourceApi;

@Controller
public class ClouderaManagerResourceV45Controller implements ClouderaManagerResourceApi {

    @Inject
    private ClouderaManagerResourceOperation clouderaManagerResourceOperation;

    @Override
    public ResponseEntity<Void> beginTrial(String mockUuid) {
        return clouderaManagerResourceOperation.beginTrial(mockUuid);
    }

    @Override
    public ResponseEntity<ApiCommand> deleteCredentialsCommand(String mockUuid, String deleteCredentialsMode) {
        return clouderaManagerResourceOperation.deleteCredentialsCommand(mockUuid, deleteCredentialsMode);
    }

    @Override
    public ResponseEntity<ApiConfigList> getConfig(String mockUuid, String view) {
        return clouderaManagerResourceOperation.getConfig(mockUuid, view);
    }

    @Override
    public ResponseEntity<ApiVersionInfo> getVersion(String mockUuid) {
        return clouderaManagerResourceOperation.getVersion(mockUuid);
    }

    @Override
    public ResponseEntity<ApiCommand> hostsDecommissionCommand(String mockUuid, ApiHostNameList body) {
        return clouderaManagerResourceOperation.hostsDecommissionCommand(mockUuid, body);
    }

    @Override
    public ResponseEntity<ApiCommand> importAdminCredentials(String mockUuid, String password, String username) {
        return clouderaManagerResourceOperation.importAdminCredentials(mockUuid, password, username);
    }

    @Override
    public ResponseEntity<ApiCommand> importClusterTemplate(String mockUuid, Boolean addRepositories, ApiClusterTemplate body) {
        return clouderaManagerResourceOperation.importClusterTemplate(mockUuid, addRepositories, body);
    }

    @Override
    public ResponseEntity<ApiCommandList> listActiveCommands(String mockUuid, String view) {
        return clouderaManagerResourceOperation.listActiveCommands(mockUuid, view);
    }

    @Override
    public ResponseEntity<ApiCommand> refreshParcelRepos(String mockUuid) {
        return clouderaManagerResourceOperation.refreshParcelRepos(mockUuid);
    }

    @Override
    public ResponseEntity<ApiConfigList> updateConfig(String mockUuid, String message, ApiConfigList body) {
        return clouderaManagerResourceOperation.updateConfig(mockUuid, message, body);
    }
}
