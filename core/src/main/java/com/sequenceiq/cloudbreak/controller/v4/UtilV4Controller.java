package com.sequenceiq.cloudbreak.controller.v4;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.CloudStorageSupportedV4Responses.cloudStorageSupportedV4Responses;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.ExposedServiceV4Responses.exposedServiceV4Responses;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Controller;

import com.sequenceiq.cloudbreak.api.endpoint.v4.util.UtilV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.filter.EnvironmentV4Filter;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.filter.StackVersionV4Filter;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.requests.DatalakePrerequisiteV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.requests.ParametersQueryV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.requests.RepoConfigValidationV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.requests.StructuredParametersQueryV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.CloudStorageSupportedV4Responses;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.DatalakePrerequisiteV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.ExposedServiceV4Responses;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.ParametersQueryV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.RepoConfigValidationV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.StackMatrixV4;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.StructuredParameterQueriesV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.StructuredParameterQueryV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.VersionCheckV4Result;
import com.sequenceiq.cloudbreak.controller.NotificationController;
import com.sequenceiq.cloudbreak.controller.validation.rds.RdsConnectionBuilder;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.service.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.ServiceEndpointCollector;
import com.sequenceiq.cloudbreak.service.StackMatrixService;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.service.cluster.RepositoryConfigValidationService;
import com.sequenceiq.cloudbreak.service.datalake.DatalakePrerequisiteService;
import com.sequenceiq.cloudbreak.service.filesystem.FileSystemSupportMatrixService;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;
import com.sequenceiq.cloudbreak.template.filesystem.query.ConfigQueryEntry;
import com.sequenceiq.cloudbreak.util.ClientVersionUtil;

@Controller
public class UtilV4Controller extends NotificationController implements UtilV4Endpoint {

    @Inject
    private StackMatrixService stackMatrixService;

    @Inject
    private FileSystemSupportMatrixService fileSystemSupportMatrixService;

    @Inject
    private DatalakePrerequisiteService datalakePrerequisiteService;

    @Inject
    private RepositoryConfigValidationService validationService;

    @Inject
    private RdsConnectionBuilder rdsConnectionBuilder;

    @Inject
    private BlueprintService blueprintService;

    @Inject
    private ServiceEndpointCollector serviceEndpointCollector;

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
    public VersionCheckV4Result checkClientVersion(String version) {
        boolean compatible = ClientVersionUtil.checkVersion(cbVersion, version);
        if (compatible) {
            return new VersionCheckV4Result(true);

        }
        return new VersionCheckV4Result(false, String.format("Versions not compatible: [server: '%s', client: '%s']", cbVersion, version));
    }

    @Override
    public StackMatrixV4 getStackMatrix() {
        return stackMatrixService.getStackMatrix();
    }

    @Override
    public CloudStorageSupportedV4Responses getCloudStorageMatrix(StackVersionV4Filter stackVersionV4Filter) {
        return cloudStorageSupportedV4Responses(fileSystemSupportMatrixService.getCloudStorageMatrix(stackVersionV4Filter.getStackVersion()));
    }

    @Override
    public DatalakePrerequisiteV4Response registerDatalakePrerequisite(Long workspaceId, EnvironmentV4Filter environmentV4Filter,
                                                                    DatalakePrerequisiteV4Request datalakePrerequisiteV4Request) {
        return datalakePrerequisiteService.prepare(workspaceId, environmentV4Filter.getEnvironment(), datalakePrerequisiteV4Request);
    }

    @Override
    public RepoConfigValidationV4Response repositoryConfigValidationRequest(RepoConfigValidationV4Request repoConfigValidationV4Request) {
        return validationService.validate(repoConfigValidationV4Request);
    }

    @Override
    public ExposedServiceV4Responses getKnoxServices(String blueprintName) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        Workspace workspace = workspaceService.get(restRequestThreadLocalService.getRequestedWorkspaceId(), user);
        return exposedServiceV4Responses(serviceEndpointCollector.getKnoxServices(blueprintName, workspace));
    }

    @Override
    public ParametersQueryV4Response getCustomParameters(ParametersQueryV4Request parametersQueryRequest) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        Workspace workspace = workspaceService.get(restRequestThreadLocalService.getRequestedWorkspaceId(), user);
        Set<String> strings = blueprintService.queryCustomParameters(parametersQueryRequest.getBlueprintName(), workspace);
        Map<String, String> result = new HashMap<>();
        for (String customParameter : strings) {
            result.put(customParameter, "");
        }
        ParametersQueryV4Response parametersQueryResponse = new ParametersQueryV4Response();
        parametersQueryResponse.setCustom(result);
        return parametersQueryResponse;
    }

    @Override
    public StructuredParameterQueriesV4Response getFileSystemParameters(StructuredParametersQueryV4Request structuredParametersQueryRequest) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        Workspace workspace = workspaceService.get(restRequestThreadLocalService.getRequestedWorkspaceId(), user);
        Set<ConfigQueryEntry> entries = blueprintService.queryFileSystemParameters(
                structuredParametersQueryRequest.getBlueprintName(),
                structuredParametersQueryRequest.getClusterName(),
                structuredParametersQueryRequest.getStorageName(),
                structuredParametersQueryRequest.getFileSystemType(),
                structuredParametersQueryRequest.getAccountName(),
                structuredParametersQueryRequest.isAttachedCluster(),
                workspace);
        List<StructuredParameterQueryV4Response> result = new ArrayList<>();
        for (ConfigQueryEntry configQueryEntry : entries) {
            result.add(conversionService.convert(configQueryEntry, StructuredParameterQueryV4Response.class));
        }
        StructuredParameterQueriesV4Response parametersQueryResponse = new StructuredParameterQueriesV4Response();
        parametersQueryResponse.setEntries(result);
        return parametersQueryResponse;
    }
}
