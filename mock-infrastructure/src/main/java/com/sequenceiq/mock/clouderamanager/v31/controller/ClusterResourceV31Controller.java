package com.sequenceiq.mock.clouderamanager.v31.controller;

import javax.inject.Inject;
import javax.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

import com.sequenceiq.mock.clouderamanager.base.ClusterResourceOperation;
import com.sequenceiq.mock.swagger.model.ApiCdhUpgradeArgs;
import com.sequenceiq.mock.swagger.model.ApiCluster;
import com.sequenceiq.mock.swagger.model.ApiCommand;
import com.sequenceiq.mock.swagger.model.ApiCommandList;
import com.sequenceiq.mock.swagger.model.ApiHostRef;
import com.sequenceiq.mock.swagger.model.ApiHostRefList;
import com.sequenceiq.mock.swagger.model.ApiRestartClusterArgs;
import com.sequenceiq.mock.swagger.v31.api.ClustersResourceApi;

@Controller
public class ClusterResourceV31Controller implements ClustersResourceApi {

    @Inject
    private ClusterResourceOperation clusterResourceOperation;

    @Override
    public ResponseEntity<ApiCommand> deployClientConfig(String mockUuid, String clusterName) {
        return clusterResourceOperation.deployClientConfig(mockUuid, clusterName);
    }

    @Override
    public ResponseEntity<ApiCommandList> listActiveCommands(String mockUuid, String clusterName, @Valid String view) {
        return clusterResourceOperation.listActiveCommands(mockUuid, clusterName, view);
    }

    @Override
    public ResponseEntity<ApiHostRefList> listHosts(String mockUuid, String clusterName, @Valid String configName, @Valid String configValue) {
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
    public ResponseEntity<ApiCommand> restartCommand(String mockUuid, String clusterName, @Valid ApiRestartClusterArgs body) {
        return clusterResourceOperation.restartCommand(mockUuid, clusterName, body);
    }

    @Override
    public ResponseEntity<ApiCommand> startCommand(String mockUuid, String clusterName) {
        return clusterResourceOperation.startCommand(mockUuid, clusterName);
    }

    @Override
    public ResponseEntity<ApiCommand> stopCommand(String mockUuid, String clusterName) {
        return clusterResourceOperation.stopCommand(mockUuid, clusterName);
    }

    @Override
    public ResponseEntity<ApiCommand> upgradeCdhCommand(String mockUuid, String clusterName, ApiCdhUpgradeArgs body) {
        return clusterResourceOperation.upgradeCdhCommand(mockUuid, clusterName, body);
    }
}
