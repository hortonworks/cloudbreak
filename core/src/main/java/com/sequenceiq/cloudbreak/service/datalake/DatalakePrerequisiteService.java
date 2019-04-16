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

import com.sequenceiq.cloudbreak.api.endpoint.v4.database.requests.DatabaseV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.responses.DatabaseV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.environment.requests.DatalakePrerequisiteV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.environment.responses.DatalakePrerequisiteV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.kerberos.requests.KerberosV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.kerberos.responses.KerberosV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.ldaps.requests.LdapV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.ldaps.responses.LdapV4Response;
import com.sequenceiq.cloudbreak.common.type.ResourceEvent;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.controller.validation.ldapconfig.LdapConfigValidator;
import com.sequenceiq.cloudbreak.controller.validation.rds.RdsConnectionValidator;
import com.sequenceiq.cloudbreak.domain.KerberosConfig;
import com.sequenceiq.cloudbreak.domain.LdapConfig;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.service.events.DefaultCloudbreakEventService;
import com.sequenceiq.cloudbreak.service.kerberos.KerberosConfigService;
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
    private KerberosConfigService kerberosConfigService;

    @Inject
    private RdsConnectionValidator rdsConnectionValidator;

    @Inject
    private LdapConfigValidator ldapConfigValidator;

    @Inject
    private DefaultCloudbreakEventService defaultCloudbreakEventService;

    @Inject
    @Named("conversionService")
    private ConversionService conversionService;

    public DatalakePrerequisiteV4Response prepare(Long workspaceId, String environment, DatalakePrerequisiteV4Request datalakePrerequisiteV4Request) {
        boolean successFullyTested = testConnections(datalakePrerequisiteV4Request);
        if (successFullyTested) {
            return prepareResources(workspaceId, environment, datalakePrerequisiteV4Request);
        } else {
            throw new BadRequestException("Some of the resource for datalake prerequistes was not available please make sure that everything "
                    + "is up and running.");
        }
    }

    private boolean testConnections(DatalakePrerequisiteV4Request datalakePrerequisiteV4Request) {
        LdapConfig ldapConfig = conversionService.convert(datalakePrerequisiteV4Request.getLdap(), LdapConfig.class);
        boolean ret = true;
        try {
            ldapConfigValidator.validateLdapConnection(ldapConfig);
            fireLdapEvent(ldapConfig, ResourceEvent.TEST_CONNECTION_SUCCESS, "Connection successfully established with the LDAP server", NOTIFY_WORKSPACE);
        } catch (Exception ex) {
            fireLdapEvent(ldapConfig, ResourceEvent.TEST_CONNECTION_FAILED, "Connection failed to established with the LDAP server", NOTIFY_WORKSPACE);
            ret = false;
        }

        for (DatabaseV4Request rdsConfigRequest : datalakePrerequisiteV4Request.getDatabases()) {
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

    private DatalakePrerequisiteV4Response prepareResources(Long workspaceId, String environment, DatalakePrerequisiteV4Request datalakePrerequisiteV4Request) {
        KerberosConfig kerberosConfig = createKerberosConfig(workspaceId, environment, datalakePrerequisiteV4Request);
        LdapConfig ldapConfig = createLdapConfig(workspaceId, environment, datalakePrerequisiteV4Request);
        Set<RDSConfig> rdsConfigs = createRdsConfig(workspaceId, environment, datalakePrerequisiteV4Request);

        DatalakePrerequisiteV4Response datalakePrerequisiteV4Response = new DatalakePrerequisiteV4Response();
        datalakePrerequisiteV4Response.setKerberos(prepareKerberosResponse(kerberosConfig));
        datalakePrerequisiteV4Response.setLdap(prepareLdapResponse(ldapConfig));
        datalakePrerequisiteV4Response.setDatabases(prepareRdsResponse(rdsConfigs));
        return datalakePrerequisiteV4Response;
    }

    private Set<RDSConfig> createRdsConfig(Long workspaceId, String environment, DatalakePrerequisiteV4Request datalakePrerequisiteV4Request) {
        Set<RDSConfig> rdsConfigSet = new HashSet<>();
        for (DatabaseV4Request rdsConfigRequest : datalakePrerequisiteV4Request.getDatabases()) {
            RDSConfig rdsConfig = prepareRdsConfig(environment, rdsConfigRequest);
            rdsConfigSet.add(rdsConfigService.createInEnvironment(rdsConfig, Set.of(environment), workspaceId));
        }
        return rdsConfigSet;
    }

    private LdapConfig createLdapConfig(Long workspaceId, String environment, DatalakePrerequisiteV4Request datalakePrerequisiteV4Request) {
        LdapConfig ldapConfig = prepareLdapConfig(environment, datalakePrerequisiteV4Request.getLdap());
        return ldapConfigService.createInEnvironment(ldapConfig, Set.of(environment), workspaceId);
    }

    private KerberosConfig createKerberosConfig(Long workspaceId, String environment, DatalakePrerequisiteV4Request datalakePrerequisiteV4Request) {
        KerberosConfig kerberosConfig = prepareKerberosConfig(environment, datalakePrerequisiteV4Request.getKerberos());
        return kerberosConfigService.createInEnvironment(kerberosConfig, Set.of(environment), workspaceId);
    }

    private LdapDetails prepareLdapDetails(LdapConfig ldapConfig) {
        return conversionService.convert(ldapConfig, LdapDetails.class);
    }

    private LdapConfig prepareLdapConfig(String environment, LdapV4Request ldapConfigRequest) {
        LdapConfig ldapConfig = conversionService.convert(ldapConfigRequest, LdapConfig.class);
        ldapConfig.setName(String.format("%s-%s-%s", environment, ldapConfigRequest.getName(), getHash()));
        return ldapConfig;
    }

    private LdapV4Response prepareLdapResponse(LdapConfig ldapConfig) {
        return conversionService.convert(ldapConfig, LdapV4Response.class);
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

    private RDSConfig prepareRdsConfig(String environment, DatabaseV4Request rdsConfigRequest) {
        RDSConfig rdsConfig = conversionService.convert(rdsConfigRequest, RDSConfig.class);
        rdsConfig.setName(String.format("%s-%s-%s", environment, rdsConfigRequest.getName(), getHash()));
        return rdsConfig;
    }

    private Set<DatabaseV4Response> prepareRdsResponse(Set<RDSConfig> rdsConfigs) {
        Set<DatabaseV4Response> rdsConfigResponses = new HashSet<>();
        for (RDSConfig rdsConfig : rdsConfigs) {
            rdsConfigResponses.add(conversionService.convert(rdsConfig, DatabaseV4Response.class));
        }
        return rdsConfigResponses;
    }

    private KerberosConfig prepareKerberosConfig(String environment, KerberosV4Request kerberosRequest) {
        KerberosConfig kerberosConfig = conversionService.convert(kerberosRequest, KerberosConfig.class);
        kerberosConfig.setName(String.format("%s-%s-%s", environment, kerberosRequest.getName(), getHash()));
        return kerberosConfig;
    }

    private String getHash() {
        return UUID.randomUUID().toString().split("-")[0];
    }

    private KerberosV4Response prepareKerberosResponse(KerberosConfig kerberosConfig) {
        return conversionService.convert(kerberosConfig, KerberosV4Response.class);
    }
}
