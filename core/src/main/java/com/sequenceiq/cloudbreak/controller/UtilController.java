package com.sequenceiq.cloudbreak.controller;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.UtilEndpoint;
import com.sequenceiq.cloudbreak.api.model.AmbariDatabaseDetailsJson;
import com.sequenceiq.cloudbreak.api.model.AmbariDatabaseTestResult;
import com.sequenceiq.cloudbreak.api.model.RDSConfigJson;
import com.sequenceiq.cloudbreak.api.model.RdsTestResult;
import com.sequenceiq.cloudbreak.controller.validation.rds.RdsConnectionValidator;

@Component
public class UtilController implements UtilEndpoint {

    @Autowired
    private RdsConnectionValidator rdsConnectionValidator;

    @Override
    public RdsTestResult testRdsConnection(@Valid RDSConfigJson rdsConfigJson) {
        RdsTestResult rdsTestResult = new RdsTestResult();
        try {
            rdsConnectionValidator.validateRdsConnection(rdsConfigJson);
            rdsTestResult.setConnectionResult("connected");
        } catch (BadRequestException e) {
            rdsTestResult.setConnectionResult(e.getMessage());
        }
        return rdsTestResult;
    }

    @Override
    public AmbariDatabaseTestResult testAmbariDatabase(@Valid AmbariDatabaseDetailsJson ambariDatabaseDetailsJson) {
        return new AmbariDatabaseTestResult();
    }
}
