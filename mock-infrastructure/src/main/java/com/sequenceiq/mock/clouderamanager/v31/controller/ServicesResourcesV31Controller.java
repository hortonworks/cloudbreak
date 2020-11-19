package com.sequenceiq.mock.clouderamanager.v31.controller;

import java.util.List;

import javax.inject.Inject;
import javax.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

import com.sequenceiq.mock.clouderamanager.ProfileAwareComponent;
import com.sequenceiq.mock.swagger.model.ApiConfig;
import com.sequenceiq.mock.swagger.model.ApiService;
import com.sequenceiq.mock.swagger.model.ApiServiceConfig;
import com.sequenceiq.mock.swagger.model.ApiServiceList;
import com.sequenceiq.mock.swagger.model.ApiServiceState;
import com.sequenceiq.mock.swagger.v31.api.ServicesResourceApi;

@Controller
public class ServicesResourcesV31Controller implements ServicesResourceApi {

    @Inject
    private ProfileAwareComponent profileAwareComponent;

    @Override
    public ResponseEntity<ApiServiceList> readServices(String mockUuid, String clusterName, @Valid String view) {
        ApiServiceList response = new ApiServiceList().items(List.of(new ApiService().name("service1").serviceState(ApiServiceState.STARTED)));
        return profileAwareComponent.exec(response);
    }

    @Override
    public ResponseEntity<ApiServiceConfig> readServiceConfig(String mockUuid, String clusterName, String serviceName, @Valid String view) {
        ApiServiceConfig response = new ApiServiceConfig();
        response.items(List.of(new ApiConfig().name(serviceName)));
        return profileAwareComponent.exec(response);
    }
}
