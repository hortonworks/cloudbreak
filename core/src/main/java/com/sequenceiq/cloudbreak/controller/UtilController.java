package com.sequenceiq.cloudbreak.controller;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Controller;

import com.sequenceiq.cloudbreak.api.endpoint.v1.UtilEndpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.requests.DatabaseV4BuildRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.responses.DatabaseV4BuildResponse;
import com.sequenceiq.cloudbreak.api.model.AmbariDatabaseDetailsJson;
import com.sequenceiq.cloudbreak.api.model.AmbariDatabaseTestResult;
import com.sequenceiq.cloudbreak.api.model.ExposedServiceResponse;
import com.sequenceiq.cloudbreak.api.model.ParametersQueryRequest;
import com.sequenceiq.cloudbreak.api.model.ParametersQueryResponse;
import com.sequenceiq.cloudbreak.api.model.VersionCheckResult;
import com.sequenceiq.cloudbreak.api.model.filesystem.CloudStorageSupportedResponse;
import com.sequenceiq.cloudbreak.api.model.stack.StackMatrix;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.controller.validation.rds.RdsConnectionBuilder;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.service.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.ServiceEndpointCollector;
import com.sequenceiq.cloudbreak.service.StackMatrixService;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.service.filesystem.FileSystemSupportMatrixService;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;
import com.sequenceiq.cloudbreak.util.ClientVersionUtil;

@Controller
public class UtilController implements UtilEndpoint {

    @Inject
    private RdsConnectionBuilder rdsConnectionBuilder;

    @Inject
    private StackMatrixService stackMatrixService;

    @Inject
    private BlueprintService blueprintService;

    @Inject
    private ServiceEndpointCollector serviceEndpointCollector;

    @Autowired
    private FileSystemSupportMatrixService fileSystemSupportMatrixService;

    @Autowired
    @Qualifier("conversionService")
    private ConversionService conversionService;

    @Inject
    private WorkspaceService workspaceService;

    @Inject
    private UserService userService;

    @Inject
    private CloudbreakRestRequestThreadLocalService restRequestThreadLocalService;

    @Value("${info.app.version:}")
    private String cbVersion;

    @Override
    public VersionCheckResult checkClientVersion(String version) {
        boolean compatible = ClientVersionUtil.checkVersion(cbVersion, version);
        if (compatible) {
            return new VersionCheckResult(true);

        }
        return new VersionCheckResult(false, String.format("Versions not compatible: [server: '%s', client: '%s']", cbVersion, version));
    }

    @Override
    public AmbariDatabaseTestResult testAmbariDatabase(@Valid AmbariDatabaseDetailsJson ambariDatabaseDetailsJson) {
        return new AmbariDatabaseTestResult();
    }

    @Override
    public DatabaseV4BuildResponse buildRdsConnection(@Valid DatabaseV4BuildRequest databaseV4BuildRequest, Set<String> targets) {
        DatabaseV4BuildResponse databaseV4BuildResponse = new DatabaseV4BuildResponse();
        try {
            String clusterName = databaseV4BuildRequest.getClusterName().replaceAll("[^a-zA-Z0-9]", "");
            Map<String, String> result = rdsConnectionBuilder.buildRdsConnection(
                    databaseV4BuildRequest.getRdsConfigRequest().getConnectionURL(),
                    databaseV4BuildRequest.getRdsConfigRequest().getConnectionUserName(),
                    databaseV4BuildRequest.getRdsConfigRequest().getConnectionPassword(),
                    clusterName,
                    targets);

            databaseV4BuildResponse.setResults(result);
        } catch (BadRequestException e) {
            throw new BadRequestException("Could not create databases in metastore - " + e.getMessage(), e);
        }
        return databaseV4BuildResponse;
    }

    @Override
    public StackMatrix getStackMatrix() {
        return stackMatrixService.getStackMatrix();
    }

    @Override
    public Collection<ExposedServiceResponse> getKnoxServices(String blueprintName) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        Workspace workspace = workspaceService.get(restRequestThreadLocalService.getRequestedWorkspaceId(), user);
        return serviceEndpointCollector.getKnoxServices(blueprintName, workspace);
    }

    @Override
    public Collection<CloudStorageSupportedResponse> getCloudStorageMatrix(String stackVersion) {
        return fileSystemSupportMatrixService.getCloudStorageMatrix(stackVersion);
    }

    @Override
    public ParametersQueryResponse getCustomParameters(ParametersQueryRequest parametersQueryRequest) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        Workspace workspace = workspaceService.get(restRequestThreadLocalService.getRequestedWorkspaceId(), user);
        Set<String> strings = blueprintService.queryCustomParameters(parametersQueryRequest.getBlueprintName(), workspace);
        Map<String, String> result = new HashMap<>();
        for (String customParameter : strings) {
            result.put(customParameter, "");
        }
        ParametersQueryResponse parametersQueryResponse = new ParametersQueryResponse();
        parametersQueryResponse.setCustom(result);
        return parametersQueryResponse;
    }
}
