package com.sequenceiq.cloudbreak.controller;

import static com.sequenceiq.cloudbreak.util.SqlUtil.getProperSqlErrorMessage;

import java.util.Set;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;
import javax.validation.Valid;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v1.RdsConfigEndpoint;
import com.sequenceiq.cloudbreak.api.model.rds.RDSConfigRequest;
import com.sequenceiq.cloudbreak.api.model.rds.RDSConfigResponse;
import com.sequenceiq.cloudbreak.api.model.rds.RDSTestRequest;
import com.sequenceiq.cloudbreak.api.model.rds.RdsTestResult;
import com.sequenceiq.cloudbreak.common.type.APIResourceType;
import com.sequenceiq.cloudbreak.common.type.ResourceEvent;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.organization.Organization;

@Component
@Transactional(TxType.NEVER)
public class RdsConfigController extends AbstractRdsConfigController implements RdsConfigEndpoint {

    @Override
    public RDSConfigResponse postPrivate(@Valid RDSConfigRequest rdsConfigRequest) {
        return createRdsConfig(rdsConfigRequest);
    }

    @Override
    public RDSConfigResponse postPublic(@Valid RDSConfigRequest rdsConfigRequest) {
        return createRdsConfig(rdsConfigRequest);
    }

    @Override
    public Set<RDSConfigResponse> getPrivates() {
        Set<RDSConfig> rdsConfigs = getRdsConfigService().retrieveRdsConfigsInDefaultOrg();
        return toJsonList(rdsConfigs);
    }

    @Override
    public RDSConfigResponse getPrivate(String name) {
        RDSConfig rdsConfig = getRdsConfigService().getByNameForDefaultOrg(name);
        return getConversionService().convert(rdsConfig, RDSConfigResponse.class);
    }

    @Override
    public RDSConfigResponse getPublic(String name) {
        RDSConfig rdsConfig = getRdsConfigService().getByNameForDefaultOrg(name);
        return getConversionService().convert(rdsConfig, RDSConfigResponse.class);
    }

    @Override
    public Set<RDSConfigResponse> getPublics() {
        Set<RDSConfig> rdsConfigs = getRdsConfigService().retrieveRdsConfigsInDefaultOrg();
        return toJsonList(rdsConfigs);
    }

    @Override
    public RDSConfigResponse get(Long id) {
        RDSConfig rdsConfig = getRdsConfigService().get(id);
        return getConversionService().convert(rdsConfig, RDSConfigResponse.class);
    }

    @Override
    public void delete(Long id) {
        executeAndNotify(user -> getRdsConfigService().delete(id), ResourceEvent.RDS_CONFIG_DELETED);
    }

    @Override
    public void deletePublic(String name) {
        executeAndNotify(user -> getRdsConfigService().delete(name), ResourceEvent.RDS_CONFIG_DELETED);
    }

    @Override
    public void deletePrivate(String name) {
        executeAndNotify(user -> getRdsConfigService().delete(name), ResourceEvent.RDS_CONFIG_DELETED);
    }

    @Override
    public RdsTestResult testRdsConnection(RDSTestRequest rdsTestRequest) {
        Organization organization = getRdsConfigService().getOrganizationService().getDefaultOrganizationForCurrentUser();
        return testRdsConnection(rdsTestRequest, organization);
    }

    @Override
    public RDSConfigRequest getRequestFromName(String name) {
        RDSConfig rdsConfig = getRdsConfigService().getByNameForDefaultOrg(name);
        return getConversionService().convert(rdsConfig, RDSConfigRequest.class);
    }

    private RDSConfigResponse createRdsConfig(RDSConfigRequest rdsConfigJson) {
        RDSConfig rdsConfig = getConversionService().convert(rdsConfigJson, RDSConfig.class);
        try {
            rdsConfig = getRdsConfigService().createInDefaultOrganization(rdsConfig);
            notify(ResourceEvent.RDS_CONFIG_CREATED);
        } catch (DataIntegrityViolationException ex) {
            String msg = String.format("Error with resource [%s], %s", APIResourceType.RDS_CONFIG, getProperSqlErrorMessage(ex));
            throw new BadRequestException(msg);
        }
        return getConversionService().convert(rdsConfig, RDSConfigResponse.class);
    }

    private Set<RDSConfigResponse> toJsonList(Set<RDSConfig> rdsConfigs) {
        return (Set<RDSConfigResponse>) getConversionService().convert(rdsConfigs,
                TypeDescriptor.forObject(rdsConfigs),
                TypeDescriptor.collection(Set.class, TypeDescriptor.valueOf(RDSConfigResponse.class)));
    }
}
