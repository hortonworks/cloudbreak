package com.sequenceiq.mock.clouderamanager.base;

import javax.inject.Inject;
import javax.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

import com.sequenceiq.mock.clouderamanager.ClouderaManagerStoreService;
import com.sequenceiq.mock.clouderamanager.CommandId;
import com.sequenceiq.mock.clouderamanager.DataProviderService;
import com.sequenceiq.mock.clouderamanager.ResponseCreatorComponent;
import com.sequenceiq.mock.spi.SpiService;
import com.sequenceiq.mock.swagger.model.ApiCommand;
import com.sequenceiq.mock.swagger.model.ApiHost;
import com.sequenceiq.mock.swagger.model.ApiHostList;
import com.sequenceiq.mock.swagger.model.ApiHostsToRemoveArgs;

@Controller
public class HostsResourceOperation {

    @Inject
    private ClouderaManagerStoreService clouderaManagerStoreService;

    @Inject
    private DataProviderService dataProviderService;

    @Inject
    private ResponseCreatorComponent responseCreatorComponent;

    @Inject
    private SpiService spiService;

    public ResponseEntity<ApiHost> deleteHost(String mockUuid, String hostId) {
        ApiHost apiHost = dataProviderService.getApiHost(mockUuid, hostId);
        spiService.terminateInstance(mockUuid, hostId);
        return responseCreatorComponent.exec(apiHost);
    }

    public ResponseEntity<ApiHost> readHost(String mockUuid, String hostId, @Valid String view) {
        ApiHost apiHost = dataProviderService.getApiHost(mockUuid, hostId);
        return responseCreatorComponent.exec(apiHost);
    }

    public ResponseEntity<ApiHostList> readHosts(String mockUuid, @Valid String configName, @Valid String configValue, @Valid String view) {
        ApiHostList apiHostList = dataProviderService.readHosts(mockUuid);
        return responseCreatorComponent.exec(apiHostList);
    }

    public ResponseEntity<ApiCommand> removeHostsFromCluster(String mockUuid, ApiHostsToRemoveArgs body) {
        return responseCreatorComponent.exec(dataProviderService.getSuccessfulApiCommand(CommandId.REMOVE_HOSTS));
    }
}
