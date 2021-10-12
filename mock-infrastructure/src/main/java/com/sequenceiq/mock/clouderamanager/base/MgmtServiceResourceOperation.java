package com.sequenceiq.mock.clouderamanager.base;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

import com.sequenceiq.mock.clouderamanager.ClouderaManagerStoreService;
import com.sequenceiq.mock.clouderamanager.CommandId;
import com.sequenceiq.mock.clouderamanager.DataProviderService;
import com.sequenceiq.mock.clouderamanager.ResponseCreatorComponent;
import com.sequenceiq.mock.swagger.model.ApiCommand;
import com.sequenceiq.mock.swagger.model.ApiCommandList;
import com.sequenceiq.mock.swagger.model.ApiRoleTypeList;
import com.sequenceiq.mock.swagger.model.ApiService;
import com.sequenceiq.mock.swagger.model.ApiServiceState;

@Controller
public class MgmtServiceResourceOperation {

    @Inject
    private ResponseCreatorComponent responseCreatorComponent;

    @Inject
    private DataProviderService dataProviderService;

    @Inject
    private ClouderaManagerStoreService clouderaManagerStoreService;

    @Inject
    private ClusterResourceOperation clusterResourceOperation;

    public ResponseEntity<Void> autoConfigure(String mockUuid) {
        return responseCreatorComponent.exec();
    }

    public ResponseEntity<ApiCommandList> listActiveCommands(String mockUuid, @Valid String view) {
        ApiCommandList response = new ApiCommandList().items(List.of());
        return responseCreatorComponent.exec(response);
    }

    public ResponseEntity<ApiRoleTypeList> listRoleTypes(String mockUuid) {
        return responseCreatorComponent.exec(new ApiRoleTypeList().items(new ArrayList<>()));
    }

    public ResponseEntity<ApiService> readService(String mockUuid, @Valid String view) {
        ApiService apiService = new ApiService().serviceState(clouderaManagerStoreService.read(mockUuid).getStatus());
        return responseCreatorComponent.exec(apiService);
    }

    public ResponseEntity<ApiCommand> restartCommand(String mockUuid) {
        startCommand(mockUuid);
        return responseCreatorComponent.exec(dataProviderService.getSuccessfulApiCommand(CommandId.MGMT_RESTART));
    }

    public ResponseEntity<ApiService> setupCMS(String mockUuid, @Valid ApiService body) {
        return responseCreatorComponent.exec(new ApiService());
    }

    public ResponseEntity<ApiCommand> startCommand(String mockUuid) {
        clouderaManagerStoreService.read(mockUuid).setStatus(ApiServiceState.STARTED);
        clusterResourceOperation.startCommand(mockUuid, "");
        return responseCreatorComponent.exec(dataProviderService.getSuccessfulApiCommand(CommandId.MGMT_START));
    }

    public ResponseEntity<ApiCommand> stopCommand(String mockUuid) {
        clouderaManagerStoreService.read(mockUuid).setStatus(ApiServiceState.STOPPED);
        return responseCreatorComponent.exec(dataProviderService.getSuccessfulApiCommand(CommandId.MGMT_STOP));
    }
}
