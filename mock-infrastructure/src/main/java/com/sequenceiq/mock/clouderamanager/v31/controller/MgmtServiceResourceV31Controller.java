package com.sequenceiq.mock.clouderamanager.v31.controller;

import java.math.BigDecimal;
import java.util.List;

import javax.inject.Inject;
import javax.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

import com.sequenceiq.mock.clouderamanager.DataProviderService;
import com.sequenceiq.mock.clouderamanager.ProfileAwareComponent;
import com.sequenceiq.mock.swagger.model.ApiCommand;
import com.sequenceiq.mock.swagger.model.ApiCommandList;
import com.sequenceiq.mock.swagger.model.ApiService;
import com.sequenceiq.mock.swagger.model.ApiServiceState;
import com.sequenceiq.mock.swagger.v31.api.MgmtServiceResourceApi;

@Controller
public class MgmtServiceResourceV31Controller implements MgmtServiceResourceApi {

    @Inject
    private ProfileAwareComponent profileAwareComponent;

    @Inject
    private DataProviderService dataProviderService;

    @Override
    public ResponseEntity<Void> autoConfigure(String mockUuid) {
        return profileAwareComponent.exec();
    }

    @Override
    public ResponseEntity<ApiCommandList> listActiveCommands(String mockUuid, @Valid String view) {
        ApiCommandList response = new ApiCommandList().items(List.of(new ApiCommand().id(new BigDecimal(1)).active(Boolean.FALSE).success(Boolean.TRUE)));
        return profileAwareComponent.exec(response);
    }

    @Override
    public ResponseEntity<ApiService> readService(String mockUuid, @Valid String view) {
        ApiService apiService = new ApiService().serviceState(ApiServiceState.STARTED);
        return profileAwareComponent.exec(apiService);
    }

    @Override
    public ResponseEntity<ApiCommand> restartCommand(String mockUuid) {
        return profileAwareComponent.exec(dataProviderService.getSuccessfulApiCommand());
    }

    @Override
    public ResponseEntity<ApiService> setupCMS(String mockUuid, @Valid ApiService body) {
        return profileAwareComponent.exec(new ApiService());
    }

    @Override
    public ResponseEntity<ApiCommand> startCommand(String mockUuid) {
        return profileAwareComponent.exec(new ApiCommand());
    }

    @Override
    public ResponseEntity<ApiCommand> stopCommand(String mockUuid) {
        return profileAwareComponent.exec(new ApiCommand());
    }
}
