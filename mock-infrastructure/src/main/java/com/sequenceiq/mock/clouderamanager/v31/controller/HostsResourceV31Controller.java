package com.sequenceiq.mock.clouderamanager.v31.controller;

import jakarta.inject.Inject;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

import com.sequenceiq.mock.clouderamanager.base.HostsResourceOperation;
import com.sequenceiq.mock.swagger.model.ApiCommand;
import com.sequenceiq.mock.swagger.model.ApiHost;
import com.sequenceiq.mock.swagger.model.ApiHostList;
import com.sequenceiq.mock.swagger.v31.api.HostsResourceApi;

@Controller
public class HostsResourceV31Controller implements HostsResourceApi {

    @Inject
    private HostsResourceOperation hostsResourceOperation;

    @Override
    public ResponseEntity<ApiHost> deleteHost(String mockUuid, String hostId) {
        return hostsResourceOperation.deleteHost(mockUuid, hostId);
    }

    @Override
    public ResponseEntity<ApiHost> readHost(String mockUuid, String hostId, String view) {
        return hostsResourceOperation.readHost(mockUuid, hostId, view);
    }

    @Override
    public ResponseEntity<ApiHostList> readHosts(String mockUuid, String configName, String configValue, String view) {
        return hostsResourceOperation.readHosts(mockUuid, configName, configValue, view);
    }

    @Override
    public ResponseEntity<ApiCommand> enterMaintenanceMode(String mockUuid, String hostId) {
        return hostsResourceOperation.enterMaintenanceMode(mockUuid, hostId);
    }
}
