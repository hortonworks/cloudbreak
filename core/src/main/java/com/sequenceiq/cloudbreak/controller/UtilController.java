package com.sequenceiq.cloudbreak.controller;

import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v1.UtilEndpoint;
import com.sequenceiq.cloudbreak.api.model.AmbariDatabaseDetailsJson;
import com.sequenceiq.cloudbreak.api.model.AmbariDatabaseTestResult;
import com.sequenceiq.cloudbreak.api.model.stack.StackMatrix;
import com.sequenceiq.cloudbreak.api.model.VersionCheckResult;
import com.sequenceiq.cloudbreak.api.model.rds.RDSBuildRequest;
import com.sequenceiq.cloudbreak.api.model.rds.RdsBuildResult;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.controller.validation.ldapconfig.LdapConfigValidator;
import com.sequenceiq.cloudbreak.controller.validation.rds.RdsConnectionBuilder;
import com.sequenceiq.cloudbreak.controller.validation.rds.RdsConnectionValidator;
import com.sequenceiq.cloudbreak.repository.LdapConfigRepository;
import com.sequenceiq.cloudbreak.repository.RdsConfigRepository;
import com.sequenceiq.cloudbreak.service.StackMatrixService;
import com.sequenceiq.cloudbreak.util.ClientVersionUtil;

@Component
public class UtilController implements UtilEndpoint {

    private static final String CONNECTED = "connected";

    @Inject
    private RdsConnectionValidator rdsConnectionValidator;

    @Inject
    private RdsConnectionBuilder rdsConnectionBuilder;

    @Inject
    private LdapConfigValidator ldapConfigValidator;

    @Inject
    private RdsConfigRepository rdsConfigRepository;

    @Inject
    private LdapConfigRepository ldapConfigRepository;

    @Inject
    private StackMatrixService stackMatrixService;

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
}
