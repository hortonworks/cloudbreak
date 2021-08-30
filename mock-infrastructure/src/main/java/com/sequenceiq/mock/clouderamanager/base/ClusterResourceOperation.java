package com.sequenceiq.mock.clouderamanager.base;

import static com.sequenceiq.mock.clouderamanager.CommandId.UPGRADE_CDH_COMMAND;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

import com.sequenceiq.mock.clouderamanager.ClouderaManagerDto;
import com.sequenceiq.mock.clouderamanager.ClouderaManagerStoreService;
import com.sequenceiq.mock.clouderamanager.CommandId;
import com.sequenceiq.mock.clouderamanager.DataProviderService;
import com.sequenceiq.mock.clouderamanager.ResponseCreatorComponent;
import com.sequenceiq.mock.swagger.model.ApiCdhUpgradeArgs;
import com.sequenceiq.mock.swagger.model.ApiCluster;
import com.sequenceiq.mock.swagger.model.ApiCommand;
import com.sequenceiq.mock.swagger.model.ApiCommandList;
import com.sequenceiq.mock.swagger.model.ApiHostList;
import com.sequenceiq.mock.swagger.model.ApiHostRef;
import com.sequenceiq.mock.swagger.model.ApiHostRefList;
import com.sequenceiq.mock.swagger.model.ApiRestartClusterArgs;
import com.sequenceiq.mock.swagger.model.ApiServiceState;

@Controller
public class ClusterResourceOperation {

    @Inject
    private DataProviderService dataProviderService;

    @Inject
    private ResponseCreatorComponent responseCreatorComponent;

    @Inject
    private ClouderaManagerStoreService clouderaManagerStoreService;

    public ResponseEntity<ApiCommand> deployClientConfig(String mockUuid, String clusterName) {
        return responseCreatorComponent.exec(dataProviderService.getSuccessfulApiCommand(CommandId.DEPLOY_CLIENT_CONFIG));
    }

    public ResponseEntity<ApiCommandList> listActiveCommands(String mockUuid, String clusterName, @Valid String view) {
        ApiCommandList something = new ApiCommandList().items(List.of());
        return responseCreatorComponent.exec(something);
    }

    public ResponseEntity<ApiHostRefList> listRefHosts(String mockUuid, String clusterName, @Valid String configName, @Valid String configValue) {
        return responseCreatorComponent.exec(dataProviderService.getHostRefList(mockUuid));
    }

    public ResponseEntity<ApiHostList> listHosts(String mockUuid, String clusterName, @Valid String configName, @Valid String configValue) {
        return responseCreatorComponent.exec(dataProviderService.getHostList(mockUuid));
    }

    public ResponseEntity<ApiCluster> readCluster(String mockUuid, String clusterName) {
        ApiCluster apiCluster = dataProviderService.readCluster(mockUuid, clusterName);
        return responseCreatorComponent.exec(apiCluster);
    }

    public ResponseEntity<ApiCommand> refresh(String mockUuid, String clusterName) {
        return responseCreatorComponent.exec(dataProviderService.getSuccessfulApiCommand(CommandId.CLUSTER_REFRESH));
    }

    public ResponseEntity<ApiHostRef> removeHost(String mockUuid, String clusterName, String hostId) {
        ApiHostRef apiHostRef = dataProviderService.getApiHostRef(mockUuid, hostId);
        return responseCreatorComponent.exec(apiHostRef);
    }

    public ResponseEntity<ApiCommand> restartCommand(String mockUuid, String clusterName, @Valid ApiRestartClusterArgs body) {
        startCommand(mockUuid, clusterName);
        return responseCreatorComponent.exec(dataProviderService.getSuccessfulApiCommand(CommandId.CLUSTER_RESTART));
    }

    public ResponseEntity<ApiCommand> startCommand(String mockUuid, String clusterName) {
        ClouderaManagerDto read = clouderaManagerStoreService.read(mockUuid);
        Map<String, ApiServiceState> newStates = new HashMap<>();
        read.getServiceStates().forEach((service, apiServiceState) -> newStates.put(service, ApiServiceState.STARTED));
        read.setServiceStates(newStates);
        read.setStatus(ApiServiceState.STARTED);
        return responseCreatorComponent.exec(new ApiCommand().id(CommandId.CLUSTER_START).active(Boolean.TRUE).name("Start"));
    }

    public ResponseEntity<ApiCommand> stopCommand(String mockUuid, String clusterName) {
        ClouderaManagerDto read = clouderaManagerStoreService.read(mockUuid);
        Map<String, ApiServiceState> newStates = new HashMap<>();
        read.getServiceStates().forEach((service, apiServiceState) -> newStates.put(service, ApiServiceState.STOPPED));
        read.setServiceStates(newStates);
        read.setStatus(ApiServiceState.STOPPED);
        return responseCreatorComponent.exec(new ApiCommand().id(CommandId.CLUSTER_STOP).active(Boolean.TRUE).name("Stop"));
    }

    public ResponseEntity<ApiCommand> upgradeCdhCommand(String mockUuid, String clusterName, ApiCdhUpgradeArgs body) {
        clouderaManagerStoreService.addOrUpdateProduct(mockUuid, "CDH", body.getCdhParcelVersion());
        return responseCreatorComponent.exec(dataProviderService.getSuccessfulApiCommand(UPGRADE_CDH_COMMAND));
    }
}
