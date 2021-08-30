package com.sequenceiq.mock.clouderamanager.v45.controller;

import javax.inject.Inject;
import javax.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

import com.sequenceiq.mock.clouderamanager.base.ServicesResourcesOperation;
import com.sequenceiq.mock.swagger.model.ApiServiceConfig;
import com.sequenceiq.mock.swagger.model.ApiServiceList;
import com.sequenceiq.mock.swagger.v45.api.ServicesResourceApi;

@Controller
public class ServicesResourcesV45Controller implements ServicesResourceApi {

    @Inject
    private ServicesResourcesOperation servicesResourcesOperation;

    @Override
    public ResponseEntity<ApiServiceList> readServices(String mockUuid, String clusterName, @Valid String view) {
        return servicesResourcesOperation.readServices(mockUuid, clusterName, view);
    }

    @Override
    public ResponseEntity<ApiServiceConfig> readServiceConfig(String mockUuid, String clusterName, String serviceName, @Valid String view) {
        return servicesResourcesOperation.readServiceConfig(mockUuid, clusterName, serviceName, view);
    }
}
