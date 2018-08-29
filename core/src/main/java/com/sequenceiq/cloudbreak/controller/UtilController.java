package com.sequenceiq.cloudbreak.controller;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v1.UtilEndpoint;
import com.sequenceiq.cloudbreak.api.model.AmbariDatabaseDetailsJson;
import com.sequenceiq.cloudbreak.api.model.AmbariDatabaseTestResult;
import com.sequenceiq.cloudbreak.api.model.ExposedServiceResponse;
import com.sequenceiq.cloudbreak.api.model.ParametersQueryRequest;
import com.sequenceiq.cloudbreak.api.model.ParametersQueryResponse;
import com.sequenceiq.cloudbreak.api.model.StructuredParameterQueriesResponse;
import com.sequenceiq.cloudbreak.api.model.StructuredParameterQueryResponse;
import com.sequenceiq.cloudbreak.api.model.StructuredParametersQueryRequest;
import com.sequenceiq.cloudbreak.api.model.VersionCheckResult;
import com.sequenceiq.cloudbreak.api.model.filesystem.CloudStorageSupportedResponse;
import com.sequenceiq.cloudbreak.api.model.rds.RDSBuildRequest;
import com.sequenceiq.cloudbreak.api.model.rds.RdsBuildResult;
import com.sequenceiq.cloudbreak.api.model.stack.StackMatrix;
import com.sequenceiq.cloudbreak.template.filesystem.query.ConfigQueryEntry;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.controller.validation.rds.RdsConnectionBuilder;
import com.sequenceiq.cloudbreak.domain.organization.Organization;
import com.sequenceiq.cloudbreak.domain.organization.User;
import com.sequenceiq.cloudbreak.service.RestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.ServiceEndpointCollector;
import com.sequenceiq.cloudbreak.service.StackMatrixService;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.service.filesystem.FileSystemSupportMatrixService;
import com.sequenceiq.cloudbreak.service.organization.OrganizationService;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.util.ClientVersionUtil;

@Component
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
    private OrganizationService organizationService;

    @Inject
    private UserService userService;

    @Inject
    private RestRequestThreadLocalService restRequestThreadLocalService;

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
    public RdsBuildResult buildRdsConnection(@Valid RDSBuildRequest rdsBuildRequest, Set<String> targets) {
        RdsBuildResult rdsBuildResult = new RdsBuildResult();
        try {
            String clusterName = rdsBuildRequest.getClusterName().replaceAll("[^a-zA-Z0-9]", "");
            Map<String, String> result = rdsConnectionBuilder.buildRdsConnection(
                    rdsBuildRequest.getRdsConfigRequest().getConnectionURL(),
                    rdsBuildRequest.getRdsConfigRequest().getConnectionUserName(),
                    rdsBuildRequest.getRdsConfigRequest().getConnectionPassword(),
                    clusterName,
                    targets);

            rdsBuildResult.setResults(result);
        } catch (BadRequestException e) {
            throw new BadRequestException("Could not create databases in metastore - " + e.getMessage(), e);
        }
        return rdsBuildResult;
    }

    @Override
    public StackMatrix getStackMatrix() {
        return stackMatrixService.getStackMatrix();
    }

    @Override
    public Collection<ExposedServiceResponse> getKnoxServices(String blueprintName) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getIdentityUser());
        Organization organization = organizationService.get(restRequestThreadLocalService.getRequestedOrgId(), user);
        return serviceEndpointCollector.getKnoxServices(blueprintName, organization);
    }

    @Override
    public Collection<CloudStorageSupportedResponse> getCloudStorageMatrix(String stackVersion) {
        return fileSystemSupportMatrixService.getCloudStorageMatrix(stackVersion);
    }

    @Override
    public ParametersQueryResponse getCustomParameters(ParametersQueryRequest parametersQueryRequest) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getIdentityUser());
        Organization organization = organizationService.get(restRequestThreadLocalService.getRequestedOrgId(), user);
        Set<String> strings = blueprintService.queryCustomParameters(parametersQueryRequest.getBlueprintName(), organization);
        Map<String, String> result = new HashMap<>();
        for (String customParameter : strings) {
            result.put(customParameter, "");
        }
        ParametersQueryResponse parametersQueryResponse = new ParametersQueryResponse();
        parametersQueryResponse.setCustom(result);
        return parametersQueryResponse;
    }

    @Override
    public StructuredParameterQueriesResponse getFileSystemParameters(StructuredParametersQueryRequest structuredParametersQueryRequest) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getIdentityUser());
        Organization organization = organizationService.get(restRequestThreadLocalService.getRequestedOrgId(), user);
        Set<ConfigQueryEntry> entries = blueprintService.queryFileSystemParameters(
                structuredParametersQueryRequest.getBlueprintName(),
                structuredParametersQueryRequest.getClusterName(),
                structuredParametersQueryRequest.getStorageName(),
                structuredParametersQueryRequest.getFileSystemType(),
                structuredParametersQueryRequest.getAccountName(),
                structuredParametersQueryRequest.isAttachedCluster(),
                organization);
        List<StructuredParameterQueryResponse> result = new ArrayList<>();
        for (ConfigQueryEntry configQueryEntry : entries) {
            result.add(conversionService.convert(configQueryEntry, StructuredParameterQueryResponse.class));
        }
        StructuredParameterQueriesResponse parametersQueryResponse = new StructuredParameterQueriesResponse();
        parametersQueryResponse.setEntries(result);
        return parametersQueryResponse;
    }
}
