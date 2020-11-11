package com.sequenceiq.mock.legacy.clouderamanager.v31.controller;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.multipart.MultipartFile;

import com.sequenceiq.mock.legacy.clouderamanager.ProfileAwareResponse;
import com.sequenceiq.mock.legacy.clouderamanager.ResponseUtil;
import com.sequenceiq.mock.legacy.clouderamanager.DataProviderService;
import com.sequenceiq.mock.legacy.clouderamanager.DefaultModelService;
import com.sequenceiq.mock.swagger.v31.api.ClouderaManagerResourceApi;
import com.sequenceiq.mock.swagger.model.ApiAddCustomCertsArguments;
import com.sequenceiq.mock.swagger.model.ApiClusterTemplate;
import com.sequenceiq.mock.swagger.model.ApiClustersPerfInspectorArgs;
import com.sequenceiq.mock.swagger.model.ApiCmServer;
import com.sequenceiq.mock.swagger.model.ApiCmServerList;
import com.sequenceiq.mock.swagger.model.ApiCollectDiagnosticDataArguments;
import com.sequenceiq.mock.swagger.model.ApiCommand;
import com.sequenceiq.mock.swagger.model.ApiCommandList;
import com.sequenceiq.mock.swagger.model.ApiConfigList;
import com.sequenceiq.mock.swagger.model.ApiDeployment2;
import com.sequenceiq.mock.swagger.model.ApiGenerateCmcaArguments;
import com.sequenceiq.mock.swagger.model.ApiHostInstallArguments;
import com.sequenceiq.mock.swagger.model.ApiHostNameList;
import com.sequenceiq.mock.swagger.model.ApiHostsPerfInspectorArgs;
import com.sequenceiq.mock.swagger.model.ApiKerberosInfo;
import com.sequenceiq.mock.swagger.model.ApiLicense;
import com.sequenceiq.mock.swagger.model.ApiLicensedFeatureUsage;
import com.sequenceiq.mock.swagger.model.ApiPrincipalList;
import com.sequenceiq.mock.swagger.model.ApiScmDbInfo;
import com.sequenceiq.mock.swagger.model.ApiShutdownReadiness;
import com.sequenceiq.mock.swagger.model.ApiVersionInfo;

@Controller
public class ClouderaManagerResourceV31Controller implements ClouderaManagerResourceApi {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerResourceV31Controller.class);

    @Inject
    private HttpServletRequest request;

    @Inject
    private DefaultModelService defaultModelService;

    @Inject
    private DataProviderService dataProviderService;

    @Override
    public ResponseEntity<ApiCommand> addCustomCerts(@Valid ApiAddCustomCertsArguments body) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<Void> beginTrial() {
        return ProfileAwareResponse.exec(defaultModelService);
    }

    @Override
    public ResponseEntity<ApiCommand> clustersPerfInspectorCommand(@Valid ApiClustersPerfInspectorArgs body) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiCommand> collectDiagnosticDataCommand(@Valid ApiCollectDiagnosticDataArguments body) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiCommand> deleteCredentialsCommand(@Valid String deleteCredentialsMode) {
        return ProfileAwareResponse.exec(dataProviderService.getSuccessfulApiCommand(), defaultModelService);
    }

    @Override
    public ResponseEntity<Void> endTrial() {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiCommand> generateCmca(@Valid ApiGenerateCmcaArguments body) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiCommand> generateCredentialsCommand() {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiConfigList> getConfig(@Valid String view) {
        return ProfileAwareResponse.exec(new ApiConfigList().items(new ArrayList<>()), defaultModelService);
    }

    @Override
    public ResponseEntity<ApiDeployment2> getDeployment2(@Valid String view) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiKerberosInfo> getKerberosInfo() {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiPrincipalList> getKerberosPrincipals(@Valid Boolean missingOnly) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiLicensedFeatureUsage> getLicensedFeatureUsage() {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<String> getLog() {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiScmDbInfo> getScmDbInfo() {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiShutdownReadiness> getShutdownReadiness(@Valid String lastActivityTime) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiVersionInfo> getVersion() {
        return ProfileAwareResponse.exec(new ApiVersionInfo().version("7.0.1"), defaultModelService);
    }

    @Override
    public ResponseEntity<ApiCommand> hostInstallCommand(@Valid ApiHostInstallArguments body) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiCommand> hostsDecommissionCommand(@Valid ApiHostNameList body) {
        return ProfileAwareResponse.exec(dataProviderService.getSuccessfulApiCommand(), defaultModelService);
    }

    @Override
    public ResponseEntity<ApiCommand> hostsOfflineOrDecommissionCommand(@Valid BigDecimal offlineTimeout, @Valid ApiHostNameList body) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiCommand> hostsPerfInspectorCommand(@Valid ApiHostsPerfInspectorArgs body) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiCommand> hostsRecommissionAndExitMaintenanceModeCommand(@Valid String recommissionType, @Valid ApiHostNameList body) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiCommand> hostsRecommissionCommand(@Valid ApiHostNameList body) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiCommand> hostsRecommissionWithStartCommand(@Valid ApiHostNameList body) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiCommand> hostsStartRolesCommand(@Valid ApiHostNameList body) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiCommand> importAdminCredentials(@Valid String password, @Valid String username) {
        return ProfileAwareResponse.exec(new ApiCommand().id(new BigDecimal(1)), defaultModelService);
    }

    @Override
    public ResponseEntity<ApiCommand> importClusterTemplate(@Valid Boolean addRepositories, @Valid ApiClusterTemplate body) {
        defaultModelService.setClouderaManagerProducts(body.getProducts());
        ApiCommand response = new ApiCommand().id(BigDecimal.ONE).name("Import ClusterTemplate").active(Boolean.TRUE);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<ApiCommand> importKerberosPrincipal(@Valid BigDecimal kvno, @Valid String password, @Valid String principal) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiCommand> inspectHostsCommand() {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiCommandList> listActiveCommands(@Valid String view) {
        ApiCommandList items = new ApiCommandList().items(
                List.of(new ApiCommand().id(new BigDecimal(1)).active(Boolean.FALSE).success(Boolean.TRUE)));
        return ProfileAwareResponse.exec(items, defaultModelService);
    }

    @Override
    public ResponseEntity<ApiCmServer> readInstance(String cmServerId) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiCmServerList> readInstances() {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiLicense> readLicense() {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiCommand> refreshParcelRepos() {
        return ProfileAwareResponse.exec(dataProviderService.getSuccessfulApiCommand(), defaultModelService);
    }

    @Override
    public ResponseEntity<ApiConfigList> updateConfig(@Valid String message, @Valid ApiConfigList body) {
        return ProfileAwareResponse.exec(new ApiConfigList().items(new ArrayList<>()), defaultModelService);
    }

    @Override
    public ResponseEntity<ApiDeployment2> updateDeployment2(@Valid Boolean deleteCurrentDeployment, @Valid ApiDeployment2 body) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiLicense> updateLicense(@Valid MultipartFile license) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }
}
