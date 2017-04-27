package com.sequenceiq.cloudbreak.controller;

import javax.inject.Inject;
import javax.validation.Valid;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.UtilEndpoint;
import com.sequenceiq.cloudbreak.api.model.AmbariDatabaseDetailsJson;
import com.sequenceiq.cloudbreak.api.model.AmbariDatabaseTestResult;
import com.sequenceiq.cloudbreak.api.model.LdapConfigRequest;
import com.sequenceiq.cloudbreak.api.model.LdapTestResult;
import com.sequenceiq.cloudbreak.api.model.RDSBuildRequest;
import com.sequenceiq.cloudbreak.api.model.RDSConfigRequest;
import com.sequenceiq.cloudbreak.api.model.RdsBuildResult;
import com.sequenceiq.cloudbreak.api.model.RdsTestResult;
import com.sequenceiq.cloudbreak.controller.validation.ldapconfig.LdapConfigValidator;
import com.sequenceiq.cloudbreak.controller.validation.rds.RdsConnectionBuilder;
import com.sequenceiq.cloudbreak.controller.validation.rds.RdsConnectionValidator;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.repository.RdsConfigRepository;

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

    @Override
    public RdsTestResult testRdsConnection(@Valid RDSConfigRequest rdsConfigRequest) {
        RdsTestResult rdsTestResult = new RdsTestResult();
        try {
            rdsConnectionValidator.validateRdsConnection(rdsConfigRequest.getConnectionURL(), rdsConfigRequest.getConnectionUserName(),
                    rdsConfigRequest.getConnectionPassword());
            rdsTestResult.setConnectionResult(CONNECTED);
        } catch (BadRequestException e) {
            rdsTestResult.setConnectionResult(e.getMessage());
        }
        return rdsTestResult;
    }

    @Override
    public RdsBuildResult buildRdsConnection(@Valid RDSBuildRequest rdsBuildRequest) {
        RdsBuildResult rdsBuildResult = new RdsBuildResult();
        try {
            rdsConnectionBuilder.buildRdsConnection(
                    rdsBuildRequest.getRdsConfigRequest().getConnectionURL(),
                    rdsBuildRequest.getRdsConfigRequest().getConnectionUserName(),
                    rdsBuildRequest.getRdsConfigRequest().getConnectionPassword(),
                    rdsBuildRequest.getClusterName());
            rdsBuildResult.setAmbariDbName(rdsBuildRequest.getClusterName() + "-ambari");
            rdsBuildResult.setHiveDbName(rdsBuildRequest.getClusterName() + "-hive");
            rdsBuildResult.setRangerDbName(rdsBuildRequest.getClusterName() + "-ranger");
        } catch (BadRequestException e) {
            throw new BadRequestException("Could not create databases in metastore.");
        }
        return rdsBuildResult;
    }

    @Override
    public RdsTestResult testRdsConnectionById(Long id) {
        RdsTestResult rdsTestResult = new RdsTestResult();
        try {
            RDSConfig config = rdsConfigRepository.findById(id);
            if (config != null) {
                rdsConnectionValidator.validateRdsConnection(config.getConnectionURL(), config.getConnectionUserName(), config.getConnectionPassword());
                rdsTestResult.setConnectionResult(CONNECTED);
            } else {
                rdsTestResult.setConnectionResult("not found");
            }
        } catch (Exception e) {
            rdsTestResult.setConnectionResult(e.getMessage());
        }
        return rdsTestResult;
    }

    @Override
    public LdapTestResult testLdapConnection(@Valid LdapConfigRequest ldapConfig) {
        LdapTestResult ldapTestResult = new LdapTestResult();
        try {
            ldapConfigValidator.validateLdapConnection(ldapConfig);
            ldapTestResult.setConnectionResult(CONNECTED);
        } catch (BadRequestException e) {
            ldapTestResult.setConnectionResult(e.getMessage());
        }
        return ldapTestResult;
    }

    @Override
    public AmbariDatabaseTestResult testAmbariDatabase(@Valid AmbariDatabaseDetailsJson ambariDatabaseDetailsJson) {
        return new AmbariDatabaseTestResult();
    }
}
