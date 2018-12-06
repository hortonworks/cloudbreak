package com.sequenceiq.cloudbreak.service.datalake;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.model.KerberosResponse;
import com.sequenceiq.cloudbreak.api.model.datalake.DatalakePrerequisiteRequest;
import com.sequenceiq.cloudbreak.api.model.datalake.DatalakePrerequisiteResponse;
import com.sequenceiq.cloudbreak.api.model.kerberos.KerberosRequest;
import com.sequenceiq.cloudbreak.api.model.ldap.LdapConfigRequest;
import com.sequenceiq.cloudbreak.api.model.ldap.LdapConfigResponse;
import com.sequenceiq.cloudbreak.api.model.rds.RDSConfigRequest;
import com.sequenceiq.cloudbreak.api.model.rds.RDSConfigResponse;
import com.sequenceiq.cloudbreak.common.type.ResourceEvent;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.controller.validation.ldapconfig.LdapConfigValidator;
import com.sequenceiq.cloudbreak.controller.validation.rds.RdsConnectionValidator;
import com.sequenceiq.cloudbreak.domain.KerberosConfig;
import com.sequenceiq.cloudbreak.domain.LdapConfig;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.service.events.DefaultCloudbreakEventService;
import com.sequenceiq.cloudbreak.service.kerberos.KerberosService;
import com.sequenceiq.cloudbreak.service.ldapconfig.LdapConfigService;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigService;
import com.sequenceiq.cloudbreak.structuredevent.event.LdapDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.RdsDetails;

@Service
public class DatalakePrerequisiteService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatalakePrerequisiteService.class);

    private static final boolean NOTIFY_WORKSPACE = false;

    @Inject
    private RdsConfigService rdsConfigService;

    @Inject
    private LdapConfigService ldapConfigService;

    @Inject
    private KerberosService kerberosService;

    @Inject
    private RdsConnectionValidator rdsConnectionValidator;

    @Inject
    private LdapConfigValidator ldapConfigValidator;

    @Inject
    private DefaultCloudbreakEventService defaultCloudbreakEventService;

    @Inject
    @Named("conversionService")
    private ConversionService conversionService;

    public DatalakePrerequisiteResponse prepare(Long workspaceId, String environment, DatalakePrerequisiteRequest datalakePrerequisiteRequest) {
        boolean successFullyTested = testConnections(datalakePrerequisiteRequest);
        if (successFullyTested) {
            return prepareResources(workspaceId, environment, datalakePrerequisiteRequest);
        } else {
            throw new BadRequestException("Some of the resource for datalake prerequistes was not available please make sure that everything "
                    + "is up and running.");
        }
    }

    private boolean testConnections(DatalakePrerequisiteRequest datalakePrerequisiteRequest) {
        LdapConfig ldapConfig = conversionService.convert(datalakePrerequisiteRequest.getLdapConfig(), LdapConfig.class);
        boolean ret = true;
        try {
            ldapConfigValidator.validateLdapConnection(ldapConfig);
            fireLdapEvent(ldapConfig, ResourceEvent.TEST_CONNECTION_SUCCESS, "Connection successfully established with the LDAP server", NOTIFY_WORKSPACE);
        } catch (Exception ex) {
            fireLdapEvent(ldapConfig, ResourceEvent.TEST_CONNECTION_FAILED, "Connection failed to established with the LDAP server", NOTIFY_WORKSPACE);
            ret = false;
        }

        for (RDSConfigRequest rdsConfigRequest : datalakePrerequisiteRequest.getRdsConfigs()) {
            RDSConfig rdsConfig = conversionService.convert(rdsConfigRequest, RDSConfig.class);
            try {
                rdsConnectionValidator.validateRdsConnection(rdsConfig);
                fireRdsEvent(rdsConfig, ResourceEvent.TEST_CONNECTION_SUCCESS, "Connection successfully established with the Remote database server",
                        NOTIFY_WORKSPACE);
            } catch (Exception ex) {
                fireRdsEvent(rdsConfig, ResourceEvent.TEST_CONNECTION_FAILED, "Connection failed to established with the Remote database server",
                        NOTIFY_WORKSPACE);
                ret = false;
            }
        }
        return ret;
    }

    private DatalakePrerequisiteResponse prepareResources(Long workspaceId, String environment, DatalakePrerequisiteRequest datalakePrerequisiteRequest) {
        KerberosConfig kerberosConfig = createKerberosConfig(workspaceId, environment, datalakePrerequisiteRequest);
        LdapConfig ldapConfig = createLdapConfig(workspaceId, environment, datalakePrerequisiteRequest);
        Set<RDSConfig> rdsConfigs = createRdsConfig(workspaceId, environment, datalakePrerequisiteRequest);

        DatalakePrerequisiteResponse datalakePrerequisiteResponse = new DatalakePrerequisiteResponse();
        datalakePrerequisiteResponse.setKerberosConfig(prepareKerberosResponse(kerberosConfig));
        datalakePrerequisiteResponse.setLdapConfig(prepareLdapResponse(ldapConfig));
        datalakePrerequisiteResponse.setRdsConfigs(prepareRdsResponse(rdsConfigs));
        return datalakePrerequisiteResponse;
    }

    private Set<RDSConfig> createRdsConfig(Long workspaceId, String environment, DatalakePrerequisiteRequest datalakePrerequisiteRequest) {
        Set<RDSConfig> rdsConfigSet = new HashSet<>();
        for (RDSConfigRequest rdsConfigRequest : datalakePrerequisiteRequest.getRdsConfigs()) {
            RDSConfig rdsConfig = prepareRdsConfig(environment, rdsConfigRequest);
            rdsConfigSet.add(rdsConfigService.createInEnvironment(rdsConfig, Set.of(environment), workspaceId));
        }
        return rdsConfigSet;
    }

    private LdapConfig createLdapConfig(Long workspaceId, String environment, DatalakePrerequisiteRequest datalakePrerequisiteRequest) {
        LdapConfig ldapConfig = prepareLdapConfig(environment, datalakePrerequisiteRequest.getLdapConfig());
        return ldapConfigService.createInEnvironment(ldapConfig, Set.of(environment), workspaceId);
    }

    private KerberosConfig createKerberosConfig(Long workspaceId, String environment, DatalakePrerequisiteRequest datalakePrerequisiteRequest) {
        KerberosConfig kerberosConfig = prepareKerberosConfig(environment, datalakePrerequisiteRequest.getKerberosConfig());
        return kerberosService.createInEnvironment(kerberosConfig, Set.of(environment), workspaceId);
    }

    private LdapDetails prepareLdapDetails(LdapConfig ldapConfig) {
        return conversionService.convert(ldapConfig, LdapDetails.class);
    }

    private LdapConfig prepareLdapConfig(String environment, LdapConfigRequest ldapConfigRequest) {
        LdapConfig ldapConfig = conversionService.convert(ldapConfigRequest, LdapConfig.class);
        ldapConfig.setName(String.format("%s-%s-%s", environment, ldapConfigRequest.getName(), getHash()));
        return ldapConfig;
    }

    private LdapConfigResponse prepareLdapResponse(LdapConfig ldapConfig) {
        return conversionService.convert(ldapConfig, LdapConfigResponse.class);
    }

    private void fireLdapEvent(LdapConfig ldapConfig, ResourceEvent event, String message, boolean notifyWorkspace) {
        defaultCloudbreakEventService.fireLdapEvent(prepareLdapDetails(ldapConfig), event.name(), message, notifyWorkspace);
    }

    private void fireRdsEvent(RDSConfig rdsConfig, ResourceEvent event, String message, boolean notifyWorkspace) {
        defaultCloudbreakEventService.fireRdsEvent(prepareRdsDetails(rdsConfig), event.name(), message, notifyWorkspace);
    }

    private RdsDetails prepareRdsDetails(RDSConfig rdsConfig) {
        return conversionService.convert(rdsConfig, RdsDetails.class);
    }

    private RDSConfig prepareRdsConfig(String environment, RDSConfigRequest rdsConfigRequest) {
        RDSConfig rdsConfig = conversionService.convert(rdsConfigRequest, RDSConfig.class);
        rdsConfig.setName(String.format("%s-%s-%s", environment, rdsConfigRequest.getName(), getHash()));
        return rdsConfig;
    }

    private Set<RDSConfigResponse> prepareRdsResponse(Set<RDSConfig> rdsConfigs) {
        Set<RDSConfigResponse> rdsConfigResponses = new HashSet<>();
        for (RDSConfig rdsConfig : rdsConfigs) {
            rdsConfigResponses.add(conversionService.convert(rdsConfig, RDSConfigResponse.class));
        }
        return rdsConfigResponses;
    }

    private KerberosConfig prepareKerberosConfig(String environment, KerberosRequest kerberosRequest) {
        KerberosConfig kerberosConfig = conversionService.convert(kerberosRequest, KerberosConfig.class);
        kerberosConfig.setName(String.format("%s-%s-%s", environment, kerberosRequest.getName(), getHash()));
        return kerberosConfig;
    }

    private String getHash() {
        return UUID.randomUUID().toString().split("-")[0];
    }

    private KerberosResponse prepareKerberosResponse(KerberosConfig kerberosConfig) {
        return conversionService.convert(kerberosConfig, KerberosResponse.class);
    }
}
