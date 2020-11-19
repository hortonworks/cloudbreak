package com.sequenceiq.mock.clouderamanager.v31.controller;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

import com.sequenceiq.mock.clouderamanager.ClouderaManagerStoreService;
import com.sequenceiq.mock.clouderamanager.DataProviderService;
import com.sequenceiq.mock.clouderamanager.ProfileAwareComponent;
import com.sequenceiq.mock.swagger.model.ApiClusterTemplate;
import com.sequenceiq.mock.swagger.model.ApiCommand;
import com.sequenceiq.mock.swagger.model.ApiCommandList;
import com.sequenceiq.mock.swagger.model.ApiConfigList;
import com.sequenceiq.mock.swagger.model.ApiHostNameList;
import com.sequenceiq.mock.swagger.model.ApiVersionInfo;
import com.sequenceiq.mock.swagger.v31.api.ClouderaManagerResourceApi;

@Controller
public class ClouderaManagerResourceV31Controller implements ClouderaManagerResourceApi {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerResourceV31Controller.class);

    @Inject
    private DataProviderService dataProviderService;

    @Inject
    private ProfileAwareComponent profileAwareComponent;

    @Inject
    private ClouderaManagerStoreService clouderaManagerStoreService;

    @Override
    public ResponseEntity<Void> beginTrial(String mockUuid) {
        return profileAwareComponent.exec();
    }

    @Override
    public ResponseEntity<ApiCommand> deleteCredentialsCommand(String mockUuid, @Valid String deleteCredentialsMode) {
        return profileAwareComponent.exec(dataProviderService.getSuccessfulApiCommand());
    }

    @Override
    public ResponseEntity<ApiConfigList> getConfig(String mockUuid, @Valid String view) {
        return profileAwareComponent.exec(new ApiConfigList().items(new ArrayList<>()));
    }

    @Override
    public ResponseEntity<ApiVersionInfo> getVersion(String mockUuid) {
        return profileAwareComponent.exec(new ApiVersionInfo().version("7.0.1"));
    }

    @Override
    public ResponseEntity<ApiCommand> hostsDecommissionCommand(String mockUuid, @Valid ApiHostNameList body) {
        return profileAwareComponent.exec(dataProviderService.getSuccessfulApiCommand());
    }

    @Override
    public ResponseEntity<ApiCommand> importAdminCredentials(String mockUuid, @Valid String password, @Valid String username) {
        return profileAwareComponent.exec(new ApiCommand().id(new BigDecimal(1)));
    }

    @Override
    public ResponseEntity<ApiCommand> importClusterTemplate(String mockUuid, @Valid Boolean addRepositories, @Valid ApiClusterTemplate body) {
        clouderaManagerStoreService.setClouderaManagerProducts(mockUuid, body.getProducts());
        ApiCommand response = new ApiCommand().id(BigDecimal.ONE).name("Import ClusterTemplate").active(Boolean.TRUE);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<ApiCommandList> listActiveCommands(String mockUuid, @Valid String view) {
        ApiCommandList items = new ApiCommandList().items(
                List.of(new ApiCommand().id(new BigDecimal(1)).active(Boolean.FALSE).success(Boolean.TRUE)));
        return profileAwareComponent.exec(items);
    }

    @Override
    public ResponseEntity<ApiCommand> refreshParcelRepos(String mockUuid) {
        return profileAwareComponent.exec(dataProviderService.getSuccessfulApiCommand());
    }

    @Override
    public ResponseEntity<ApiConfigList> updateConfig(String mockUuid, @Valid String message, @Valid ApiConfigList body) {
        return profileAwareComponent.exec(new ApiConfigList().items(new ArrayList<>()));
    }
}
