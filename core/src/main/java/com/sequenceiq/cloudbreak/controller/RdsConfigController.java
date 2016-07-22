package com.sequenceiq.cloudbreak.controller;

import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.RdsConfigEndpoint;
import com.sequenceiq.cloudbreak.api.model.IdJson;
import com.sequenceiq.cloudbreak.api.model.RDSConfigJson;
import com.sequenceiq.cloudbreak.api.model.RDSConfigResponse;
import com.sequenceiq.cloudbreak.common.type.APIResourceType;
import com.sequenceiq.cloudbreak.controller.validation.rds.RdsConnectionValidator;
import com.sequenceiq.cloudbreak.domain.CbUser;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.service.DuplicateKeyValueException;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigService;

@Component
public class RdsConfigController implements RdsConfigEndpoint {

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
    public IdJson postPrivate(RDSConfigJson rdsConfigRequest) {
        CbUser user = authenticatedUserService.getCbUser();
        MDCBuilder.buildUserMdcContext(user);
        return createRdsConfig(user, rdsConfigRequest, false);
    }

    @Override
    public IdJson postPublic(RDSConfigJson rdsConfigRequest) {
        CbUser user = authenticatedUserService.getCbUser();
        MDCBuilder.buildUserMdcContext(user);
        return createRdsConfig(user, rdsConfigRequest, true);
    }

    @Override
    public Set<RDSConfigResponse> getPrivates() {
        CbUser user = authenticatedUserService.getCbUser();
        MDCBuilder.buildUserMdcContext(user);
        Set<RDSConfig> rdsConfigs = rdsConfigService.retrievePrivateRdsConfigs(user);
        return toJsonList(rdsConfigs);
    }

    @Override
    public RDSConfigResponse getPrivate(String name) {
        CbUser user = authenticatedUserService.getCbUser();
        MDCBuilder.buildUserMdcContext(user);
        RDSConfig rdsConfig = rdsConfigService.getPrivateRdsConfig(name, user);
        return conversionService.convert(rdsConfig, RDSConfigResponse.class);
    }

    @Override
    public RDSConfigResponse getPublic(String name) {
        CbUser user = authenticatedUserService.getCbUser();
        RDSConfig rdsConfig = rdsConfigService.getPublicRdsConfig(name, user);
        return conversionService.convert(rdsConfig, RDSConfigResponse.class);
    }

    @Override
    public Set<RDSConfigResponse> getPublics() {
        CbUser user = authenticatedUserService.getCbUser();
        MDCBuilder.buildUserMdcContext(user);
        Set<RDSConfig> rdsConfigs = rdsConfigService.retrieveAccountRdsConfigs(user);
        return toJsonList(rdsConfigs);
    }

    @Override
    public RDSConfigResponse get(Long id) {
        CbUser user = authenticatedUserService.getCbUser();
        MDCBuilder.buildUserMdcContext(user);
        RDSConfig rdsConfig = rdsConfigService.get(id);
        return conversionService.convert(rdsConfig, RDSConfigResponse.class);
    }

    @Override
    public void delete(Long id) {
        CbUser user = authenticatedUserService.getCbUser();
        MDCBuilder.buildUserMdcContext(user);
        rdsConfigService.delete(id, user);
    }

    @Override
    public void deletePublic(String name) {
        CbUser user = authenticatedUserService.getCbUser();
        MDCBuilder.buildUserMdcContext(user);
        rdsConfigService.delete(name, user);
    }

    @Override
    public void deletePrivate(String name) {
        CbUser user = authenticatedUserService.getCbUser();
        MDCBuilder.buildUserMdcContext(user);
        rdsConfigService.delete(name, user);
    }

    private IdJson createRdsConfig(CbUser user, RDSConfigJson rdsConfigJson, boolean publicInAccount) {
        rdsConnectionValidator.validateRdsConnection(rdsConfigJson);
        RDSConfig rdsConfig = conversionService.convert(rdsConfigJson, RDSConfig.class);
        rdsConfig.setPublicInAccount(publicInAccount);
        try {
            rdsConfig = rdsConfigService.create(user, rdsConfig);
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateKeyValueException(APIResourceType.RDS_CONFIG, rdsConfig.getName(), e);
        }
        return new IdJson(rdsConfig.getId());
    }

    private Set<RDSConfigResponse> toJsonList(Set<RDSConfig> rdsConfigs) {
        return (Set<RDSConfigResponse>) conversionService.convert(rdsConfigs,
                TypeDescriptor.forObject(rdsConfigs),
                TypeDescriptor.collection(Set.class, TypeDescriptor.valueOf(RDSConfigResponse.class)));
    }

}