package com.sequenceiq.cloudbreak.controller;

import static com.sequenceiq.cloudbreak.util.SqlUtil.getProperSqlErrorMessage;

import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v1.RdsConfigEndpoint;
import com.sequenceiq.cloudbreak.api.model.rds.RDSConfigRequest;
import com.sequenceiq.cloudbreak.api.model.rds.RDSConfigResponse;
import com.sequenceiq.cloudbreak.api.model.rds.RDSTestRequest;
import com.sequenceiq.cloudbreak.api.model.rds.RdsTestResult;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.common.type.APIResourceType;
import com.sequenceiq.cloudbreak.common.type.ResourceEvent;
import com.sequenceiq.cloudbreak.controller.validation.rds.RdsConnectionValidator;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigService;

@Component
public class RdsConfigController extends NotificationController implements RdsConfigEndpoint {

    @Autowired
    private RdsConfigService rdsConfigService;

    @Autowired
    private AuthenticatedUserService authenticatedUserService;

    @Autowired
    private RdsConnectionValidator rdsConnectionValidator;

    @Autowired
    @Qualifier("conversionService")
    private ConversionService conversionService;

    @Override
    public RDSConfigResponse postPrivate(RDSConfigRequest rdsConfigRequest) {
        IdentityUser user = authenticatedUserService.getCbUser();
        return createRdsConfig(user, rdsConfigRequest, false);
    }

    @Override
    public RDSConfigResponse postPublic(RDSConfigRequest rdsConfigRequest) {
        IdentityUser user = authenticatedUserService.getCbUser();
        return createRdsConfig(user, rdsConfigRequest, true);
    }

    @Override
    public Set<RDSConfigResponse> getPrivates() {
        IdentityUser user = authenticatedUserService.getCbUser();
        Set<RDSConfig> rdsConfigs = rdsConfigService.retrievePrivateRdsConfigs(user);
        return toJsonList(rdsConfigs);
    }

    @Override
    public RDSConfigResponse getPrivate(String name) {
        IdentityUser user = authenticatedUserService.getCbUser();
        RDSConfig rdsConfig = rdsConfigService.getPrivateRdsConfig(name, user);
        return conversionService.convert(rdsConfig, RDSConfigResponse.class);
    }

    @Override
    public RDSConfigResponse getPublic(String name) {
        IdentityUser user = authenticatedUserService.getCbUser();
        RDSConfig rdsConfig = rdsConfigService.getPublicRdsConfig(name, user);
        return conversionService.convert(rdsConfig, RDSConfigResponse.class);
    }

    @Override
    public Set<RDSConfigResponse> getPublics() {
        IdentityUser user = authenticatedUserService.getCbUser();
        Set<RDSConfig> rdsConfigs = rdsConfigService.retrieveAccountRdsConfigs(user);
        return toJsonList(rdsConfigs);
    }

    @Override
    public RDSConfigResponse get(Long id) {
        RDSConfig rdsConfig = rdsConfigService.get(id);
        return conversionService.convert(rdsConfig, RDSConfigResponse.class);
    }

    @Override
    public void delete(Long id) {
        executeAndNotify(user -> rdsConfigService.delete(id, user), ResourceEvent.RDS_CONFIG_DELETED);
    }

    @Override
    public void deletePublic(String name) {
        executeAndNotify(user -> rdsConfigService.delete(name, user), ResourceEvent.RDS_CONFIG_DELETED);
    }

    @Override
    public void deletePrivate(String name) {
        executeAndNotify(user -> rdsConfigService.delete(name, user), ResourceEvent.RDS_CONFIG_DELETED);
    }

    @Override
    public RdsTestResult testRdsConnection(RDSTestRequest rdsTestRequest) {
        Long existingRDSConfigId = rdsTestRequest.getId();
        RDSConfigRequest configRequest = rdsTestRequest.getRdsConfig();
        if (existingRDSConfigId == null && configRequest == null) {
            throw new BadRequestException("Either an RDSConfig id or an RDSConfig request needs to be specified in the request. ");
        }

        RdsTestResult rdsTestResult = new RdsTestResult();
        if (existingRDSConfigId != null) {
            try {
                RDSConfig config = rdsConfigService.get(existingRDSConfigId);
                rdsTestResult = testRDSConnectivity(config.getConnectionURL(), config.getConnectionUserName(), config.getConnectionPassword());
            } catch (NotFoundException e) {
                rdsTestResult.setConnectionResult("not found");
            }
        } else {
            rdsTestResult = testRDSConnectivity(configRequest.getConnectionURL(), configRequest.getConnectionUserName(), configRequest.getConnectionPassword());
        }
        return rdsTestResult;
    }

    private RDSConfigResponse createRdsConfig(IdentityUser user, RDSConfigRequest rdsConfigJson, boolean publicInAccount) {
        if (rdsConfigJson.isValidated()) {
            rdsConnectionValidator.validateRdsConnection(rdsConfigJson.getConnectionURL(), rdsConfigJson.getConnectionUserName(),
                    rdsConfigJson.getConnectionPassword());
        }
        RDSConfig rdsConfig = conversionService.convert(rdsConfigJson, RDSConfig.class);
        rdsConfig.setPublicInAccount(publicInAccount);
        try {
            rdsConfig = rdsConfigService.create(user, rdsConfig);
            notify(user, ResourceEvent.RDS_CONFIG_CREATED);
        } catch (DataIntegrityViolationException ex) {
            String msg = String.format("Error with resource [%s], error: [%s]", APIResourceType.RDS_CONFIG, getProperSqlErrorMessage(ex));
            throw new BadRequestException(msg);
        }
        return conversionService.convert(rdsConfig, RDSConfigResponse.class);
    }

    private Set<RDSConfigResponse> toJsonList(Set<RDSConfig> rdsConfigs) {
        return (Set<RDSConfigResponse>) conversionService.convert(rdsConfigs,
                TypeDescriptor.forObject(rdsConfigs),
                TypeDescriptor.collection(Set.class, TypeDescriptor.valueOf(RDSConfigResponse.class)));
    }

    private RdsTestResult testRDSConnectivity(String connectionURL, String connectionUserName, String connectionPassword) {
        RdsTestResult rdsTestResult = new RdsTestResult();
        try {
            rdsConnectionValidator.validateRdsConnection(connectionURL, connectionUserName, connectionPassword);
            rdsTestResult.setConnectionResult("connected");
        } catch (RuntimeException e) {
            rdsTestResult.setConnectionResult(e.getMessage());
        }
        return rdsTestResult;
    }
}
