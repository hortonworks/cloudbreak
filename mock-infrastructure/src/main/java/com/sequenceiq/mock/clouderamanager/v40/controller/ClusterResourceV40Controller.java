package com.sequenceiq.mock.clouderamanager.v40.controller;

import jakarta.inject.Inject;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

import com.sequenceiq.mock.clouderamanager.base.ClusterResourceOperation;
import com.sequenceiq.mock.swagger.model.ApiCluster;
import com.sequenceiq.mock.swagger.model.ApiCommand;
import com.sequenceiq.mock.swagger.model.ApiCommandList;
import com.sequenceiq.mock.swagger.model.ApiHostRef;
import com.sequenceiq.mock.swagger.model.ApiHostRefList;
import com.sequenceiq.mock.swagger.model.ApiRestartClusterArgs;
import com.sequenceiq.mock.swagger.model.ApiRollingRestartClusterArgs;
import com.sequenceiq.mock.swagger.v40.api.ClustersResourceApi;

@Controller
public class ClusterResourceV40Controller implements ClustersResourceApi {

    @Inject
    private ClusterResourceOperation clusterResourceOperation;

    @Override
    public ResponseEntity<ApiCommand> deployClientConfig(String mockUuid, String clusterName) {
        return clusterResourceOperation.deployClientConfig(mockUuid, clusterName);
    }

    @Override
    public ResponseEntity<ApiCommandList> listActiveCommands(String mockUuid, String clusterName, String view) {
        return clusterResourceOperation.listActiveCommands(mockUuid, clusterName, view);
    }

    @Override
    public ResponseEntity<ApiHostRefList> listHosts(String mockUuid, String clusterName, String configName, String configValue) {
        return clusterResourceOperation.listRefHosts(mockUuid, clusterName, configName, configValue);
    }

    @Override
    public ResponseEntity<ApiCluster> readCluster(String mockUuid, String clusterName) {
        return clusterResourceOperation.readCluster(mockUuid, clusterName);
    }

    @Override
    public ResponseEntity<ApiCommand> refresh(String mockUuid, String clusterName) {
        return clusterResourceOperation.refresh(mockUuid, clusterName);
    }

    @Override
    public ResponseEntity<ApiHostRef> removeHost(String mockUuid, String clusterName, String hostId) {
        return clusterResourceOperation.removeHost(mockUuid, clusterName, hostId);
    }

    @Override
    public ResponseEntity<ApiCommand> restartCommand(String mockUuid, String clusterName, ApiRestartClusterArgs body) {
        return clusterResourceOperation.restartCommand(mockUuid, clusterName);
    }

    @Override
    public ResponseEntity<ApiCommand> rollingRestart(String mockUuid, String clusterName, ApiRollingRestartClusterArgs body) {
        return clusterResourceOperation.restartCommand(mockUuid, clusterName);
    }

    @Override
    public ResponseEntity<ApiCommand> startCommand(String mockUuid, String clusterName) {
        return clusterResourceOperation.startCommand(mockUuid, clusterName);
    }

    @Override
    public ResponseEntity<ApiCommand> stopCommand(String mockUuid, String clusterName) {
        return clusterResourceOperation.stopCommand(mockUuid, clusterName);
    }
}
