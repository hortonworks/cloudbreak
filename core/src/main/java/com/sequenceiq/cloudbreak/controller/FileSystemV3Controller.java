package com.sequenceiq.cloudbreak.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v3.FileSystemV3Endpoint;
import com.sequenceiq.cloudbreak.api.model.StructuredParameterQueriesResponse;
import com.sequenceiq.cloudbreak.api.model.StructuredParameterQueryResponse;
import com.sequenceiq.cloudbreak.api.model.StructuredParametersQueryRequest;
import com.sequenceiq.cloudbreak.blueprint.filesystem.query.ConfigQueryEntry;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.domain.organization.Organization;
import com.sequenceiq.cloudbreak.domain.organization.User;
import com.sequenceiq.cloudbreak.service.RestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.service.organization.OrganizationService;
import com.sequenceiq.cloudbreak.service.user.UserService;

@Component
public class FileSystemV3Controller implements FileSystemV3Endpoint {

    @Inject
    private UserService userService;

    @Inject
    private OrganizationService organizationService;

    @Inject
    private BlueprintService blueprintService;

    @Inject
    private RestRequestThreadLocalService restRequestThreadLocalService;

    @Inject
    @Named("conversionService")
    private ConversionService conversionService;

    @Override
    public StructuredParameterQueriesResponse getFileSystemParameters(Long organizationId, StructuredParametersQueryRequest structuredParametersQueryRequest) {
        Organization organization = getOrganization(organizationId);
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

    private Organization getOrganization(Long organizationId) {
        IdentityUser identityUser = restRequestThreadLocalService.getIdentityUser();
        User user = userService.getOrCreate(identityUser);
        return organizationService.get(organizationId, user);
    }
}
