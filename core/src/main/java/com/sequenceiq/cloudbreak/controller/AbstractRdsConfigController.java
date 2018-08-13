package com.sequenceiq.cloudbreak.controller;

import javax.inject.Inject;
import javax.inject.Named;

import org.springframework.core.convert.ConversionService;

import com.sequenceiq.cloudbreak.api.model.rds.RDSConfigRequest;
import com.sequenceiq.cloudbreak.api.model.rds.RDSTestRequest;
import com.sequenceiq.cloudbreak.api.model.rds.RdsTestResult;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.organization.Organization;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigService;

public abstract class AbstractRdsConfigController extends NotificationController {

    @Inject
    private RdsConfigService rdsConfigService;

    @Inject
    @Named("conversionService")
    private ConversionService conversionService;

    public RdsTestResult testRdsConnection(RDSTestRequest rdsTestRequest, Organization organization) {
        String existingRDSConfigName = rdsTestRequest.getName();
        RDSConfigRequest configRequest = rdsTestRequest.getRdsConfig();
        if (existingRDSConfigName != null) {
            return new RdsTestResult(rdsConfigService.testRdsConnection(existingRDSConfigName, organization));
        } else if (configRequest != null) {
            RDSConfig rdsConfig = conversionService.convert(configRequest, RDSConfig.class);
            return new RdsTestResult(rdsConfigService.testRdsConnection(rdsConfig));
        }
        throw new BadRequestException("Either an RDSConfig id, name or an RDSConfig request needs to be specified in the request. ");
    }

    public RdsConfigService getRdsConfigService() {
        return rdsConfigService;
    }

    public ConversionService getConversionService() {
        return conversionService;
    }
}
