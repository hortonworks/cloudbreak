package com.sequenceiq.cloudbreak.controller;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v1.UtilEndpoint;
import com.sequenceiq.cloudbreak.api.model.AmbariDatabaseDetailsJson;
import com.sequenceiq.cloudbreak.api.model.AmbariDatabaseTestResult;
import com.sequenceiq.cloudbreak.api.model.ExposedServiceResponse;
import com.sequenceiq.cloudbreak.api.model.ParametersQueryRequest;
import com.sequenceiq.cloudbreak.api.model.ParametersQueryResponse;
import com.sequenceiq.cloudbreak.api.model.VersionCheckResult;
import com.sequenceiq.cloudbreak.api.model.rds.RDSBuildRequest;
import com.sequenceiq.cloudbreak.api.model.rds.RdsBuildResult;
import com.sequenceiq.cloudbreak.api.model.stack.StackMatrix;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.controller.validation.rds.RdsConnectionBuilder;
import com.sequenceiq.cloudbreak.converter.stack.cluster.ServiceEndpointCollector;
import com.sequenceiq.cloudbreak.service.AuthenticatedUserService;
import com.sequenceiq.cloudbreak.service.StackMatrixService;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
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
    private AuthenticatedUserService authenticatedUserService;

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
        IdentityUser cbUser = authenticatedUserService.getCbUser();
        return serviceEndpointCollector.getKnoxServices(cbUser, blueprintName);
    }

    @Override
    public ParametersQueryResponse getCustomParameters(ParametersQueryRequest parametersQueryRequest) {
        IdentityUser user = authenticatedUserService.getCbUser();
        return blueprintService.queryCustomParameters(parametersQueryRequest.getBlueprintName(), user);
    }
}
