package com.sequenceiq.mock.clouderamanager.v40.controller;

import java.math.BigDecimal;
import java.util.List;

import javax.inject.Inject;
import javax.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

import com.sequenceiq.mock.clouderamanager.DataProviderService;
import com.sequenceiq.mock.clouderamanager.ProfileAwareComponent;
import com.sequenceiq.mock.swagger.model.ApiCluster;
import com.sequenceiq.mock.swagger.model.ApiCommand;
import com.sequenceiq.mock.swagger.model.ApiCommandList;
import com.sequenceiq.mock.swagger.model.ApiHostRef;
import com.sequenceiq.mock.swagger.model.ApiHostRefList;
import com.sequenceiq.mock.swagger.model.ApiRestartClusterArgs;
import com.sequenceiq.mock.swagger.v40.api.ClustersResourceApi;

@Controller
public class ClusterResourceV40Controller implements ClustersResourceApi {

    @Inject
    private DataProviderService dataProviderService;

    @Inject
    private ProfileAwareComponent profileAwareComponent;

    @Override
    public ResponseEntity<ApiCommand> deployClientConfig(String mockUuid, String clusterName) {
        return profileAwareComponent.exec(dataProviderService.getSuccessfulApiCommand());
    }

    @Override
    public ResponseEntity<ApiCommandList> listActiveCommands(String mockUuid, String clusterName, @Valid String view) {
        ApiCommandList something = new ApiCommandList().items(List.of(new ApiCommand().name("something")));
        return profileAwareComponent.exec(something);
    }

    @Override
    public ResponseEntity<ApiHostRefList> listHosts(String mockUuid, String clusterName, @Valid String configName, @Valid String configValue) {
        return profileAwareComponent.exec(dataProviderService.getHostRefList(mockUuid));
    }

    @Override
    public ResponseEntity<ApiCluster> readCluster(String mockUuid, String clusterName) {
        return profileAwareComponent.exec(new ApiCluster());
    }

    @Override
    public ResponseEntity<ApiCommand> refresh(String mockUuid, String clusterName) {
        return profileAwareComponent.exec(dataProviderService.getSuccessfulApiCommand());
    }

    @Override
    public ResponseEntity<ApiHostRef> removeHost(String mockUuid, String clusterName, String hostId) {
        return profileAwareComponent.exec(dataProviderService.getApiHostRef(mockUuid, hostId));
    }

    @Override
    public ResponseEntity<ApiCommand> restartCommand(String mockUuid, String clusterName, @Valid ApiRestartClusterArgs body) {
        return profileAwareComponent.exec(dataProviderService.getSuccessfulApiCommand());
    }

    @Override
    public ResponseEntity<ApiCommand> startCommand(String mockUuid, String clusterName) {
        return profileAwareComponent.exec(new ApiCommand().id(BigDecimal.ONE).active(Boolean.TRUE).name("Start"));
    }

    @Override
    public ResponseEntity<ApiCommand> stopCommand(String mockUuid, String clusterName) {
        return profileAwareComponent.exec(new ApiCommand().id(BigDecimal.ONE).active(Boolean.TRUE).name("Stop"));
    }
}
