package com.sequenceiq.mock.clouderamanager.v31.controller;

import jakarta.inject.Inject;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

import com.sequenceiq.mock.clouderamanager.base.ServicesResourcesOperation;
import com.sequenceiq.mock.swagger.model.ApiServiceConfig;
import com.sequenceiq.mock.swagger.model.ApiServiceList;
import com.sequenceiq.mock.swagger.v31.api.ServicesResourceApi;

@Controller
public class ServicesResourcesV31Controller implements ServicesResourceApi {

    @Inject
    private ServicesResourcesOperation servicesResourcesOperation;

    @Override
    public ResponseEntity<ApiServiceList> readServices(String mockUuid, String clusterName, String view) {
        return servicesResourcesOperation.readServices(mockUuid, clusterName, view);
    }

    @Override
    public ResponseEntity<ApiServiceConfig> readServiceConfig(String mockUuid, String clusterName, String serviceName, String view) {
        return servicesResourcesOperation.readServiceConfig(mockUuid, clusterName, serviceName, view);
    }

    @Override
    public ResponseEntity<ApiServiceConfig> updateServiceConfig(String mockUuid, String clusterName, String serviceName, String message, ApiServiceConfig body) {
        return servicesResourcesOperation.updateServiceConfig(mockUuid, clusterName, serviceName, message, body);
    }
}
