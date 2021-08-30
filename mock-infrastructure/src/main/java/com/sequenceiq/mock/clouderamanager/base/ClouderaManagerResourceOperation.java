package com.sequenceiq.mock.clouderamanager.base;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

import com.sequenceiq.mock.clouderamanager.ClouderaManagerStoreService;
import com.sequenceiq.mock.clouderamanager.CommandId;
import com.sequenceiq.mock.clouderamanager.DataProviderService;
import com.sequenceiq.mock.clouderamanager.ResponseCreatorComponent;
import com.sequenceiq.mock.swagger.model.ApiClusterTemplate;
import com.sequenceiq.mock.swagger.model.ApiCommand;
import com.sequenceiq.mock.swagger.model.ApiCommandList;
import com.sequenceiq.mock.swagger.model.ApiConfigList;
import com.sequenceiq.mock.swagger.model.ApiHostNameList;
import com.sequenceiq.mock.swagger.model.ApiVersionInfo;

@Controller
public class ClouderaManagerResourceOperation {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerResourceOperation.class);

    @Inject
    private DataProviderService dataProviderService;

    @Inject
    private ResponseCreatorComponent responseCreatorComponent;

    @Inject
    private ClouderaManagerStoreService clouderaManagerStoreService;

    public ResponseEntity<Void> beginTrial(String mockUuid) {
        return responseCreatorComponent.exec();
    }

    public ResponseEntity<ApiCommand> deleteCredentialsCommand(String mockUuid, @Valid String deleteCredentialsMode) {
        return responseCreatorComponent.exec(dataProviderService.getSuccessfulApiCommand(CommandId.DELETE_CRED));
    }

    public ResponseEntity<ApiConfigList> getConfig(String mockUuid, @Valid String view) {
        return responseCreatorComponent.exec(new ApiConfigList().items(new ArrayList<>()));
    }

    public ResponseEntity<ApiVersionInfo> getVersion(String mockUuid) {
        String cmVersion = clouderaManagerStoreService.read(mockUuid).getClusterTemplate().getCmVersion();
        return responseCreatorComponent.exec(new ApiVersionInfo().version(cmVersion));
    }

    public ResponseEntity<ApiCommand> hostsDecommissionCommand(String mockUuid, @Valid ApiHostNameList body) {
        return responseCreatorComponent.exec(dataProviderService.getSuccessfulApiCommand(CommandId.HOST_DECOMMISSION));
    }

    public ResponseEntity<ApiCommand> importAdminCredentials(String mockUuid, @Valid String password, @Valid String username) {
        return responseCreatorComponent.exec(new ApiCommand().id(Integer.valueOf(1)));
    }

    public ResponseEntity<ApiCommand> importClusterTemplate(String mockUuid, @Valid Boolean addRepositories, @Valid ApiClusterTemplate body) {
        clouderaManagerStoreService.importClusterTemplate(mockUuid, body);
        ApiCommand response = new ApiCommand().id(Integer.valueOf(1)).name("Import ClusterTemplate").active(Boolean.TRUE);
        return ResponseEntity.ok(response);
    }

    public ResponseEntity<ApiCommandList> listActiveCommands(String mockUuid, @Valid String view) {
        ApiCommandList items = new ApiCommandList().items(List.of());
        return responseCreatorComponent.exec(items);
    }

    public ResponseEntity<ApiCommand> refreshParcelRepos(String mockUuid) {
        return responseCreatorComponent.exec(dataProviderService.getSuccessfulApiCommand(CommandId.REFRESH_PARCEL));
    }

    public ResponseEntity<ApiConfigList> updateConfig(String mockUuid, @Valid String message, @Valid ApiConfigList body) {
        return responseCreatorComponent.exec(new ApiConfigList().items(new ArrayList<>()));
    }
}
